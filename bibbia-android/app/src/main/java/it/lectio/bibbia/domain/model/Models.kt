package it.lectio.bibbia.domain.model

/**
 * Sistema di numerazione di capitoli e versetti. Serve a confrontare traduzioni che numerano
 * diversamente (es. i Salmi nella Vulgata seguono la numerazione greca/latina).
 */
enum class Versification {
    /** Numerazione "ebraica" usata da KJV, Riveduta, Diodati, CEI. */
    STANDARD,

    /** Numerazione della Vulgata (Salmi secondo la Settanta). */
    VULGATE,
}

/** Da dove proviene il testo di una traduzione. */
sealed interface TranslationOrigin {
    /** Testo incluso nell'APK (assets), installato localmente al primo avvio. */
    data class Bundled(val assetPath: String) : TranslationOrigin

    /** Testo scaricabile una sola volta e poi conservato offline. */
    data class Downloadable(val url: String, val approximateSizeMb: Int) : TranslationOrigin
}

/** Regole di pulizia del testo sorgente, specifiche di ogni edizione. */
data class TextConventions(
    /** Le parole tra [parentesi quadre] sono aggiunte dei traduttori, da mostrare in corsivo (KJV). */
    val bracketsAreItalics: Boolean = false,
    /** Le parentesi quadre sono marcatori editoriali da rimuovere (Vulgata eBible). */
    val stripBrackets: Boolean = false,
    /** Il carattere ¶ segna l'inizio di un paragrafo (KJV). */
    val pilcrowMarksParagraph: Boolean = false,
)

/**
 * Metadati di una traduzione. Le traduzioni sono descritte in [it.lectio.bibbia.data.source.TranslationCatalog];
 * aggiungerne una nuova non richiede modifiche al resto del codice.
 */
data class Translation(
    val id: String,
    /** Sigla breve mostrata nell'interfaccia (KJV, RIV, VUL). */
    val abbreviation: String,
    /** Etichetta della lingua come appare in home ("Italiano", "Latino"...). */
    val languageLabel: String,
    val name: String,
    val edition: String,
    val language: BibleLanguage,
    val description: String,
    val license: String,
    val attribution: String,
    val sourceUrl: String,
    val versification: Versification,
    val canonOrder: CanonOrder,
    val origin: TranslationOrigin,
    val conventions: TextConventions = TextConventions(),
    /** Numero atteso di versetti, usato solo per stimare l'avanzamento dell'installazione. */
    val expectedVerses: Int,
)

enum class InstallStatus { NOT_INSTALLED, INSTALLING, INSTALLED, FAILED }

data class TranslationState(
    val translation: Translation,
    val status: InstallStatus,
    val verseCount: Int = 0,
    val installedAt: Long? = null,
    val error: InstallError? = null,
    /** Avanzamento 0..1 durante l'installazione. */
    val progress: Float = 0f,
)

/** Errori di installazione, tradotti in testo dall'interfaccia. */
enum class InstallError { NO_CONNECTION, TIMEOUT, SERVER, INVALID_DATA, STORAGE, UNKNOWN }

/** Un libro così come presente in una specifica traduzione. */
data class Book(
    val translationId: String,
    val info: BookInfo,
    val name: String,
    val position: Int,
    val chapterCount: Int,
) {
    val id: String get() = info.id
    val testament: Testament get() = info.testament
}

/** Riferimento a un capitolo, indipendente dalla traduzione. */
data class ChapterRef(val bookId: String, val chapter: Int)

/** Riferimento a un versetto, indipendente dalla traduzione. */
data class VerseRef(val bookId: String, val chapter: Int, val verse: Int) {
    val chapterRef: ChapterRef get() = ChapterRef(bookId, chapter)
}

data class Verse(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val number: Int,
    /** Testo con le convenzioni della sorgente ([...] = corsivo per la KJV). */
    val text: String,
    val paragraphStart: Boolean = false,
) {
    val ref: VerseRef get() = VerseRef(bookId, chapter, number)
}

data class Chapter(
    val translationId: String,
    val book: Book,
    val number: Int,
    val verses: List<Verse>,
)

enum class HighlightColor { OCHRE, SAGE, SKY, ROSE }

data class Highlight(val translationId: String, val ref: VerseRef, val color: HighlightColor)

data class Bookmark(
    val id: Long,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    /** null = segnalibro sull'intero capitolo. */
    val verse: Int?,
    val label: String?,
    val preview: String,
    val createdAt: Long,
)

data class ReadingPosition(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int = 1,
)

data class SearchHit(
    val translationId: String,
    val ref: VerseRef,
    val text: String,
)

data class SearchResults(
    val query: String,
    val hits: List<SearchHit>,
    val totalCount: Int,
)
