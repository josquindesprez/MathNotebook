package it.lectio.bibbia.ui.books

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.lectio.bibbia.R
import it.lectio.bibbia.domain.model.Book
import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.Testament
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.SectionLabel
import it.lectio.bibbia.ui.theme.BibbiaTheme

/**
 * Scelta rapida Libro → Capitolo, condivisa tra la lettura (finestra a tutto schermo) e l'elenco dei
 * libri raggiunto dalla home.
 *
 * @param testament se non null mostra solo quel testamento.
 */
@Composable
fun BookChapterChooser(
    books: List<Book>,
    current: ChapterRef?,
    onChoose: (ChapterRef) -> Unit,
    modifier: Modifier = Modifier,
    testament: Testament? = null,
    header: @Composable () -> Unit = {},
) {
    var openBookId by rememberSaveable { mutableStateOf<String?>(null) }
    val openBook = books.firstOrNull { it.id == openBookId }

    BackHandler(enabled = openBook != null) { openBookId = null }

    if (openBook == null) {
        BookList(
            books = if (testament == null) books else books.filter { it.testament == testament },
            currentBookId = current?.bookId,
            showTestamentHeaders = testament == null,
            header = header,
            onBook = { book ->
                if (book.chapterCount == 1) onChoose(ChapterRef(book.id, 1)) else openBookId = book.id
            },
            modifier = modifier,
        )
    } else {
        ChapterGrid(
            book = openBook,
            currentChapter = current?.takeIf { it.bookId == openBook.id }?.chapter,
            onBack = { openBookId = null },
            onChapter = { onChoose(ChapterRef(openBook.id, it)) },
            modifier = modifier,
        )
    }
}

@Composable
private fun BookList(
    books: List<Book>,
    currentBookId: String?,
    showTestamentHeaders: Boolean,
    header: @Composable () -> Unit,
    onBook: (Book) -> Unit,
    modifier: Modifier,
) {
    val colors = BibbiaTheme.colors
    val listState = rememberLazyListState()
    // Porta in vista il libro corrente.
    LaunchedEffect(books.isNotEmpty()) {
        val index = books.indexOfFirst { it.id == currentBookId }
        if (index > 3) listState.scrollToItem(index + 1)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { header() }
        var lastTestament: Testament? = null
        books.forEach { book ->
            if (showTestamentHeaders && book.testament != lastTestament) {
                lastTestament = book.testament
                item(key = "header-${book.testament}") {
                    SectionLabel(
                        text = stringResource(
                            if (book.testament == Testament.OLD) R.string.old_testament else R.string.new_testament,
                        ),
                        color = colors.rubric,
                        modifier = Modifier.padding(start = PageMargin, end = PageMargin, top = 28.dp, bottom = 8.dp),
                    )
                }
            }
            item(key = book.id) {
                val isCurrent = book.id == currentBookId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBook(book) }
                        .padding(horizontal = PageMargin, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrent) colors.rubric else colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = book.chapterCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterGrid(
    book: Book,
    currentChapter: Int?,
    onBack: () -> Unit,
    onChapter: (Int) -> Unit,
    modifier: Modifier,
) {
    val colors = BibbiaTheme.colors
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = PageMargin, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹  ", style = MaterialTheme.typography.titleLarge, color = colors.inkMuted)
            Column {
                Text(book.name, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                Text(
                    text = stringResource(R.string.chapters_count, book.chapterCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }
        Hairline(Modifier.padding(horizontal = PageMargin))
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 56.dp),
            contentPadding = PaddingValues(horizontal = PageMargin - 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items((1..book.chapterCount).toList(), key = { it }) { chapter ->
                val isCurrent = chapter == currentChapter
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .then(
                            if (isCurrent) Modifier.border(BorderStroke(1.dp, colors.rubric), CircleShape) else Modifier,
                        )
                        .clickable { onChapter(chapter) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chapter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrent) colors.rubric else colors.ink,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
