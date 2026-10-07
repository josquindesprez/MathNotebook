package it.lectio.bibbia.data

import android.app.Application
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.repository.SearchRepository
import it.lectio.bibbia.data.repository.SearchScope
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.domain.model.VerseRef
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SearchRepositoryTest {

    private lateinit var db: BibleDatabase
    private lateinit var search: SearchRepository

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        val translations = TranslationRepository(db, FakeSourceFactory())
        translations.install("riveduta")
        translations.install("vulgata")
        translations.install("kjv")
        search = SearchRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `parola semplice in italiano`() = runTest {
        val results = search.search("sete", "riveduta")
        assertThat(results.hits.map { it.ref }).containsExactly(VerseRef("JHN", 4, 14))
        assertThat(results.totalCount).isEqualTo(1)
    }

    @Test
    fun `prefisso - assetata trova anche le forme flesse`() = runTest {
        val results = search.search("asset", "riveduta")
        assertThat(results.hits.map { it.ref }).containsExactly(VerseRef("PSA", 42, 2))
    }

    @Test
    fun `accenti e maiuscole indifferenti`() = runTest {
        assertThat(search.search("CITTA", "riveduta").hits.map { it.ref }).containsExactly(VerseRef("REV", 21, 2))
        assertThat(search.search("Gerusalemme", "riveduta").hits).hasSize(1)
        assertThat(search.search("creo", "riveduta").hits.map { it.ref }).containsExactly(VerseRef("GEN", 1, 1))
    }

    @Test
    fun `legature latine`() = runTest {
        assertThat(search.search("caelum", "vulgata").hits.map { it.ref }).containsExactly(VerseRef("GEN", 1, 1))
    }

    @Test
    fun `inglese - love trova love e loved, risultati in ordine canonico`() = runTest {
        val results = search.search("love", "kjv")
        assertThat(results.hits.map { it.ref }).containsExactly(VerseRef("JHN", 3, 16), VerseRef("1JN", 4, 8)).inOrder()
        assertThat(search.search("light", "kjv").hits.map { it.ref }).containsExactly(VerseRef("GEN", 1, 4))
    }

    @Test
    fun `parole multiple in AND e frase esatta`() = runTest {
        assertThat(search.search("principio Parola", "riveduta").hits.map { it.ref }).containsExactly(VerseRef("JHN", 1, 1))
        assertThat(search.search("\"anima mia\"", "riveduta").hits.map { it.ref })
            .containsExactly(VerseRef("PSA", 42, 1), VerseRef("PSA", 42, 2))
        assertThat(search.search("\"mia anima\"", "riveduta").hits).isEmpty()
    }

    @Test
    fun `filtro per testamento e per traduzione`() = runTest {
        val all = search.search("principio", "riveduta")
        assertThat(all.hits).hasSize(2)
        assertThat(search.search("principio", "riveduta", SearchScope.OLD_TESTAMENT).hits.map { it.ref.bookId }).containsExactly("GEN")
        assertThat(search.search("principio", "riveduta", SearchScope.NEW_TESTAMENT).hits.map { it.ref.bookId }).containsExactly("JHN")
        assertThat(search.search("principio", "kjv").hits).isEmpty()
    }

    @Test
    fun `limite risultati e conteggio totale`() = runTest {
        val results = search.search("e", "riveduta", limit = 2)
        assertThat(results.hits).hasSize(2)
        assertThat(results.totalCount).isGreaterThan(2)
    }

    @Test
    fun `query vuote o senza parole`() = runTest {
        assertThat(search.search("", "riveduta").hits).isEmpty()
        assertThat(search.search("  ?! ", "riveduta").hits).isEmpty()
        assertThat(search.search("nessunaparolacosì", "riveduta").hits).isEmpty()
    }

    @Test
    fun `ricerca su traduzione non installata`() = runTest {
        assertThat(search.search("sete", "diodati").hits).isEmpty()
    }
}
