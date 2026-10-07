package it.lectio.bibbia.domain.model

/** Testamento a cui appartiene un libro. I deuterocanonici sono considerati parte dell'Antico Testamento. */
enum class Testament { OLD, NEW }

/** Lingua di una traduzione; determina i nomi dei libri mostrati. */
enum class BibleLanguage(val code: String) {
    ITALIAN("it"),
    ENGLISH("en"),
    LATIN("la");

    companion object {
        fun fromCode(code: String): BibleLanguage = entries.firstOrNull { it.code == code } ?: ITALIAN
    }
}

/**
 * Descrizione di un libro biblico, indipendente da qualunque traduzione.
 *
 * [id] è il codice USFM a tre caratteri (GEN, PSA, JHN, ...): è l'identificatore stabile usato in
 * tutto il database, nei segnalibri e nelle evidenziazioni.
 */
data class BookInfo(
    val id: String,
    val testament: Testament,
    val deuterocanonical: Boolean,
    val italian: String,
    val english: String,
    val latin: String,
    /** Abbreviazioni italiane (stile CEI) per il parser dei riferimenti. */
    val italianAbbreviations: List<String>,
    /** Abbreviazioni inglesi e alias aggiuntivi. */
    val englishAbbreviations: List<String>,
    /** Nomi/alias latini aggiuntivi. */
    val latinAliases: List<String> = emptyList(),
) {
    fun name(language: BibleLanguage): String = when (language) {
        BibleLanguage.ITALIAN -> italian
        BibleLanguage.ENGLISH -> english
        BibleLanguage.LATIN -> latin
    }

    /** Abbreviazione breve (italiana) usata in spazi ristretti. */
    val shortName: String get() = italianAbbreviations.first()
}

/** Ordine dei libri. Ogni traduzione dichiara quale ordine segue. */
enum class CanonOrder {
    /** Ordine protestante/KJV: 66 libri. */
    PROTESTANT,

    /** Ordine della Vulgata Clementina (deuterocanonici intercalati, Maccabei alla fine dell'AT). */
    VULGATE,

    /** Ordine cattolico moderno (CEI, ecc.). */
    CATHOLIC,
}

object Canon {
    private fun ot(
        id: String, it: String, en: String, la: String,
        itAbbr: List<String>, enAbbr: List<String>, laAlias: List<String> = emptyList(),
        dc: Boolean = false,
    ) = BookInfo(id, Testament.OLD, dc, it, en, la, itAbbr, enAbbr, laAlias)

    private fun nt(
        id: String, it: String, en: String, la: String,
        itAbbr: List<String>, enAbbr: List<String>, laAlias: List<String> = emptyList(),
    ) = BookInfo(id, Testament.NEW, false, it, en, la, itAbbr, enAbbr, laAlias)

