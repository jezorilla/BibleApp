package com.example.bibliya.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

enum class AppTheme { LIGHT, DARK, SEPIA }

enum class LineSpacing(val factor: Float) {
    NORMAL(1.3f),
    RELAXED(1.75f)
}

data class Settings(
    val theme: AppTheme = AppTheme.LIGHT,
    val accent: AccentColor = AccentColor.BLUE,
    val fontSize: Float = 22f,           // sp, range 16..40
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val gridView: Boolean = false,       // picker list/grid mode
)

enum class AccentColor(val seed: Color) {
    BLUE(Color(0xFF3578E5)),
    TEAL(Color(0xFF0F9D8C)),
    GREEN(Color(0xFF258933)),
    ORANGE(Color(0xFFDD7B29)),
    RED(Color(0xFFD64541)),
    PURPLE(Color(0xFF7B52C0));

    fun tonalScheme(dark: Boolean) = if (dark) darkScheme() else lightScheme()
    fun lightScheme() = androidx.compose.material3.lightColorScheme(seed = seed)
}

private fun darkSchemeOf(seed: Color) =
    androidx.compose.material3.darkColorScheme(seed = seed)

@Suppress("UNUSED_PARAMETER") // dark computed from theme, not system
private fun AccentColor.darkScheme() = darkSchemeOf(seed)

@Composable
fun BibliyaTheme(
    settings: Settings,
    content: @Composable () -> Unit
) {
    val dark = settings.theme == AppTheme.DARK
    val sepia = settings.theme == AppTheme.SEPIA
    val scheme = settings.accent.tonalScheme(dark).run {
        if (!sepia) this
        else copy(
            background = Color(0xFFFBF1DC),
            onBackground = Color(0xFF3B3228),
            surface = Color(0xFFF3E7CB),
            onSurface = Color(0xFF241E16),
            onSurfaceVariant = Color(0xFF6B5E4B),
            outline = Color(0xFFAF9C7E),
            outlineVariant = Color(0xFFD6C6A8),
        )
    }
    val background = when {
        dark -> Color(0xFF18181B)
        sepia -> Color(0xFFFBF1DC)
        else -> Color(0xFFFCFCFA)
    }

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { win ->
            win.statusBarColor = scheme.background.toArgb()
            win.navigationBarColor = win.statusBarColor
        }
    }

    androidx.compose.material3.MaterialTheme(
        colorScheme = scheme,
        content = { androidx.compose.material3.MaterialTheme(content = content) }
    )
}

/** Current background tint (chapter background is kept light for readability). */
@Composable
fun chapterBackground(settings: Settings): Color = when (settings.theme) {
    AppTheme.DARK -> Color(0xFF18181B)
    AppTheme.SEPIA -> Color(0xFFFDF6E7)
    AppTheme.LIGHT -> Color(0xFFFCFCFA)
}
