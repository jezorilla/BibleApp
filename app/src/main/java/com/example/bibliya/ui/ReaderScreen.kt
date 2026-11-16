package com.example.bibliya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bibliya.AppState
import com.example.bibliya.BibleViewModel
import com.example.bibliya.BibleRegistry
import com.example.bibliya.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(state: AppState, vm: BibleViewModel, onOpenPicker: () -> Unit) {
    val settings = state.settings
    val book = state.edition?.book(state.book)
    val verses = book?.verses(state.chapter).orEmpty()
    val scheme = MaterialTheme.colorScheme
    val background = chapterBackground(settings)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // On location jumps (verse selection / search / prev-next), scroll + flash the target.
    LaunchedEffect(state.book, state.chapter, state.targetVerse) {
        val index = (state.targetVerse - 1).coerceIn(0, verses.lastIndex)
        if (verses.isNotEmpty()) listState.scrollToItem(index)
    }
    LaunchedEffect(state.targetVerse) {
        if (state.targetVerse > 1) {
            delay(1400)
            vm.clearTargetVerse()
        }
    }

    Column(Modifier.fillMaxSize().background(background).systemBarsPadding()) {
        ReaderTopBar(state, vm, onOpenPicker)
        Box(
            Modifier
                .weight(1f)
                .swipeChapter(onLeft = vm::nextChapter, onRight = vm::previousChapter)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(verses, key = { i, _ -> state.book * 10000 + state.chapter * 1000 + i }) { i, verse ->
                    VerseRow(
                        verse = verse,
                        number = i + 1,
                        settings = settings,
                        scheme = scheme,
                        background = background,
                        highlighted = i + 1 == state.targetVerse,
                    )
                }
            }
        }
        ChapterNavBar(state, vm, scheme)
    }

    if (state.settingsOpen) {
        ModalBottomSheet(onDismissRequest = vm::closeSettingsSheet) {
            SettingsSheetContent(vm)
        }
    }
    if (state.versionDialogOpen) {
        VersionDialog(state, vm)
    }
}
private fun Modifier.swipeChapter(onLeft: () -> Unit, onRight: () -> Unit): Modifier = composed {
    val density = LocalDensity.current
    val threshold = with(density) { 120.dp.toPx() }
    pointerInput(onLeft, onRight) {
        var totalX = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalX = 0f },
            onDragEnd = {
                when {
                    totalX > threshold -> onLeft()
                    totalX < -threshold -> onRight()
                }
            },
            onHorizontalDrag = { change, amount ->
                change.consume()
                totalX += amount
            }
        )
    }
}

@Composable
private fun ReaderTopBar(state: AppState, vm: BibleViewModel, onOpenPicker: () -> Unit) {
    val settings = state.settings
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val shortLabel = BibleRegistry.versions.firstOrNull { it.id == state.currentVersionId }?.shortLabel
        ?: state.currentVersionId
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::openSettingsSheet, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${vm.currentBookName()} ${state.chapter}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .clickable(onClick = onOpenPicker)
                        .padding(end = 6.dp),
                )
                Icon(
                    painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        TextButton(
            onClick = { vm.setVersionDialogOpen(true) },
            modifier = Modifier.height(56.dp).padding(start = 4.dp),
        ) {
            Text(shortLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

@Composable
private fun ChapterNavBar(state: AppState, vm: BibleViewModel, scheme: androidx.compose.material3.ColorScheme) {
    Row(Modifier.fillMaxWidth().background(scheme.surface)) {
        val prevLabel = stringResource(R.string.previous)
        val nextLabel = stringResource(R.string.next)
        Button(
            onClick = vm::previousChapter,
            modifier = Modifier.weight(1f).height(72.dp),
            shape = RoundedCornerShape(0),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.surface, contentColor = scheme.onSurface),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(6.dp))
                Text(prevLabel, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = vm::nextChapter,
            modifier = Modifier.weight(1f).height(72.dp),
            shape = RoundedCornerShape(0),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.surface, contentColor = scheme.onSurface),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nextLabel, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/** One verse: small superscript number + text. Background flashes when highlighted. */
@Composable
private fun VerseRow(
    verse: String,
    number: Int,
    settings: Settings,
    scheme: androidx.compose.material3.ColorScheme,
    background: Color,
    highlighted: Boolean,
) {
    val fs = settings.fontSize
    val rowBackground = if (highlighted) scheme.secondaryContainer.copy(alpha = 0.45f) else background
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .padding(horizontal = 12.dp)
            .padding(top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val numColor = scheme.onBackground.copy(alpha = 0.55f)
        val verseNumberShift = with(LocalDensity.current) { -(fs * 0.20f).dp.roundToPx() }
        Box(
            Modifier
                .width(with(LocalDensity.current) { (fs * 0.75f * 2.4f).dp })
        ) {
            Text(
                "$number",
                fontSize = (fs * 0.68f).sp,
                lineHeight = (fs * 0.72f).sp,
                fontWeight = FontWeight.Medium,
                color = numColor,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(0, verseNumberShift) },
            )
        }
        Text(
            verse,
            fontSize = fs.sp,
            lineHeight = (fs * settings.lineSpacing.factor).sp,
            color = scheme.onBackground.copy(alpha = 0.93f),
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun VersionDialog(state: AppState, vm: BibleViewModel) {
    if (state.edition == null) return
    AlertDialog(
        onDismissRequest = { vm.setVersionDialogOpen(false) },
        title = { Text(stringResource(R.string.pick_verse_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                BibleRegistry.versions.forEach { v ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.setVersionDialogOpen(false)
                                vm.setVersion(v.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.currentVersionId == v.id,
                            onClick = {
                                vm.setVersionDialogOpen(false)
                                vm.setVersion(v.id)
                            },
                        )
                        Text(v.displayName, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.setVersionDialogOpen(false) }) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