    val books: List<BookInfo> = listOf(
        ot("GEN", "Genesi", "Genesis", "Genesis", listOf("Gen"), listOf("Gn", "Ge")),
        ot("EXO", "Esodo", "Exodus", "Exodus", listOf("Es"), listOf("Ex", "Exod")),
        ot("LEV", "Levitico", "Leviticus", "Leviticus", listOf("Lv", "Lev"), listOf("Le")),
        ot("NUM", "Numeri", "Numbers", "Numeri", listOf("Nm", "Num"), listOf("Nu")),
        ot("DEU", "Deuteronomio", "Deuteronomy", "Deuteronomium", listOf("Dt", "Deut"), listOf("De")),
        ot("JOS", "Giosuè", "Joshua", "Josue", listOf("Gs", "Gios"), listOf("Josh", "Jos")),
        ot("JDG", "Giudici", "Judges", "Judicum", listOf("Gdc", "Giud"), listOf("Judg", "Jdg")),
        ot("RUT", "Rut", "Ruth", "Ruth", listOf("Rt"), listOf("Ru")),
        ot("1SA", "1 Samuele", "1 Samuel", "I Regum", listOf("1Sam", "1Sm"), listOf("1Sa"), listOf("1Samuelis")),
        ot("2SA", "2 Samuele", "2 Samuel", "II Regum", listOf("2Sam", "2Sm"), listOf("2Sa"), listOf("2Samuelis")),
        ot("1KI", "1 Re", "1 Kings", "III Regum", listOf("1Re", "1Rg"), listOf("1Kgs", "1Ki", "1Kings")),
        ot("2KI", "2 Re", "2 Kings", "IV Regum", listOf("2Re", "2Rg"), listOf("2Kgs", "2Ki", "2Kings"), listOf("4Regum")),
        ot("1CH", "1 Cronache", "1 Chronicles", "I Paralipomenon", listOf("1Cr", "1Cron"), listOf("1Chr", "1Ch")),
        ot("2CH", "2 Cronache", "2 Chronicles", "II Paralipomenon", listOf("2Cr", "2Cron"), listOf("2Chr", "2Ch")),
        ot("EZR", "Esdra", "Ezra", "I Esdræ", listOf("Esd"), listOf("Ezr"), listOf("Esdras", "1Esdrae")),
        ot("NEH", "Neemia", "Nehemiah", "II Esdræ", listOf("Ne", "Nee"), listOf("Neh"), listOf("Nehemias", "2Esdrae")),
        ot("TOB", "Tobia", "Tobit", "Tobiæ", listOf("Tb", "Tob"), listOf("Tobit"), listOf("Tobias"), dc = true),
        ot("JDT", "Giuditta", "Judith", "Judith", listOf("Gdt"), listOf("Jdt", "Jdth"), dc = true),
        ot("EST", "Ester", "Esther", "Esther", listOf("Est"), listOf("Esth")),
        ot("JOB", "Giobbe", "Job", "Job", listOf("Gb"), listOf("Jb")),
        ot("PSA", "Salmi", "Psalms", "Psalmi", listOf("Sal", "Salmo", "Sl"), listOf("Ps", "Psa", "Psalm", "Pss"), listOf("Psalmus", "Psalterium")),
        ot("PRO", "Proverbi", "Proverbs", "Proverbia", listOf("Pr", "Prv", "Prov"), listOf("Prv", "Pro")),
        ot("ECC", "Ecclesiaste", "Ecclesiastes", "Ecclesiastes", listOf("Qo", "Qoelet", "Qohelet", "Eccle"), listOf("Eccl", "Ecc", "Qoh")),
        ot("SNG", "Cantico dei Cantici", "Song of Solomon", "Canticum Canticorum", listOf("Ct", "Cant", "Cantico"), listOf("Song", "Sng", "SoS", "SongofSongs", "Canticles"), listOf("Canticum")),
        ot("WIS", "Sapienza", "Wisdom", "Sapientia", listOf("Sap"), listOf("Wis", "Ws"), dc = true),
        ot("SIR", "Siracide", "Sirach", "Ecclesiasticus", listOf("Sir", "Siracide"), listOf("Ecclus"), dc = true),
        ot("ISA", "Isaia", "Isaiah", "Isaias", listOf("Is"), listOf("Isa")),
        ot("JER", "Geremia", "Jeremiah", "Jeremias", listOf("Ger"), listOf("Jer", "Je")),
        ot("LAM", "Lamentazioni", "Lamentations", "Lamentationes", listOf("Lam"), listOf("La")),
        ot("BAR", "Baruc", "Baruch", "Baruch", listOf("Bar"), listOf("Ba"), dc = true),
        ot("EZK", "Ezechiele", "Ezekiel", "Ezechiel", listOf("Ez"), listOf("Ezek", "Ezk", "Eze")),
        ot("DAN", "Daniele", "Daniel", "Daniel", listOf("Dn"), listOf("Dan", "Da")),
        ot("HOS", "Osea", "Hosea", "Osee", listOf("Os"), listOf("Hos", "Ho")),
        ot("JOL", "Gioele", "Joel", "Joel", listOf("Gl"), listOf("Jl", "Joe")),
        ot("AMO", "Amos", "Amos", "Amos", listOf("Am"), listOf("Amo")),
        ot("OBA", "Abdia", "Obadiah", "Abdias", listOf("Abd"), listOf("Obad", "Ob", "Oba")),
        ot("JON", "Giona", "Jonah", "Jonas", listOf("Gn", "Gion"), listOf("Jon", "Jnh")),
        ot("MIC", "Michea", "Micah", "Michæa", listOf("Mi", "Mic"), listOf("Mc")),
        ot("NAM", "Naum", "Nahum", "Nahum", listOf("Na"), listOf("Nah", "Nam")),
        ot("HAB", "Abacuc", "Habakkuk", "Habacuc", listOf("Ab", "Abac"), listOf("Hab", "Hb")),
        ot("ZEP", "Sofonia", "Zephaniah", "Sophonias", listOf("Sof"), listOf("Zeph", "Zep", "Zp")),
        ot("HAG", "Aggeo", "Haggai", "Aggæus", listOf("Ag"), listOf("Hag", "Hg")),
        ot("ZEC", "Zaccaria", "Zechariah", "Zacharias", listOf("Zc", "Zac"), listOf("Zech", "Zec")),
        ot("MAL", "Malachia", "Malachi", "Malachias", listOf("Ml", "Mal"), listOf("Mal")),
        ot("1MA", "1 Maccabei", "1 Maccabees", "I Machabæorum", listOf("1Mac", "1Mc"), listOf("1Macc", "1Ma"), dc = true),
        ot("2MA", "2 Maccabei", "2 Maccabees", "II Machabæorum", listOf("2Mac", "2Mc"), listOf("2Macc", "2Ma"), dc = true),

        nt("MAT", "Matteo", "Matthew", "Matthæus", listOf("Mt"), listOf("Matt", "Mat"), listOf("Matthaeum")),
        nt("MRK", "Marco", "Mark", "Marcus", listOf("Mc", "Mr"), listOf("Mk", "Mrk", "Mar"), listOf("Marcum")),
        nt("LUK", "Luca", "Luke", "Lucas", listOf("Lc"), listOf("Lk", "Luk"), listOf("Lucam")),
        nt("JHN", "Giovanni", "John", "Joannes", listOf("Gv", "Giov"), listOf("Jn", "Jhn", "Joh"), listOf("Joannem", "Iohannes")),
        nt("ACT", "Atti degli Apostoli", "Acts", "Actus Apostolorum", listOf("At", "Atti"), listOf("Act", "Ac"), listOf("Actus")),
        nt("ROM", "Romani", "Romans", "ad Romanos", listOf("Rm", "Rom"), listOf("Ro")),
        nt("1CO", "1 Corinzi", "1 Corinthians", "I ad Corinthios", listOf("1Cor"), listOf("1Co")),
        nt("2CO", "2 Corinzi", "2 Corinthians", "II ad Corinthios", listOf("2Cor"), listOf("2Co")),
        nt("GAL", "Galati", "Galatians", "ad Galatas", listOf("Gal"), listOf("Ga")),
        nt("EPH", "Efesini", "Ephesians", "ad Ephesios", listOf("Ef"), listOf("Eph")),
        nt("PHP", "Filippesi", "Philippians", "ad Philippenses", listOf("Fil"), listOf("Phil", "Php")),
        nt("COL", "Colossesi", "Colossians", "ad Colossenses", listOf("Col"), listOf("Co")),
        nt("1TH", "1 Tessalonicesi", "1 Thessalonians", "I ad Thessalonicenses", listOf("1Ts", "1Tess"), listOf("1Th", "1Thess")),
        nt("2TH", "2 Tessalonicesi", "2 Thessalonians", "II ad Thessalonicenses", listOf("2Ts", "2Tess"), listOf("2Th", "2Thess")),
        nt("1TI", "1 Timoteo", "1 Timothy", "I ad Timotheum", listOf("1Tm", "1Tim"), listOf("1Ti")),
        nt("2TI", "2 Timoteo", "2 Timothy", "II ad Timotheum", listOf("2Tm", "2Tim"), listOf("2Ti")),
        nt("TIT", "Tito", "Titus", "ad Titum", listOf("Tt", "Tit"), listOf("Ti")),
        nt("PHM", "Filemone", "Philemon", "ad Philemonem", listOf("Fm", "Filem"), listOf("Phlm", "Phm")),
        nt("HEB", "Ebrei", "Hebrews", "ad Hebræos", listOf("Eb"), listOf("Heb")),
        nt("JAS", "Giacomo", "James", "Jacobi", listOf("Gc", "Giac"), listOf("Jas", "Jm")),
        nt("1PE", "1 Pietro", "1 Peter", "I Petri", listOf("1Pt", "1Pie"), listOf("1Pet", "1Pe")),
        nt("2PE", "2 Pietro", "2 Peter", "II Petri", listOf("2Pt", "2Pie"), listOf("2Pet", "2Pe")),
        nt("1JN", "1 Giovanni", "1 John", "I Joannis", listOf("1Gv", "1Giov"), listOf("1Jn", "1Jhn", "1Jo")),
        nt("2JN", "2 Giovanni", "2 John", "II Joannis", listOf("2Gv", "2Giov"), listOf("2Jn", "2Jhn", "2Jo")),
        nt("3JN", "3 Giovanni", "3 John", "III Joannis", listOf("3Gv", "3Giov"), listOf("3Jn", "3Jhn", "3Jo")),
        nt("JUD", "Giuda", "Jude", "Judæ", listOf("Gd", "Giuda"), listOf("Jud", "Jude")),
        nt("REV", "Apocalisse", "Revelation", "Apocalypsis", listOf("Ap", "Apoc"), listOf("Rev", "Re", "Rv", "Revelations")),
    )

