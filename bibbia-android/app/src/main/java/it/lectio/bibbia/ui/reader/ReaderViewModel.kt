package it.lectio.bibbia.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.data.repository.BibleRepository
import it.lectio.bibbia.data.repository.BookmarkRepository
import it.lectio.bibbia.data.repository.ComparedVerse
import it.lectio.bibbia.data.repository.ReaderSettings
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.domain.ChapterNavigator
import it.lectio.bibbia.domain.ReferenceParser
import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.HighlightColor
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.ReadingPosition
import it.lectio.bibbia.domain.model.Translation
import it.lectio.bibbia.domain.model.TranslationState
import it.lectio.bibbia.domain.model.Verse
import it.lectio.bibbia.domain.model.VerseRef
import it.lectio.bibbia.navigation.ReaderRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Posizione corrente del lettore: traduzione + capitolo. */
data class ReaderLocation(val translationId: String, val bookId: String, val chapter: Int) {
    val chapterRef: ChapterRef get() = ChapterRef(bookId, chapter)
}

data class ReaderUiState(
    val location: ReaderLocation? = null,
    val translation: Translation = TranslationCatalog.default,
    val translationStatus: InstallStatus = InstallStatus.NOT_INSTALLED,
    val installProgress: Float = 0f,
    val installedTranslations: List<Translation> = emptyList(),
    val books: List<Book> = emptyList(),
    val book: Book? = null,
    val verses: List<Verse> = emptyList(),
    val versesLoaded: Boolean = false,
    val bookmarkedVerses: Set<Int> = emptySet(),
    val chapterBookmarkId: Long? = null,
    val highlights: Map<Int, HighlightColor> = emptyMap(),
    val previous: ChapterRef? = null,
    val next: ChapterRef? = null,
    val settings: ReaderSettings = ReaderSettings(),
    val settingsLoaded: Boolean = false,
    val selectedVerse: Int? = null,
) {
    val chapter: Int get() = location?.chapter ?: 0
    val isReady: Boolean get() = translationStatus == InstallStatus.INSTALLED
}

/** Richiesta di scorrimento verso un versetto; [id] distingue richieste ripetute sullo stesso versetto. */
data class ScrollRequest(val verse: Int, val flash: Boolean, val id: Long = System.nanoTime())

sealed interface ReaderEvent {
    data class BookmarkAdded(val bookmarkId: Long, val label: String) : ReaderEvent
    data object BookmarkRemoved : ReaderEvent
    data object ReferenceNotFound : ReaderEvent
    data object ChapterNotInTranslation : ReaderEvent
    data class Copied(val label: String) : ReaderEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ReaderViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val route: ReaderRoute,
    private val translations: TranslationRepository,
    private val bible: BibleRepository,
    private val bookmarks: BookmarkRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val selectedVerse = MutableStateFlow<Int?>(null)
    private val visibleVerse = MutableStateFlow(1)

    private val _scrollRequest = MutableStateFlow<ScrollRequest?>(null)
    val scrollRequest: StateFlow<ScrollRequest?> = _scrollRequest

    // Dichiarata dopo visibleVerse/_scrollRequest: restoreLocation() li usa.
    private val location = MutableStateFlow<ReaderLocation?>(restoreLocation())

    private val _comparison = MutableStateFlow<List<ComparedVerse>?>(null)
    val comparison: StateFlow<List<ComparedVerse>?> = _comparison

    private val events = Channel<ReaderEvent>(Channel.BUFFERED)
    val eventFlow: Flow<ReaderEvent> = events.receiveAsFlow()

    private val translationStates = translations.observeStates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val books: Flow<List<Book>> = location.filterNotNull()
        .map { it.translationId }
        .distinctUntilChanged()
        .flatMapLatest { bible.observeBooks(it) }

    private val chapterKey = location.filterNotNull().distinctUntilChanged()

    /** I versetti portano con sé la posizione a cui appartengono, per non mostrare il capitolo precedente. */
    private val verses: Flow<Pair<ReaderLocation, List<Verse>>> = chapterKey.flatMapLatest { loc ->
        bible.observeChapter(loc.translationId, loc.chapterRef).map { loc to it }
    }

    private val chapterBookmarks = chapterKey.flatMapLatest { loc ->
        bookmarks.observeForChapter(loc.translationId, loc.chapterRef)
    }

    private val highlights = chapterKey.flatMapLatest { loc ->
        bookmarks.observeHighlights(loc.translationId, loc.chapterRef)
    }

    private data class Annotations(
        val bookmarked: Set<Int>,
        val chapterBookmarkId: Long?,
        val highlights: Map<Int, HighlightColor>,
    )

    private val annotations: Flow<Annotations> = combine(chapterBookmarks, highlights) { bm, hl ->
        Annotations(
            bookmarked = bm.mapNotNull { it.verse }.toSet(),
            chapterBookmarkId = bm.firstOrNull { it.verse == null }?.id,
            highlights = hl.associate { it.ref.verse to it.color },
        )
    }

