package it.lectio.bibbia.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.BookmarkRepository
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.domain.model.Bookmark
import it.lectio.bibbia.navigation.ReaderRoute
import it.lectio.bibbia.ui.ReferenceFormatter
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.BibbiaTopBar
import it.lectio.bibbia.ui.components.EmptyState
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.formatDate
import it.lectio.bibbia.ui.reader.BookmarkLabelDialog
import it.lectio.bibbia.ui.theme.BibbiaTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookmarksUiState(val loaded: Boolean = false, val bookmarks: List<Bookmark> = emptyList())

class BookmarksViewModel(private val repository: BookmarkRepository) : ViewModel() {
    val uiState: StateFlow<BookmarksUiState> = repository.observeAll()
        .map { BookmarksUiState(true, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun delete(bookmark: Bookmark) {
        viewModelScope.launch { repository.delete(bookmark.id) }
    }

    fun restore(bookmark: Bookmark) {
        viewModelScope.launch { repository.restore(bookmark) }
    }

    fun rename(id: Long, label: String) {
        viewModelScope.launch { repository.rename(id, label) }
    }
}

@Composable
fun BookmarksScreen(onBack: () -> Unit, onOpen: (ReaderRoute) -> Unit) {
    val viewModel = appViewModel { c, _ -> BookmarksViewModel(c.bookmarkRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = BibbiaTheme.colors
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    val deletedMessage = stringResource(R.string.bookmark_deleted)
    val undo = stringResource(R.string.undo)

    Scaffold(
        containerColor = colors.paper,
        topBar = { BibbiaTopBar(stringResource(R.string.bookmarks), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.loaded && state.bookmarks.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.bookmarks_empty_title),
                    message = stringResource(R.string.bookmarks_empty_message),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = ReadingMaxWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(state.bookmarks, key = { it.id }) { bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            onOpen = {
                                onOpen(ReaderRoute(bookmark.translationId, bookmark.bookId, bookmark.chapter, bookmark.verse ?: 0))
                            },
                            onEdit = { editing = bookmark.id },
                            onDelete = {
                                viewModel.delete(bookmark)
                                scope.launch {
                                    val result = snackbar.showSnackbar(deletedMessage, actionLabel = undo)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(bookmark)
                                }
                            },
                        )
                        Hairline(Modifier.padding(horizontal = PageMargin))
                    }
                }
            }
        }
    }

    editing?.let { id ->
        val bookmark = state.bookmarks.firstOrNull { it.id == id }
        BookmarkLabelDialog(
            initial = bookmark?.label.orEmpty(),
            onDismiss = { editing = null },
            onSave = { label ->
                viewModel.rename(id, label)
                editing = null
            },
        )
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = BibbiaTheme.colors
    var menu by remember { mutableStateOf(false) }
    val translation = TranslationCatalog.find(bookmark.translationId)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = PageMargin, end = 8.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("★", color = colors.rubric, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    ReferenceFormatter.format(bookmark.bookId, bookmark.chapter, bookmark.verse, bookmark.translationId),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                )
            }
            bookmark.label?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    "“$it”",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = colors.rubric,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                bookmark.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(translation?.abbreviation, formatDate(bookmark.createdAt)).joinToString("  ·  ").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
                color = colors.inkFaint,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Outlined.MoreVert, stringResource(R.string.options), tint = colors.inkFaint)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surfaceRaised) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (bookmark.label == null) R.string.add_note else R.string.edit_note)) },
                    onClick = {
                        menu = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = colors.rubric) },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
        }
    }
}
