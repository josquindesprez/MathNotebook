package it.lectio.bibbia.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import it.lectio.bibbia.domain.model.HighlightColor
import it.lectio.bibbia.domain.model.Verse
import it.lectio.bibbia.util.VerseText

/** Un capoverso della modalità "pagina": più versetti in prosa continua. */
data class ReaderParagraph(val verses: List<Verse>)

/**
 * Divide un capitolo in capoversi.
 *
 * Se la sorgente indica i paragrafi (¶ nella KJV) si usano quelli. Altrimenti si impagina in modo
 * automatico: si va a capo dopo una frase conclusa quando il capoverso è già abbastanza lungo,
 * per evitare blocchi di testo eccessivi.
 */
object ChapterLayout {
    private const val SOFT_LIMIT = 650
    private const val HARD_LIMIT = 1400
    private val sentenceEnd = Regex("""[.!?»”"’)]\s*$""")

    fun paragraphs(verses: List<Verse>): List<ReaderParagraph> {
        if (verses.isEmpty()) return emptyList()
        val hasMarkers = verses.drop(1).any { it.paragraphStart }
        val result = ArrayList<ReaderParagraph>()
        var current = ArrayList<Verse>()
        var length = 0
        for (verse in verses) {
            val breakHere = current.isNotEmpty() && if (hasMarkers) {
                verse.paragraphStart
            } else {
                val previousEnds = sentenceEnd.containsMatchIn(current.last().text)
                (length >= SOFT_LIMIT && previousEnds) || length >= HARD_LIMIT
            }
            if (breakHere) {
                result.add(ReaderParagraph(current))
                current = ArrayList()
                length = 0
            }
            current.add(verse)
            length += verse.text.length
        }
        if (current.isNotEmpty()) result.add(ReaderParagraph(current))
        return result
    }
}

/** Stile dei singoli elementi del testo, risolto dal tema corrente. */
data class VerseStyle(
    val numberColor: Color,
    val bookmarkColor: Color,
    val selectionColor: Color,
    val flashColor: Color,
    val highlightColors: Map<HighlightColor, Color>,
    val showNumbers: Boolean,
    val italicBrackets: Boolean,
)

/** Testo annotato di un gruppo di versetti, con gli offset di inizio di ciascun versetto. */
data class AnnotatedVerses(val text: AnnotatedString, val starts: List<Pair<Int, Int>>) {
    /** Numero del versetto che contiene l'offset di carattere indicato. */
    fun verseAt(offset: Int): Int? = starts.lastOrNull { it.first <= offset }?.second

    fun startOf(verse: Int): Int? = starts.firstOrNull { it.second == verse }?.first
}

fun buildVerses(
    verses: List<Verse>,
    style: VerseStyle,
    inline: Boolean,
    bookmarked: Set<Int>,
    highlights: Map<Int, HighlightColor>,
    selected: Int?,
    flashing: Int?,
): AnnotatedVerses {
    val starts = ArrayList<Pair<Int, Int>>(verses.size)
    val text = buildAnnotatedString {
        verses.forEachIndexed { index, verse ->
            if (index > 0) append(' ')
            starts.add(length to verse.number)
            if (style.showNumbers) {
                val numberStyle = if (inline) {
                    SpanStyle(color = style.numberColor, fontSize = 0.6.em, baselineShift = BaselineShift(0.38f))
                } else {
                    SpanStyle(color = style.numberColor, fontSize = 0.66.em)
                }
                withStyle(numberStyle) { append(verse.number.toString()) }
                if (verse.number in bookmarked) {
                    withStyle(SpanStyle(color = style.bookmarkColor, fontSize = 0.5.em, baselineShift = BaselineShift(0.5f))) {
                        append(" ★")
                    }
                }
                append(' ')
                if (!inline) append(' ')
            } else if (verse.number in bookmarked) {
                withStyle(SpanStyle(color = style.bookmarkColor, fontSize = 0.5.em, baselineShift = BaselineShift(0.5f))) {
                    append("★ ")
                }
            }
            val background = when {
                verse.number == selected -> style.selectionColor
                verse.number == flashing -> style.flashColor
                else -> highlights[verse.number]?.let { style.highlightColors[it] }
            }
            val bodyStart = length
            VerseText.segments(verse.text, style.italicBrackets).forEach { (segment, italic) ->
                if (italic) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(segment) }
                } else {
                    append(segment)
                }
            }
            if (background != null) {
                addStyle(SpanStyle(background = background), bodyStart, length)
            }
        }
    }
    return AnnotatedVerses(text, starts)
}
