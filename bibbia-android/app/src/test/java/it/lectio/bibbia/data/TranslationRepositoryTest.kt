package it.lectio.bibbia.data

import android.app.Application
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.repository.BibleRepository
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.data.source.SourceException
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.InstallError
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.VerseRef
import it.lectio.bibbia.data.source.TranslationCatalog
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
class TranslationRepositoryTest {

    private lateinit var db: BibleDatabase
    private lateinit var sources: FakeSourceFactory
    private lateinit var translations: TranslationRepository
    private lateinit var bible: BibleRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        sources = FakeSourceFactory()
        translations = TranslationRepository(db, sources, clock = { 1_000L })
        bible = BibleRepository(db, translations)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `database vuoto - nessuna traduzione installata, nessun libro`() = runTest {
        val states = translations.observeStates().first()
        assertThat(states.map { it.status }.toSet()).containsExactly(InstallStatus.NOT_INSTALLED)
        assertThat(bible.getBooks("riveduta")).isEmpty()
        assertThat(bible.getChapter("riveduta", ChapterRef("GEN", 1))).isEmpty()
        assertThat(bible.getVerse("riveduta", VerseRef("JHN", 3, 16))).isNull()
    }

    @Test
    fun `installazione crea libri ordinati, capitoli e versetti`() = runTest {
        val result = translations.install("riveduta")
        assertThat(result.getOrThrow()).isEqualTo(12)

        val books = bible.getBooks("riveduta")
        assertThat(books.map { it.id }).containsExactly("GEN", "EXO", "PSA", "ISA", "JHN", "REV").inOrder()
        assertThat(books.first().name).isEqualTo("Genesi")
        assertThat(books.first { it.id == "PSA" }.chapterCount).isEqualTo(137)

        val chapter = bible.getChapter("riveduta", ChapterRef("PSA", 42))
        assertThat(chapter.map { it.number }).containsExactly(1, 2).inOrder()

        val state = translations.observeStates().first().first { it.translation.id == "riveduta" }
        assertThat(state.status).isEqualTo(InstallStatus.INSTALLED)
        assertThat(state.verseCount).isEqualTo(12)
        assertThat(state.installedAt).isEqualTo(1_000L)
    }

    @Test
    fun `ordine canonico della Vulgata con deuterocanonici`() = runTest {
        translations.install("vulgata")
        val books = bible.getBooks("vulgata")
        assertThat(books.map { it.id }).containsExactly("GEN", "TOB", "PSA", "JHN").inOrder()
        assertThat(books.first { it.id == "PSA" }.name).isEqualTo("Psalmi")
        // Le parentesi editoriali sono rimosse.
        assertThat(bible.getVerse("vulgata", VerseRef("PSA", 41, 2))?.text).startsWith("Quemadmodum")
    }

    @Test
    fun `errore di rete - stato FAILED, nessun dato parziale, altre traduzioni intatte`() = runTest {
        translations.install("riveduta")
        sources.failures["diodati"] = SourceException(InstallError.NO_CONNECTION, "offline")

        val result = translations.install("diodati")

        assertThat(result.isFailure).isTrue()
        val states = translations.observeStates().first().associateBy { it.translation.id }
        assertThat(states.getValue("diodati").status).isEqualTo(InstallStatus.FAILED)
        assertThat(states.getValue("diodati").error).isEqualTo(InstallError.NO_CONNECTION)
        assertThat(bible.getBooks("diodati")).isEmpty()
        // La lettura offline della traduzione già installata continua a funzionare.
        assertThat(states.getValue("riveduta").status).isEqualTo(InstallStatus.INSTALLED)
        assertThat(bible.getChapter("riveduta", ChapterRef("JHN", 3))).hasSize(1)
    }

    @Test
    fun `testo corrotto a metà - transazione annullata`() = runTest {
        sources.setText("kjv", "GEN 1:1 In the beginning.\nriga non valida\n")
        val result = translations.install("kjv")
        assertThat(result.isFailure).isTrue()
        assertThat(db.verseDao().count("kjv")).isEqualTo(0)
        val state = translations.observeStates().first().first { it.translation.id == "kjv" }
        assertThat(state.error).isEqualTo(InstallError.INVALID_DATA)
    }

    @Test
    fun `testo vuoto - errore dati non validi`() = runTest {
        sources.setText("kjv", "\n\n")
        assertThat(translations.install("kjv").isFailure).isTrue()
        val state = translations.observeStates().first().first { it.translation.id == "kjv" }
        assertThat(state.status).isEqualTo(InstallStatus.FAILED)
        assertThat(state.error).isEqualTo(InstallError.INVALID_DATA)
    }

    @Test
    fun `riprova dopo un errore riesce`() = runTest {
        sources.failures["kjv"] = SourceException(InstallError.TIMEOUT, "lento")
        assertThat(translations.install("kjv").isFailure).isTrue()
        sources.failures.clear()
        assertThat(translations.install("kjv").isSuccess).isTrue()
        assertThat(translations.isInstalled("kjv")).isTrue()
    }

    @Test
    fun `installazione idempotente e ripresa delle traduzioni incluse`() = runTest {
        translations.install("riveduta")
        val openedAfterFirst = sources.opened
        translations.install("riveduta")
        assertThat(sources.opened).isEqualTo(openedAfterFirst)

        translations.ensureBundledInstalled()
        TranslationCatalog.bundled.forEach { assertThat(translations.isInstalled(it.id)).isTrue() }
        assertThat(translations.isInstalled("diodati")).isFalse() // scaricabile: mai senza richiesta esplicita
    }

    @Test
    fun `KJV - paragrafi e corsivi conservati`() = runTest {
        translations.install("kjv")
        val john = bible.getVerse("kjv", VerseRef("JHN", 3, 16))!!
        assertThat(john.paragraphStart).isTrue()
        assertThat(john.text).doesNotContain("¶")
        assertThat(bible.getVerse("kjv", VerseRef("GEN", 1, 4))!!.text).contains("[it was]")
    }

    @Test
    fun `confronto fra traduzioni con numerazione dei salmi`() = runTest {
        translations.install("riveduta")
        translations.install("vulgata")
        translations.install("kjv")
        val targets = listOf(TranslationCatalog.KJV, TranslationCatalog.RIVEDUTA, TranslationCatalog.VULGATA)
        val compared = bible.compare("riveduta", VerseRef("PSA", 42, 1), targets).associateBy { it.translation.id }
        assertThat(compared.getValue("kjv").text).contains("As the hart")
        assertThat(compared.getValue("riveduta").text).contains("Come la cerva")
        assertThat(compared.getValue("vulgata").ref).isEqualTo(VerseRef("PSA", 41, 1))
        assertThat(compared.getValue("vulgata").text).isNotNull()

        val john = bible.compare("riveduta", VerseRef("JHN", 3, 16), targets).associateBy { it.translation.id }
        assertThat(john.getValue("vulgata").text).startsWith("Sic enim Deus")
    }

    @Test
    fun `rimozione di una traduzione scaricata`() = runTest {
        sources.setText("diodati", TestTexts.RIVEDUTA)
        translations.install("diodati")
        assertThat(translations.isInstalled("diodati")).isTrue()
        translations.uninstall("diodati")
        assertThat(translations.isInstalled("diodati")).isFalse()
        assertThat(db.verseDao().count("diodati")).isEqualTo(0)
    }
}
