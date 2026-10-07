package it.lectio.bibbia.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import it.lectio.bibbia.domain.model.ReadingPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, SEPIA, DARK }

enum class ReaderFont { GARAMOND, LITERATA, SYSTEM_SERIF }

/** VERSES = un versetto per capoverso; PAGE = prosa continua come in un libro stampato. */
enum class ReaderLayout { VERSES, PAGE }

data class ReaderSettings(
    val fontSizeSp: Float = DEFAULT_FONT_SIZE,
    val font: ReaderFont = ReaderFont.GARAMOND,
    val lineHeight: Float = DEFAULT_LINE_HEIGHT,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val layout: ReaderLayout = ReaderLayout.VERSES,
    val showVerseNumbers: Boolean = true,
    val justify: Boolean = false,
    val keepScreenOn: Boolean = false,
) {
    companion object {
        const val DEFAULT_FONT_SIZE = 20f
        const val MIN_FONT_SIZE = 14f
        const val MAX_FONT_SIZE = 34f
        const val DEFAULT_LINE_HEIGHT = 1.55f
        const val MIN_LINE_HEIGHT = 1.2f
        const val MAX_LINE_HEIGHT = 2.2f
    }
}

/** Preferenze e ultima posizione di lettura, persistite localmente con DataStore. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val fontSize = floatPreferencesKey("font_size")
        val font = stringPreferencesKey("font")
        val lineHeight = floatPreferencesKey("line_height")
        val theme = stringPreferencesKey("theme")
        val layout = stringPreferencesKey("layout")
        val verseNumbers = booleanPreferencesKey("verse_numbers")
        val justify = booleanPreferencesKey("justify")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")

        val posTranslation = stringPreferencesKey("pos_translation")
        val posBook = stringPreferencesKey("pos_book")
        val posChapter = intPreferencesKey("pos_chapter")
        val posVerse = intPreferencesKey("pos_verse")

        val currentTranslation = stringPreferencesKey("current_translation")
    }

    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val readerSettings: Flow<ReaderSettings> = preferences.map { it.toReaderSettings() }.distinctUntilChanged()

    val lastPosition: Flow<ReadingPosition?> = preferences.map { p ->
        val translation = p[Keys.posTranslation] ?: return@map null
        val book = p[Keys.posBook] ?: return@map null
        ReadingPosition(translation, book, p[Keys.posChapter] ?: 1, p[Keys.posVerse] ?: 1)
    }.distinctUntilChanged()

    /** Traduzione scelta per la lettura (può differire da quella dell'ultima posizione). */
    val currentTranslationId: Flow<String?> = preferences.map { it[Keys.currentTranslation] }.distinctUntilChanged()

    suspend fun currentSettings(): ReaderSettings = readerSettings.first()

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        dataStore.edit { p ->
            val current = p.toReaderSettings()
            val next = transform(current)
            p[Keys.fontSize] = next.fontSizeSp.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE)
            p[Keys.font] = next.font.name
            p[Keys.lineHeight] = next.lineHeight.coerceIn(ReaderSettings.MIN_LINE_HEIGHT, ReaderSettings.MAX_LINE_HEIGHT)
            p[Keys.theme] = next.theme.name
            p[Keys.layout] = next.layout.name
            p[Keys.verseNumbers] = next.showVerseNumbers
            p[Keys.justify] = next.justify
            p[Keys.keepScreenOn] = next.keepScreenOn
        }
    }

    suspend fun savePosition(position: ReadingPosition) {
        dataStore.edit { p ->
            p[Keys.posTranslation] = position.translationId
            p[Keys.posBook] = position.bookId
            p[Keys.posChapter] = position.chapter
            p[Keys.posVerse] = position.verse
            p[Keys.currentTranslation] = position.translationId
        }
    }

    suspend fun setCurrentTranslation(id: String) {
        dataStore.edit { it[Keys.currentTranslation] = id }
    }

    private fun Preferences.toReaderSettings(): ReaderSettings {
        val defaults = ReaderSettings()
        return ReaderSettings(
            fontSizeSp = (this[Keys.fontSize] ?: defaults.fontSizeSp)
                .coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE),
            font = this[Keys.font].toEnum(defaults.font),
            lineHeight = (this[Keys.lineHeight] ?: defaults.lineHeight)
                .coerceIn(ReaderSettings.MIN_LINE_HEIGHT, ReaderSettings.MAX_LINE_HEIGHT),
            theme = this[Keys.theme].toEnum(defaults.theme),
            layout = this[Keys.layout].toEnum(defaults.layout),
            showVerseNumbers = this[Keys.verseNumbers] ?: defaults.showVerseNumbers,
            justify = this[Keys.justify] ?: defaults.justify,
            keepScreenOn = this[Keys.keepScreenOn] ?: defaults.keepScreenOn,
        )
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
}
