package it.lectio.bibbia.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import it.lectio.bibbia.BibbiaApplication
import it.lectio.bibbia.MainActivity
import it.lectio.bibbia.data.repository.ReaderLayout
import it.lectio.bibbia.data.repository.ThemeMode
import it.lectio.bibbia.domain.model.ReadingPosition
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Genera gli screenshot di docs/screenshots. Non gira nei test normali: si attiva con
 * `SHOTS=/percorso/cartella ./gradlew :app:testDebugUnitTest --tests '*ScreenshotCapture*'`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = BibbiaApplication::class, qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotCapture {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val out = File(System.getenv("SHOTS") ?: "/tmp")

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        val v = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun capture() {
        assumeTrue(System.getenv("SHOTS") != null)
        val c = (compose.activity.application as BibbiaApplication).container
        runBlocking {
            c.translationRepository.ensureBundledInstalled()
            c.settingsRepository.savePosition(ReadingPosition("riveduta", "PSA", 42, 1))
            c.bookmarkRepository.add("riveduta", it.lectio.bibbia.domain.model.ChapterRef("PSA", 42), 1, "Come la cerva agogna i rivi dell’acque", "Sete di Dio")
            c.bookmarkRepository.setHighlight("riveduta", it.lectio.bibbia.domain.model.VerseRef("PSA", 42, 5), it.lectio.bibbia.domain.model.HighlightColor.OCHRE)
        }
        compose.waitUntil(10_000) { runCatching { compose.onNodeWithText("Salmi 42").fetchSemanticsNode(); true }.getOrDefault(false) }
        shot("01-home")
        compose.onNodeWithText("Salmi 42").performClick()
        compose.waitUntil(10_000) { runCatching { compose.onNodeWithText("SALMI").fetchSemanticsNode(); true }.getOrDefault(false) }
        shot("02-reader-light")
        runBlocking { c.settingsRepository.update { it.copy(theme = ThemeMode.DARK) } }
        shot("03-reader-dark")
        runBlocking { c.settingsRepository.update { it.copy(theme = ThemeMode.SEPIA, layout = ReaderLayout.PAGE) } }
        shot("04-reader-page-sepia")
        runBlocking { c.settingsRepository.update { it.copy(theme = ThemeMode.LIGHT, layout = ReaderLayout.VERSES) } }
        compose.onNodeWithText("Salmi 42", useUnmergedTree = true).performClick()
        shot("05-picker")
    }
}
