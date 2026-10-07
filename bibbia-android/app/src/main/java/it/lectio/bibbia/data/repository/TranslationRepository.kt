package it.lectio.bibbia.data.repository

import androidx.room.withTransaction
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.database.BookEntity
import it.lectio.bibbia.data.database.TranslationEntity
import it.lectio.bibbia.data.database.VerseEntity
import it.lectio.bibbia.data.source.InvalidSourceException
import it.lectio.bibbia.data.source.SourceException
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.data.source.TranslationSourceFactory
import it.lectio.bibbia.data.source.VplParser
import it.lectio.bibbia.domain.model.Canon
import it.lectio.bibbia.domain.model.InstallError
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.Translation
import it.lectio.bibbia.domain.model.TranslationOrigin
import it.lectio.bibbia.domain.model.TranslationState
import it.lectio.bibbia.util.TextFolding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Gestisce il catalogo delle traduzioni e la loro installazione nel database locale.
 *
 * L'installazione è atomica: avviene in un'unica transazione, quindi una traduzione è o completa
 * o assente. Se l'app viene chiusa a metà, al riavvio lo stato INSTALLING viene trattato come
 * "da reinstallare".
 */
class TranslationRepository(
    private val db: BibleDatabase,
    private val sourceFactory: TranslationSourceFactory,
    private val catalog: List<Translation> = TranslationCatalog.all,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val translationDao = db.translationDao()
    private val bookDao = db.bookDao()
    private val verseDao = db.verseDao()

    private val progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    private val installMutex = Mutex()

    fun translation(id: String): Translation? = catalog.firstOrNull { it.id == id }

    fun observeStates(): Flow<List<TranslationState>> =
        combine(translationDao.observeAll(), progress) { entities, progressMap ->
            val byId = entities.associateBy { it.id }
            catalog.map { t ->
                val entity = byId[t.id]
                val inProgress = progressMap[t.id]
                val status = when {
                    inProgress != null -> InstallStatus.INSTALLING
                    entity == null -> InstallStatus.NOT_INSTALLED
                    // INSTALLING senza un'installazione attiva = interrotta: verrà ripresa.
                    entity.status == InstallStatus.INSTALLING.name -> InstallStatus.NOT_INSTALLED
                    else -> runCatching { InstallStatus.valueOf(entity.status) }.getOrDefault(InstallStatus.NOT_INSTALLED)
                }
                TranslationState(
                    translation = t,
                    status = status,
                    verseCount = entity?.verseCount ?: 0,
                    installedAt = entity?.installedAt,
                    error = entity?.error?.let { e -> runCatching { InstallError.valueOf(e) }.getOrDefault(InstallError.UNKNOWN) }
                        ?.takeIf { status == InstallStatus.FAILED },
                    progress = inProgress ?: 0f,
                )
            }
        }

    fun observeInstalled(): Flow<List<Translation>> =
        observeStates().map { states -> states.filter { it.status == InstallStatus.INSTALLED }.map { it.translation } }

    suspend fun isInstalled(id: String): Boolean =
        translationDao.get(id)?.status == InstallStatus.INSTALLED.name

    /** Installa (o completa l'installazione di) tutte le traduzioni incluse nell'APK. Nessuna rete. */
    suspend fun ensureBundledInstalled() {
        for (t in catalog.filter { it.origin is TranslationOrigin.Bundled }) {
            if (!isInstalled(t.id)) install(t.id)
        }
    }

    /**
     * Installa una traduzione. Non lancia eccezioni per errori previsti (rete, dati): lo stato
     * FAILED con il relativo [InstallError] viene salvato e restituito.
     */
    suspend fun install(id: String): Result<Int> = installMutex.withLock {
        val translation = translation(id) ?: return Result.failure(IllegalArgumentException("Traduzione sconosciuta: $id"))
        if (isInstalled(id)) return Result.success(translationDao.get(id)?.verseCount ?: 0)

        progress.update { it + (id to 0f) }
        translationDao.upsert(TranslationEntity(id = id, status = InstallStatus.INSTALLING.name))
        try {
            val count = withContext(Dispatchers.IO) { importText(translation) }
            translationDao.upsert(
                TranslationEntity(
                    id = id,
                    status = InstallStatus.INSTALLED.name,
                    verseCount = count,
                    installedAt = clock(),
                ),
            )
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val reason = when (e) {
                is SourceException -> e.reason
                is InvalidSourceException -> InstallError.INVALID_DATA
                is android.database.sqlite.SQLiteFullException -> InstallError.STORAGE
                is IOException -> InstallError.NO_CONNECTION
                else -> InstallError.UNKNOWN
            }
            translationDao.upsert(
                TranslationEntity(id = id, status = InstallStatus.FAILED.name, error = reason.name),
            )
            Result.failure(e)
        } finally {
            progress.update { it - id }
        }
    }

    /** Rimuove il testo di una traduzione scaricata (segnalibri ed evidenziazioni restano). */
    suspend fun uninstall(id: String) {
        db.withTransaction {
            verseDao.deleteForTranslation(id)
            bookDao.deleteForTranslation(id)
            translationDao.upsert(TranslationEntity(id = id, status = InstallStatus.NOT_INSTALLED.name))
        }
    }

    private suspend fun importText(translation: Translation): Int {
        val source = sourceFactory.create(translation)
        val stream = source.open()
        val chapterCounts = LinkedHashMap<String, Int>()
        var total = 0
        db.withTransaction {
            verseDao.deleteForTranslation(translation.id)
            bookDao.deleteForTranslation(translation.id)
            // Il parser è sincrono: si legge tutto in memoria (~35.000 righe) e poi si inserisce a blocchi.
            val pending = ArrayList<VerseEntity>(translation.expectedVerses)
            stream.bufferedReader(Charsets.UTF_8).use { reader ->
                VplParser.parse(reader, translation.conventions) { raw ->
                    pending.add(
                        VerseEntity(
                            translationId = translation.id,
                            bookId = raw.bookId,
                            chapter = raw.chapter,
                            verse = raw.verse,
                            text = raw.text,
                            searchText = TextFolding.fold(raw.text.replace("[", "").replace("]", "")),
                            paragraphStart = raw.paragraphStart,
                        ),
                    )
                    chapterCounts[raw.bookId] = maxOf(chapterCounts[raw.bookId] ?: 0, raw.chapter)
                }
            }
            if (pending.isEmpty()) throw InvalidSourceException("Il testo non contiene versetti")
            pending.chunked(BATCH_SIZE).forEach { chunk ->
                verseDao.insertAll(chunk)
                total += chunk.size
                val fraction = (total.toFloat() / translation.expectedVerses).coerceAtMost(0.99f)
                progress.update { it + (translation.id to fraction) }
            }
            bookDao.insertAll(
                chapterCounts.map { (bookId, chapters) ->
                    BookEntity(
                        translationId = translation.id,
                        bookId = bookId,
                        position = Canon.position(bookId, translation.canonOrder),
                        chapterCount = chapters,
                    )
                },
            )
        }
        return total
    }

    private companion object {
        const val BATCH_SIZE = 1_000
    }
}
