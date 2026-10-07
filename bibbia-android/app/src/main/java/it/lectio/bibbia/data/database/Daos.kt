package it.lectio.bibbia.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations")
    fun observeAll(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE id = :id")
    suspend fun get(id: String): TranslationEntity?

    @Upsert
    suspend fun upsert(entity: TranslationEntity)
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE translationId = :translationId ORDER BY position")
    fun observeBooks(translationId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE translationId = :translationId ORDER BY position")
    suspend fun getBooks(translationId: String): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE translationId = :translationId")
    suspend fun deleteForTranslation(translationId: String)
}

/** Riga di risultato della ricerca: versetto + posizione del libro per l'ordinamento canonico. */
data class VerseSearchRow(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

@Dao
interface VerseDao {
    @Query(
        """SELECT * FROM verses
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter
           ORDER BY verse""",
    )
    fun observeChapter(translationId: String, bookId: String, chapter: Int): Flow<List<VerseEntity>>

    @Query(
        """SELECT * FROM verses
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter
           ORDER BY verse""",
    )
    suspend fun getChapter(translationId: String, bookId: String, chapter: Int): List<VerseEntity>

    @Query(
        """SELECT * FROM verses
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter AND verse = :verse""",
    )
    suspend fun getVerse(translationId: String, bookId: String, chapter: Int, verse: Int): VerseEntity?

    @Insert
    suspend fun insertAll(verses: List<VerseEntity>)

    @Query("DELETE FROM verses WHERE translationId = :translationId")
    suspend fun deleteForTranslation(translationId: String)

    @Query("SELECT COUNT(*) FROM verses WHERE translationId = :translationId")
    suspend fun count(translationId: String): Int

    /**
     * Ricerca full-text. [match] è un'espressione MATCH di FTS4 già costruita e sicura.
     * [testament] = '' per tutti, altrimenti 'OT'/'NT' filtrato tramite [bookIds].
     */
    @Query(
        """SELECT v.translationId, v.bookId, v.chapter, v.verse, v.text
           FROM verses_fts
           JOIN verses v ON v.id = verses_fts.rowid
           JOIN books b ON b.translationId = v.translationId AND b.bookId = v.bookId
           WHERE verses_fts MATCH :match AND v.translationId = :translationId
             AND (:allBooks = 1 OR v.bookId IN (:bookIds))
           ORDER BY b.position, v.chapter, v.verse
           LIMIT :limit""",
    )
    suspend fun search(
        match: String,
        translationId: String,
        allBooks: Boolean,
        bookIds: List<String>,
        limit: Int,
    ): List<VerseSearchRow>

    @Query(
        """SELECT COUNT(*)
           FROM verses_fts
           JOIN verses v ON v.id = verses_fts.rowid
           WHERE verses_fts MATCH :match AND v.translationId = :translationId
             AND (:allBooks = 1 OR v.bookId IN (:bookIds))""",
    )
    suspend fun searchCount(
        match: String,
        translationId: String,
        allBooks: Boolean,
        bookIds: List<String>,
    ): Int
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query(
        """SELECT * FROM bookmarks
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter""",
    )
    fun observeForChapter(translationId: String, bookId: String, chapter: Int): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun get(id: Long): BookmarkEntity?

    @Query(
        """SELECT * FROM bookmarks
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter
             AND ((:verse IS NULL AND verse IS NULL) OR verse = :verse)
           LIMIT 1""",
    )
    suspend fun find(translationId: String, bookId: String, chapter: Int, verse: Int?): BookmarkEntity?

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("UPDATE bookmarks SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String?)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface HighlightDao {
    @Query(
        """SELECT * FROM highlights
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter""",
    )
    fun observeForChapter(translationId: String, bookId: String, chapter: Int): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsert(highlight: HighlightEntity)

    @Query(
        """DELETE FROM highlights
           WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter AND verse = :verse""",
    )
    suspend fun delete(translationId: String, bookId: String, chapter: Int, verse: Int)
}
