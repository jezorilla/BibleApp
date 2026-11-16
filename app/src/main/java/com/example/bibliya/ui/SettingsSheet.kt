package com.example.bibliya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bibliya.BibleViewModel
import com.example.bibliya.R

/** Theme row: preview swatch background + text colors. */
private data class ThemeOption(
    val theme: AppTheme,
    val bg: Color,
    val fg: Color,
    val label: String
)

@Composable
fun SettingsSheetContent(vm: BibleViewModel) {
    val settings = vm.state.settings
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .verticalScroll(rememberScrollState())
            .padding(20.dp, 24.dp, 20.dp, 12.dp)
    ) {
        Text(stringResource(R.string.display), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        ThemeChooser(settings, vm)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.accent_color), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        var selectedAccent by remember { mutableStateOf(settings.accent) }
        Row(Modifier.align(Alignment.Start), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (color in AccentColor.entries) {
                val boxColor = color.seed
                val selected = selectedAccent == color
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(boxColor)
                        .clickable { selectedAccent = color; vm.setAccent(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == AccentColor.RED || color == AccentColor.BLUE) Color.White else Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        FontSizeChooser(settings, vm)
        Spacer(Modifier.height(12.dp))
        LineSpacingChooser(settings, vm)
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.saved_note),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ThemeChooser(settings: Settings, vm: BibleViewModel) {
    val entries = listOf(
        ThemeOption(AppTheme.LIGHT, Color(0xFFF7F4E9), Color(0xFF1F1B16), "Light"),
        ThemeOption(AppTheme.DARK, Color(0xFF232125), Color(0xFFF2EEF4), "Dark"),
        ThemeOption(AppTheme.SEPIA, Color(0xFFF7E9C7), Color(0xFF3B3228), "Sepia"),
    )
    for ((idx, entry) in entries.withIndex()) {
        val (theme, bg, fg, label) = entry
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { vm.setTheme(theme) }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp, 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .padding(6.dp, 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(label, color = fg, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.weight(1f))
            if (settings.theme == theme) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun FontSizeChooser(settings: Settings, vm: BibleViewModel) {
    var sliderValue by remember { mutableStateOf(settings.fontSize) }
    Text(stringResource(R.string.font_size), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = { sliderValue = (sliderValue - 2f).coerceIn(16f, 40f); vm.setFontSize(sliderValue) },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Default.Clear, contentDescription = "A-", modifier = Modifier.size(22.dp))
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                vm.setFontSize(it)
            },
            valueRange = 16f..40f,
            steps = (40 - 16).toInt(),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { sliderValue = (sliderValue + 2f).coerceIn(16f, 40f); vm.setFontSize(sliderValue) },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "A+", modifier = Modifier.size(22.dp))
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.preview_text),
        fontSize = sliderValue.sp,
        lineHeight = (sliderValue * settings.lineSpacing.factor).sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(16.dp),
    )
}

@Composable
private fun LineSpacingChooser(settings: Settings, vm: BibleViewModel) {
    Text(stringResource(R.string.line_spacing), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((labelRes, spacing) in listOf(
            R.string.normal_spacing to LineSpacing.NORMAL,
            R.string.relaxed_spacing to LineSpacing.RELAXED,
        )) {
            val selected = settings.lineSpacing == spacing
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { vm.setLineSpacing(spacing) }
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(labelRes), fontSize = 17.sp, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
