package it.lectio.bibbia.ui

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.FakeSourceFactory
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.inMemoryDatabase
import it.lectio.bibbia.data.repository.BibleRepository
import it.lectio.bibbia.data.repository.BookmarkRepository
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.HighlightColor
import it.lectio.bibbia.domain.model.ReadingPosition
import it.lectio.bibbia.navigation.ReaderRoute
import it.lectio.bibbia.ui.reader.ReaderUiState
import it.lectio.bibbia.ui.reader.ReaderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lettura: navigazione fra capitoli, "vai a", cambio traduzione, salvataggio automatico
 * della posizione e ripristino dopo una ricreazione (rotazione / morte del processo).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReaderViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: BibleDatabase
    private lateinit var translations: TranslationRepository
    private lateinit var bible: BibleRepository
    private lateinit var bookmarks: BookmarkRepository
    private lateinit var settings: SettingsRepository
    private lateinit var storeScope: CoroutineScope
    private val collectors = mutableListOf<Job>()
    private val viewModels = mutableListOf<ReaderViewModel>()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = inMemoryDatabase()
        translations = TranslationRepository(db, FakeSourceFactory())
        translations.install("riveduta")
        translations.install("vulgata")
        bible = BibleRepository(db, translations)
        bookmarks = BookmarkRepository(db)
        storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { folder.newFile("s.preferences_pb").also { it.delete() } })
    }

    @After
    fun tearDown() {
        collectors.forEach { it.cancel() }
        viewModels.forEach { it.viewModelScope.cancel() }
        Thread.sleep(50) // lascia terminare le continuazioni già in volo
        storeScope.cancel()
        db.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(route: ReaderRoute, handle: SavedStateHandle = SavedStateHandle()): ReaderViewModel {
        val vm = ReaderViewModel(handle, route, translations, bible, bookmarks, settings)
        viewModels += vm
        // uiState è WhileSubscribed: lo si mantiene attivo come farebbe la UI.
        collectors += CoroutineScope(Dispatchers.Unconfined).launch { vm.uiState.collect {} }
        return vm
    }

    private fun ReaderViewModel.await(predicate: (ReaderUiState) -> Boolean): ReaderUiState =
        runBlocking { withTimeout(5_000) { uiState.first(predicate) } }

    @Test
    fun `apre il riferimento richiesto e calcola precedente e successivo`() {
        val vm = viewModel(ReaderRoute("riveduta", "PSA", 42, 1))
        val state = vm.await { it.versesLoaded && it.books.isNotEmpty() }
        assertThat(state.book?.name).isEqualTo("Salmi")
        assertThat(state.verses.map { it.number }).containsExactly(1, 2).inOrder()
        assertThat(state.previous).isEqualTo(ChapterRef("PSA", 41))
        assertThat(state.next).isEqualTo(ChapterRef("PSA", 43))
    }

    @Test
    fun `capitolo successivo attraverso il confine del libro`() {
        val vm = viewModel(ReaderRoute("riveduta", "GEN", 2))
        vm.await { it.books.isNotEmpty() && it.next != null }
        vm.goToNext()
        val state = vm.await { it.location?.bookId == "EXO" && it.versesLoaded }
        assertThat(state.chapter).isEqualTo(1)
        assertThat(state.verses.single().text).contains("figliuoli d’Israele")
        vm.goToPrevious()
        vm.await { it.location?.chapterRef == ChapterRef("GEN", 2) }
    }

    @Test
    fun `vai a riferimento`() {
        val vm = viewModel(ReaderRoute("riveduta", "GEN", 1))
        vm.await { it.versesLoaded }
        assertThat(vm.goToReference("Gv 3,16")).isTrue()
        assertThat(vm.scrollRequest.value?.verse).isEqualTo(16)
        vm.await { it.location?.chapterRef == ChapterRef("JHN", 3) && it.versesLoaded }
        assertThat(vm.goToReference("nonsense 9")).isFalse()
    }

    @Test
    fun `cambio traduzione converte la numerazione dei salmi`() {
        val vm = viewModel(ReaderRoute("riveduta", "PSA", 42))
        vm.await { it.versesLoaded }
        vm.switchTranslation("vulgata")
        val state = vm.await { it.location?.translationId == "vulgata" && it.versesLoaded }
        assertThat(state.chapter).isEqualTo(41)
        assertThat(state.verses.map { it.number }).containsExactly(1, 2)
        assertThat(state.book?.name).isEqualTo("Psalmi")
    }

    @Test
    fun `libro assente nella traduzione - si ripiega su un capitolo valido`() {
        val vm = viewModel(ReaderRoute("riveduta", "TOB", 1))
        val state = vm.await { it.location?.bookId != "TOB" && it.versesLoaded }
        assertThat(state.location?.bookId).isEqualTo("GEN")
    }

    @Test
    fun `la posizione viene salvata automaticamente`() {
        val vm = viewModel(ReaderRoute("riveduta", "ISA", 40, 31))
        vm.await { it.versesLoaded }
        val saved = runBlocking { withTimeout(5_000) { settings.lastPosition.first { it?.bookId == "ISA" } } }
        assertThat(saved).isEqualTo(ReadingPosition("riveduta", "ISA", 40, 31))
    }

    @Test
    fun `senza parametri riprende dall ultima posizione`() {
        runBlocking { settings.savePosition(ReadingPosition("riveduta", "JHN", 4, 14)) }
        val vm = viewModel(ReaderRoute())
        val state = vm.await { it.location != null && it.versesLoaded }
        assertThat(state.location?.chapterRef).isEqualTo(ChapterRef("JHN", 4))
        assertThat(vm.scrollRequest.value?.verse).isEqualTo(14)
    }

    @Test
    fun `ripristino dopo rotazione o morte del processo`() {
        val handle = SavedStateHandle()
        val first = viewModel(ReaderRoute("riveduta", "GEN", 1), handle)
        first.await { it.versesLoaded }
        first.goTo(ChapterRef("PSA", 137))
        first.await { it.location?.chapter == 137 }

        // Nuovo ViewModel con lo stesso SavedStateHandle e la rotta originale.
        val second = viewModel(ReaderRoute("riveduta", "GEN", 1), handle)
        val state = second.await { it.versesLoaded }
        assertThat(state.location?.chapterRef).isEqualTo(ChapterRef("PSA", 137))
    }

    @Test
    fun `segnalibro e evidenziazione dal lettore`() {
        val vm = viewModel(ReaderRoute("riveduta", "PSA", 42))
        vm.await { it.versesLoaded }
        vm.toggleVerseBookmark(1)
        vm.await { 1 in it.bookmarkedVerses }
        vm.toggleChapterBookmark()
        vm.await { it.chapterBookmarkId != null }
        vm.setHighlight(2, HighlightColor.SKY)
        vm.await { it.highlights[2] == HighlightColor.SKY }
        vm.toggleVerseBookmark(1)
        vm.await { it.bookmarkedVerses.isEmpty() }
    }

    @Test
    fun `traduzione non installata - stato non pronto`() {
        val vm = viewModel(ReaderRoute("diodati", "GEN", 1))
        val state = vm.await { it.location != null }
        assertThat(state.isReady).isFalse()
        assertThat(state.verses).isEmpty()
    }
}
