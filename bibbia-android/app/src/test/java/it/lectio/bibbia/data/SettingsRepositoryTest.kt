package it.lectio.bibbia.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.repository.ReaderFont
import it.lectio.bibbia.data.repository.ReaderLayout
import it.lectio.bibbia.data.repository.ReaderSettings
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.repository.ThemeMode
import it.lectio.bibbia.domain.model.ReadingPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Persistenza delle preferenze: test JVM puro, DataStore su un file temporaneo. */
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun <T> withRepository(file: File, block: suspend (SettingsRepository) -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            return runBlocking { block(SettingsRepository(store)) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `valori predefiniti al primo avvio`() {
        val file = File(folder.root, "a.preferences_pb")
        withRepository(file) { repo ->
            assertThat(repo.readerSettings.first()).isEqualTo(ReaderSettings())
            assertThat(repo.lastPosition.first()).isNull()
            assertThat(repo.currentTranslationId.first()).isNull()
        }
    }

    @Test
    fun `le preferenze sopravvivono al riavvio`() {
        val file = File(folder.root, "b.preferences_pb")
        withRepository(file) { repo ->
            repo.update {
                it.copy(
                    fontSizeSp = 24f,
                    font = ReaderFont.LITERATA,
                    lineHeight = 1.8f,
                    theme = ThemeMode.DARK,
                    layout = ReaderLayout.PAGE,
                    showVerseNumbers = false,
                    justify = true,
                    keepScreenOn = true,
                )
            }
        }
        // Nuova istanza sullo stesso file = app riavviata.
        withRepository(file) { repo ->
            val s = repo.readerSettings.first()
            assertThat(s.fontSizeSp).isEqualTo(24f)
            assertThat(s.font).isEqualTo(ReaderFont.LITERATA)
            assertThat(s.lineHeight).isEqualTo(1.8f)
            assertThat(s.theme).isEqualTo(ThemeMode.DARK)
            assertThat(s.layout).isEqualTo(ReaderLayout.PAGE)
            assertThat(s.showVerseNumbers).isFalse()
            assertThat(s.justify).isTrue()
            assertThat(s.keepScreenOn).isTrue()
        }
    }

    @Test
    fun `valori fuori limite vengono riportati nell intervallo`() {
        val file = File(folder.root, "c.preferences_pb")
        withRepository(file) { repo ->
            repo.update { it.copy(fontSizeSp = 200f, lineHeight = 0.1f) }
            val s = repo.readerSettings.first()
            assertThat(s.fontSizeSp).isEqualTo(ReaderSettings.MAX_FONT_SIZE)
            assertThat(s.lineHeight).isEqualTo(ReaderSettings.MIN_LINE_HEIGHT)
        }
    }

    @Test
    fun `ultima posizione di lettura e traduzione corrente`() {
        val file = File(folder.root, "d.preferences_pb")
        withRepository(file) { repo ->
            repo.savePosition(ReadingPosition("riveduta", "PSA", 42, 5))
        }
        withRepository(file) { repo ->
            assertThat(repo.lastPosition.first()).isEqualTo(ReadingPosition("riveduta", "PSA", 42, 5))
            assertThat(repo.currentTranslationId.first()).isEqualTo("riveduta")
            repo.setCurrentTranslation("kjv")
            assertThat(repo.currentTranslationId.first()).isEqualTo("kjv")
            // Cambiare traduzione non cancella la posizione.
            assertThat(repo.lastPosition.first()?.bookId).isEqualTo("PSA")
        }
    }
}
