package it.lectio.bibbia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.data.repository.BookmarkRepository
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.domain.ReferenceParser
import it.lectio.bibbia.domain.model.Bookmark
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.ReadingPosition
import it.lectio.bibbia.domain.model.TranslationOrigin
import it.lectio.bibbia.domain.model.TranslationState
import it.lectio.bibbia.navigation.ReaderRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loaded: Boolean = false,
    val lastPosition: ReadingPosition? = null,
    val currentTranslationId: String = TranslationCatalog.default.id,
    /** Traduzioni incluse (sempre mostrate) + quelle scaricate. */
    val translations: List<TranslationState> = emptyList(),
    val recentBookmarks: List<Bookmark> = emptyList(),
    val bookmarkCount: Int = 0,
) {
    /** Avanzamento complessivo della preparazione iniziale dei testi, o null se completata. */
    val preparation: Float?
        get() {
            val bundled = translations.filter { it.translation.origin is TranslationOrigin.Bundled }
            if (bundled.isEmpty() || bundled.all { it.status == InstallStatus.INSTALLED }) return null
            return bundled.sumOf { s ->
                when (s.status) {
                    InstallStatus.INSTALLED -> 1.0
                    InstallStatus.INSTALLING -> s.progress.toDouble()
                    else -> 0.0
                }
            }.toFloat() / bundled.size
        }
}

class HomeViewModel(
    private val translations: TranslationRepository,
    private val settings: SettingsRepository,
    bookmarks: BookmarkRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        settings.lastPosition,
        settings.currentTranslationId,
        translations.observeStates(),
        bookmarks.observeAll(),
    ) { last, current, states, allBookmarks ->
        HomeUiState(
            loaded = true,
            lastPosition = last,
            currentTranslationId = current ?: last?.translationId ?: TranslationCatalog.default.id,
            translations = states.filter {
                it.translation.origin is TranslationOrigin.Bundled || it.status == InstallStatus.INSTALLED
            },
            recentBookmarks = allBookmarks.take(3),
            bookmarkCount = allBookmarks.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Imposta la traduzione di lettura. */
    fun selectTranslation(id: String) {
        viewModelScope.launch { settings.setCurrentTranslation(id) }
    }

    /** Converte un riferimento scritto in una destinazione di lettura, o null se non riconosciuto. */
    fun routeFor(reference: String): ReaderRoute? {
        val parsed = ReferenceParser.parse(reference) ?: return null
        return ReaderRoute(
            translationId = uiState.value.currentTranslationId,
            bookId = parsed.bookId,
            chapter = parsed.chapter,
            verse = parsed.verse ?: 0,
        )
    }

    fun retry(id: String) {
        viewModelScope.launch { translations.install(id) }
    }
}
