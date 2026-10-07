package it.lectio.bibbia.util

/** Utilità per il testo dei versetti indipendenti dall'interfaccia. */
object VerseText {

    /** Testo semplice, senza marcatori di corsivo ([...]). */
    fun plain(text: String): String = text.replace("[", "").replace("]", "")

    /** Anteprima breve per segnalibri e risultati, tagliata su un confine di parola. */
    fun preview(text: String, maxLength: Int = 160): String {
        val plain = plain(text).trim()
        if (plain.length <= maxLength) return plain
        val cut = plain.lastIndexOf(' ', maxLength).takeIf { it > maxLength / 2 } ?: maxLength
        return plain.substring(0, cut).trimEnd(',', ';', ':', ' ') + "…"
    }

    /** Segmenti del testo: (testo, corsivo). Le parti tra [ ] sono aggiunte dei traduttori KJV. */
    fun segments(text: String, bracketsAreItalics: Boolean): List<Pair<String, Boolean>> {
        if (!bracketsAreItalics || !text.contains('[')) return listOf(plain(text) to false)
        val result = ArrayList<Pair<String, Boolean>>()
        var italic = false
        val current = StringBuilder()
        for (c in text) {
            when (c) {
                '[' -> {
                    if (current.isNotEmpty()) result.add(current.toString() to italic)
                    current.clear()
                    italic = true
                }
                ']' -> {
                    if (current.isNotEmpty()) result.add(current.toString() to italic)
                    current.clear()
                    italic = false
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString() to italic)
        return result
    }
}
