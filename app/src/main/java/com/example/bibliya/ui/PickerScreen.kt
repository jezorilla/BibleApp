package com.example.bibliya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bibliya.AppState
import com.example.bibliya.BibleEdition
import com.example.bibliya.BibleViewModel
import com.example.bibliya.Book
import com.example.bibliya.PickerStep
import com.example.bibliya.R
import com.example.bibliya.SearchHit
import com.example.bibliya.TestamentFilter

@Composable
fun PickerScreen(state: AppState, vm: BibleViewModel) {
    val edition = state.edition ?: return
    Scaffold(
        topBar = { TopControls(state, vm) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
                .padding(top = inner.calculateTopPadding()),
        ) {
            BreadcrumbRow(state, vm)
            Box(Modifier.weight(1f)) {
                when {
                    state.picker.query.isNotBlank() -> SearchResultsArea(state, vm, edition)
                    state.picker.step == PickerStep.BOOKS -> BookList(state, vm, edition)
                    state.picker.step == PickerStep.CHAPTERS -> ChapterGrid(state, vm)
                    state.picker.step == PickerStep.VERSES -> VerseGrid(state, vm)
                }
            }
        }
    }
}

@Composable
private fun TopControls(state: AppState, vm: BibleViewModel) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(12.dp, 10.dp, 12.dp, 8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val cats = listOf(
                TestamentFilter.ALL to R.string.filter_all,
                TestamentFilter.OLD to R.string.filter_ot,
                TestamentFilter.NEW to R.string.filter_nt,
            )
            for ((filter, label) in cats) {
                val sel = state.picker.filter == filter
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { vm.setPickerFilter(filter) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(label),
                        fontSize = 16.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val focusManager = LocalFocusManager.current
            TextField(
                value = state.picker.query,
                onValueChange = vm::setSearchQuery,
                placeholder = { Text(stringResource(R.string.search_hint), fontSize = 16.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                trailingIcon = {
                    if (state.picker.busy) CircularProgressIndicator(Modifier.size(18.dp)) else null
                },
            )
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.IconToggleButton(
                checked = state.settings.gridView,
                onCheckedChange = { vm.toggleGridView() },
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(if (state.settings.gridView) R.drawable.ic_list else R.drawable.ic_grid),
                    contentDescription = stringResource(if (state.settings.gridView) R.string.view_list else R.string.view_grid),
                    modifier = Modifier.size(28.dp),
                    tint = accent,
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbRow(state: AppState, vm: BibleViewModel) {
    val step = state.picker.step
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val labels = listOf(R.string.book_step, R.string.chapter_step, R.string.verse_step)
        for ((idx, label) in labels.withIndex()) {
            val cur = PickerStep.entries[idx] == step
            if (idx > 0) {
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
            Text(
                stringResource(label),
                fontSize = 16.sp,
                fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal,
                color = if (cur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { vm.setPickerStep(PickerStep.entries[idx]) }.padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
// Old Testament = book numbers 1-39, New Testament = 40 and up.
private fun Book.matches(filter: TestamentFilter): Boolean = when (filter) {
    TestamentFilter.ALL -> true
    TestamentFilter.OLD -> number in 1..39
    TestamentFilter.NEW -> number > 39
}

@Composable
private fun BookList(state: AppState, vm: BibleViewModel, edition: BibleEdition) {
    val books = remember(edition, state.picker.filter) {
        edition.books.filter { it.matches(state.picker.filter) }
    }
    if (state.settings.gridView) {
        BookGrid(books, vm)
    } else {
        BookRows(books, vm)
    }
}

@Composable
private fun BookGrid(books: List<Book>, vm: BibleViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(books, key = { it.number }) { book ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { vm.setLocationState(book.number, 1, 1, PickerStep.CHAPTERS) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("${book.number}. ${book.name}", fontSize = 15.sp, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun BookRows(books: List<Book>, vm: BibleViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(books, key = { it.number }) { book ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.setLocationState(book.number, 1, 1, PickerStep.CHAPTERS) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${book.number}.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(36.dp),
                )
                Text(book.name, fontSize = 17.sp)
            }
        }
    }
}
@Composable
private fun ChapterGrid(state: AppState, vm: BibleViewModel) {
    val book = state.edition?.book(state.book) ?: return
    LazyVerticalGrid(
        columns = GridCells.Adaptive(64.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(book.chapterCount, key = { it }) { c ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { vm.setLocationState(state.book, c, 1, PickerStep.VERSES) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$c", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VerseGrid(state: AppState, vm: BibleViewModel) {
    val book = state.edition?.book(state.book) ?: return
    val count = book.verseCount(state.chapter)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(46.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(count, key = { it }) { v ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { vm.setLocation(state.book, state.chapter, v); vm.closePicker() }
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$v", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SearchResultsArea(state: AppState, vm: BibleViewModel, edition: BibleEdition) {
    val results = state.picker.results
    if (results.isEmpty()) {
        val books = vm.findBookMatches(state.picker.query)
        when {
            books.isNotEmpty() -> BookChips(books, vm)
            state.picker.busy -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            else -> EmptyResults()
        }
        return
    }
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(
            results,
            key = { it.bookNumber * 1_000_000 + it.chapter * 1_000 + it.verse },
        ) { hit ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.selectSearchHit(hit) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${hit.bookName} ${hit.chapter}:${hit.verse}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.primary,
                    )
                    Text(
                        hit.verseText,
                        fontSize = 15.sp,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Book-name matches shown as a tappable list while a search has no verse hits yet. */
@Composable
private fun BookChips(books: List<Book>, vm: BibleViewModel) {
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(books, key = { it.number }) { book ->
            Text(
                book.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setLocationState(book.number, 1, 1, PickerStep.CHAPTERS) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun EmptyResults() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.no_results),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}