package it.lectio.bibbia.ui.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.lectio.bibbia.domain.model.ChapterRef
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.ReaderLayout
import it.lectio.bibbia.data.repository.ReaderSettings
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.TranslationOrigin
import it.lectio.bibbia.domain.model.Verse
import it.lectio.bibbia.navigation.ReaderRoute
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.EmptyState
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.theme.BibbiaTheme
import it.lectio.bibbia.ui.theme.family
import it.lectio.bibbia.ui.theme.sizeFactor
import it.lectio.bibbia.util.VerseText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val HEADER_ITEMS = 1

@Composable
fun ReaderScreen(
    route: ReaderRoute,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val viewModel = appViewModel { c, handle ->
        ReaderViewModel(
            savedStateHandle = handle,
            route = route,
            translations = c.translationRepository,
            bible = c.bibleRepository,
            bookmarks = c.bookmarkRepository,
            settings = c.settingsRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollRequest by viewModel.scrollRequest.collectAsStateWithLifecycle()
    val comparison by viewModel.comparison.collectAsStateWithLifecycle()

    val colors = BibbiaTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var labelDialog by rememberSaveable { mutableStateOf<Long?>(null) }
    var barsVisible by rememberSaveable { mutableStateOf(true) }
    var flashingVerse by remember { mutableStateOf<Int?>(null) }

    // Messaggi una tantum dal ViewModel.
    val bookmarkAdded = stringResource(R.string.bookmark_added)
    val addNote = stringResource(R.string.bookmark_add_note)
    val bookmarkRemoved = stringResource(R.string.bookmark_removed)
    val referenceNotFound = stringResource(R.string.reference_not_found)
    val chapterMissing = stringResource(R.string.chapter_not_in_translation)
    val copied = stringResource(R.string.copied)
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ReaderEvent.BookmarkAdded -> scope.launch {
                    val result = snackbarHost.showSnackbar(
                        message = "$bookmarkAdded · ${event.label}",
                        actionLabel = addNote,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) labelDialog = event.bookmarkId
                }
                ReaderEvent.BookmarkRemoved -> scope.launch { snackbarHost.showSnackbar(bookmarkRemoved) }
                ReaderEvent.ReferenceNotFound -> scope.launch { snackbarHost.showSnackbar(referenceNotFound) }
                ReaderEvent.ChapterNotInTranslation -> scope.launch { snackbarHost.showSnackbar(chapterMissing) }
                is ReaderEvent.Copied -> scope.launch { snackbarHost.showSnackbar("$copied · ${event.label}") }
            }
        }
    }

    // Schermo sempre acceso durante la lettura, se richiesto.
    val view = LocalView.current
    DisposableEffect(state.settings.keepScreenOn) {
        view.keepScreenOn = state.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Una LazyListState per capitolo: cambia capitolo → si riparte dall'alto; rotazione → posizione mantenuta.
    val location = state.location
    val listState = rememberSaveable(location, state.settings.layout, saver = LazyListState.Saver) { LazyListState() }

    val paragraphs = remember(state.verses) { ChapterLayout.paragraphs(state.verses) }
    val paragraphLayouts = remember(state.verses) { mutableStateMapOf<Int, TextLayoutResult>() }
    val paragraphTexts = remember { mutableStateMapOf<Int, AnnotatedVerses>() }

    // Scorrimento verso un versetto (da ricerca, segnalibri, "vai a", ripresa lettura).
    LaunchedEffect(scrollRequest, state.versesLoaded, state.settings.layout) {
        val request = scrollRequest ?: return@LaunchedEffect
        if (!state.versesLoaded || state.verses.isEmpty()) return@LaunchedEffect
        if (request.verse <= 1) {
            listState.scrollToItem(0)
        } else if (state.settings.layout == ReaderLayout.VERSES) {
            val index = state.verses.indexOfFirst { it.number == request.verse }
            if (index >= 0) listState.scrollToItem(HEADER_ITEMS + index)
        } else {
            val pIndex = paragraphs.indexOfFirst { p -> p.verses.any { it.number == request.verse } }
            if (pIndex >= 0) {
                listState.scrollToItem(HEADER_ITEMS + pIndex)
                // Dopo l'impaginazione, porta la riga del versetto in cima.
                delay(32)
                val layout = paragraphLayouts[pIndex]
                val start = paragraphTexts[pIndex]?.startOf(request.verse)
                if (layout != null && start != null) {
                    val top = layout.getLineTop(layout.getLineForOffset(start))
                    listState.scrollBy(top)
                }
            }
        }
        viewModel.onScrollRequestHandled(request)
        if (request.flash) {
            flashingVerse = request.verse
            delay(1600)
            flashingVerse = null
        }
    }

    // Traccia il versetto visibile per salvare automaticamente la posizione.
    LaunchedEffect(listState, state.verses, state.settings.layout) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val verse = if (index < HEADER_ITEMS) {
                    1
                } else if (state.settings.layout == ReaderLayout.VERSES) {
                    state.verses.getOrNull(index - HEADER_ITEMS)?.number
                } else {
                    paragraphs.getOrNull(index - HEADER_ITEMS)?.verses?.firstOrNull()?.number
                }
                if (verse != null) viewModel.onVisibleVerseChanged(verse)
            }
    }

    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) barsVisible = false else if (available.y > 6f) barsVisible = true
                return Offset.Zero
            }
        }
    }
    val showBars = barsVisible || state.selectedVerse != null || !state.isReady

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        when {
            location == null || !state.settingsLoaded -> Unit
            state.translationStatus != InstallStatus.INSTALLED -> TranslationNotReady(
                state = state,
                onRetry = viewModel::retryInstall,
                modifier = Modifier.padding(top = statusBarTop + 64.dp),
            )
            state.versesLoaded && state.verses.isEmpty() -> EmptyState(
                title = stringResource(R.string.chapter_empty_title),
                message = stringResource(R.string.chapter_empty_message),
                modifier = Modifier.padding(top = statusBarTop + 96.dp),
            )
            else -> ChapterText(
                state = state,
                paragraphs = paragraphs,
                listState = listState,
                flashingVerse = flashingVerse,
                contentPadding = PaddingValues(top = statusBarTop + 56.dp, bottom = navBarBottom + 72.dp),
                onVerseTap = { verse -> viewModel.selectVerse(verse) },
                onParagraphLayout = { index, text, layout ->
                    paragraphTexts[index] = text
                    paragraphLayouts[index] = layout
                },
                onPrevious = viewModel::goToPrevious,
                onNext = viewModel::goToNext,
                modifier = Modifier
                    .nestedScroll(hideOnScroll)
                    .chapterSwipe(onPrevious = viewModel::goToPrevious, onNext = viewModel::goToNext),
            )
        }

        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                state = state,
                onBack = onBack,
                onTranslation = viewModel::switchTranslation,
                onSearch = onOpenSearch,
                onToggleChapterBookmark = viewModel::toggleChapterBookmark,
                onSettings = { showSettings = true },
                modifier = Modifier.padding(top = statusBarTop),
            )
        }

        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val selected = state.selectedVerse
            if (selected != null) {
                VerseActionBar(
                    state = state,
                    verse = selected,
                    onBookmark = { viewModel.toggleVerseBookmark(selected) },
                    onHighlight = { color -> viewModel.setHighlight(selected, color) },
                    onCompare = { viewModel.compare(selected) },
                    onCopy = {
                        copyVerse(context, state, selected)
                        viewModel.onCopied(selected)
                    },
                    onClose = { viewModel.selectVerse(null) },
                    modifier = Modifier.padding(bottom = navBarBottom),
                )
            } else if (state.isReady && state.book != null) {
                ChapterBar(
                    state = state,
                    onPrevious = viewModel::goToPrevious,
                    onNext = viewModel::goToNext,
                    onTitle = { showPicker = true },
                    modifier = Modifier.padding(bottom = navBarBottom),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + 64.dp),
        )
    }

    if (showPicker) {
        ChapterPickerDialog(
            state = state,
            onDismiss = { showPicker = false },
            onChoose = { ref ->
                showPicker = false
                viewModel.goTo(ref)
            },
            onReference = { text -> viewModel.goToReference(text).also { if (it) showPicker = false } },
        )
    }

    if (showSettings) {
        ReaderSettingsSheet(
            settings = state.settings,
            onChange = viewModel::updateSettings,
            onDismiss = { showSettings = false },
        )
    }

    comparison?.let { compared ->
        CompareSheet(
            items = compared,
            onDismiss = viewModel::dismissComparison,
        )
    }

    labelDialog?.let { id ->
        BookmarkLabelDialog(
            onDismiss = { labelDialog = null },
            onSave = { label ->
                viewModel.renameBookmark(id, label)
                labelDialog = null
            },
        )
    }
}

