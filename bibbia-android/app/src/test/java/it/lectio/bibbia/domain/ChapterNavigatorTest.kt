package it.lectio.bibbia.domain

import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.ChapterRef
import org.junit.Test

class ChapterNavigatorTest {

    private fun book(id: String, position: Int, chapters: Int) =
        Book("t", Canon.requireBook(id), id, position, chapters)

    private val navigator = ChapterNavigator(
        // Volutamente non ordinati: l'ordine è dato da position.
        listOf(book("MAT", 3, 28), book("GEN", 0, 50), book("MAL", 2, 4), book("EXO", 1, 40)),
    )

    @Test
    fun `capitolo successivo e precedente nello stesso libro`() {
        assertThat(navigator.next(ChapterRef("GEN", 1))).isEqualTo(ChapterRef("GEN", 2))
        assertThat(navigator.previous(ChapterRef("EXO", 20))).isEqualTo(ChapterRef("EXO", 19))
    }

    @Test
    fun `attraversa i confini dei libri`() {
        assertThat(navigator.next(ChapterRef("GEN", 50))).isEqualTo(ChapterRef("EXO", 1))
        assertThat(navigator.previous(ChapterRef("EXO", 1))).isEqualTo(ChapterRef("GEN", 50))
        assertThat(navigator.next(ChapterRef("MAL", 4))).isEqualTo(ChapterRef("MAT", 1))
        assertThat(navigator.previous(ChapterRef("MAT", 1))).isEqualTo(ChapterRef("MAL", 4))
    }

    @Test
    fun `inizio e fine della Bibbia`() {
        assertThat(navigator.previous(ChapterRef("GEN", 1))).isNull()
        assertThat(navigator.next(ChapterRef("MAT", 28))).isNull()
    }

    @Test
    fun `libro assente o capitolo fuori limite`() {
        assertThat(navigator.next(ChapterRef("TOB", 1))).isNull()
        assertThat(navigator.contains(ChapterRef("GEN", 51))).isFalse()
        assertThat(navigator.coerce(ChapterRef("GEN", 51))).isEqualTo(ChapterRef("GEN", 50))
        assertThat(navigator.coerce(ChapterRef("TOB", 3))).isEqualTo(ChapterRef("GEN", 1))
    }

    @Test
    fun `navigatore vuoto`() {
        val empty = ChapterNavigator(emptyList())
        assertThat(empty.isEmpty).isTrue()
        assertThat(empty.next(ChapterRef("GEN", 1))).isNull()
        assertThat(empty.coerce(ChapterRef("GEN", 1))).isNull()
    }
}
