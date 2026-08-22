# Architettura

## Scelta dello stack

Requisiti: app desktop Windows 11, leggera, completamente locale/offline,
nessun account/backend/telemetria. Evitare Electron salvo motivo tecnico
valido.

| Opzione | Pro | Contro (in questo ambiente) |
|---|---|---|
| **Tauri + TypeScript** | Binari molto piccoli, sandbox Rust, ottimo per app leggere offline | Richiede installare **Rust/Cargo** e le **MSVC C++ Build Tools**: toolchain assente su questa macchina, installazione pesante (GB, spesso privilegi admin, riavvii). Non disponibile "gratis" in questo ambiente. |
| **C#/.NET (WPF) + WebView2** | .NET 9 SDK e WebView2 runtime **già presenti** su questa macchina, nessuna installazione aggiuntiva. WebView2 è preinstallato su Windows 11. App nativa, leggera, avvio rapido. Permette comunque di riusare librerie JS mature per la matematica (MathLive, KaTeX) dentro la WebView. | Legato a Windows (accettabile: il target è Windows 11). Richiede un ponte JS↔C# per file system/dialoghi nativi. |
| **Electron** | Ecosistema enorme | Bundle pesante (Chromium+Node dedicati), consumo memoria elevato, esplicitamente da evitare salvo motivo valido. Nessun vantaggio reale qui: WebView2 dà lo stesso HTML/CSS/JS ma riusando il runtime di sistema. |

**Decisione**: **.NET 9 (WPF) + Microsoft WebView2** come shell nativa,
con un frontend **TypeScript + React (Vite)** caricato localmente dalla
WebView (nessuna rete a runtime). Se in futuro Rust verrà installato sulla
macchina di sviluppo, la UI web è portabile praticamente invariata anche
sotto Tauri (stesso bundle `web/dist`), quindi la scelta non è un vicolo
cieco.

Motivazione sintetica: a parità di "leggerezza" e assenza di rete, questa
combinazione **non richiede installare nulla di nuovo** in questo ambiente,
mantiene l'app nativa Windows, ed evita Electron.

## Confronto editor/rendering matematico

| Libreria | Navigazione tastiera | Editing strutturato (placeholder, Tab) | Matrici | Frazioni | Integrali | Selezione/copy-paste |
|---|---|---|---|---|---|---|
| KaTeX | — (solo rendering, non editabile) | no | rendering ok, non editabile | rendering ok | rendering ok | no |
| MathJax | — (solo rendering) | no | rendering ok | rendering ok | rendering ok | no |
| ProseMirror/Tiptap + estensione math custom | da costruire da zero | da costruire da zero | da costruire da zero | da costruire da zero | da costruire da zero | ok (eredita da ProseMirror) |
| **MathLive** | **nativa**, Tab/Shift+Tab tra placeholder, frecce dentro/fuori da frazioni/apici/radici | **nativa**: `\placeholder{}`, `executeCommand('insert', ...)` | template nativi editabili | native | native | nativo, incluso LaTeX/MathML/ASCIIMath sulla clipboard |

**Decisione**: **MathLive** per il campo di editing interattivo (Math Cell),
**KaTeX** per rendering statico non editabile (anteprime, testo inline nelle
Text Cell, output di Calculation Cell). Costruire un editor strutturato da
zero su ProseMirror avrebbe richiesto reimplementare esattamente ciò che
MathLive offre già (placeholder, navigazione, matrici) — lavoro non
giustificato per gli obiettivi del progetto.

Punto chiave: MathLive è la **superficie di editing**, non la fonte di
verità. La fonte di verità resta il nostro AST (sotto). MathLive
legge/scrive LaTeX; il bridge `latexToAst` / `astToLatex` mantiene i due
mondi sincronizzati ad ogni modifica.

## Pipeline unificata

```
                 ┌────────────────┐
  input tastiera │  shorthand      │
  (frac(a,b), … )│  parser         │──┐
                 └────────────────┘  │
                                      ▼
                              ┌───────────────┐        ┌────────────────┐
  click sulla palette ───────▶│   Math AST    │───────▶│  AstToLatex      │──▶ MathLive <math-field>
                              │ (fonte di      │        │  AstToTypst      │      (superficie di editing
  modifica diretta in         │  verità)      │◀───────│  AstToMathML     │       visuale, placeholder,
  MathLive (utente scrive) ──▶│               │ latexToAst │ AstToPlainText │       Tab-navigation)
                              └───────────────┘        └────────────────┘
                                      │
                                      ▼
                              salvataggio in .mathnb
                              (AST serializzato, non LaTeX)
```

Tre punti di ingresso (sintassi da tastiera, click sulla palette, editing
diretto in MathLive) producono/aggiornano **lo stesso nodo AST**. Il file
`.mathnb` salva l'AST, non il LaTeX: LaTeX/Typst/MathML/testo sono derivati
a richiesta (rendering o copy-as-*), mai la fonte di verità.

## Moduli (`web/src`)

