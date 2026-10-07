package it.lectio.bibbia.data.source

import it.lectio.bibbia.domain.model.BibleLanguage
import it.lectio.bibbia.domain.model.CanonOrder
import it.lectio.bibbia.domain.model.TextConventions
import it.lectio.bibbia.domain.model.Translation
import it.lectio.bibbia.domain.model.TranslationOrigin
import it.lectio.bibbia.domain.model.Versification

/**
 * Elenco delle traduzioni conosciute dall'app. Fonti e licenze sono documentate in
 * LICENSES/ATTRIBUTIONS.md.
 *
 * Per aggiungere una traduzione (es. CEI 2008 tramite fonte autorizzata) basta aggiungere una voce qui
 * e, se necessario, una nuova implementazione di [TranslationSource].
 */
object TranslationCatalog {

    val KJV = Translation(
        id = "kjv",
        abbreviation = "KJV",
        languageLabel = "Inglese",
        name = "King James Version",
        edition = "Authorized Version, testo standard di Cambridge del 1769",
        language = BibleLanguage.ENGLISH,
        description = "La traduzione inglese autorizzata del 1611, nel testo normalizzato del 1769. " +
            "Le parole in corsivo sono state aggiunte dai traduttori per chiarezza.",
        license = "Pubblico dominio (nel Regno Unito soggetta a Letters Patent della Corona per la stampa).",
        attribution = "Testo: King James Version, pubblico dominio. Fonte digitale: eBible.org " +
            "(eng-kjv2006), per cortesia di CrossWire Bible Society.",
        sourceUrl = "https://ebible.org/eng-kjv2006/",
        versification = Versification.STANDARD,
        canonOrder = CanonOrder.PROTESTANT,
        origin = TranslationOrigin.Bundled("bibles/eng-kjv2006_vpl.txt.gzip"),
        conventions = TextConventions(bracketsAreItalics = true, pilcrowMarksParagraph = true),
        expectedVerses = 31_102,
    )

    val RIVEDUTA = Translation(
        id = "riveduta",
        abbreviation = "RIV",
        languageLabel = "Italiano",
        name = "La Sacra Bibbia — Riveduta",
        edition = "Versione Riveduta di Giovanni Luzzi, edizione 1927",
        language = BibleLanguage.ITALIAN,
        description = "Revisione della Diodati curata da Giovanni Luzzi (1924, ed. 1927), in un italiano " +
            "classico e solenne.",
        license = "Pubblico dominio.",
        attribution = "Testo: La Sacra Bibbia, versione Riveduta (G. Luzzi), 1927, pubblico dominio. " +
            "Fonte digitale: eBible.org (ita1927).",
        sourceUrl = "https://ebible.org/ita1927/",
        versification = Versification.STANDARD,
        canonOrder = CanonOrder.PROTESTANT,
        origin = TranslationOrigin.Bundled("bibles/ita1927_vpl.txt.gzip"),
        expectedVerses = 31_102,
    )

    val VULGATA = Translation(
        id = "vulgata",
        abbreviation = "VUL",
        languageLabel = "Latino",
        name = "Biblia Sacra Vulgata — Vulgata Clementina",
        edition = "Editio Sixto-Clementina (1592/1598), testo secondo l'edizione Migne (1880)",
        language = BibleLanguage.LATIN,
        description = "La Vulgata di san Girolamo nell'edizione ufficiale promulgata da Clemente VIII. " +
            "I Salmi seguono la numerazione latina (es. Sal 42 = Psalmus 41).",
        license = "Pubblico dominio.",
        attribution = "Testo: Vulgata Clementina, pubblico dominio. Fonte digitale: eBible.org (latVUC).",
        sourceUrl = "https://ebible.org/latVUC/",
        versification = Versification.VULGATE,
        canonOrder = CanonOrder.VULGATE,
        origin = TranslationOrigin.Bundled("bibles/latVUC_vpl.txt.gzip"),
        conventions = TextConventions(stripBrackets = true),
        expectedVerses = 35_809,
    )

    /** Esempio di traduzione scaricabile su richiesta: dimostra il percorso download → offline. */
    val DIODATI = Translation(
        id = "diodati",
        abbreviation = "DIO",
        languageLabel = "Italiano",
        name = "La Sacra Bibbia — Diodati",
        edition = "Traduzione di Giovanni Diodati (1641), revisione 1885",
        language = BibleLanguage.ITALIAN,
        description = "La storica traduzione di Giovanni Diodati. Da scaricare una sola volta (circa 5 MB); " +
            "poi resta disponibile offline.",
        license = "Pubblico dominio.",
        attribution = "Testo: Diodati 1885, pubblico dominio. Fonte digitale: eBible.org (ita1885).",
        sourceUrl = "https://ebible.org/ita1885/",
        versification = Versification.STANDARD,
        canonOrder = CanonOrder.PROTESTANT,
        origin = TranslationOrigin.Downloadable("https://ebible.org/Scriptures/ita1885_vpl.zip", 5),
        expectedVerses = 31_095,
    )

    val all: List<Translation> = listOf(KJV, RIVEDUTA, VULGATA, DIODATI)

    val bundled: List<Translation> = all.filter { it.origin is TranslationOrigin.Bundled }

    val default: Translation = RIVEDUTA

    fun find(id: String): Translation? = all.firstOrNull { it.id == id }
}
