package it.lectio.bibbia.data

import android.app.Application
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.repository.BookmarkRepository
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.HighlightColor
import it.lectio.bibbia.domain.model.VerseRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BookmarkRepositoryTest {

    private lateinit var db: BibleDatabase
    private lateinit var repo: BookmarkRepository
    private var now = 100L

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repo = BookmarkRepository(db, clock = { now++ })
    }

    @After
    fun tearDown() = db.close()

    private val psalm42 = ChapterRef("PSA", 42)

    @Test
    fun `aggiunta con anteprima, nota e data`() = runTest {
        val id = repo.add("riveduta", psalm42, 1, "Come la cerva agogna i rivi dell’acque", label = "  Sete di Dio ")
        val all = repo.observeAll().first()
        assertThat(all).hasSize(1)
        with(all.single()) {
            assertThat(this.id).isEqualTo(id)
            assertThat(verse).isEqualTo(1)
            assertThat(label).isEqualTo("Sete di Dio")
            assertThat(preview).startsWith("Come la cerva")
            assertThat(createdAt).isEqualTo(100L)
        }
    }

    @Test
    fun `nessun duplicato per lo stesso versetto`() = runTest {
        val a = repo.add("riveduta", psalm42, 1, "x")
        val b = repo.add("riveduta", psalm42, 1, "x")
        assertThat(a).isEqualTo(b)
        assertThat(repo.observeAll().first()).hasSize(1)
    }

    @Test
    fun `segnalibro di capitolo distinto da quello di versetto`() = runTest {
        repo.add("riveduta", psalm42, null, "capitolo")
        repo.add("riveduta", psalm42, 1, "versetto")
        assertThat(repo.find("riveduta", psalm42, null)?.preview).isEqualTo("capitolo")
        assertThat(repo.find("riveduta", psalm42, 1)?.preview).isEqualTo("versetto")
        assertThat(repo.observeForChapter("riveduta", psalm42).first()).hasSize(2)
        assertThat(repo.observeForChapter("kjv", psalm42).first()).isEmpty()
    }

    @Test
    fun `toggle aggiunge e rimuove`() = runTest {
        assertThat(repo.toggle("riveduta", psalm42, 2, "testo")).isTrue()
        assertThat(repo.toggle("riveduta", psalm42, 2, "testo")).isFalse()
        assertThat(repo.observeAll().first()).isEmpty()
    }

    @Test
    fun `ordinamento dal più recente, rinomina, elimina e ripristina`() = runTest {
        repo.add("riveduta", ChapterRef("PSA", 42), 1, "primo")
        repo.add("riveduta", ChapterRef("ISA", 40), 31, "secondo")
        repo.add("riveduta", ChapterRef("JHN", 1), 1, "terzo")
        val all = repo.observeAll().first()
        assertThat(all.map { it.bookId }).containsExactly("JHN", "ISA", "PSA").inOrder()

        repo.rename(all[1].id, "Ali d'aquila")
        assertThat(repo.observeAll().first()[1].label).isEqualTo("Ali d'aquila")
        repo.rename(all[1].id, "   ")
        assertThat(repo.observeAll().first()[1].label).isNull()

        val removed = all[0]
        repo.delete(removed.id)
        assertThat(repo.observeAll().first().map { it.bookId }).containsExactly("ISA", "PSA").inOrder()
        repo.restore(removed)
        assertThat(repo.observeAll().first().map { it.bookId }).containsExactly("JHN", "ISA", "PSA").inOrder()
    }

    @Test
    fun `evidenziazioni - imposta, cambia colore, rimuovi`() = runTest {
        val ref = VerseRef("PSA", 42, 1)
        repo.setHighlight("riveduta", ref, HighlightColor.OCHRE)
        repo.setHighlight("riveduta", ref, HighlightColor.SAGE)
        val highlights = repo.observeHighlights("riveduta", psalm42).first()
        assertThat(highlights.single().color).isEqualTo(HighlightColor.SAGE)
        repo.setHighlight("riveduta", ref, null)
        assertThat(repo.observeHighlights("riveduta", psalm42).first()).isEmpty()
    }
}
