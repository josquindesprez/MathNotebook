package it.lectio.bibbia.domain

import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.Versification
import org.junit.Test

class VersificationMapperTest {

    @Test
    fun `salmi ebraici verso vulgata`() {
        assertThat(VersificationMapper.hebrewToVulgatePsalm(23)).isEqualTo(22)
        assertThat(VersificationMapper.hebrewToVulgatePsalm(42)).isEqualTo(41)
        assertThat(VersificationMapper.hebrewToVulgatePsalm(51)).isEqualTo(50)
        assertThat(VersificationMapper.hebrewToVulgatePsalm(10)).isEqualTo(9)
        assertThat(VersificationMapper.hebrewToVulgatePsalm(1)).isEqualTo(1)
        assertThat(VersificationMapper.hebrewToVulgatePsalm(150)).isEqualTo(150)
    }

    @Test
    fun `andata e ritorno sui salmi non accorpati`() {
        for (p in (11..113) + (117..146)) {
            val v = VersificationMapper.hebrewToVulgatePsalm(p)
            assertThat(VersificationMapper.vulgateToHebrewPsalm(v)).isEqualTo(p)
        }
    }

    @Test
    fun `solo i salmi cambiano e solo fra sistemi diversi`() {
        val gen = ChapterRef("GEN", 1)
        assertThat(VersificationMapper.mapChapter(gen, Versification.STANDARD, Versification.VULGATE)).isEqualTo(gen)
        val ps = ChapterRef("PSA", 42)
        assertThat(VersificationMapper.mapChapter(ps, Versification.STANDARD, Versification.STANDARD)).isEqualTo(ps)
        assertThat(VersificationMapper.mapChapter(ps, Versification.STANDARD, Versification.VULGATE))
            .isEqualTo(ChapterRef("PSA", 41))
        assertThat(VersificationMapper.mapChapter(ChapterRef("PSA", 41), Versification.VULGATE, Versification.STANDARD))
            .isEqualTo(ChapterRef("PSA", 42))
    }
}
