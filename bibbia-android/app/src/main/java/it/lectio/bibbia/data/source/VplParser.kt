package it.lectio.bibbia.data.source

import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.TextConventions
import java.io.BufferedReader

/** Versetto letto da una sorgente, già normalizzato secondo le convenzioni dell'edizione. */
data class RawVerse(
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val paragraphStart: Boolean,
)

class InvalidSourceException(message: String) : Exception(message)

/**
 * Parser del formato "VPL" (verse per line) distribuito da eBible.org:
 *
 *     GEN 1:1 In the beginning God created the heaven and the earth.
 *
 * I codici dei libri di eBible differiscono in parte da USFM (SOL, EZE, JOH...): vengono convertiti
 * negli identificatori stabili del [Canon].
 */
object VplParser {

    private val lineRegex = Regex("""^([1-4]?[A-Z]{2,3})\s+(\d+):(\d+)\s+(.*)$""")

    private val ebibleToUsfm = mapOf(
        "SOL" to "SNG", "EZE" to "EZK", "JOE" to "JOL", "NAH" to "NAM", "MAR" to "MRK",
        "JOH" to "JHN", "PHI" to "PHP", "JAM" to "JAS", "1JO" to "1JN", "2JO" to "2JN", "3JO" to "3JN",
    )

    fun bookIdFor(code: String): String? {
        val id = ebibleToUsfm[code] ?: code
        return if (Canon.book(id) != null) id else null
    }

    /**
     * Legge le righe e chiama [onVerse] per ciascun versetto valido. Le righe vuote vengono ignorate;
     * libri sconosciuti (es. apocrifi non previsti dal canone) vengono saltati.
     * @return numero di versetti letti.
     */
    fun parse(reader: BufferedReader, conventions: TextConventions, onVerse: (RawVerse) -> Unit): Int {
        var count = 0
        var lineNumber = 0
        reader.lineSequence().forEach { rawLine ->
            lineNumber++
            val line = rawLine.trimStart('﻿').trimEnd()
            if (line.isBlank()) return@forEach
            val match = lineRegex.matchEntire(line)
                ?: throw InvalidSourceException("Riga $lineNumber non valida")
            val (code, chapter, verse, body) = match.destructured
            val bookId = bookIdFor(code) ?: return@forEach
            val cleaned = cleanText(body, conventions)
            onVerse(
                RawVerse(
                    bookId = bookId,
                    chapter = chapter.toInt(),
                    verse = verse.toInt(),
                    text = cleaned.first,
                    paragraphStart = cleaned.second,
                ),
            )
            count++
        }
        return count
    }

    /** @return testo pulito e flag "inizio paragrafo". */
    fun cleanText(body: String, conventions: TextConventions): Pair<String, Boolean> {
        var text = body
        var paragraph = false
        if (conventions.pilcrowMarksParagraph && text.contains('¶')) {
            paragraph = true
        }
        text = text.replace("¶", "")
        if (conventions.stripBrackets) {
            text = text.replace("[", "").replace("]", "")
        }
        text = text.replace(Regex("\\s+"), " ").trim()
        return text to paragraph
    }
}
