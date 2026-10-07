package it.lectio.bibbia.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.ComparedVerse
import it.lectio.bibbia.data.repository.ReaderSettings
import it.lectio.bibbia.domain.model.HighlightColor
import it.lectio.bibbia.ui.ReferenceFormatter
import it.lectio.bibbia.ui.books.BookChapterChooser
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.components.SectionLabel
import it.lectio.bibbia.ui.settings.ReadingControls
import it.lectio.bibbia.ui.theme.BibbiaTheme
import it.lectio.bibbia.util.VerseText

@Composable
fun ReaderTopBar(
    state: ReaderUiState,
    onBack: () -> Unit,
    onTranslation: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleChapterBookmark: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BibbiaTheme.colors
    var translationMenu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.paper),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = colors.inkMuted)
            }
            Spacer(Modifier.weight(1f))
            // Traduzione
            Box {
                TextButton(onClick = { translationMenu = true }) {
                    Text(
                        state.translation.abbreviation,
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.12.em),
                        color = colors.rubric,
                    )
                }
                DropdownMenu(
                    expanded = translationMenu,
                    onDismissRequest = { translationMenu = false },
                    containerColor = colors.surfaceRaised,
                ) {
                    state.installedTranslations.forEach { t ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "${t.abbreviation} · ${t.languageLabel}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (t.id == state.translation.id) colors.rubric else colors.ink,
                                    )
                                    Text(t.name, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                                }
                            },
                            onClick = {
                                translationMenu = false
                                onTranslation(t.id)
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, stringResource(R.string.search), tint = colors.inkMuted)
            }
            IconButton(onClick = onToggleChapterBookmark) {
                if (state.chapterBookmarkId != null) {
                    Icon(Icons.Filled.Bookmark, stringResource(R.string.remove_chapter_bookmark), tint = colors.rubric)
                } else {
                    Icon(Icons.Outlined.BookmarkBorder, stringResource(R.string.bookmark_chapter), tint = colors.inkMuted)
                }
            }
            TextButton(onClick = onSettings) {
                Text("Aa", style = MaterialTheme.typography.titleMedium, color = colors.inkMuted)
            }
        }
        Hairline()
    }
}

@Composable
fun ChapterBar(
    state: ReaderUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BibbiaTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.paper),
    ) {
        Hairline()
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavLabel(
                text = state.previous?.let { chapterLabel(state, it) },
                leading = true,
                onClick = onPrevious,
                description = stringResource(R.string.previous_chapter),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = state.book?.let { "${it.name} ${state.chapter}" } ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1.4f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onTitle)
                    .padding(vertical = 8.dp),
            )
            NavLabel(
                text = state.next?.let { chapterLabel(state, it) },
                leading = false,
                onClick = onNext,
                description = stringResource(R.string.next_chapter),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavLabel(text: String?, leading: Boolean, onClick: () -> Unit, description: String, modifier: Modifier) {
    val colors = BibbiaTheme.colors
    Box(modifier, contentAlignment = if (leading) Alignment.CenterStart else Alignment.CenterEnd) {
        if (text != null) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics { contentDescription = "$description: $text" }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = colors.inkMuted)
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!leading) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = colors.inkMuted)
            }
        }
    }
}

@Composable
fun VerseActionBar(
    state: ReaderUiState,
    verse: Int,
    onBookmark: () -> Unit,
    onHighlight: (HighlightColor?) -> Unit,
    onCompare: () -> Unit,
    onCopy: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BibbiaTheme.colors
    var showColors by rememberSaveable(verse) { mutableStateOf(false) }
    val bookmarked = verse in state.bookmarkedVerses
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised),
    ) {
        Hairline()
        Column(modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.book?.name ?: ""} ${state.chapter}:$verse",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.rubric,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.close), tint = colors.inkMuted)
                }
            }
            if (showColors) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    HighlightColor.entries.forEach { color ->
                        val selected = state.highlights[verse] == color
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.highlights.getValue(color))
                                .border(
                                    BorderStroke(if (selected) 1.6.dp else 0.6.dp, if (selected) colors.rubric else colors.hairline),
                                    CircleShape,
                                )
                                .clickable(role = Role.Button) { onHighlight(color) }
                                .semantics { contentDescription = color.name },
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onHighlight(null) }) {
                        Text(stringResource(R.string.highlight_none), color = colors.inkMuted)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    ActionItem(
                        icon = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        label = stringResource(if (bookmarked) R.string.action_unbookmark else R.string.action_bookmark),
                        tint = if (bookmarked) colors.rubric else colors.ink,
                        onClick = onBookmark,
                    )
                    ActionItem(Icons.Outlined.BorderColor, stringResource(R.string.action_highlight), colors.ink) { showColors = true }
                    if (state.installedTranslations.size > 1) {
                        ActionItem(Icons.AutoMirrored.Outlined.CompareArrows, stringResource(R.string.action_compare), colors.ink, onCompare)
                    }
                    ActionItem(Icons.Outlined.ContentCopy, stringResource(R.string.action_copy), colors.ink, onCopy)
                }
            }
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val colors = BibbiaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
    }
}