private fun copyVerse(context: Context, state: ReaderUiState, verseNumber: Int) {
    val verse = state.verses.firstOrNull { it.number == verseNumber } ?: return
    val reference = "${state.book?.name ?: verse.bookId} ${verse.chapter}:${verse.number}"
    val text = "${VerseText.plain(verse.text)}\n— $reference (${state.translation.abbreviation})"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(reference, text))
}

/** Scorrimento orizzontale per cambiare capitolo, come voltare pagina. */
private fun Modifier.chapterSwipe(onPrevious: () -> Unit, onNext: () -> Unit): Modifier =
    pointerInput(Unit) {
        val threshold = 96.dp.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                when {
                    total > threshold -> onPrevious()
                    total < -threshold -> onNext()
                }
            },
            onHorizontalDrag = { change, amount ->
                total += amount
                change.consume()
            },
        )
    }

/** Stile del testo biblico, derivato dalle preferenze. */
@Composable
fun readerTextStyle(settings: ReaderSettings, languageTag: String): TextStyle {
    val colors = BibbiaTheme.colors
    val size = settings.fontSizeSp * settings.font.sizeFactor()
    return TextStyle(
        fontFamily = settings.font.family(),
        fontSize = size.sp,
        lineHeight = (size * settings.lineHeight).sp,
        color = colors.ink,
        textAlign = if (settings.justify) TextAlign.Justify else TextAlign.Start,
        hyphens = if (settings.justify) Hyphens.Auto else Hyphens.None,
        lineBreak = LineBreak.Paragraph,
        localeList = LocaleList(languageTag),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )
}

