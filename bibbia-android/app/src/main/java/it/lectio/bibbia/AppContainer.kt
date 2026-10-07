package it.lectio.bibbia

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.repository.BibleRepository
import it.lectio.bibbia.data.repository.BookmarkRepository
import it.lectio.bibbia.data.repository.SearchRepository
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.data.source.DefaultTranslationSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Contenitore delle dipendenze (DI manuale). Un'unica istanza per processo, creata da
 * [BibbiaApplication].
 */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: BibleDatabase = BibleDatabase.create(context)

    val translationRepository = TranslationRepository(database, DefaultTranslationSourceFactory(context))
    val bibleRepository = BibleRepository(database, translationRepository)
    val bookmarkRepository = BookmarkRepository(database)
    val searchRepository = SearchRepository(database)
    val settingsRepository = SettingsRepository(context.settingsDataStore)
}
