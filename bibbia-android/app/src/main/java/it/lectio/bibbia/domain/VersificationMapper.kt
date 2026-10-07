package it.lectio.bibbia.domain

import it.lectio.bibbia.domain.model.ChapterRef
import it.lectio.bibbia.domain.model.Versification

/**
 * Converte i riferimenti fra sistemi di numerazione diversi.
 *
 * Attualmente gestisce la differenza principale tra numerazione ebraica (KJV, Riveduta, CEI) e
 * numerazione della Vulgata nei Salmi (es. Salmo 42 ebraico = Salmo 41 latino).
 * La numerazione dei versetti nei Salmi può differire di uno (la Vulgata conta il titolo come
 * versetto): il confronto mostra quindi sempre il riferimento effettivo di ciascuna traduzione.
 */
object VersificationMapper {

    fun mapChapter(ref: ChapterRef, from: Versification, to: Versification): ChapterRef {
        if (from == to || ref.bookId != "PSA") return ref
        val mapped = when (to) {
            Versification.VULGATE -> hebrewToVulgatePsalm(ref.chapter)
            Versification.STANDARD -> vulgateToHebrewPsalm(ref.chapter)
        }
        return ChapterRef(ref.bookId, mapped)
    }

    fun hebrewToVulgatePsalm(psalm: Int): Int = when (psalm) {
        in 1..8 -> psalm
        9, 10 -> 9
        in 11..113 -> psalm - 1
        114, 115 -> 113
        116 -> 114 // 116:1-9 = 114; 116:10-19 = 115
        in 117..146 -> psalm - 1
        147 -> 146 // 147:1-11 = 146; 147:12-20 = 147
        else -> psalm
    }

    fun vulgateToHebrewPsalm(psalm: Int): Int = when (psalm) {
        in 1..8 -> psalm
        9 -> 9 // 9 = 9 + 10
        in 10..112 -> psalm + 1
        113 -> 114 // 113 = 114 + 115
        114, 115 -> 116
        in 116..145 -> psalm + 1
        146, 147 -> 147
        else -> psalm
    }
}
