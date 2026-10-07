# Fonti, licenze e attribuzioni

Verifica effettuata il 6 ottobre 2026. Ogni testo incluso nell'app è stato scelto **solo dopo**
aver verificato che la licenza ne consente la redistribuzione. Nessun testo è stato ottenuto
aggirando copyright, DRM, paywall o condizioni d'uso.

I file originali dei testi sono in `app/src/main/assets/bibles/` (compressi gzip, contenuto
identico ai file `*_vpl.txt` distribuiti da eBible.org). Le schede "about" originali di eBible
sono conservate in questa cartella (`*_about.htm`).

## Testi biblici inclusi

### King James Version (KJV) — inglese

| | |
|---|---|
| Edizione | Authorized Version, testo standard di Cambridge del 1769, solo protocanonici |
| Fonte digitale | eBible.org, modulo `eng-kjv2006` — <https://ebible.org/eng-kjv2006/> (file `eng-kjv2006_vpl.zip`) |
| Licenza | **Pubblico dominio** |
| Condizioni | Nel Regno Unito la stampa è regolata dalle *Letters Patent* della Corona (Cambridge University Press, Oxford University Press, Collins). Fuori dal Regno Unito il testo è pienamente di pubblico dominio. Un'app non è una stampa del testo; nessuna restrizione alla redistribuzione digitale indicata dalla fonte. |
| Attribuzione mostrata | "King James Version, pubblico dominio. Fonte digitale: eBible.org (eng-kjv2006), per cortesia di CrossWire Bible Society." |
| Note | Le parole tra `[parentesi quadre]` sono le aggiunte dei traduttori (tradizionalmente in corsivo) e vengono mostrate in corsivo. Il segno `¶` indica l'inizio di paragrafo e viene usato nella modalità "Pagina". |

### La Sacra Bibbia — Riveduta (Luzzi, 1927) — italiano

| | |
|---|---|
| Edizione | Versione Riveduta di Giovanni Luzzi (1924), edizione 1927 |
| Fonte digitale | eBible.org, modulo `ita1927` — <https://ebible.org/ita1927/> (contributore: Società Biblica in Italia) |
| Licenza | **Pubblico dominio** (dichiarato dalla fonte; Giovanni Luzzi è morto nel 1948: diritti scaduti in Italia e UE, 70 anni *post mortem auctoris*) |
| Attribuzione mostrata | "La Sacra Bibbia, versione Riveduta (G. Luzzi), 1927, pubblico dominio. Fonte digitale: eBible.org (ita1927)." |

### Biblia Sacra Vulgata — Vulgata Clementina — latino

| | |
|---|---|
| Edizione | Editio Sixto-Clementina (1592, ed. 1598), secondo l'edizione Migne (1880) |
| Fonte digitale | eBible.org, modulo `latVUC` — <https://ebible.org/latVUC/> |
| Licenza | **Pubblico dominio** |
| Attribuzione mostrata | "Vulgata Clementina, pubblico dominio. Fonte digitale: eBible.org (latVUC)." |
| Note | Comprende i deuterocanonici (Tobia, Giuditta, Sapienza, Siracide, Baruc, 1–2 Maccabei). I Salmi seguono la numerazione latina/greca (Sal 42 ebraico = Psalmus 41): l'app converte automaticamente i riferimenti quando si cambia traduzione o si confronta. Le parentesi quadre editoriali presenti nel file di origine vengono rimosse. |

## Testo scaricabile su richiesta (non incluso nell'APK)

### La Sacra Bibbia — Diodati (revisione 1885) — italiano

| | |
|---|---|
| Fonte | eBible.org, modulo `ita1885` — <https://ebible.org/Scriptures/ita1885_vpl.zip> |
| Licenza | **Pubblico dominio** |
| Uso | Scaricata solo se l'utente lo chiede esplicitamente (schermata "Traduzioni"); poi conservata offline. È l'unico caso in cui l'app accede alla rete. |

## CEI 2008 — perché non è inclusa

La traduzione *La Sacra Bibbia* CEI 2008 è © Fondazione di Religione Santi Francesco d'Assisi e
Caterina da Siena (editrice per conto della Conferenza Episcopale Italiana), con tutti i diritti
riservati. La riproduzione e la redistribuzione, anche digitale, richiedono un'autorizzazione
esplicita dell'editore. Non esiste una licenza aperta né un'API ufficiale che ne consenta
l'inclusione in un'app di terze parti. Per questo **il testo CEI 2008 non è incluso** e non viene
scaricato da fonti non autorizzate.

Il codice è predisposto per aggiungerla in futuro tramite una fonte autorizzata: basta una voce in
`TranslationCatalog` (con `canonOrder = CanonOrder.CATHOLIC` e l'attribuzione richiesta) e, se la
fonte usa un formato diverso, una nuova implementazione di `TranslationSource`.

## Caratteri tipografici

| Carattere | Licenza | Copyright | File |
|---|---|---|---|
| EB Garamond | SIL Open Font License 1.1 | © 2017 The EB Garamond Project Authors | `LICENSES/OFL-EBGaramond.txt` |
| Literata | SIL Open Font License 1.1 | © 2017 The Literata Project Authors | `LICENSES/OFL-Literata.txt` |

I file dei caratteri (`app/src/main/res/font/`) provengono da Google Fonts e sono inclusi senza
modifiche, come consentito dalla OFL.

## Librerie

AndroidX (Jetpack Compose, Room, Navigation, DataStore, Lifecycle), Kotlin e kotlinx
(coroutines, serialization): Apache License 2.0. Solo per i test: JUnit (EPL 1.0), Robolectric
(MIT), Truth (Apache 2.0).
