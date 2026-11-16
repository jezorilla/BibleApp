package com.example.bibliya

import android.content.Context
import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One Bible edition. To add a version later: drop an XML in assets + add one entry here. */
data class BibleVersion(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val assetPath: String,
)

data class Book(
    val number: Int,
    val name: String,
    val chapters: Map<Int, List<String>>,  // chapter number -> verses (1-based index in list)
) {
    val chapterCount: Int get() = chapters.keys.maxOrNull() ?: 0
    fun verses(chapter: Int): List<String> = chapters[chapter].orEmpty()
    fun verseCount(chapter: Int): Int = chapters[chapter]?.size ?: 0
}

class BibleEdition(
    val version: BibleVersion,
    val books: List<Book>,
) {
    val bookCount: Int get() = books.size
    fun book(number: Int): Book? = books.firstOrNull { it.number == number }
}

object BibleRegistry {
    val versions: List<BibleVersion> = listOf(
        BibleVersion(
            id = "tagmb",
            displayName = "Magandang Balita Bible (Tagalog)",
            shortLabel = "TAGMB",
            assetPath = "bibles/TAGMB_12.xml"
        )
    )
}

/** Strips footnote markers like [1] and circled letters ⓐ ⓑ from verse text. */
object VerseCleaner {
    private val FOOTNOTE = Regex("\\[\\d+]")
    private val CIRCLED = Regex("[\\u24B6-\\u24E9]")
    private val SPACES = Regex("\\s+")

    fun clean(text: String): String = SPACES
        .replace(CIRCLED.replace(FOOTNOTE.replace(text, " "), " "), " ")
        .trim()
}

/** Accent-insensitive matching: fold case and remove diacritics. */
object SearchUtil {
    private val COMBINING = Regex("\\p{M}+")
    private val SPACES = Regex("\\s+")

    fun fold(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        return SPACES.replace(COMBINING.replace(n, ""), " ")
            .lowercase(Locale.ROOT).trim()
    }
}
data class SearchHit(
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val verseText: String,
    val matchIndexInFolded: Int,
)

private class BookBuilder(val name: String) {
    val chapters = LinkedHashMap<Int, MutableList<String>>()
}

class BibleRepository {
    /** Parse once (on Dispatchers.Default) and keep in memory.
     *  First launch parses the XML; afterwards a JSON cache makes startup fast. */
    suspend fun load(context: Context, version: BibleVersion): BibleEdition =
        withContext(Dispatchers.Default) {
            readCache(context, version)?.let { return@withContext it }
            val edition = parseAsset(context, version)
            writeCache(context, version, edition)
            edition
        }

    /** Verse-text search across the whole edition. Call on Dispatchers.Default. */
    suspend fun searchVerses(edition: BibleEdition, query: String, maxResults: Int = 120): List<SearchHit> =
        withContext(Dispatchers.Default) {
            val q = SearchUtil.fold(query)
            if (q.isEmpty()) return@withContext emptyList()
            val hits = ArrayList<SearchHit>(maxResults)
            for (book in edition.books) {
                for ((chapter, verses) in book.chapters) {
                    for ((i, verse) in verses.withIndex()) {
                        val folded = SearchUtil.fold(verse)
                        val idx = folded.indexOf(q)
                        if (idx >= 0) {
                            hits.add(SearchHit(book.number, book.name, chapter, i + 1, verse, idx))
                            if (hits.size >= maxResults) return@withContext hits
                        }
                    }
                }
            }
            hits
        }

    /** Interpret "Juan 3:16" / "Juan 3" as a book/chapter(/verse) reference. */
    fun resolveReference(edition: BibleEdition, query: String): Triple<Int, Int, Int?>? {
        val m = Regex("^(.*?)\\s+(\\d+)(?::(\\d+))?$").find(query.trim()) ?: return null
        val prefix = SearchUtil.fold(m.groupValues[1])
        if (prefix.isEmpty()) return null
        val chapter = m.groupValues[2].toInt()
        val verse = m.groupValues[3].toIntOrNull()
        edition.books.firstOrNull { SearchUtil.fold(it.name).startsWith(prefix) }?.let {
            return Triple(it.number, chapter, verse)
        }
        return null
    }