    private data class Content(
        val books: List<Book>,
        val verses: Pair<ReaderLocation, List<Verse>>,
        val annotations: Annotations,
    )

    private val content: Flow<Content> = combine(books, verses, annotations) { b, v, a -> Content(b, v, a) }

    val uiState: StateFlow<ReaderUiState> = combine(
        location,
        translationStates,
        content,
        settings.readerSettings,
        selectedVerse,
    ) { loc, states, c, readerSettings, selected ->
        val translation = loc?.let { translations.translation(it.translationId) } ?: TranslationCatalog.default
        val state: TranslationState? = states.firstOrNull { it.translation.id == translation.id }
        val navigator = ChapterNavigator(c.books)
        val ref = loc?.chapterRef
        ReaderUiState(
            location = loc,
            translation = translation,
            translationStatus = state?.status ?: InstallStatus.NOT_INSTALLED,
            installProgress = state?.progress ?: 0f,
            installedTranslations = states.filter { it.status == InstallStatus.INSTALLED }.map { it.translation },
            books = c.books,
            book = ref?.let { navigator.book(it.bookId) },
            verses = if (c.verses.first == loc) c.verses.second else emptyList(),
            versesLoaded = c.verses.first == loc,
            bookmarkedVerses = c.annotations.bookmarked,
            chapterBookmarkId = c.annotations.chapterBookmarkId,
            highlights = c.annotations.highlights,
            previous = ref?.let { navigator.previous(it) },
            next = ref?.let { navigator.next(it) },
            settings = readerSettings,
            settingsLoaded = true,
            selectedVerse = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState(location = location.value))

    init {
        if (location.value == null) {
            viewModelScope.launch { resolveInitialLocation() }
        }
        // Salva automaticamente l'ultima posizione di lettura (anche offline: è tutto locale).
        viewModelScope.launch {
            combine(location.filterNotNull(), visibleVerse) { loc, verse -> loc to verse }
                .debounce(400)
                .distinctUntilChanged()
                .collect { (loc, verse) ->
                    savedStateHandle[KEY_VERSE] = verse
                    settings.savePosition(ReadingPosition(loc.translationId, loc.bookId, loc.chapter, verse))
                }
        }
        // Un capitolo inesistente in questa traduzione (es. libro deuterocanonico) → primo capitolo valido.
        viewModelScope.launch {
            books.collect { list ->
                val loc = location.value ?: return@collect
                if (list.isEmpty()) return@collect
                val navigator = ChapterNavigator(list)
                if (!navigator.contains(loc.chapterRef)) {
                    navigator.coerce(loc.chapterRef)?.let { fixed ->
                        if (fixed != loc.chapterRef) {
                            events.trySend(ReaderEvent.ChapterNotInTranslation)
                            setLocation(loc.copy(bookId = fixed.bookId, chapter = fixed.chapter), scrollTo = null)
                        }
                    }
                }
            }
        }
    }

    private fun restoreLocation(): ReaderLocation? {
        val t = savedStateHandle.get<String>(KEY_TRANSLATION)
        val b = savedStateHandle.get<String>(KEY_BOOK)
        val c = savedStateHandle.get<Int>(KEY_CHAPTER)
        if (t != null && b != null && c != null) {
            visibleVerse.value = savedStateHandle.get<Int>(KEY_VERSE) ?: 1
            return ReaderLocation(t, b, c)
        }
        val translationId = route.translationId
        if (translationId != null && route.bookId != null && route.chapter > 0) {
            val loc = ReaderLocation(translationId, route.bookId, route.chapter)
            persistLocation(loc)
            if (route.verse > 0) {
                visibleVerse.value = route.verse
                _scrollRequest.value = ScrollRequest(route.verse, flash = true)
            }
            return loc
        }
        return null
    }

    private suspend fun resolveInitialLocation() {
        val last = settings.lastPosition.first()
        val currentId = route.translationId ?: settings.currentTranslationId.first()
        val translationId = currentId?.takeIf { translations.translation(it) != null }
            ?: last?.translationId?.takeIf { translations.translation(it) != null }
            ?: TranslationCatalog.default.id
        val loc = if (last != null) {
            val mapped = bible.mapChapter(ChapterRef(last.bookId, last.chapter), last.translationId, translationId)
            ReaderLocation(translationId, mapped.bookId, mapped.chapter)
        } else {
            ReaderLocation(translationId, "GEN", 1)
        }
        val verse = route.verse.takeIf { it > 0 } ?: last?.verse ?: 1
        setLocation(loc, scrollTo = verse.takeIf { it > 1 }, flash = false)
    }

    private fun persistLocation(loc: ReaderLocation) {
        savedStateHandle[KEY_TRANSLATION] = loc.translationId
        savedStateHandle[KEY_BOOK] = loc.bookId
        savedStateHandle[KEY_CHAPTER] = loc.chapter
    }

    private fun setLocation(loc: ReaderLocation, scrollTo: Int?, flash: Boolean = false) {
        persistLocation(loc)
        selectedVerse.value = null
        visibleVerse.value = scrollTo ?: 1
        location.value = loc
        _scrollRequest.value = ScrollRequest(scrollTo ?: 0, flash = flash && scrollTo != null)
    }

    fun goTo(ref: ChapterRef, verse: Int? = null) {
        val loc = location.value ?: return
        setLocation(loc.copy(bookId = ref.bookId, chapter = ref.chapter), scrollTo = verse, flash = verse != null)
    }

    fun goToNext() = uiState.value.next?.let { goTo(it) }

    fun goToPrevious() = uiState.value.previous?.let { goTo(it) }

    /** "Vai a riferimento": restituisce false se il testo non è un riferimento riconosciuto. */
    fun goToReference(text: String): Boolean {
        val parsed = ReferenceParser.parse(text)
        if (parsed == null) {
            events.trySend(ReaderEvent.ReferenceNotFound)
            return false
        }
        goTo(ChapterRef(parsed.bookId, parsed.chapter), parsed.verse)
        return true
    }

    fun switchTranslation(translationId: String) {
        val loc = location.value ?: return
        if (loc.translationId == translationId) return
        val mapped = bible.mapChapter(loc.chapterRef, loc.translationId, translationId)
        val verse = visibleVerse.value
        viewModelScope.launch { settings.setCurrentTranslation(translationId) }
        setLocation(ReaderLocation(translationId, mapped.bookId, mapped.chapter), scrollTo = verse.takeIf { it > 1 })
    }

    fun onScrollRequestHandled(request: ScrollRequest) {
        _scrollRequest.update { if (it?.id == request.id) null else it }
    }

    fun onVisibleVerseChanged(verse: Int) {
        if (verse > 0) visibleVerse.value = verse
    }

    fun selectVerse(verse: Int?) {
        selectedVerse.update { if (it == verse) null else verse }
    }

    fun toggleVerseBookmark(verse: Int) {
        val loc = location.value ?: return
        val text = uiState.value.verses.firstOrNull { it.number == verse }?.text ?: return
        viewModelScope.launch {
            val added = bookmarks.toggle(loc.translationId, loc.chapterRef, verse, text)
            if (added) {
                val id = bookmarks.find(loc.translationId, loc.chapterRef, verse)?.id ?: return@launch
                events.send(ReaderEvent.BookmarkAdded(id, referenceLabel(loc, verse)))
            } else {
                events.send(ReaderEvent.BookmarkRemoved)
            }
            selectedVerse.value = null
        }
    }

    fun toggleChapterBookmark() {
        val loc = location.value ?: return
        val first = uiState.value.verses.firstOrNull()?.text ?: return
        viewModelScope.launch {
            val added = bookmarks.toggle(loc.translationId, loc.chapterRef, null, first)
            if (added) {
                val id = bookmarks.find(loc.translationId, loc.chapterRef, null)?.id ?: return@launch
                events.send(ReaderEvent.BookmarkAdded(id, referenceLabel(loc, null)))
            } else {
                events.send(ReaderEvent.BookmarkRemoved)
            }
        }
    }

    fun renameBookmark(id: Long, label: String) {
        viewModelScope.launch { bookmarks.rename(id, label) }
    }

    fun setHighlight(verse: Int, color: HighlightColor?) {
        val loc = location.value ?: return
        viewModelScope.launch {
            bookmarks.setHighlight(loc.translationId, VerseRef(loc.bookId, loc.chapter, verse), color)
            selectedVerse.value = null
        }
    }

    fun compare(verse: Int) {
        val loc = location.value ?: return
        viewModelScope.launch {
            val targets = uiState.value.installedTranslations
            _comparison.value = bible.compare(loc.translationId, VerseRef(loc.bookId, loc.chapter, verse), targets)
        }
    }

    fun dismissComparison() {
        _comparison.value = null
    }

    fun onCopied(verse: Int) {
        val loc = location.value ?: return
        events.trySend(ReaderEvent.Copied(referenceLabel(loc, verse)))
        selectedVerse.value = null
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    fun retryInstall() {
        val loc = location.value ?: return
        viewModelScope.launch { translations.install(loc.translationId) }
    }

    private fun referenceLabel(loc: ReaderLocation, verse: Int?): String {
        val book = uiState.value.book?.name ?: loc.bookId
        return if (verse != null) "$book ${loc.chapter}:$verse" else "$book ${loc.chapter}"
    }

    companion object {
        private const val KEY_TRANSLATION = "reader_translation"
        private const val KEY_BOOK = "reader_book"
        private const val KEY_CHAPTER = "reader_chapter"
        private const val KEY_VERSE = "reader_verse"
    }
}
