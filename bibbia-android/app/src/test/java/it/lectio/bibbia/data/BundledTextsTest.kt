package it.lectio.bibbia.data

import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.source.RawVerse
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.data.source.VplParser
import it.lectio.bibbia.domain.model.Translation
import it.lectio.bibbia.domain.model.TranslationOrigin
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Verifica l'integrità dei testi inclusi nell'APK: sono la base della lettura offline.
 * Legge direttamente i file in src/main/assets.
 */
class BundledTextsTest {

    private fun load(t: Translation): List<RawVerse> {
        val path = (t.origin as TranslationOrigin.Bundled).assetPath
        val candidates = listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path"))
        val file = candidates.first { it.exists() }
        val verses = mutableListOf<RawVerse>()
        GZIPInputStream(file.inputStream()).bufferedReader().use { VplParser.parse(it, t.conventions) { v -> verses += v } }
        return verses
    }

    private fun List<RawVerse>.text(book: String, chapter: Int, verse: Int) =
        first { it.bookId == book && it.chapter == chapter && it.verse == verse }.text

    @Test
    fun `KJV completa`() {
        val verses = load(TranslationCatalog.KJV)
        assertThat(verses).hasSize(TranslationCatalog.KJV.expectedVerses)
        assertThat(verses.map { it.bookId }.distinct()).hasSize(66)
        assertThat(verses.text("JHN", 3, 16)).startsWith("For God so loved the world")
        assertThat(verses.text("PSA", 42, 1)).contains("As the hart panteth")
        assertThat(verses.filter { it.paragraphStart }).isNotEmpty()
        assertThat(verses.none { it.text.contains('¶') }).isTrue()
    }

    @Test
    fun `Riveduta completa`() {
        val verses = load(TranslationCatalog.RIVEDUTA)
        assertThat(verses).hasSize(TranslationCatalog.RIVEDUTA.expectedVerses)
        assertThat(verses.map { it.bookId }.distinct()).hasSize(66)
        assertThat(verses.text("JHN", 3, 16)).startsWith("Poiché Iddio ha tanto amato il mondo")
        assertThat(verses.text("PSA", 42, 1)).contains("Come la cerva")
    }

    @Test
    fun `Vulgata Clementina completa con deuterocanonici e numerazione latina`() {
        val verses = load(TranslationCatalog.VULGATA)
        assertThat(verses).hasSize(TranslationCatalog.VULGATA.expectedVerses)
        val books = verses.map { it.bookId }.distinct()
        assertThat(books).hasSize(73)
        assertThat(books).containsAtLeast("TOB", "JDT", "WIS", "SIR", "BAR", "1MA", "2MA")
        assertThat(verses.text("JHN", 3, 16)).startsWith("Sic enim Deus dilexit mundum")
        assertThat(verses.text("PSA", 41, 2)).startsWith("Quemadmodum desiderat cervus")
        assertThat(verses.none { it.text.contains('[') || it.text.contains(']') }).isTrue()
    }

    @Test
    fun `nessun versetto duplicato`() {
        TranslationCatalog.bundled.forEach { t ->
            val keys = load(t).map { Triple(it.bookId, it.chapter, it.verse) }
            assertThat(keys.toSet()).hasSize(keys.size)
        }
    }
}
