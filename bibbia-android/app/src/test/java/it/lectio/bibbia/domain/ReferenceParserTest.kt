package it.lectio.bibbia.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReferenceParserTest {

    private fun parse(s: String) = ReferenceParser.parse(s)

    @Test
    fun `nome completo con capitolo e versetto`() {
        assertThat(parse("Giovanni 3:16")).isEqualTo(ParsedReference("JHN", 3, 16))
    }

    @Test
    fun `virgola italiana e abbreviazioni CEI`() {
        assertThat(parse("Gv 3,16")).isEqualTo(ParsedReference("JHN", 3, 16))
        assertThat(parse("Sal 23")).isEqualTo(ParsedReference("PSA", 23))
        assertThat(parse("Mt 5, 3")).isEqualTo(ParsedReference("MAT", 5, 3))
        assertThat(parse("Mc 1,1")).isEqualTo(ParsedReference("MRK", 1, 1))
        assertThat(parse("Gn 1")).isEqualTo(ParsedReference("JON", 1)) // CEI: Gn = Giona
        assertThat(parse("Qo 3,1")).isEqualTo(ParsedReference("ECC", 3, 1))
        assertThat(parse("Ap 21")).isEqualTo(ParsedReference("REV", 21))
    }

    @Test
    fun `solo capitolo`() {
        assertThat(parse("Salmi 137")).isEqualTo(ParsedReference("PSA", 137))
        assertThat(parse("Isaia 40")).isEqualTo(ParsedReference("ISA", 40))
        assertThat(parse("Salmo 42")).isEqualTo(ParsedReference("PSA", 42))
    }

    @Test
    fun `solo libro porta al capitolo 1`() {
        assertThat(parse("Isaia")).isEqualTo(ParsedReference("ISA", 1))
        assertThat(parse("Apocalisse")).isEqualTo(ParsedReference("REV", 1))
    }

    @Test
    fun `libri numerati in varie forme`() {
        assertThat(parse("1 Cor 13,4-7")).isEqualTo(ParsedReference("1CO", 13, 4, 7))
        assertThat(parse("1Gv 4:8")).isEqualTo(ParsedReference("1JN", 4, 8))
        assertThat(parse("I Giovanni 4")).isEqualTo(ParsedReference("1JN", 4))
        assertThat(parse("Prima Corinzi 13")).isEqualTo(ParsedReference("1CO", 13))
        assertThat(parse("2 Re 2")).isEqualTo(ParsedReference("2KI", 2))
        assertThat(parse("1 Samuele 3,10")).isEqualTo(ParsedReference("1SA", 3, 10))
    }

    @Test
    fun `inglese e latino`() {
        assertThat(parse("John 3:16")).isEqualTo(ParsedReference("JHN", 3, 16))
        assertThat(parse("Psalm 23")).isEqualTo(ParsedReference("PSA", 23))
        assertThat(parse("Ps 23:1")).isEqualTo(ParsedReference("PSA", 23, 1))
        assertThat(parse("1 Kings 19")).isEqualTo(ParsedReference("1KI", 19))
        assertThat(parse("Rev 22:20")).isEqualTo(ParsedReference("REV", 22, 20))
        assertThat(parse("Psalmi 41")).isEqualTo(ParsedReference("PSA", 41))
        assertThat(parse("III Regum 19")).isEqualTo(ParsedReference("1KI", 19))
        assertThat(parse("Romanos 8")).isEqualTo(ParsedReference("ROM", 8))
    }

    @Test
    fun `accenti, maiuscole, punti e spazi sono indifferenti`() {
        assertThat(parse("giosue 1")).isEqualTo(ParsedReference("JOS", 1))
        assertThat(parse("GIOSUÈ 1")).isEqualTo(ParsedReference("JOS", 1))
        assertThat(parse("  gv.3.16  ")).isEqualTo(ParsedReference("JHN", 3, 16))
        assertThat(parse("Cantico dei Cantici 2")).isEqualTo(ParsedReference("SNG", 2))
        assertThat(parse("Gen 1:1–5")).isEqualTo(ParsedReference("GEN", 1, 1, 5))
    }

    @Test
    fun `prefisso univoco di un nome`() {
        assertThat(parse("Deuter 6")).isEqualTo(ParsedReference("DEU", 6))
        assertThat(parse("Lament 3")).isEqualTo(ParsedReference("LAM", 3))
    }

    @Test
    fun `input non validi`() {
        assertThat(parse("")).isNull()
        assertThat(parse("   ")).isNull()
        assertThat(parse("3:16")).isNull()
        assertThat(parse("Pippo 3")).isNull()
        assertThat(parse("Gio 3")).isNull() // ambiguo: Giosuè, Giobbe, Gioele, Giona, Giovanni
        assertThat(parse("Giovanni 0")).isNull()
    }

    @Test
    fun `intervallo invertito viene ignorato`() {
        assertThat(parse("Gv 3,16-10")).isEqualTo(ParsedReference("JHN", 3, 16, null))
    }
}