@Composable
private fun ChapterText(
    state: ReaderUiState,
    paragraphs: List<ReaderParagraph>,
    listState: LazyListState,
    flashingVerse: Int?,
    contentPadding: PaddingValues,
    onVerseTap: (Int) -> Unit,
    onParagraphLayout: (Int, AnnotatedVerses, TextLayoutResult) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BibbiaTheme.colors
    val settings = state.settings
    val textStyle = readerTextStyle(settings, state.translation.language.code)
    val verseStyle = VerseStyle(
        numberColor = colors.rubric.copy(alpha = 0.85f),
        bookmarkColor = colors.rubric,
        selectionColor = colors.selection,
        flashColor = colors.highlights.values.first().copy(alpha = 0.7f),
        highlightColors = colors.highlights,
        showNumbers = settings.showVerseNumbers,
        italicBrackets = state.translation.conventions.bracketsAreItalics,
    )
    val verseGap = (settings.fontSizeSp * 0.45f).dp

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            ChapterHeader(state)
        }
        if (settings.layout == ReaderLayout.VERSES) {
            itemsIndexed(state.verses, key = { _, v -> v.number }) { _, verse ->
                VerseItem(
                    verse = verse,
                    state = state,
                    style = verseStyle,
                    textStyle = textStyle,
                    flashing = flashingVerse,
                    onTap = onVerseTap,
                    modifier = Modifier.padding(bottom = verseGap),
                )
            }
        } else {
            itemsIndexed(paragraphs, key = { _, p -> "p" + p.verses.first().number }) { index, paragraph ->
                ParagraphItem(
                    index = index,
                    paragraph = paragraph,
                    state = state,
                    style = verseStyle,
                    textStyle = textStyle.copy(
                        textIndent = if (index > 0) TextIndent(firstLine = 1.5.em) else TextIndent.None,
                    ),
                    flashing = flashingVerse,
                    onTap = onVerseTap,
                    onLayout = onParagraphLayout,
                    modifier = Modifier.padding(bottom = (settings.fontSizeSp * 0.2f).dp),
                )
            }
        }
        item(key = "footer") {
            ChapterFooter(state, onPrevious, onNext)
        }
    }
}

