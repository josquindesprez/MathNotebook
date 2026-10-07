package it.lectio.bibbia.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.repository.ThemeMode
import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.ui.books.BookChapterChooser
import it.lectio.bibbia.ui.theme.BibbiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scelta Libro → Capitolo: interazione, tema scuro e ripristino dopo un cambio di configurazione. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BookChapterChooserTest {

    @get:Rule
    val compose = createComposeRule()

    private val books = listOf(
        Book("riveduta", Canon.requireBook("GEN"), "Genesi", 0, 50),
        Book("riveduta", Canon.requireBook("PSA"), "Salmi", 18, 150),
        Book("riveduta", Canon.requireBook("OBA"), "Abdia", 30, 1),
        Book("riveduta", Canon.requireBook("JHN"), "Giovanni", 42, 21),
    )

    @Test
    fun `libro poi capitolo`() {
        var chosen: ChapterRef? = null
        compose.setContent {
            BibbiaTheme(ThemeMode.DARK) {
                BookChapterChooser(books = books, current = null, onChoose = { chosen = it })
            }
        }
        compose.onNodeWithText("Nuovo Testamento".uppercase()).assertIsDisplayed()
        compose.onNodeWithText("Salmi").performClick()
        compose.onNodeWithText("150 capitoli").assertIsDisplayed()
        compose.onNodeWithText("7").performClick()
        assertThat(chosen).isEqualTo(ChapterRef("PSA", 7))
    }

    @Test
    fun `libro di un solo capitolo apre subito`() {
        var chosen: ChapterRef? = null
        compose.setContent {
            BibbiaTheme(ThemeMode.LIGHT) {
                BookChapterChooser(books = books, current = null, onChoose = { chosen = it })
            }
        }
        compose.onNodeWithText("Abdia").performClick()
        assertThat(chosen).isEqualTo(ChapterRef("OBA", 1))
    }

    @Test
    fun `il libro aperto sopravvive alla rotazione`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            BibbiaTheme(ThemeMode.SEPIA) {
                BookChapterChooser(books = books, current = ChapterRef("JHN", 3), onChoose = {})
            }
        }
        compose.onNodeWithText("Giovanni").performClick()
        compose.onNodeWithText("21 capitoli").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("21 capitoli").assertIsDisplayed()
    }
}