    /** Book-name search (accent/case-insensitive). */
    fun searchBooks(edition: BibleEdition, query: String): List<Book> {
        val q = SearchUtil.fold(query)
        if (q.isEmpty()) return emptyList()
        return edition.books.filter { SearchUtil.fold(it.name).contains(q) }
    }


    private fun parseAsset(context: Context, version: BibleVersion): BibleEdition {
        val raw = context.assets.open(version.assetPath).readBytes()
        val body = if (raw.size >= 3 && raw[0] == 0xEF.toByte() && raw[1] == 0xBB.toByte() && raw[2] == 0xBF.toByte()) {
            raw.copyOfRange(3, raw.size) // strip UTF-8 BOM
        } else raw
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(body), null)
        val builders = LinkedHashMap<Int, BookBuilder>()
        var currentBook: BookBuilder? = null
        var currentChapter = -1
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                when (parser.name) {
                    "BIBLEBOOK" -> {
                        val num = parser.getAttributeValue(null, "bnumber")?.toIntOrNull() ?: -1
                        if (num >= 0) {
                            currentBook = BookBuilder(parser.getAttributeValue(null, "bname") ?: "")
                            builders[num] = currentBook
                        }
                    }
                    "CHAPTER" -> {
                        currentChapter = parser.getAttributeValue(null, "cnumber")?.toIntOrNull() ?: -1
                    }
                    "VERS" -> {
                        val text = parser.nextText()
                        if (currentBook != null && currentChapter > 0) {
                            currentBook!!.chapters.getOrPut(currentChapter) { mutableListOf() }
                                .add(VerseCleaner.clean(text ?: ""))
                        }
                    }
                }
            }
            event = parser.next()
        }
        val books = builders.map { (num, b) ->
            Book(num, b.name, b.chapters.mapValues { it.value.toList() })
        }
        return BibleEdition(version, books)
    }

    private fun readCache(context: Context, version: BibleVersion): BibleEdition? {
        return try {
        val f = cacheFile(context, version)
        if (!f.exists() || f.length() < 1000) return null
        val root = JSONObject(f.readText(Charsets.UTF_8))
        if (root.getString("id") != version.id) return null
        val books = ArrayList<Book>(70)
        val arr = root.getJSONArray("books")
        for (i in 0 until arr.length()) {
            val b = arr.getJSONObject(i)
            val chapters = LinkedHashMap<Int, List<String>>()
            val ca = b.getJSONArray("chapters")
            for (j in 0 until ca.length()) {
                val c = ca.getJSONObject(j)
                val ja = JSONArray(c.getString("v"))
                val list = ArrayList<String>(ja.length())
                for (k in 0 until ja.length()) list.add(ja.getString(k))
                chapters[c.getInt("n")] = list
            }
            books.add(Book(b.getInt("n"), b.getString("name"), chapters))
        }
        BibleEdition(version, books)
    } catch (e: Exception) {
        null
    }
    }

    private fun writeCache(context: Context, version: BibleVersion, edition: BibleEdition) {
        try {
            val f = cacheFile(context, version)
            val root = JSONObject()
            root.put("id", version.id)
            val books = JSONArray()
            for (b in edition.books) {
                val bo = JSONObject()
                bo.put("n", b.number)
                bo.put("name", b.name)
                val ca = JSONArray()
                for ((cn, verses) in b.chapters) {
                    val ja = JSONArray()
                    for (v in verses) ja.put(v)
                    val co = JSONObject()
                    co.put("n", cn)
                    co.put("v", ja)
                    ca.put(co)
                }
                bo.put("chapters", ca)
                books.put(bo)
            }
            root.put("books", books)
            f.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            // best-effort cache
        }
    }

    private fun cacheFile(context: Context, version: BibleVersion): File =
        File(context.cacheDir, "bible_${version.id}.json")
}

