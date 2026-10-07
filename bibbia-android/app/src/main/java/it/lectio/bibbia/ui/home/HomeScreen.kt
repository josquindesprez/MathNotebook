package it.lectio.bibbia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lectio.bibbia.R
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.Testament
import it.lectio.bibbia.domain.model.TranslationState
import it.lectio.bibbia.navigation.ReaderRoute
import it.lectio.bibbia.ui.ReferenceFormatter
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.components.SectionLabel
import it.lectio.bibbia.ui.reader.GoToReferenceField
import it.lectio.bibbia.ui.theme.BibbiaTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onRead: (ReaderRoute) -> Unit,
    onBooks: (Testament) -> Unit,
    onBookmarks: () -> Unit,
    onSearch: () -> Unit,
    onTranslations: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val viewModel = appViewModel { c, _ ->
        HomeViewModel(c.translationRepository, c.settingsRepository, c.bookmarkRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = BibbiaTheme.colors
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notFound = stringResource(R.string.reference_not_found)
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paper),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ReadingMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = insets.calculateTopPadding() + 40.dp,
                bottom = insets.calculateBottomPadding() + 32.dp,
            ),
        ) {
            // Titolo
            item {
                Column(Modifier.padding(horizontal = PageMargin)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 0.32.em),
                        color = colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = colors.inkMuted,
                    )
                    state.preparation?.let { progress ->
                        Spacer(Modifier.height(18.dp))
                        Text(
                            stringResource(R.string.preparing_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = colors.rubric,
                            trackColor = colors.hairline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Hairline()
                }
            }

            // Continua lettura
            item {
                Section(stringResource(R.string.continue_reading)) {
                    val last = state.lastPosition
                    val translation = state.translations.firstOrNull { it.translation.id == (last?.translationId ?: state.currentTranslationId) }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onRead(ReaderRoute()) }
                            .padding(horizontal = PageMargin, vertical = 6.dp),
                    ) {
                        Text(
                            text = last?.let { ReferenceFormatter.format(it.bookId, it.chapter, null, it.translationId) }
                                ?: stringResource(R.string.start_reading),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.ink,
                        )
                        translation?.let {
                            Text(
                                text = it.translation.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted,
                            )
                        }
                    }
                    GoToReferenceField(
                        onSubmit = { text ->
                            val route = viewModel.routeFor(text)
                            if (route != null) onRead(route) else scope.launch { snackbar.showSnackbar(notFound) }
                            route != null
                        },
                        modifier = Modifier.padding(horizontal = PageMargin, vertical = 12.dp),
                    )
                }
            }

            // Traduzioni
            item {
                Section(stringResource(R.string.translations)) {
                    state.translations.forEach { t ->
                        TranslationRow(
                            state = t,
                            current = t.translation.id == state.currentTranslationId,
                            onClick = {
                                if (t.status == InstallStatus.INSTALLED) {
                                    viewModel.selectTranslation(t.translation.id)
                                    onRead(ReaderRoute(translationId = t.translation.id))
                                } else if (t.status == InstallStatus.FAILED) {
                                    viewModel.retry(t.translation.id)
                                }
                            },
                        )
                    }
                    LinkRow(stringResource(R.string.manage_translations), onTranslations)
                }
            }

            // Libri
            item {
                Section(stringResource(R.string.books)) {
                    LinkRow(stringResource(R.string.old_testament), { onBooks(Testament.OLD) }, large = true)
                    LinkRow(stringResource(R.string.new_testament), { onBooks(Testament.NEW) }, large = true)
                }
            }

            // Segnalibri
            item {
                Section(stringResource(R.string.bookmarks)) {
                    if (state.recentBookmarks.isEmpty()) {
                        Text(
                            stringResource(R.string.bookmarks_empty_short),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = colors.inkMuted,
                            modifier = Modifier.padding(horizontal = PageMargin, vertical = 6.dp),
                        )
                    }
                    state.recentBookmarks.forEach { b ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRead(ReaderRoute(b.translationId, b.bookId, b.chapter, b.verse ?: 0))
                                }
                                .padding(horizontal = PageMargin, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("★", color = colors.rubric, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ReferenceFormatter.format(b.bookId, b.chapter, b.verse, b.translationId),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink,
                                )
                                b.label?.let {
                                    Text(
                                        "“$it”",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = colors.inkMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    LinkRow(
                        if (state.bookmarkCount > 0) {
                            stringResource(R.string.all_bookmarks_count, state.bookmarkCount)
                        } else {
                            stringResource(R.string.all_bookmarks)
                        },
                        onBookmarks,
                    )
                }
            }

            // Strumenti
            item {
                Section(null) {
                    LinkRow(stringResource(R.string.search), onSearch)
                    LinkRow(stringResource(R.string.settings), onSettings)
                    LinkRow(stringResource(R.string.about), onAbout)
                }
            }
        }

        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = insets.calculateBottomPadding() + 16.dp),
        )
    }
}

@Composable
private fun Section(title: String?, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 26.dp)) {
        if (title != null) {
            SectionLabel(
                title,
                color = BibbiaTheme.colors.rubric,
                modifier = Modifier.padding(horizontal = PageMargin, vertical = 6.dp),
            )
        }
        content()
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit, large: Boolean = false) {
    val colors = BibbiaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PageMargin, vertical = if (large) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (large) colors.ink else colors.inkMuted,
            modifier = Modifier.weight(1f),
        )
        Text("›", style = MaterialTheme.typography.titleLarge, color = colors.inkFaint)
    }
}

@Composable
private fun TranslationRow(state: TranslationState, current: Boolean, onClick: () -> Unit) {
    val colors = BibbiaTheme.colors
    val t = state.translation
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PageMargin, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(18.dp)) {
            if (current) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.rubric),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    t.abbreviation,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.12.em),
                    color = if (current) colors.rubric else colors.ink,
                )
                Spacer(Modifier.width(12.dp))
                Text(t.languageLabel, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            }
            val subtitle = when (state.status) {
                InstallStatus.INSTALLING -> stringResource(R.string.installing_percent, (state.progress * 100).toInt())
                InstallStatus.FAILED -> stringResource(R.string.install_failed_tap)
                InstallStatus.NOT_INSTALLED -> stringResource(R.string.waiting)
                InstallStatus.INSTALLED -> t.name
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.status == InstallStatus.FAILED) colors.rubric else colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
