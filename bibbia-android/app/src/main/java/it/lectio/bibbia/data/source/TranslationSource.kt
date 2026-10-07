package it.lectio.bibbia.data.source

import android.content.Context
import it.lectio.bibbia.domain.model.InstallError
import it.lectio.bibbia.domain.model.Translation
import it.lectio.bibbia.domain.model.TranslationOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/** Errore durante il reperimento del testo, con una causa interpretabile dall'interfaccia. */
class SourceException(val reason: InstallError, message: String, cause: Throwable? = null) :
    IOException(message, cause)

/**
 * Fornisce il testo completo di una traduzione in formato VPL (UTF-8).
 * Le implementazioni possono leggere da assets, da file o dalla rete: il resto dell'app non lo sa.
 */
fun interface TranslationSource {
    /** Apre il flusso del testo. Il chiamante lo chiude. Eseguito su un thread di I/O. */
    suspend fun open(): InputStream
}

/** Crea la sorgente adatta all'origine dichiarata nel catalogo. */
fun interface TranslationSourceFactory {
    fun create(translation: Translation): TranslationSource
}

class DefaultTranslationSourceFactory(private val context: Context) : TranslationSourceFactory {
    override fun create(translation: Translation): TranslationSource = when (val origin = translation.origin) {
        is TranslationOrigin.Bundled -> AssetTranslationSource(context, origin.assetPath)
        is TranslationOrigin.Downloadable -> RemoteTranslationSource(
            url = origin.url,
            cacheDir = context.cacheDir,
        )
    }
}

/** Testo incluso nell'APK, compresso con gzip. Funziona sempre offline. */
class AssetTranslationSource(private val context: Context, private val assetPath: String) : TranslationSource {
    override suspend fun open(): InputStream = withContext(Dispatchers.IO) {
        try {
            val raw = context.assets.open(assetPath)
            if (assetPath.endsWith(".gz")) GZIPInputStream(raw.buffered()) else raw
        } catch (e: IOException) {
            throw SourceException(InstallError.INVALID_DATA, "Testo incluso non leggibile: $assetPath", e)
        }
    }
}

/**
 * Scarica un archivio zip "VPL" di eBible.org una sola volta. Dopo l'installazione il testo vive nel
 * database locale e la rete non serve più.
 */
class RemoteTranslationSource(
    private val url: String,
    private val cacheDir: File,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) : TranslationSource {

    override suspend fun open(): InputStream = withContext(Dispatchers.IO) {
        val zipFile = File(cacheDir, "download-${url.hashCode()}.zip")
        try {
            download(zipFile)
            extractVpl(zipFile)
        } finally {
            zipFile.delete()
        }
    }

    private fun download(target: File) {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
            }
        } catch (e: IOException) {
            throw SourceException(InstallError.NO_CONNECTION, "Connessione non riuscita", e)
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw SourceException(InstallError.SERVER, "Risposta del server: HTTP $code")
            }
            connection.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
        } catch (e: SourceException) {
            throw e
        } catch (e: UnknownHostException) {
            throw SourceException(InstallError.NO_CONNECTION, "Nessuna connessione a Internet", e)
        } catch (e: SocketTimeoutException) {
            throw SourceException(InstallError.TIMEOUT, "Tempo scaduto", e)
        } catch (e: IOException) {
            throw SourceException(InstallError.NO_CONNECTION, e.message ?: "Errore di rete", e)
        } finally {
            connection.disconnect()
        }
    }

    /** Estrae in memoria il file *_vpl.txt dall'archivio. */
    private fun extractVpl(zipFile: File): InputStream {
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.endsWith("_vpl.txt")) {
                    val bytes = zip.readBytes()
                    return bytes.inputStream()
                }
            }
        }
        throw SourceException(InstallError.INVALID_DATA, "Archivio senza testo VPL")
    }
}
