package it.lectio.bibbia.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.data.repository.SearchRepository
import it.lectio.bibbia.data.repository.SearchScope
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.domain.model.SearchResults
import it.lectio.bibbia.domain.model.Translation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val translationId: String = TranslationCatalog.default.id,
    val scope: SearchScope = SearchScope.ALL,
    val installed: List<Translation> = emptyList(),
    val searching: Boolean = false,
    val results: SearchResults? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val search: SearchRepository,
    translations: TranslationRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    private val translationId = MutableStateFlow(savedStateHandle.get<String>(KEY_TRANSLATION))
    private val scope = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SCOPE)?.let { runCatching { SearchScope.valueOf(it) }.getOrNull() } ?: SearchScope.ALL,
    )

    private data class Request(val query: String, val translationId: String?, val scope: SearchScope)

    private sealed interface Outcome {
        data object Idle : Outcome
        data object Searching : Outcome
        data class Done(val results: SearchResults) : Outcome
    }

    private val outcome = combine(query, translationId, scope) { q, t, s -> Request(q.trim(), t, s) }
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { request ->
            flow {
                val tid = request.translationId
                if (request.query.length < MIN_QUERY_LENGTH || tid == null) {
                    emit(Outcome.Idle)
                } else {
                    emit(Outcome.Searching)
                    val results = runCatching { search.search(request.query, tid, request.scope) }
                        .getOrElse { SearchResults(request.query, emptyList(), 0) }
                    emit(Outcome.Done(results))
                }
            }
        }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        translationId,
        scope,
        translations.observeInstalled(),
        outcome,
    ) { q, t, s, installed, o ->
        SearchUiState(
            query = q,
            translationId = t ?: TranslationCatalog.default.id,
            scope = s,
            installed = installed,
            searching = o is Outcome.Searching,
            results = (o as? Outcome.Done)?.results,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState(query = query.value))

    init {
        if (translationId.value == null) {
            viewModelScope.launch {
                val id = settings.currentTranslationId.first() ?: TranslationCatalog.default.id
                setTranslation(id)
            }
        }
    }

    fun setQuery(text: String) {
        query.value = text
        savedStateHandle[KEY_QUERY] = text
    }

    fun setTranslation(id: String) {
        translationId.value = id
        savedStateHandle[KEY_TRANSLATION] = id
    }

    fun setScope(value: SearchScope) {
        scope.value = value
        savedStateHandle[KEY_SCOPE] = value.name
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val KEY_QUERY = "query"
        private const val KEY_TRANSLATION = "translation"
        private const val KEY_SCOPE = "scope"
    }
}
