package it.lectio.bibbia.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.SearchRepository
import it.lectio.bibbia.data.repository.SearchScope
import it.lectio.bibbia.domain.model.SearchHit
import it.lectio.bibbia.navigation.ReaderRoute
import it.lectio.bibbia.ui.ReferenceFormatter
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.BibbiaTopBar
import it.lectio.bibbia.ui.components.EmptyState
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.theme.BibbiaTheme
import it.lectio.bibbia.util.TextFolding
import it.lectio.bibbia.util.VerseText

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (ReaderRoute) -> Unit) {
    val viewModel = appViewModel { c, handle ->
        SearchViewModel(handle, c.searchRepository, c.translationRepository, c.settingsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = BibbiaTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { if (state.query.isEmpty()) focus.requestFocus() }

    Scaffold(
        containerColor = colors.paper,
        topBar = { BibbiaTopBar(stringResource(R.string.search), onBack) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = ReadingMaxWidth)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.inkFaint) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.clear), tint = colors.inkFaint)
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_hint), color = colors.inkFaint) },
                    textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.rubric,
                        unfocusedBorderColor = colors.hairline,
                        cursorColor = colors.rubric,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageMargin)
                        .focusRequester(focus),
                )
                Spacer(Modifier.height(12.dp))
                // Filtri: traduzione e ambito
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PageMargin),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.installed, key = { it.id }) { t ->
                        FilterPill(t.abbreviation, t.id == state.translationId) { viewModel.setTranslation(t.id) }
                    }
                    item { Spacer(Modifier.padding(horizontal = 4.dp)) }
                    item {
                        FilterPill(stringResource(R.string.scope_all), state.scope == SearchScope.ALL) {
                            viewModel.setScope(SearchScope.ALL)
                        }
                    }
                    item {
                        FilterPill(stringResource(R.string.scope_ot), state.scope == SearchScope.OLD_TESTAMENT) {
                            viewModel.setScope(SearchScope.OLD_TESTAMENT)
                        }
                    }
                    item {
                        FilterPill(stringResource(R.string.scope_nt), state.scope == SearchScope.NEW_TESTAMENT) {
                            viewModel.setScope(SearchScope.NEW_TESTAMENT)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.searching) {
                    LinearProgressIndicator(
                        color = colors.rubric,
                        trackColor = colors.hairline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                    )
                } else {
                    Hairline()
                }

                val results = state.results
                when {
                    state.installed.isEmpty() -> EmptyState(
                        stringResource(R.string.search_no_texts_title),
                        stringResource(R.string.search_no_texts_message),
                    )
                    results == null -> if (!state.searching) {
                        EmptyState(stringResource(R.string.search_intro_title), stringResource(R.string.search_intro_message))
                    }
                    results.hits.isEmpty() -> EmptyState(
                        stringResource(R.string.search_no_results_title),
                        stringResource(R.string.search_no_results_message),
                    )
                    else -> {
                        val terms = remember(results.query) { SearchRepository.highlightTerms(results.query) }
                        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                            item {
                                Text(
                                    text = if (results.totalCount > results.hits.size) {
                                        stringResource(R.string.search_count_limited, results.hits.size, results.totalCount)
                                    } else {
                                        pluralStringResource(R.plurals.search_count, results.totalCount, results.totalCount)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.inkMuted,
                                    modifier = Modifier.padding(horizontal = PageMargin, vertical = 12.dp),
                                )
                            }
                            items(results.hits, key = { "${it.ref.bookId}-${it.ref.chapter}-${it.ref.verse}" }) { hit ->
                                SearchHitRow(hit, terms) {
                                    onOpen(ReaderRoute(hit.translationId, hit.ref.bookId, hit.ref.chapter, hit.ref.verse))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = BibbiaTheme.colors
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.rubric else colors.inkMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(
                BorderStroke(if (selected) 1.dp else 0.6.dp, if (selected) colors.rubric else colors.hairline),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun SearchHitRow(hit: SearchHit, terms: List<String>, onClick: () -> Unit) {
    val colors = BibbiaTheme.colors
    val text = remember(hit, terms) {
        highlightMatches(VerseText.plain(hit.text), terms, SpanStyle(color = colors.rubric, fontWeight = FontWeight.SemiBold))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PageMargin, vertical = 12.dp),
    ) {
        Text(
            ReferenceFormatter.format(hit.ref.bookId, hit.ref.chapter, hit.ref.verse, hit.translationId),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Evidenzia nel testo originale le parole che iniziano con uno dei termini cercati, confrontando
 * le forme normalizzate (senza accenti, æ → ae).
 */
fun highlightMatches(text: String, terms: List<String>, style: SpanStyle): AnnotatedString {
    if (terms.isEmpty()) return AnnotatedString(text)
    val (folded, map) = TextFolding.foldWithMapping(text)
    return buildAnnotatedString {
        append(text)
        var i = 0
        while (i < folded.length) {
            val atWordStart = folded[i].isLetterOrDigit() && (i == 0 || !folded[i - 1].isLetterOrDigit())
            if (atWordStart) {
                val term = terms.firstOrNull { folded.startsWith(it, i) }
                if (term != null) {
                    var end = i + term.length
                    while (end < folded.length && folded[end].isLetterOrDigit()) end++
                    val startOrig = map[i]
                    val endOrig = map[end - 1] + 1
                    addStyle(style, startOrig, endOrig)
                    i = end
                    continue
                }
            }
            i++
        }
    }
}
