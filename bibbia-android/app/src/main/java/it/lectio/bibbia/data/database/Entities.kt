package it.lectio.bibbia.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/** Stato di installazione di una traduzione. I metadati descrittivi stanno nel catalogo. */
@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey val id: String,
    val status: String,
    val verseCount: Int = 0,
    val installedAt: Long? = null,
    val error: String? = null,
)

@Entity(tableName = "books", primaryKeys = ["translationId", "bookId"])
data class BookEntity(
    val translationId: String,
    val bookId: String,
    val position: Int,
    val chapterCount: Int,
)

@Entity(
    tableName = "verses",
    indices = [Index(value = ["translationId", "bookId", "chapter", "verse"], unique = true)],
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    /** Testo da mostrare (con le convenzioni della sorgente, es. [corsivo] KJV). */
    val text: String,
    /** Testo normalizzato per la ricerca (vedi TextFolding). */
    val searchText: String,
    val paragraphStart: Boolean,
)

/** Indice full-text (FTS4) con contenuto esterno: non duplica il testo, indicizza [VerseEntity.searchText]. */
@Fts4(contentEntity = VerseEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "verses_fts")
data class VerseFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val searchText: String,
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["translationId", "bookId", "chapter", "verse"])],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    /** null = segnalibro sull'intero capitolo. */
    val verse: Int?,
    val label: String?,
    val preview: String,
    val createdAt: Long,
)

@Entity(tableName = "highlights", primaryKeys = ["translationId", "bookId", "chapter", "verse"])
data class HighlightEntity(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val color: String,
    val createdAt: Long,
)
