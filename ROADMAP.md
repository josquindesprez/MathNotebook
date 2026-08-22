# ROADMAP.md

## Milestone 1 — editor matematico minimo (stato: in corso, questa iterazione)

- [x] Documenti architetturali
- [x] Scaffold shell desktop (.NET/WPF + WebView2) + frontend (Vite/React/TS)
- [x] Math AST core (tipi M1: Number, Identifier, Symbol, Binary/Unary,
      Fraction, Power, Subscript, Root, Relation, Placeholder, Group)
- [x] Tokenizer + parser sintassi rapida (M1: potenze, pedici, radici,
      frazioni, greche, operatori/relazioni base)
- [x] Renderer AstToLatex, AstToTypst, AstToMathML, AstToPlainText
- [x] Notebook vuoto, Text Cell, Math Cell (MathLive)
- [x] Navigazione placeholder (Tab/Shift+Tab, nativa MathLive)
- [x] Palette laterale: Basic, Powers, Fractions, Roots, Greek
- [x] Salvataggio/caricamento `.mathnb` (schemaVersion 1)
- [x] Test automatici parser/AST/renderer/document-io
- [x] Build e avvio della shell WPF verificati end-to-end su questa macchina
      (screenshot reale: finestra, toolbar, Text/Math cell, palette con le
      categorie M1, campo MathLive interattivo e reattivo)

## Milestone 2 — algebra lineare (completata)

- [x] Vectors, Matrices (nodi AST + parser + palette + template MathLive)
- [x] Matrix templates (2×2, 2×3, 3×2, 3×3, custom con popup righe/colonne
      — `CustomMatrixDialog.tsx`)
- [x] Transpose, inverse notation (`A^T`, `A^-1`), determinant, rank, norm,
      dot product (parser + renderer + palette "Linear Algebra")
- [x] Identity/zero matrix (template fissi + notazione `I_n`)
- [x] Set symbols: ℝ, ℕ, ℤ, ℚ, ℂ, ∈, ∉, ⊂, ⊆, ∪, ∩, ∅ (parser: `R`/`N`/`Z`/`Q`/`C`
      riservati; palette "Sets")
- [x] Notazione: `v ∈ R^3`, `T:R^2 -> R^3` (nuovo: `RelationOp` `:`/`to`),
      `A v = λv` (richiede spazio tra simboli di una lettera, vedi SYNTAX.md)
- [x] Parser sintassi rapida esteso (M2 in SYNTAX.md)
- [x] Bridge LaTeX → AST esteso per il round-trip via MathLive: matrici
      (`\begin{pmatrix}...\end{pmatrix}`, mai implementato prima), valore
      assoluto `|x|`, norma `\|v\|`, `\mathbb{R}` ecc. — nel farlo sono
      stati trovati e corretti 4 bug pre-esistenti nel bridge (vedi
      `web/src/parser/latex-bridge.test.ts`): un'equazione `A=B` produceva
      un AST corrotto invece di una `RelationNode`, le matrici non
      venivano proprio riconosciute, il separatore di riga `\\` aveva un
      controllo sbagliato di posizione, e `|...|`/`\|...\|` si
      autoconsumavano durante il parsing.
- [x] Test aggiuntivi: 57 test totali (parser M1+M2, bridge LaTeX, renderer,
      document-io, palette search) tutti verdi
- [x] Verifica visuale nella shell WPF reale (screenshot: categorie Sets/
      Vectors/Matrices/Linear Algebra nella palette, dialog Custom matrix)

## Milestone 3 — calcolo differenziale

- Derivative, second derivative, partial derivative
- Integral (indefinito e definito), summation, product, limit
- Funzioni trigonometriche e funzioni generiche `f(x)`, `f'(x)`, `f''(x)`
- Palette: categoria Calculus completa, contestuale su `f(x)`

## Milestone 4 — produttività

- Favorites (pin/unpin, sezione sempre in cima)
- Recent symbols
- Fuzzy search sulla palette (già presente in forma base da M1, qui rifinita)
- Command palette (`Ctrl+K`)
- Notebook tabs (più notebook aperti contemporaneamente)
- Autosave con recovery, Recent notebooks nella schermata iniziale
- Focus mode
- Copy as LaTeX / Copy as Typst dal menu contestuale e da scorciatoia

## Milestone 5 — calcolo simbolico

- Layer locale SymPy (processo Python locale invocato dalla shell .NET,
  nessuna rete, nessuna API a pagamento)
- Calculation Cell con `Run` esplicito
- simplify, expand, factor, solve, differentiate, integrate
- determinante/inversa/prodotto di matrici calcolati

## Milestone 6 — grafici

- Graph Cell, libreria di plotting leggera (valutare function-plot/JSXGraph
  rispetto a Plotly per peso e semplicità d'integrazione offline)
- `y=f(x)`, funzioni multiple, assi, zoom, pan, griglia

## Fuori scope (esplicitamente)

- CAS generico stile Mathematica
- Account, sync cloud, telemetria, funzioni a pagamento
- Calcolo automatico implicito su Math Cell (va sempre richiesto esplicitamente)
