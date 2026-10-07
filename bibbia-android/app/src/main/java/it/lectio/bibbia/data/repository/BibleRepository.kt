package it.lectio.bibbia.data.repository

import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.database.BookEntity
import it.lectio.bibbia.data.database.VerseEntity
import it.lectio.bibbia.domain.VersificationMapper
import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.Translation
import it.lectio.bibbia.domain.model.Verse
import it.lectio.bibbia.domain.model.VerseRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Una voce del confronto tra traduzioni. */
data class ComparedVerse(
    val translation: Translation,
    /** Riferimento effettivo nella numerazione di quella traduzione. */
    val ref: VerseRef,
    val text: String?,
)

/** Accesso in sola lettura ai testi installati. Tutto locale: nessuna chiamata di rete. */
class BibleRepository(
    db: BibleDatabase,
    private val translations: TranslationRepository,
) {
    private val bookDao = db.bookDao()
    private val verseDao = db.verseDao()

    fun observeBooks(translationId: String): Flow<List<Book>> =
        bookDao.observeBooks(translationId).map { list -> list.mapNotNull { it.toBook() } }

    suspend fun getBooks(translationId: String): List<Book> =
        bookDao.getBooks(translationId).mapNotNull { it.toBook() }

    fun observeChapter(translationId: String, ref: ChapterRef): Flow<List<Verse>> =
        verseDao.observeChapter(translationId, ref.bookId, ref.chapter).map { list -> list.map { it.toVerse() } }

    suspend fun getChapter(translationId: String, ref: ChapterRef): List<Verse> =
        verseDao.getChapter(translationId, ref.bookId, ref.chapter).map { it.toVerse() }

    suspend fun getVerse(translationId: String, ref: VerseRef): Verse? =
        verseDao.getVerse(translationId, ref.bookId, ref.chapter, ref.verse)?.toVerse()

    /**
     * Converte un capitolo da una traduzione a un'altra tenendo conto della numerazione
     * (es. Salmo 42 → Psalmus 41 nella Vulgata).
     */
    fun mapChapter(ref: ChapterRef, fromId: String, toId: String): ChapterRef {
        val from = translations.translation(fromId) ?: return ref
        val to = translations.translation(toId) ?: return ref
        return VersificationMapper.mapChapter(ref, from.versification, to.versification)
    }

    /** Lo stesso versetto in più traduzioni, per il confronto. */
    suspend fun compare(fromTranslationId: String, ref: VerseRef, targets: List<Translation>): List<ComparedVerse> =
        targets.map { target ->
            val mappedChapter = mapChapter(ref.chapterRef, fromTranslationId, target.id)
            val mapped = VerseRef(ref.bookId, mappedChapter.chapter, ref.verse)
            ComparedVerse(target, mapped, getVerse(target.id, mapped)?.text)
        }

    private fun BookEntity.toBook(): Book? {
        val info = Canon.book(bookId) ?: return null
        val language = translations.translation(translationId)?.language
            ?: it.lectio.bibbia.domain.model.BibleLanguage.ITALIAN
        return Book(
            translationId = translationId,
            info = info,
            name = info.name(language),
            position = position,
            chapterCount = chapterCount,
        )
    }

    private fun VerseEntity.toVerse() = Verse(
        translationId = translationId,
        bookId = bookId,
        chapter = chapter,
        number = verse,
        text = text,
        paragraphStart = paragraphStart,
    )
}