```
web/src/
  ast/
    types.ts        definizioni dei nodi (vedi sotto)
    build.ts         helper per costruire nodi (factory functions)
  parser/
    tokenizer.ts      tokenizzatore della sintassi rapida
    parser.ts        parser (discesa ricorsiva/Pratt) -> Math AST
    latex-bridge.ts   AST -> stringa LaTeX di input per MathLive,
                      e parsing del LaTeX prodotto da MathLive -> AST
  render/
    toLatex.ts
    toTypst.ts
    toMathML.ts
    toPlainText.ts
    index.ts          dispatch comune (Renderer interface)
  notebook/
    model.ts          Cell, Notebook, versioning
    store.ts          stato applicativo (Zustand), undo/redo
    document-io.ts    load/save verso il bridge nativo
  palette/
    categories.ts     definizione categorie e template (§ UI_SPEC)
    search.ts         fuzzy search
    context.ts        euristica di riordino contestuale
  components/
    NotebookView.tsx
    cells/TextCell.tsx
    cells/MathCell.tsx
    palette/PaletteSidebar.tsx
    CommandPalette.tsx
    StatusBar.tsx
  bridge/
    hostBridge.ts     wrapper sopra window.chrome.webview, con fallback
                      browser (localStorage + File System Access API) per
                      sviluppo rapido in `npm run dev` senza la shell WPF
  app.tsx / main.tsx
```

## AST matematico

Nodo base:

```ts
interface MathNodeBase {
  id: string;        // uuid, stabile per undo/redo e per il bridge con MathLive
  type: string;
}
```

Tipi (Milestone 1 in grassetto, resto per M2/M3):

- **NumberNode** `{ value: string }` — stringa per preservare notazione (es. `0.500`).
- **IdentifierNode** `{ name: string }` — variabili, `x`, `A`, `theta` già risolto in simbolo se noto.
- **SymbolNode** `{ symbol: string, kind: 'greek'|'set'|'constant'|'misc' }` — π, ∅, ℝ, ∞...
- **BinaryOperationNode** `{ op: '+'|'-'|'*'|'/'|'cdot'|'circ', left, right }`
- **UnaryOperationNode** `{ op: '-'|'+'|'!', operand }`
- **FractionNode** `{ numerator, denominator }`
- **PowerNode** `{ base, exponent }`
- **SubscriptNode** `{ base, subscript }`
- **RootNode** `{ radicand, index? }` (index assente = radice quadrata)
- **FunctionNode** `{ name: string, args: MathNode[] }` — `sin`, `det`, `dot`, `norm`, `transpose`, funzioni utente
- **VectorNode** `{ orientation: 'row'|'column', entries: MathNode[] }`
- **MatrixNode** `{ rows: MathNode[][] }`
- IntegralNode `{ integrand, variable, lower?, upper? }`
- DerivativeNode `{ expression, variable, order }`
- PartialDerivativeNode `{ expression, variable, order }`
- SumNode / ProductNode `{ expression, index, lower, upper }`
- LimitNode `{ expression, variable, approaches, direction? }`
- RelationNode `{ op: '='|'!='|'<'|'>'|'<='|'>='|'~~'|'equiv', left, right }`
- SetNode `{ op: 'in'|'notin'|'subset'|'subseteq'|'union'|'intersect'|... , operands }`
- PiecewiseNode `{ cases: { expression, condition }[] }`
- SystemNode `{ equations: MathNode[] }`
- PlaceholderNode `{}` — slot vuoto navigabile (usato sia nell'AST interno
  sia mappato su `\placeholder{}` di MathLive)
- GroupNode `{ expression }` — parentesi esplicite quando semanticamente rilevanti

Ogni nodo è puro dato (no metodi), serializzabile 1:1 in JSON: questo è
anche il formato salvato dentro `.mathnb` (vedi DOCUMENT_FORMAT.md).

## Renderer

Interfaccia comune:

```ts
interface MathRenderer<T> {
  render(node: MathNode): T;
}
```

Implementazioni indipendenti (`AstToLatex`, `AstToTypst`, `AstToMathML`,
`AstToPlainText`) — ciascuna è un dispatch su `node.type`, senza dipendenze
tra loro. Nessuna passa attraverso LaTeX come formato intermedio (evita
accoppiare il formato interno a LaTeX, come richiesto).

`AstToPlainText` usa Unicode quando possibile (es. `α`, `≤`, `√`, apici
unicode per potenze semplici `x²`), fallback a notazione ASCII (`x^2`) per
casi non rappresentabili in Unicode (esponenti complessi, frazioni con
espressioni).

**Nota verificata su questa macchina**: dopo la chiusura pulita della
finestra (`Window.Closed` chiama esplicitamente `WebView.Dispose()`), i
processi figlio `msedgewebview2.exe` possono restare visibili in Task
Manager per circa un minuto prima di terminare da soli. È il
comportamento standard del runtime WebView2 Evergreen (mantiene un pool
di processi "caldi" per accelerare il prossimo avvio), non una perdita di
processi dell'app: `MathNotebook.exe` stesso termina immediatamente e non
resta nulla in esecuzione dopo la finestra di grazia.

## Bridge nativo (WPF ↔ WebView)

Protocollo JSON minimale su `postMessage`, richiesta/risposta con `id`:

```ts
// web -> host
{ id, method: 'file.open' | 'file.save' | 'file.saveAs' | 'file.recent' | 'window.setTitle', params }
// host -> web
{ id, result } | { id, error }
```

Il modulo `bridge/hostBridge.ts` espone funzioni async (`openFile()`,
`saveFile(doc)`, ...) e, se `window.chrome.webview` non esiste (sviluppo in
browser puro), usa un fallback locale (download/upload di file, o
`localStorage`) così `npm run dev` resta utilizzabile senza la shell.

## Versioning e testing

- Parser, AST e renderer sono puri (no DOM), testati con `vitest` senza
  bisogno di UI o di WebView2.
- Il documento `.mathnb` ha `schemaVersion` esplicito; il loader applica
  migrazioni incrementali (vedi DOCUMENT_FORMAT.md).
