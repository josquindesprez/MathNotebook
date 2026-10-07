package it.lectio.bibbia.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/**
 * Lettura. Tutti i parametri sono opzionali: senza parametri si riprende dall'ultima posizione.
 * chapter/verse = 0 significa "non specificato".
 */
@Serializable
data class ReaderRoute(
    val translationId: String? = null,
    val bookId: String? = null,
    val chapter: Int = 0,
    val verse: Int = 0,
)

/** Elenco dei libri di un testamento ("OLD" / "NEW"). */
@Serializable
data class BooksRoute(val testament: String)

@Serializable
data object BookmarksRoute

@Serializable
data object SearchRoute

@Serializable
data object SettingsRoute

@Serializable
data object TranslationsRoute

@Serializable
data object AboutRoute
