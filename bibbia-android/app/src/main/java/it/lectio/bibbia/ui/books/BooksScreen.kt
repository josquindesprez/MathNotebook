package it.lectio.bibbia.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.BibleRepository
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.ReadingPosition
import it.lectio.bibbia.domain.model.Testament
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.BibbiaTopBar
import it.lectio.bibbia.ui.components.EmptyState
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.theme.BibbiaTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BooksUiState(
    val loaded: Boolean = false,
    val translationId: String = TranslationCatalog.default.id,
    val books: List<Book> = emptyList(),
    val lastPosition: ReadingPosition? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BooksViewModel(bible: BibleRepository, settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<BooksUiState> = settings.currentTranslationId
        .map { it ?: TranslationCatalog.default.id }
        .flatMapLatest { id ->
            combine(bible.observeBooks(id), settings.lastPosition) { books, last ->
                BooksUiState(true, id, books, last?.takeIf { it.translationId == id })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BooksUiState())
}

@Composable
fun BooksScreen(
    testament: Testament,
    onBack: () -> Unit,
    onChoose: (translationId: String, ref: ChapterRef) -> Unit,
) {
    val viewModel = appViewModel { c, _ -> BooksViewModel(c.bibleRepository, c.settingsRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = BibbiaTheme.colors
    Scaffold(
        containerColor = colors.paper,
        topBar = {
            BibbiaTopBar(
                title = stringResource(if (testament == Testament.OLD) R.string.old_testament else R.string.new_testament),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = ReadingMaxWidth)) {
                if (state.loaded && state.books.isEmpty()) {
                    EmptyState(stringResource(R.string.books_unavailable_title), stringResource(R.string.books_unavailable_message))
                } else {
                    BookChapterChooser(
                        books = state.books,
                        current = state.lastPosition?.let { ChapterRef(it.bookId, it.chapter) },
                        testament = testament,
                        onChoose = { ref -> onChoose(state.translationId, ref) },
                    )
                }
            }
        }
    }
}
