package it.lectio.bibbia.data.repository

import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.SearchHit
import it.lectio.bibbia.domain.model.SearchResults
import it.lectio.bibbia.domain.model.Testament
import it.lectio.bibbia.domain.model.VerseRef
import it.lectio.bibbia.util.TextFolding

/** Ambito della ricerca. */
enum class SearchScope { ALL, OLD_TESTAMENT, NEW_TESTAMENT }

/**
 * Ricerca full-text locale (SQLite FTS4) sui testi installati.
 *
 * La query viene normalizzata come il testo indicizzato ([TextFolding]); ogni parola deve comparire
 * nel versetto (AND) e viene cercata anche come prefisso ("ama" trova "amato", "love" trova "loved").
 * Una frase tra virgolette ("acqua viva") viene cercata come sequenza esatta.
 */
class SearchRepository(db: BibleDatabase) {
    private val verseDao = db.verseDao()

    suspend fun search(
        query: String,
        translationId: String,
        scope: SearchScope = SearchScope.ALL,
        limit: Int = DEFAULT_LIMIT,
    ): SearchResults {
        val match = buildMatchExpression(query) ?: return SearchResults(query, emptyList(), 0)
        val bookIds = when (scope) {
            SearchScope.ALL -> emptyList()
            SearchScope.OLD_TESTAMENT -> Canon.books.filter { it.testament == Testament.OLD }.map { it.id }
            SearchScope.NEW_TESTAMENT -> Canon.books.filter { it.testament == Testament.NEW }.map { it.id }
        }
        val allBooks = scope == SearchScope.ALL
        val rows = verseDao.search(match, translationId, allBooks, bookIds, limit)
        val total = if (rows.size < limit) rows.size else verseDao.searchCount(match, translationId, allBooks, bookIds)
        return SearchResults(
            query = query,
            hits = rows.map { SearchHit(it.translationId, VerseRef(it.bookId, it.chapter, it.verse), it.text) },
            totalCount = total,
        )
    }

    companion object {
        const val DEFAULT_LIMIT = 300

        /**
         * Costruisce un'espressione MATCH sicura: solo lettere e cifre ripiegate, quindi nessun
         * operatore FTS può essere iniettato dall'utente. Restituisce null se la query è vuota.
         */
        fun buildMatchExpression(query: String): String? {
            val parts = ArrayList<String>()
            val phraseRegex = Regex("\"([^\"]*)\"")
            phraseRegex.findAll(query).forEach { m ->
                val terms = TextFolding.queryTerms(m.groupValues[1])
                if (terms.isNotEmpty()) parts.add("\"" + terms.joinToString(" ") + "\"")
            }
            val rest = phraseRegex.replace(query, " ")
            TextFolding.queryTerms(rest).forEach { term -> parts.add("$term*") }
            return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
        }

        /** Parole da evidenziare nei risultati (ripiegate). */
        fun highlightTerms(query: String): List<String> =
            TextFolding.queryTerms(query.replace("\"", " ")).distinct()
    }
}
