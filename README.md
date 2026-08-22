# Math Notebook

Un quaderno digitale locale per studiare matematica: a metà tra un notebook
Jupyter, la semplicità di Desmos, le tavolozze di GeoGebra e un editor di
formule strutturato.

**Non è** un CAS. Non è un clone di Mathematica. L'obiettivo unico è:

> rendere la scrittura matematica da tastiera fluida anche per chi non
> conosce LaTeX.

## Principi

- Completamente locale, offline, nessun account, nessun backend, nessuna
  telemetria, nessuna funzione cloud obbligatoria.
- Scrittura e calcolo sono separati: una Math Cell non calcola mai da sola.
- Tastiera, tavolozza visuale ed editing strutturato diretto convergono
  sulla **stessa** struttura matematica interna (AST). Non esistono due
  sistemi paralleli.
- Interfaccia minimale in stile Notion/Obsidian/VS Code notebook, non
  software scientifico anni '90.

## Stack

- **Shell desktop**: .NET 9 / WPF + WebView2 (vedi [ARCHITECTURE.md](ARCHITECTURE.md#scelta-dello-stack)
  per il confronto con Tauri e Electron).
- **Frontend**: TypeScript + React + Vite, bundle statico caricato dalla
  WebView, nessuna rete richiesta a runtime.
- **Editing matematico strutturato**: [MathLive](https://cortexjs.io/mathlive/)
  (mathfield interattivo con placeholder e navigazione da tastiera nativa).
- **Rendering statico**: [KaTeX](https://katex.org/).
- **AST matematico**: modulo TypeScript indipendente da LaTeX, con renderer
  verso LaTeX, Typst, MathML e testo semplice (vedi [ARCHITECTURE.md](ARCHITECTURE.md)).

## Documenti

- [ARCHITECTURE.md](ARCHITECTURE.md) — stack, moduli, AST, pipeline di conversione.
- [SYNTAX.md](SYNTAX.md) — sintassi rapida da tastiera.
- [DOCUMENT_FORMAT.md](DOCUMENT_FORMAT.md) — formato `.mathnb`.
- [UI_SPEC.md](UI_SPEC.md) — layout, celle, palette, navigazione da tastiera.
- [ROADMAP.md](ROADMAP.md) — milestone.

## Struttura repository

```
MathNotebook/
  app/            host desktop .NET (WPF + WebView2)
  web/            frontend TypeScript/React (Vite), AST, parser, renderer, UI
  docs/           documenti architetturali (questo elenco)
```

## Sviluppo

```
cd web
npm install
npm run dev        # sviluppo rapido in browser, senza shell WPF
npm test            # test del parser/AST/renderer
npm run build        # bundle statico in web/dist, consumato dalla shell WPF

cd ../app
dotnet run           # avvia la shell desktop, carica web/dist
```
