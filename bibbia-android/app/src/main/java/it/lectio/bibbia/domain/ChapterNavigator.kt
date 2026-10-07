package it.lectio.bibbia.domain

import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.ChapterRef

/**
 * Navigazione sequenziale tra i capitoli di una traduzione, attraverso i confini dei libri
 * (Genesi 50 → Esodo 1, Malachia 4 ← Matteo 1).
 */
class ChapterNavigator(books: List<Book>) {

    private val ordered: List<Book> = books.sortedBy { it.position }
    private val indexById: Map<String, Int> = ordered.withIndex().associate { (i, b) -> b.id to i }

    val isEmpty: Boolean get() = ordered.isEmpty()

    fun book(bookId: String): Book? = indexById[bookId]?.let { ordered[it] }

    fun contains(ref: ChapterRef): Boolean {
        val book = book(ref.bookId) ?: return false
        return ref.chapter in 1..book.chapterCount
    }

    fun previous(ref: ChapterRef): ChapterRef? {
        val index = indexById[ref.bookId] ?: return null
        if (ref.chapter > 1) return ChapterRef(ref.bookId, ref.chapter - 1)
        val prevBook = ordered.getOrNull(index - 1) ?: return null
        return ChapterRef(prevBook.id, prevBook.chapterCount)
    }

    fun next(ref: ChapterRef): ChapterRef? {
        val index = indexById[ref.bookId] ?: return null
        val book = ordered[index]
        if (ref.chapter < book.chapterCount) return ChapterRef(ref.bookId, ref.chapter + 1)
        val nextBook = ordered.getOrNull(index + 1) ?: return null
        return ChapterRef(nextBook.id, 1)
    }

    /** Riporta un riferimento dentro i limiti della traduzione (capitolo troppo alto → ultimo capitolo). */
    fun coerce(ref: ChapterRef): ChapterRef? {
        val book = book(ref.bookId) ?: return ordered.firstOrNull()?.let { ChapterRef(it.id, 1) }
        return ChapterRef(book.id, ref.chapter.coerceIn(1, book.chapterCount))
    }
}
