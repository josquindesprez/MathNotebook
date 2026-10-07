package it.lectio.bibbia.domain

import it.lectio.bibbia.domain.model.BookInfo
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.util.TextFolding

/** Risultato del parsing di un riferimento come "Gv 3,16" o "Salmi 137". */
data class ParsedReference(
    val bookId: String,
    val chapter: Int,
    val verse: Int? = null,
    val endVerse: Int? = null,
)

/**
 * Interpreta riferimenti biblici scritti a mano, in italiano, inglese o latino.
 *
 * Formati supportati (esempi):
 * - "Giovanni 3:16", "Gv 3,16", "gv3.16", "Jn 3:16", "John 3 16"
 * - "Salmi 137", "Sal 23", "Psalm 23", "Ps 23"
 * - "Isaia 40" (capitolo intero), "Isaia" (capitolo 1)
 * - "1 Cor 13,4-7", "1Gv 4:8", "I Giovanni 4", "Prima Corinzi 13"
 * - "Gen 1:1–5" (intervallo di versetti)
 *
 * In caso di abbreviazioni ambigue tra lingue vale l'uso italiano (CEI): "Gn" = Giona, "Mc" = Marco.
 */
object ReferenceParser {

    private val referenceRegex = Regex(
        """^\s*((?:[1-4]\s*)?[^\d]+?)\s*(?:(\d{1,3})(?:\s*[:,.\s]\s*(\d{1,3})(?:\s*[-–—]\s*(\d{1,3}))?)?)?\s*$""",
    )

    private val ordinalPrefixes = listOf(
        "prima " to "1", "primo " to "1", "seconda " to "2", "secondo " to "2", "terza " to "3", "terzo " to "3",
        "first " to "1", "second " to "2", "third " to "3",
        "iii " to "3", "ii " to "2", "iv " to "4", "i " to "1",
    )

    /** Mappa alias normalizzato → libro. Le voci italiane hanno la precedenza. */
    private val aliases: Map<String, BookInfo> by lazy {
        val map = LinkedHashMap<String, BookInfo>()
        fun put(alias: String, book: BookInfo) {
            val key = normalizeBookKey(alias)
            if (key.isNotEmpty() && key !in map) map[key] = book
        }
        // 1. Italiano (nomi + abbreviazioni CEI)
        Canon.books.forEach { b -> put(b.italian, b); b.italianAbbreviations.forEach { put(it, b) } }
        // 2. Inglese
        Canon.books.forEach { b -> put(b.english, b); b.englishAbbreviations.forEach { put(it, b) } }
        // 3. Latino
        Canon.books.forEach { b -> put(b.latin, b); b.latinAliases.forEach { put(it, b) } }
        // Forme singolari/alternative frequenti
        Canon.book("PSA")?.let { put("Salmo", it); put("Psalm", it) }
        Canon.book("REV")?.let { put("Apocalisse di Giovanni", it); put("Rivelazione", it) }
        Canon.book("ACT")?.let { put("Atti", it) }
        Canon.book("SNG")?.let { put("Cantico", it); put("Song of Songs", it) }
        Canon.book("ECC")?.let { put("Qoelet", it); put("Qohelet", it) }
        map
    }

    /** Normalizza il nome di un libro: minuscole, niente accenti/punti/spazi, ordinali → cifre. */
    fun normalizeBookKey(raw: String): String {
        var s = TextFolding.fold(raw).trim()
        for ((prefix, digit) in ordinalPrefixes) {
            if (s.startsWith(prefix)) {
                s = digit + s.removePrefix(prefix)
                break
            }
        }
        // "ad romanos" → "romanos" anche come alias latino senza preposizione
        return s.replace(" ", "")
    }

    /** Trova il libro corrispondente a un nome o un'abbreviazione, o null se sconosciuto/ambiguo. */
    fun findBook(name: String): BookInfo? {
        val key = normalizeBookKey(name)
        if (key.isEmpty()) return null
        aliases[key]?.let { return it }
        // Latino: "ad Romanos" → "Romanos"
        aliases["ad$key"]?.let { return it }
        if (key.length < 3) return null
        // Prefisso univoco di un nome completo ("Deuter", "Apocal", "Lament")
        val candidates = aliases.entries
            .filter { (alias, _) -> alias.length > key.length && alias.startsWith(key) }
            .map { it.value }
            .distinctBy { it.id }
        return candidates.singleOrNull()
    }

    fun parse(input: String): ParsedReference? {
        val match = referenceRegex.matchEntire(input.trim()) ?: return null
        val (bookPart, chapterPart, versePart, endPart) = match.destructured
        val book = findBook(bookPart) ?: return null
        val chapter = chapterPart.toIntOrNull() ?: 1
        if (chapter < 1) return null
        val verse = versePart.toIntOrNull()?.takeIf { it >= 1 }
        val endVerse = endPart.toIntOrNull()?.takeIf { verse != null && it >= verse }
        return ParsedReference(book.id, chapter, verse, endVerse)
    }
}