@Composable
private fun ChapterHeader(state: ReaderUiState) {
    val colors = BibbiaTheme.colors
    val book = state.book ?: return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = ReadingMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = PageMargin)
            .padding(top = 28.dp, bottom = 28.dp)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "${book.name} ${state.chapter}"
            },
    ) {
        Text(
            text = book.name.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.28.em),
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.chapter.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = colors.rubric,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(28.dp)
                .height(0.8.dp)
                .background(colors.hairline),
        )
    }
}

@Composable
private fun VerseItem(
    verse: Verse,
    state: ReaderUiState,
    style: VerseStyle,
    textStyle: TextStyle,
    flashing: Int?,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(verse, style, state.bookmarkedVerses, state.highlights, state.selectedVerse, flashing) {
        buildVerses(
            verses = listOf(verse),
            style = style,
            inline = false,
            bookmarked = state.bookmarkedVerses,
            highlights = state.highlights,
            selected = state.selectedVerse,
            flashing = flashing,
        )
    }
    Text(
        text = annotated.text,
        style = textStyle,
        modifier = modifier
            .widthIn(max = ReadingMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = PageMargin)
            .pointerInput(verse.number) { detectTapGestures { onTap(verse.number) } },
    )
}

@Composable
private fun ParagraphItem(
    index: Int,
    paragraph: ReaderParagraph,
    state: ReaderUiState,
    style: VerseStyle,
    textStyle: TextStyle,
    flashing: Int?,
    onTap: (Int) -> Unit,
    onLayout: (Int, AnnotatedVerses, TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(paragraph, style, state.bookmarkedVerses, state.highlights, state.selectedVerse, flashing) {
        buildVerses(
            verses = paragraph.verses,
            style = style,
            inline = true,
            bookmarked = state.bookmarkedVerses,
            highlights = state.highlights,
            selected = state.selectedVerse,
            flashing = flashing,
        )
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated.text,
        style = textStyle,
        onTextLayout = {
            layout = it
            onLayout(index, annotated, it)
        },
        modifier = modifier
            .widthIn(max = ReadingMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = PageMargin)
            .pointerInput(annotated) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    annotated.verseAt(result.getOffsetForPosition(position))?.let(onTap)
                }
            },
    )
}

@Composable
private fun ChapterFooter(state: ReaderUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = BibbiaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = ReadingMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = PageMargin)
            .padding(top = 28.dp, bottom = 24.dp),
    ) {
        Text("❧", style = MaterialTheme.typography.titleLarge, color = colors.inkFaint)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth()) {
            state.previous?.let { prev ->
                Text(
                    text = "‹ " + chapterLabel(state, prev),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.inkMuted,
                    modifier = Modifier
                        .clickable(onClick = onPrevious)
                        .padding(vertical = 10.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            state.next?.let { next ->
                Text(
                    text = chapterLabel(state, next) + " ›",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                    modifier = Modifier
                        .clickable(onClick = onNext)
                        .padding(vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = state.translation.name + " · " + state.translation.license,
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
        )
    }
}

fun chapterLabel(state: ReaderUiState, ref: ChapterRef): String {
    val name = state.books.firstOrNull { it.id == ref.bookId }?.name ?: ref.bookId
    return if (ref.bookId == state.book?.id) ref.chapter.toString() else "$name ${ref.chapter}"
}

@Composable
private fun TranslationNotReady(state: ReaderUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BibbiaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageMargin, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.translation.name, style = MaterialTheme.typography.headlineSmall, color = colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        when (state.translationStatus) {
            InstallStatus.INSTALLING -> {
                Text(
                    stringResource(R.string.preparing_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { state.installProgress },
                    color = colors.rubric,
                    trackColor = colors.hairline,
                    modifier = Modifier.width(180.dp),
                )
            }
            else -> {
                val downloadable = state.translation.origin is TranslationOrigin.Downloadable
                Text(
                    text = stringResource(
                        when {
                            state.translationStatus == InstallStatus.FAILED -> R.string.install_failed
                            downloadable -> R.string.translation_not_downloaded
                            else -> R.string.preparing_text
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.rubric, contentColor = colors.paper),
                ) {
                    Text(
                        stringResource(if (state.translationStatus == InstallStatus.FAILED) R.string.retry else R.string.download),
                    )
                }
            }
        }
    }
}
