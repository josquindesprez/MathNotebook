# UI_SPEC.md

## Layout

```
┌─────────────────────────────────────────────────────────────┬───────────────┐
│  Tab bar: [Algebra lineare] [Derivate] [+]                    │  Math Palette │
├─────────────────────────────────────────────────────────────┤  (richiudibile)│
│                                                                 │  Search       │
│                     Notebook (area centrale)                   │  ───────────  │
│                                                                 │  ★ Favorites  │
│   [Text Cell]                                                   │  Recent       │
│   [Math Cell]                                                   │  ───────────  │
│   [Calculation Cell]                                             │  Basic        │
│   [Graph Cell]                                                   │  Powers       │
│                                                                 │  Fractions    │
│                                                                 │  Roots        │
│                                                                 │  Algebra      │
│                                                                 │  ...          │
├─────────────────────────────────────────────────────────────┴───────────────┤
│  Status bar: Math mode · LaTeX available · Saved                              │
└───────────────────────────────────────────────────────────────────────────────┘
```

- Colonna centrale a larghezza fissa leggibile (max ~760px, centrata, molto
  spazio bianco attorno), come un vero quaderno — non full-width.
- Sidebar destra ridimensionabile e richiudibile (`Alt+P` o click sull'icona),
  stato ricordato per sessione.
- Focus mode (scorciatoia `Ctrl+.`): nasconde tab bar, palette e status bar,
  lascia solo il notebook a piena larghezza.

## Celle

Ogni cella ha:
- un piccolo **gutter** a sinistra (drag handle per riordino, indicatore
  tipo cella, menu "..." con Duplica/Elimina/Cambia tipo);
- stato **selected** (bordo/outline sottile) vs **editing** (focus dentro il contenuto).

### Comportamento comune

| Scorciatoia | Effetto |
|---|---|
| `Enter` (cella selezionata, non in editing) | entra in modalità editing |
| `Enter` (dentro Text Cell) | newline normale |
| `Shift+Enter` | esegue/conferma la cella e sposta il focus sulla successiva (creandola se è l'ultima) |
| `Ctrl+Enter` | esegue/conferma la cella e mantiene il focus sulla cella corrente |
| `Tab` (dentro Math Cell) | prossimo placeholder della struttura (nativo MathLive) |
| `Shift+Tab` | placeholder precedente |
| `Tab` (cella selezionata, non in editing) | entra in editing (equivalente a Enter) |
| `Ctrl+↑` / `Ctrl+↓` | sposta la selezione alla cella precedente/successiva |
| `Ctrl+Shift+↑` / `Ctrl+Shift+↓` | sposta **la cella stessa** su/giù |
| `Ctrl+Shift+D` | duplica la cella selezionata |
| `Ctrl+Backspace` (cella vuota, selezionata) | elimina la cella |
| `Esc` (in editing) | esce in modalità selezionata (non elimina il contenuto) |

Click su una cella: seleziona. Doppio click / click sul contenuto: entra in
editing. Frecce ↑/↓ quando la cella è selezionata (non editing) spostano la
selezione come `Ctrl+↑/↓`.

### Raggruppamento celle

Un bordo sinistro colorato (3px) permette di raggruppare visivamente
sequenze di celle correlate — utile ad esempio per separare i passaggi di
un esercizio dal successivo. Nessuna scelta di colore richiesta: dal menu
"..." di una cella, **Start new group here** apre un nuovo gruppo a
partire da lì, con un colore diverso (ciclico su una tavolozza di 6, vedi
`model.ts`/`computeGroupColorIndices`) rispetto al gruppo precedente;
**Merge with previous group** lo riunisce al gruppo sopra. Il colore non è
mai scelto dall'utente, solo "diverso dal precedente" — coerente con la
richiesta originale: comporlo manualmente aggiungerebbe una scelta
superflua. Puramente visivo: non incide su parsing, valutazione o
struttura del documento (campo opzionale `groupStart` su ogni cella, vedi
DOCUMENT_FORMAT.md).

### Text Cell

- Editor testo semplice con Markdown di base: `**bold**`, `*italic*`,
  `` `code` ``, liste `-`/`1.`, titoli `#`/`##`.
- Matematica inline con `$...$`: renderizzata con KaTeX in sola lettura
  all'interno del testo (per editing strutturato di una singola formula si
  usa una Math Cell dedicata).
- Flusso copia-incolla: selezionare in una Math Cell e premere `Ctrl+C`
  copia LaTeX "nudo", pronto per essere incollato tra due `$` già scritti
  in una Text Cell. `MathCell.tsx` sovrascrive il comportamento di default
  di MathLive (che avvolgerebbe il LaTeX copiato in `$$...$$`, causando
  `$$$...$$$` se incollato tra `$...$` già presenti — non renderizzabile).

### Math Cell

- Il contenuto è un singolo `<math-field>` MathLive a piena larghezza della
  colonna, senza bordo visibile finché non è in focus (per sembrare pagina,
  non form).
- Digitare sintassi rapida (SYNTAX.md) o LaTeX diretto è entrambi permesso;
  il riconoscimento avviene "as you type" tramite gli `inlineShortcuts`
  nativi di MathLive (sqrt, lettere greche, `>=`, ...) più le nostre
  aggiunte registrate sullo stesso meccanismo — alias in linguaggio
  naturale (`alla`, `per`, `fratto`, ...) e numeri in italiano — vedi
  ARCHITECTURE.md e components/cells/MathCell.tsx.
- Nessuna valutazione automatica: la cella mostra solo la struttura scritta.
- Placeholder vuoti sono renderizzati come un piccolo quadrato tratteggiato
  `▢`, coerente con MathLive di default.

### Calculation Cell (M5, non prioritaria)

- Come Math Cell ma con un pulsante "Run" (o `Ctrl+Enter`) esplicito che
  invoca il layer SymPy locale e mostra il risultato sotto, in un blocco
  distinto (`= risultato`), mai sovrascrivendo l'input.

### Graph Cell (M6, non prioritaria)

- Campo espressione(i) in stile Desmos (una riga per funzione) sopra il
  canvas del grafico; pan/zoom con mouse, griglia sempre visibile.

## Math Palette

- Colonna destra, con in cima una **search bar** (fuzzy, vedi sotto), poi
  **Favorites** (sempre in cima, pinnabili), poi **Recent symbols**, poi le
  categorie in accordion (una sola espansa per volta di default, ma tutte
  ricercabili anche chiuse).
- Ogni pulsante mostra il simbolo/template reso graficamente (non il nome);
  hover mostra un tooltip con nome, anteprima e sintassi da tastiera
  equivalente (vedi "Help integrato" sotto).
- Click su un pulsante: se la Math Cell attiva ha un placeholder in focus,
  inserisce il template lì; altrimenti inserisce alla posizione del cursore
  o, se nessuna cella è in editing, crea una nuova Math Cell.
- Ordinamento contestuale: un'euristica leggera guarda il contenuto della
  cella attualmente in editing (es. presenza di `A =`, `f(x)`, un
  `MatrixNode` già presente) e porta in cima le categorie/template più
  probabili (Algebra lineare se c'è una matrice, Calcolo se c'è `f(x)`).
  Nessun ML: regole esplicite in `palette/context.ts`.

### Categorie (M1 in grassetto)

**Basic**, **Powers**, **Fractions**, **Roots**, Algebra, Linear Algebra,
Matrices, Vectors, Calculus, Sets, Logic, **Greek**, Functions,
Trigonometry, Relations, Arrows, Probability, Misc.

### Fuzzy search

- Match su nome del template/simbolo, alias (es. "integral" trova anche
  "definite integral", "double integral"), e sulla sintassi da tastiera
  associata (cercare "frac" trova Fraction).
- Algoritmo: subsequence match con punteggio (bonus per match all'inizio di
  parola, penalità per distanza tra i caratteri) — libreria leggera o
  implementazione locale minimale, nessuna dipendenza pesante.

### Custom matrix

Popup minimale con due campi numerici `Rows` / `Columns` (default 2×2,
max ragionevole 8×8) e pulsante "Insert"; genera un `MatrixNode` con celle
`PlaceholderNode`.

## Command Palette (`Ctrl+K` / `Ctrl+Shift+P`)

Elenco fuzzy-searchable di comandi (non simboli): "Insert matrix", "Insert
integral", "Change cell to Text", "Copy as LaTeX", "New notebook",
"Export...". Separata concettualmente dalla Math Palette: la Command
Palette esegue azioni sull'app/documento, la Math Palette inserisce
strutture matematiche.

## Help integrato

Tooltip on-hover su ogni elemento della palette:

```
Dot product
a · b

Keyboard: dot(a,b)
```

Obiettivo esplicito: l'utente impara gradualmente la sintassi da tastiera
osservando i tooltip, senza doverla memorizzare in anticipo.

## Menu contestuale su una Math Cell

```
Copy
Copy as LaTeX          (anche Ctrl+Shift+C)
Copy as Typst
Copy as Plain Text
Copy as Image           (M4+)
─────────────
Change cell to: Text / Math / Calculation / Graph
Duplicate cell           (Ctrl+Shift+D)
Delete cell
```

## Status bar

Discreta, testo piccolo, allineata a destra: stato del documento
(`Saved` / `Saving…` / `Unsaved changes`), e quando una Math Cell è in
focus: `Math mode` + tipo di struttura selezionata (es. `Fraction` /
`Matrix 2×2`) + `LaTeX available` (indica che Ctrl+Shift+C è pronto).

## Tema

Light e dark mode, seguono di default il tema di sistema Windows;
selezionabili anche manualmente nelle impostazioni del documento/app.
Tipografia: font sans-serif ad alta leggibilità per il testo (es. Inter/
system-ui), MathLive/KaTeX gestiscono autonomamente il font matematico
(Latin Modern / KaTeX font).

## Scorciatoie globali

```
Ctrl+N          Nuovo notebook
Ctrl+O          Apri
Ctrl+S          Salva
Ctrl+Shift+S    Salva con nome

Ctrl+Z          Undo
Ctrl+Y          Redo

Ctrl+K / Ctrl+Shift+P   Command palette

Alt+1..Alt+9    Salta a categoria palette (Basic, Algebra, Linear Algebra,
                Calculus, Greek, Sets, Logic, Functions, Misc — in
                quest'ordine)
Alt+P           Mostra/nascondi Math Palette
Ctrl+.          Focus mode

Ctrl+Shift+C    Copy as LaTeX (selezione/cella corrente)

Tab / Shift+Tab  placeholder successivo/precedente (dentro una struttura)
```

Le combinazioni possono cambiare in caso di conflitto con scorciatoie di
sistema Windows; questo documento è la sorgente di verità da aggiornare se
succede.