    private val byId: Map<String, BookInfo> = books.associateBy { it.id }

    fun book(id: String): BookInfo? = byId[id]

    fun requireBook(id: String): BookInfo = byId[id] ?: error("Libro sconosciuto: $id")

    private val protestantOrder: List<String> = books.filterNot { it.deuterocanonical }.map { it.id }

    private val vulgateOrder: List<String> = listOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH",
        "EZR", "NEH", "TOB", "JDT", "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "WIS", "SIR", "ISA", "JER",
        "LAM", "BAR", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG",
        "ZEC", "MAL", "1MA", "2MA",
    ) + books.filter { it.testament == Testament.NEW }.map { it.id }

    private val catholicOrder: List<String> = listOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH",
        "EZR", "NEH", "TOB", "JDT", "EST", "1MA", "2MA", "JOB", "PSA", "PRO", "ECC", "SNG", "WIS", "SIR",
        "ISA", "JER", "LAM", "BAR", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB",
        "ZEP", "HAG", "ZEC", "MAL",
    ) + books.filter { it.testament == Testament.NEW }.map { it.id }

    /** Posizione di un libro nell'ordine indicato; i libri non previsti finiscono in coda. */
    fun position(bookId: String, order: CanonOrder): Int {
        val list = when (order) {
            CanonOrder.PROTESTANT -> protestantOrder
            CanonOrder.VULGATE -> vulgateOrder
            CanonOrder.CATHOLIC -> catholicOrder
        }
        val index = list.indexOf(bookId)
        return if (index >= 0) index else list.size + books.indexOfFirst { it.id == bookId }.coerceAtLeast(0)
    }
}
