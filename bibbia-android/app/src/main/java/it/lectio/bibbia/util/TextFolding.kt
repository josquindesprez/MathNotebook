package it.lectio.bibbia.util

import java.text.Normalizer

/**
 * Normalizzazione del testo per ricerca e confronti: minuscole, niente accenti, legature sciolte
 * (æ → ae), apostrofi e punteggiatura trasformati in spazi.
 *
 * La stessa funzione viene usata sia per indicizzare i versetti sia per le query, così "citta"
 * trova "città" e "caelum" trova "cælum".
 */
object TextFolding {

    private val combiningMarks = Regex("\\p{Mn}+")

    /** Ripiega un singolo carattere; può restituire più caratteri (æ → ae) o uno spazio. */
    fun foldChar(c: Char): String = when (c) {
        'æ', 'Æ' -> "ae"
        'œ', 'Œ' -> "oe"
        'ß' -> "ss"
        else -> {
            if (c.isLetterOrDigit()) {
                val decomposed = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
                combiningMarks.replace(decomposed, "").lowercase()
            } else {
                " "
            }
        }
    }

    /** Testo ripiegato con spazi singoli, adatto all'indice full-text. */
    fun fold(text: String): String {
        val sb = StringBuilder(text.length)
        var lastSpace = true
        for (c in text) {
            val f = foldChar(c)
            if (f == " ") {
                if (!lastSpace) sb.append(' ')
                lastSpace = true
            } else {
                sb.append(f)
                lastSpace = false
            }
        }
        return sb.toString().trimEnd()
    }

    /**
     * Ripiega [text] mantenendo, per ogni carattere del risultato, l'indice del carattere originale.
     * Serve a evidenziare nel testo originale le parole trovate nel testo ripiegato.
     */
    fun foldWithMapping(text: String): Pair<String, IntArray> {
        val sb = StringBuilder(text.length)
        val map = ArrayList<Int>(text.length)
        text.forEachIndexed { index, c ->
            val f = foldChar(c)
            for (ch in f) {
                sb.append(ch)
                map.add(index)
            }
        }
        return sb.toString() to map.toIntArray()
    }

    /** Parole significative di una query, già ripiegate. */
    fun queryTerms(query: String): List<String> =
        fold(query).split(' ').filter { it.isNotBlank() }
}
