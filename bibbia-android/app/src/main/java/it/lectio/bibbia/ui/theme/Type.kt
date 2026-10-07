package it.lectio.bibbia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.ReaderFont

val Garamond = FontFamily(
    Font(R.font.ebgaramond_regular, FontWeight.Normal),
    Font(R.font.ebgaramond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.ebgaramond_semibold, FontWeight.SemiBold),
)

val Literata = FontFamily(
    Font(R.font.literata_regular, FontWeight.Normal),
    Font(R.font.literata_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.literata_semibold, FontWeight.SemiBold),
)

fun ReaderFont.family(): FontFamily = when (this) {
    ReaderFont.GARAMOND -> Garamond
    ReaderFont.LITERATA -> Literata
    ReaderFont.SYSTEM_SERIF -> FontFamily.Serif
}

/**
 * Il Garamond ha un occhio piccolo: a parità di corpo appare più minuto di Literata.
 * Questo fattore uniforma la dimensione percepita fra i caratteri.
 */
fun ReaderFont.sizeFactor(): Float = when (this) {
    ReaderFont.GARAMOND -> 1.08f
    ReaderFont.LITERATA -> 0.94f
    ReaderFont.SYSTEM_SERIF -> 0.95f
}

/** Tipografia dell'interfaccia: tutta serif, gerarchia per corpo e spaziatura, non per peso. */
val BibbiaTypography = Typography(
    displayLarge = TextStyle(fontFamily = Garamond, fontSize = 64.sp, lineHeight = 68.sp),
    displayMedium = TextStyle(fontFamily = Garamond, fontSize = 48.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Garamond, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Garamond, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Garamond, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Garamond, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Garamond, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Garamond, fontSize = 19.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Garamond, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Garamond, fontSize = 19.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = Garamond, fontSize = 17.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = Garamond, fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Garamond, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.04.em),
    labelMedium = TextStyle(fontFamily = Garamond, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.06.em),
    labelSmall = TextStyle(fontFamily = Garamond, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em),
)

/** Etichette di sezione in maiuscoletto spaziato ("TRADUZIONI", "LIBRI"). */
val SectionLabelStyle = TextStyle(
    fontFamily = Garamond,
    fontSize = 13.sp,
    letterSpacing = 0.22.em,
    fontWeight = FontWeight.Normal,
)
