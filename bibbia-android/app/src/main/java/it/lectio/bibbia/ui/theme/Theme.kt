package it.lectio.bibbia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import it.lectio.bibbia.data.repository.ThemeMode
import it.lectio.bibbia.domain.model.HighlightColor

/**
 * Palette "carta e inchiostro". Il colore d'accento è un rosso rubrica smorzato, come nei
 * breviari: lo si usa con parsimonia (numeri di capitolo e di versetto, segnalibri).
 */
@Immutable
data class BibbiaColors(
    val paper: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val rubric: Color,
    val hairline: Color,
    val selection: Color,
    val surfaceRaised: Color,
    val highlights: Map<HighlightColor, Color>,
    val isDark: Boolean,
)

val LightPaper = BibbiaColors(
    paper = Color(0xFFFBF8F1),
    ink = Color(0xFF1E1B17),
    inkMuted = Color(0xFF6B6357),
    inkFaint = Color(0xFFA39A8C),
    rubric = Color(0xFF8B3A2F),
    hairline = Color(0xFFE4DCCC),
    selection = Color(0xFFEDE5D5),
    surfaceRaised = Color(0xFFF5F0E5),
    highlights = mapOf(
        HighlightColor.OCHRE to Color(0xFFF2E3B3),
        HighlightColor.SAGE to Color(0xFFDDE6D1),
        HighlightColor.SKY to Color(0xFFD9E3EC),
        HighlightColor.ROSE to Color(0xFFEFDAD5),
    ),
    isDark = false,
)

val SepiaPaper = BibbiaColors(
    paper = Color(0xFFF2E7D0),
    ink = Color(0xFF3A2E21),
    inkMuted = Color(0xFF75644F),
    inkFaint = Color(0xFFAA9A80),
    rubric = Color(0xFF8E3B2A),
    hairline = Color(0xFFDDCDAE),
    selection = Color(0xFFE6D7B9),
    surfaceRaised = Color(0xFFEDE0C5),
    highlights = mapOf(
        HighlightColor.OCHRE to Color(0xFFEBD48F),
        HighlightColor.SAGE to Color(0xFFD3DBB8),
        HighlightColor.SKY to Color(0xFFCFD8D6),
        HighlightColor.ROSE to Color(0xFFE6C9B8),
    ),
    isDark = false,
)

/** Notte: non nero puro ma un carbone caldo, con testo color pergamena e contrasto moderato. */
val NightPaper = BibbiaColors(
    paper = Color(0xFF1B1916),
    ink = Color(0xFFE3DACB),
    inkMuted = Color(0xFFA59C8D),
    inkFaint = Color(0xFF6E665A),
    rubric = Color(0xFFCF9580),
    hairline = Color(0xFF34302A),
    selection = Color(0xFF2E2A24),
    surfaceRaised = Color(0xFF24211D),
    highlights = mapOf(
        HighlightColor.OCHRE to Color(0xFF4A4026),
        HighlightColor.SAGE to Color(0xFF34402F),
        HighlightColor.SKY to Color(0xFF2E3A45),
        HighlightColor.ROSE to Color(0xFF4A3330),
    ),
    isDark = true,
)

val LocalBibbiaColors = staticCompositionLocalOf { LightPaper }

private fun BibbiaColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = rubric,
        onPrimary = paper,
        primaryContainer = selection,
        onPrimaryContainer = ink,
        secondary = inkMuted,
        onSecondary = paper,
        secondaryContainer = selection,
        onSecondaryContainer = ink,
        tertiary = rubric,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = paper,
        surfaceContainerLow = paper,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = selection,
        surfaceTint = Color.Transparent,
        inverseSurface = ink,
        inverseOnSurface = paper,
        inversePrimary = rubric,
        outline = inkFaint,
        outlineVariant = hairline,
        error = rubric,
        onError = paper,
        scrim = Color.Black.copy(alpha = 0.32f),
    )
}

object BibbiaTheme {
    val colors: BibbiaColors
        @Composable get() = LocalBibbiaColors.current
}

@Composable
fun resolveColors(mode: ThemeMode): BibbiaColors = when (mode) {
    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) NightPaper else LightPaper
    ThemeMode.LIGHT -> LightPaper
    ThemeMode.SEPIA -> SepiaPaper
    ThemeMode.DARK -> NightPaper
}

@Composable
fun BibbiaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colors = resolveColors(mode)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !colors.isDark
            controller.isAppearanceLightNavigationBars = !colors.isDark
        }
    }
    CompositionLocalProvider(LocalBibbiaColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = BibbiaTypography,
            content = content,
        )
    }
}
