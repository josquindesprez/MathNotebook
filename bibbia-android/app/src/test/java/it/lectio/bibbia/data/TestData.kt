package it.lectio.bibbia.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.lectio.bibbia.data.database.BibleDatabase
import it.lectio.bibbia.data.source.TranslationSource
import it.lectio.bibbia.data.source.TranslationSourceFactory

/** Testi di prova in formato VPL (eBible), brevi ma con le caratteristiche delle fonti reali. */
object TestTexts {
    val RIVEDUTA = """
        GEN 1:1 Nel principio Iddio creò i cieli e la terra.
        GEN 1:2 E la terra era informe e vuota, e le tenebre coprivano la faccia dell’abisso.
        GEN 2:1 Così furono compiti i cieli e la terra.
        EXO 1:1 Or questi sono i nomi dei figliuoli d’Israele.
        PSA 42:1 Come la cerva agogna i rivi dell’acque, così l’anima mia agogna te, o Dio.
        PSA 42:2 L’anima mia è assetata di Dio, dell’Iddio vivente.
        PSA 137:1 Là presso i fiumi di Babilonia, sedevamo e piangevamo.
        ISA 40:31 ma quelli che sperano nell’Eterno acquistan nuove forze.
        JOH 1:1 Nel principio era la Parola, e la Parola era con Dio.
        JOH 3:16 Poiché Iddio ha tanto amato il mondo, che ha dato il suo unigenito Figliuolo.
        JOH 4:14 ma chi beve dell’acqua ch’io gli darò, non avrà mai più sete.
        REV 21:2 E vidi la santa città, la nuova Gerusalemme, scender giù dal cielo d’appresso a Dio.
    """.trimIndent()

    val VULGATA = """
        GEN 1:1 In principio creavit Deus cælum et terram.
        TOB 1:1 Tobias ex tribu et civitate Nephthali.
        PSA 41:1 In finem. Intellectus filiis Core.
        PSA 41:2 [Quemadmodum desiderat cervus ad fontes aquarum, ita desiderat anima mea ad te, Deus.
        JOH 3:16 Sic enim Deus dilexit mundum, ut Filium suum unigenitum daret.
    """.trimIndent()

    val KJV = """
        GEN 1:1 In the beginning God created the heaven and the earth.
        GEN 1:4 And God saw the light, that [it was] good.
        PSA 42:1 To the chief Musician. As the hart panteth after the water brooks, so panteth my soul after thee, O God.
        JOH 3:16 ¶ For God so loved the world, that he gave his only begotten Son.
        1JO 4:8 He that loveth not knoweth not God; for God is love.
    """.trimIndent()
}

/** Sorgenti in memoria; una sorgente può essere sostituita per simulare errori. */
class FakeSourceFactory(
    private val texts: MutableMap<String, String> = mutableMapOf(
        "riveduta" to TestTexts.RIVEDUTA,
        "vulgata" to TestTexts.VULGATA,
        "kjv" to TestTexts.KJV,
    ),
) : TranslationSourceFactory {
    val failures = mutableMapOf<String, Exception>()
    var opened = 0
        private set

    fun setText(id: String, text: String) {
        texts[id] = text
    }

    override fun create(translation: it.lectio.bibbia.domain.model.Translation) = TranslationSource {
        opened++
        failures[translation.id]?.let { throw it }
        (texts[translation.id] ?: error("Nessun testo di prova per ${translation.id}")).byteInputStream()
    }
}

fun inMemoryDatabase(): BibleDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), BibleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
