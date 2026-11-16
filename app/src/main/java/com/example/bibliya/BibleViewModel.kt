package com.example.bibliya

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.example.bibliya.ui.AccentColor
import com.example.bibliya.ui.AppTheme
import com.example.bibliya.ui.LineSpacing
import com.example.bibliya.ui.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PickerStep { BOOKS, CHAPTERS, VERSES }
enum class TestamentFilter { ALL, OLD, NEW }

class PickerState(
    val step: PickerStep = PickerStep.BOOKS,
    val filter: TestamentFilter = TestamentFilter.ALL,
    val query: String = "",
    val results: List<SearchHit> = emptyList(),
    val busy: Boolean = false,
)

/** Single source of truth for every screen. All changes go through the ViewModel. */
class AppState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentVersionId: String = "",
    val edition: BibleEdition? = null,
    val book: Int = 1,
    val chapter: Int = 1,
    val targetVerse: Int = 1,
    val settings: Settings = Settings(),
    val picker: PickerState = PickerState(),
    val pickerOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val versionDialogOpen: Boolean = false,
)

class BibleViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)
    private val repo = BibleRepository()
    private val editions = HashMap<String, BibleEdition>()

    var state by mutableStateOf(AppState())
        private set

    private var searchJob: Job? = null

    init {
        val first = BibleRegistry.versions.first().id
        val versionId = prefs.getString(PREF_VERSION, first) ?: first
        state = state.copy(settings = loadSettings())
        viewModelScope.launch {
            loadVersion(versionId, restoreLocation = true)
        }
    }

    private suspend fun loadVersion(versionId: String, restoreLocation: Boolean) {
        state = state.copy(isLoading = true, error = null)
        try {
            val edition = editions.getOrPut(versionId) {
                repo.load(getApplication(), BibleRegistry.versions.first { it.id == versionId })
            }
            val (book, chapter) = if (restoreLocation) readSavedLocation(edition) else 1 to 1
            withContext(Dispatchers.Main) {
                state = state.copy(
                    isLoading = false,
                    edition = edition,
                    currentVersionId = versionId,
                    book = book,
                    chapter = chapter,
                    picker = state.picker.copy(results = emptyList(), busy = false),
                )
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                state = state.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    private fun readSavedLocation(edition: BibleEdition): Pair<Int, Int> {
        val book = prefs.getInt(PREF_BOOK, 1).coerceIn(1, edition.bookCount)
        val b = edition.book(book) ?: return 1 to 1
        val chapter = prefs.getInt(PREF_CHAPTER, 1).coerceIn(1, b.chapterCount)
        return book to chapter
    }

    fun retry() {
        viewModelScope.launch {
            loadVersion(state.currentVersionId, restoreLocation = true)
        }
    }

    // ---------- location ----------

    fun setLocation(book: Int, chapter: Int, targetVerse: Int = 1) {
        val ed = edition() ?: return
        val b = book.coerceIn(1, ed.bookCount)
        val bookObj = ed.book(b) ?: return
        val c = chapter.coerceIn(1, bookObj.chapterCount)
        state = state.copy(
            book = b,
            chapter = c,
            targetVerse = targetVerse.coerceIn(1, bookObj.verseCount(c)),
        )
        persistLocation()
    }

    /** Goes to the previous chapter, crossing book boundaries automatically. */
    fun previousChapter() {
        val ed = edition() ?: return
        if (state.chapter > 1) {
            setLocation(state.book, state.chapter - 1, 1)
        } else {
            ed.book(state.book - 1)?.let { prev ->
                setLocation(state.book - 1, prev.chapterCount.coerceAtLeast(1), 1)
            }
        }
    }

    /** Goes to the next chapter, crossing book boundaries automatically. */
    fun nextChapter() {
        val ed = edition() ?: return
        val cur = ed.book(state.book) ?: return
        if (state.chapter < cur.chapterCount) {
            setLocation(state.book, state.chapter + 1, 1)
        } else {
            ed.book(state.book + 1)?.let { next ->
                setLocation(state.book + 1, 1, 1)
            }
        }
    }

    fun currentBookName(): String = edition()?.book(state.book)?.name ?: ""

    /** Clears the "highlight this verse" effect after the reader displayed it. */
    fun clearTargetVerse() {
        if (state.targetVerse > 0) {
            state = state.copy(targetVerse = 1)
        }
    }

    /** Switch edition, keeping the book/chapter if it still exists in the new one. */
    fun setVersion(versionId: String) {
        if (versionId == state.currentVersionId) return
        state = state.copy(currentVersionId = versionId, isLoading = true)
        viewModelScope.launch {
            val edition = editions.getOrPut(versionId) {
                repo.load(getApplication(), BibleRegistry.versions.first { it.id == versionId })
            }
            val b = state.book.coerceIn(1, edition.bookCount)
            val bookObj = edition.book(b) ?: return@launch
            val c = state.chapter.coerceIn(1, bookObj.chapterCount)
            withContext(Dispatchers.Main) {
                state = state.copy(
                    edition = edition,
                    isLoading = false,
                    book = b,
                    chapter = c,
                    targetVerse = 1,
                    picker = state.picker.copy(results = emptyList(), busy = false),
                )
            }
        }
    }

    private fun edition(): BibleEdition? = editions[state.currentVersionId] ?: state.edition

    private fun persistLocation() {
        prefs.edit()
            .putInt(PREF_BOOK, state.book)
            .putInt(PREF_CHAPTER, state.chapter)
            .putInt(PREF_VERSE, state.targetVerse)
            .putString(PREF_VERSION, state.currentVersionId)
            .apply()
    }

    // ---------- picker & search ----------

    fun openPicker() {
        if (state.edition != null) state = state.copy(pickerOpen = true)
    }

    fun closePicker() {
        state = state.copy(pickerOpen = false)
    }

    fun setPickerStep(step: PickerStep) {
        state = state.copy(picker = state.picker.copy(step = step, results = emptyList()))
    }

    fun setPickerFilter(filter: TestamentFilter) {
        state = state.copy(picker = state.picker.copy(filter = filter, step = PickerStep.BOOKS))
    }

    fun setSearchQuery(query: String) {
        state = state.copy(picker = state.picker.copy(query = query, results = emptyList()))
        searchJob?.cancel()
        if (query.isBlank()) return
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(300) // debounce
            val q = state.picker.query
            if (q.isBlank()) return@launch
            val ed = edition() ?: return@launch
            withContext(Dispatchers.Main) {
                state = state.copy(picker = state.picker.copy(busy = true))
            }
            val ref = repo.resolveReference(ed, q)
            if (ref != null) {
                val (b, c, v) = ref
                val bookObj = ed.book(b)
                if (bookObj != null) {
                    val vv = if (v != null && v <= bookObj.verseCount(c.coerceIn(1, bookObj.chapterCount))) {
                        v
                    } else 1
                    withContext(Dispatchers.Main) {
                        setLocation(b, c.coerceIn(1, bookObj.chapterCount), vv)
                        closePicker()
                        state = state.copy(picker = state.picker.copy(busy = false))
                    }
                } else withContext(Dispatchers.Main) {
                    state = state.copy(picker = state.picker.copy(busy = false, step = PickerStep.BOOKS))
                }
                return@launch
            }
            val books = repo.searchBooks(ed, q)
            if (books.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    state = state.copy(picker = state.picker.copy(busy = false, results = emptyList()))
                }
                // show as book chips; handled by screen based on query
                return@launch
            }
            val results = repo.searchVerses(ed, q)
            withContext(Dispatchers.Main) {
                state = state.copy(
                    picker = state.picker.copy(busy = false, results = results)
                )
            }
        }
    }

    fun selectSearchHit(hit: SearchHit) {
        setLocation(hit.bookNumber, hit.chapter, hit.verse)
        closePicker()
    }

    // ---------- settings ----------

    fun setTheme(theme: AppTheme) {
        state = state.copy(settings = state.settings.copy(theme = theme))
        prefs.edit().putInt(PREF_THEME, theme.ordinal).apply()
    }

    fun setAccent(accent: AccentColor) {
        state = state.copy(settings = state.settings.copy(accent = accent))
        prefs.edit().putInt(PREF_ACCENT, accent.ordinal).apply()
    }

    fun setFontSize(size: Float) {
        val v = size.coerceIn(16f, 40f)
        state = state.copy(settings = state.settings.copy(fontSize = v))
        prefs.edit().putFloat(PREF_FONT, v).apply()
    }

    /** "+/-" buttons step by 2 sp. */
    fun adjustFontSize(delta: Float) = setFontSize(state.settings.fontSize + delta)

    fun setLineSpacing(spacing: LineSpacing) {
        state = state.copy(settings = state.settings.copy(lineSpacing = spacing))
        prefs.edit().putInt(PREF_SPACING, spacing.ordinal).apply()
    }

    fun toggleGridView() {
        state = state.copy(settings = state.settings.copy(gridView = !state.settings.gridView))
        prefs.edit().putBoolean(PREF_GRID, state.settings.gridView).apply()
    }

    private fun loadSettings(): Settings {
        val theme = AppTheme.entries[prefs.getInt(PREF_THEME, 0).coerceIn(0, AppTheme.entries.size - 1)]
        val accent = AccentColor.entries[prefs.getInt(PREF_ACCENT, 0).coerceIn(0, AccentColor.entries.size - 1)]
        val spacing = LineSpacing.entries[prefs.getInt(PREF_SPACING, 0).coerceIn(0, LineSpacing.entries.size - 1)]
        val font = prefs.getFloat(PREF_FONT, 22f).coerceIn(16f, 40f)
        return Settings(
            theme = theme,
            accent = accent,
            fontSize = font,
            lineSpacing = spacing,
            gridView = prefs.getBoolean(PREF_GRID, false),
        )
    }

    fun openSettingsSheet() {
        state = state.copy(settingsOpen = true)
    }

    fun closeSettingsSheet() {
        state = state.copy(settingsOpen = false)
    }

    fun setVersionDialogOpen(show: Boolean) {
        state = state.copy(versionDialogOpen = show)
    }

    /** In-memory book-name matching used by the picker search UI. */
    fun findBookMatches(query: String): List<Book> {
        edition()?.let { return repo.searchBooks(it, query) }
        return emptyList()
    }

    fun setLocationState(book: Int, chapter: Int, targetVerse: Int = 1, step: PickerStep) {
        setLocation(book, chapter, targetVerse)
        setPickerStep(step)
    }


    // ---------- preference keys ----------

    private companion object {
        const val PREF_VERSION = "pref_version"
        const val PREF_BOOK = "pref_book"
        const val PREF_CHAPTER = "pref_chapter"
        const val PREF_VERSE = "pref_verse"
        const val PREF_THEME = "pref_theme"
        const val PREF_ACCENT = "pref_accent"
        const val PREF_FONT = "pref_font"
        const val PREF_SPACING = "pref_spacing"
        const val PREF_GRID = "pref_grid"
    }
}
