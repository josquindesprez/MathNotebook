package it.lectio.bibbia.data.repository

import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.database.BookmarkEntity
import it.lectio.bibbia.data.database.HighlightEntity
import it.lectio.bibbia.domain.model.Bookmark
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.Highlight
import it.lectio.bibbia.domain.model.HighlightColor
import it.lectio.bibbia.domain.model.VerseRef
import it.lectio.bibbia.util.VerseText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Segnalibri ed evidenziazioni: dati personali, conservati solo sul dispositivo. */
class BookmarkRepository(
    db: BibleDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val bookmarkDao = db.bookmarkDao()
    private val highlightDao = db.highlightDao()

    fun observeAll(): Flow<List<Bookmark>> = bookmarkDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeForChapter(translationId: String, ref: ChapterRef): Flow<List<Bookmark>> =
        bookmarkDao.observeForChapter(translationId, ref.bookId, ref.chapter).map { list -> list.map { it.toModel() } }

    suspend fun find(translationId: String, ref: ChapterRef, verse: Int?): Bookmark? =
        bookmarkDao.find(translationId, ref.bookId, ref.chapter, verse)?.toModel()

    /** Aggiunge un segnalibro (se non esiste già) e ne restituisce l'id. */
    suspend fun add(
        translationId: String,
        ref: ChapterRef,
        verse: Int?,
        previewText: String,
        label: String? = null,
    ): Long {
        bookmarkDao.find(translationId, ref.bookId, ref.chapter, verse)?.let { return it.id }
        return bookmarkDao.insert(
            BookmarkEntity(
                translationId = translationId,
                bookId = ref.bookId,
                chapter = ref.chapter,
                verse = verse,
                label = label?.trim()?.takeIf { it.isNotEmpty() },
                preview = VerseText.preview(previewText),
                createdAt = clock(),
            ),
        )
    }

    /** Aggiunge o rimuove; restituisce true se ora il segnalibro esiste. */
    suspend fun toggle(translationId: String, ref: ChapterRef, verse: Int?, previewText: String): Boolean {
        val existing = bookmarkDao.find(translationId, ref.bookId, ref.chapter, verse)
        return if (existing != null) {
            bookmarkDao.delete(existing.id)
            false
        } else {
            add(translationId, ref, verse, previewText)
            true
        }
    }

    suspend fun rename(id: Long, label: String?) =
        bookmarkDao.updateLabel(id, label?.trim()?.takeIf { it.isNotEmpty() })

    suspend fun delete(id: Long) = bookmarkDao.delete(id)

    /** Ripristina un segnalibro eliminato (per "Annulla"). */
    suspend fun restore(bookmark: Bookmark): Long = bookmarkDao.insert(
        BookmarkEntity(
            translationId = bookmark.translationId,
            bookId = bookmark.bookId,
            chapter = bookmark.chapter,
            verse = bookmark.verse,
            label = bookmark.label,
            preview = bookmark.preview,
            createdAt = bookmark.createdAt,
        ),
    )

    fun observeHighlights(translationId: String, ref: ChapterRef): Flow<List<Highlight>> =
        highlightDao.observeForChapter(translationId, ref.bookId, ref.chapter).map { list ->
            list.mapNotNull { e ->
                val color = runCatching { HighlightColor.valueOf(e.color) }.getOrNull() ?: return@mapNotNull null
                Highlight(e.translationId, VerseRef(e.bookId, e.chapter, e.verse), color)
            }
        }

    suspend fun setHighlight(translationId: String, ref: VerseRef, color: HighlightColor?) {
        if (color == null) {
            highlightDao.delete(translationId, ref.bookId, ref.chapter, ref.verse)
        } else {
            highlightDao.upsert(
                HighlightEntity(translationId, ref.bookId, ref.chapter, ref.verse, color.name, clock()),
            )
        }
    }

    private fun BookmarkEntity.toModel() = Bookmark(
        id = id,
        translationId = translationId,
        bookId = bookId,
        chapter = chapter,
        verse = verse,
        label = label,
        preview = preview,
        createdAt = createdAt,
    )
}
