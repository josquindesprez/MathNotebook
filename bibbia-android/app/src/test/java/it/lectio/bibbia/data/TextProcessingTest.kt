package it.lectio.bibbia.data

import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.repository.SearchRepository
import it.lectio.bibbia.data.source.InvalidSourceException
import it.lectio.bibbia.data.source.RawVerse
import it.lectio.bibbia.data.source.VplParser
import it.lectio.bibbia.domain.model.TextConventions
import it.lectio.bibbia.util.TextFolding
import it.lectio.bibbia.util.VerseText
import org.junit.Assert.assertThrows
import org.junit.Test

class TextProcessingTest {

    @Test
    fun `folding rimuove accenti, legature e punteggiatura`() {
        assertThat(TextFolding.fold("Città")).isEqualTo("citta")
        assertThat(TextFolding.fold("cælum et terram.")).isEqualTo("caelum et terram")
        assertThat(TextFolding.fold("dell’abisso, e  le tenebre")).isEqualTo("dell abisso e le tenebre")
        assertThat(TextFolding.fold("PERCHÉ")).isEqualTo("perche")
    }

    @Test
    fun `mappa degli indici del folding`() {
        val (folded, map) = TextFolding.foldWithMapping("Æra è")
        assertThat(folded).isEqualTo("aera e")
        assertThat(map.toList()).isEqualTo(listOf(0, 0, 1, 2, 3, 4))
    }

    @Test
    fun `espressione FTS sicura`() {
        assertThat(SearchRepository.buildMatchExpression("sete")).isEqualTo("sete*")
        assertThat(SearchRepository.buildMatchExpression("Gerusalemme  città")).isEqualTo("gerusalemme* citta*")
        assertThat(SearchRepository.buildMatchExpression("\"acqua viva\" Dio")).isEqualTo("\"acqua viva\" dio*")
        // Operatori e caratteri speciali dell'utente non arrivano a SQLite.
        assertThat(SearchRepository.buildMatchExpression("love OR -hate* NEAR(x)")).isEqualTo("love* or* hate* near* x*")
        assertThat(SearchRepository.buildMatchExpression("  ,;  ")).isNull()
        assertThat(SearchRepository.buildMatchExpression("")).isNull()
    }

    @Test
    fun `parser VPL converte i codici eBible e applica le convenzioni`() {
        val text = """
            GEN 1:1 In the beginning God created the heaven and the earth.
            JOH 3:16 ¶ For God so loved the world, that he gave [his] only begotten Son.
            SOL 1:1 The song of songs.

            PSA 41:2 [Quemadmodum desiderat cervus ad fontes aquarum,
        """.trimIndent()
        val kjv = mutableListOf<RawVerse>()
        val count = VplParser.parse(text.reader().buffered(), TextConventions(bracketsAreItalics = true, pilcrowMarksParagraph = true)) { kjv += it }
        assertThat(count).isEqualTo(4)
        assertThat(kjv.map { it.bookId }).containsExactly("GEN", "JHN", "SNG", "PSA").inOrder()
        assertThat(kjv[1].paragraphStart).isTrue()
        assertThat(kjv[1].text).isEqualTo("For God so loved the world, that he gave [his] only begotten Son.")
        assertThat(kjv[0].paragraphStart).isFalse()

        val latin = mutableListOf<RawVerse>()
        VplParser.parse(text.reader().buffered(), TextConventions(stripBrackets = true)) { latin += it }
        assertThat(latin.last().text).isEqualTo("Quemadmodum desiderat cervus ad fontes aquarum,")
    }

    @Test
    fun `parser VPL rifiuta righe malformate e ignora libri sconosciuti`() {
        assertThrows(InvalidSourceException::class.java) {
            VplParser.parse("questo non è VPL".reader().buffered(), TextConventions()) {}
        }
        val seen = mutableListOf<RawVerse>()
        VplParser.parse("XYZ 1:1 sconosciuto\nGEN 1:1 noto".reader().buffered(), TextConventions()) { seen += it }
        assertThat(seen.map { it.bookId }).containsExactly("GEN")
    }

    @Test
    fun `segmenti in corsivo KJV e anteprime`() {
        assertThat(VerseText.segments("that [it was] good", true))
            .containsExactly("that " to false, "it was" to true, " good" to false).inOrder()
        assertThat(VerseText.segments("that [it was] good", false)).containsExactly("that it was good" to false)
        val long = "parola ".repeat(60)
        val preview = VerseText.preview(long, 50)
        assertThat(preview.length).isAtMost(51)
        assertThat(preview).endsWith("…")
    }

    @Test
    fun `gzip riconosciuto dal contenuto, non dall estensione`() {
        val plain = "GEN 1:1 Testo".toByteArray()
        val zipped = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(plain) }
        }.toByteArray()
        assertThat(it.lectio.bibbia.data.source.maybeGunzip(zipped.inputStream()).readBytes()).isEqualTo(plain)
        assertThat(it.lectio.bibbia.data.source.maybeGunzip(plain.inputStream()).readBytes()).isEqualTo(plain)
    }
}