/** Finestra a tutto schermo: "Vai a riferimento" + scelta Libro → Capitolo. */
@Composable
fun ChapterPickerDialog(
    state: ReaderUiState,
    onDismiss: () -> Unit,
    onChoose: (it.lectio.bibbia.domain.model.ChapterRef) -> Unit,
    onReference: (String) -> Boolean,
) {
    val colors = BibbiaTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = ReadingMaxWidth)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, end = PageMargin, top = 4.dp),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close), tint = colors.inkMuted)
                    }
                    Text(
                        stringResource(R.string.choose_book_chapter),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                    )
                }
                GoToReferenceField(
                    onSubmit = onReference,
                    modifier = Modifier.padding(horizontal = PageMargin, vertical = 8.dp),
                )
                Hairline(Modifier.padding(top = 8.dp))
                BookChapterChooser(
                    books = state.books,
                    current = state.location?.chapterRef,
                    onChoose = onChoose,
                )
            }
        }
    }
}

/** Campo "Vai a riferimento" (es. "Gv 3,16", "Salmi 137", "Isaia 40"). */
@Composable
fun GoToReferenceField(onSubmit: (String) -> Boolean, modifier: Modifier = Modifier) {
    val colors = BibbiaTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.goto_hint), color = colors.inkFaint) },
        label = { Text(stringResource(R.string.goto_label)) },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { if (onSubmit(text)) text = "" }),
        trailingIcon = {
            if (text.isNotBlank()) {
                TextButton(onClick = { if (onSubmit(text)) text = "" }) {
                    Text(stringResource(R.string.go), color = colors.rubric)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.rubric,
            unfocusedBorderColor = colors.hairline,
            focusedLabelColor = colors.rubric,
            unfocusedLabelColor = colors.inkMuted,
            cursorColor = colors.rubric,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = BibbiaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        contentColor = colors.ink,
        scrimColor = colors.ink.copy(alpha = 0.18f),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageMargin)
                .padding(bottom = 32.dp),
        ) {
            ReadingControls(settings = settings, onChange = onChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareSheet(items: List<ComparedVerse>, onDismiss: () -> Unit) {
    val colors = BibbiaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.paper,
        contentColor = colors.ink,
        scrimColor = colors.ink.copy(alpha = 0.18f),
    ) {
        LazyColumn(Modifier.padding(horizontal = PageMargin).padding(bottom = 32.dp)) {
            item {
                SectionLabel(stringResource(R.string.compare_title), color = colors.rubric)
                Spacer(Modifier.height(12.dp))
            }
            items(items, key = { it.translation.id }) { item ->
                Column(Modifier.padding(vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            item.translation.abbreviation,
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.16.em),
                            color = colors.rubric,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            ReferenceFormatter.format(item.ref.bookId, item.ref.chapter, item.ref.verse, item.translation.id),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.text?.let { VerseText.plain(it) } ?: stringResource(R.string.compare_missing),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = if (item.text == null) FontStyle.Italic else FontStyle.Normal,
                        ),
                        color = if (item.text == null) colors.inkFaint else colors.ink,
                    )
                }
                Hairline()
            }
        }
    }
}

@Composable
fun BookmarkLabelDialog(
    initial: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val colors = BibbiaTheme.colors
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paper,
        title = { Text(stringResource(R.string.bookmark_note_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.bookmark_note_hint), color = colors.inkFaint) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.rubric,
                    unfocusedBorderColor = colors.hairline,
                    cursorColor = colors.rubric,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save), color = colors.rubric) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.inkMuted) }
        },
    )
}
