package it.lectio.bibbia.ui

import it.lectio.bibbia.R
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.domain.model.BibleLanguage
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.InstallError
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Formattazione dei riferimenti nella lingua della traduzione: "Salmi 42:1", "Psalmi 41". */
object ReferenceFormatter {
    fun bookName(bookId: String, translationId: String?): String {
        val language = translationId?.let { TranslationCatalog.find(it)?.language } ?: BibleLanguage.ITALIAN
        return Canon.book(bookId)?.name(language) ?: bookId
    }

    fun format(bookId: String, chapter: Int, verse: Int? = null, translationId: String? = null): String {
        val name = bookName(bookId, translationId)
        return if (verse != null && verse > 0) "$name $chapter:$verse" else "$name $chapter"
    }
}

fun formatDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ITALIAN).format(Date(millis))

fun InstallError.messageRes(): Int = when (this) {
    InstallError.NO_CONNECTION -> R.string.error_no_connection
    InstallError.TIMEOUT -> R.string.error_timeout
    InstallError.SERVER -> R.string.error_server
    InstallError.INVALID_DATA -> R.string.error_invalid_data
    InstallError.STORAGE -> R.string.error_storage
    InstallError.UNKNOWN -> R.string.error_unknown
}
