# SYNTAX.md — sintassi rapida da tastiera

Questa sintassi è riconosciuta dentro una Math Cell mentre digiti (o
incollata) e viene convertita nello stesso Math AST prodotto dalla
palette. Non è LaTeX: è pensata per essere scritta velocemente su una
tastiera qualunque.

Molti costrutti coincidono già con le scorciatoie native di MathLive
(potenze, pedici, `sqrt`, lettere greche per nome, `>=`/`<=`/`!=`): il
parser di Math Notebook copre in aggiunta i costrutti che MathLive non
riconosce di default (`frac(...)`, matrici `[[...]]`, operatori di algebra
lineare, integrali/somme/limiti con notazione `_`/`^`).

## Stato per milestone

- **M1** (implementato): potenze, pedici, radici, frazioni, lettere greche, operatori/relazioni di base.
- **M2** (implementato): vettori, matrici, algebra lineare (`dot`, `norm`, `det`, `rank`, `transpose`, `inv`, `A^T`, `A^-1`), insiemi numerici (`R`,`N`,`Z`,`Q`,`C` riservati), segnatura di funzione (`T:R^2 -> R^3`), valore assoluto `|x|`.
- **M2.1** (implementato): alias in linguaggio naturale italiano per gli
  operatori più comuni (`alla`, `per`, `fratto`/`diviso`, `piu`/`meno`,
  `radice`) — vedi sezione dedicata sotto.
- **M3** (da fare): integrali, derivate, sommatorie, prodotti, limiti, funzioni trigonometriche — il parser li supporta già in larga parte (vedi tabella sotto), manca la palette dedicata.

## Regole generali

- Gli spazi sono opzionali salvo dove ambiguo; il parser è tollerante.
- `_` introduce un pedice, `^` un esponente. Per pedici/esponenti multi-carattere usa le parentesi: `x^(n+1)`, `a_(i,j)`.
- I nomi delle funzioni note (`sin`, `cos`, `det`, `dot`, `norm`, ...) sono riservati: `sin(x)` produce un `FunctionNode`, non una moltiplicazione implicita.
- `->` per `\to` (limiti, frecce), `:` per la segnatura di funzione (`T:R^2 -> R^3`).
- **Lettere singole adiacenti senza spazio formano UN SOLO identificatore**
  (es. `Av` è la variabile "Av", non "A per v"). Per la moltiplicazione
  implicita tra due simboli di una lettera (tipico di matrici/vettori:
  `A v`, `A B`) serve uno spazio o un `*` esplicito: `A v = lambda v`,
  non `Av = lambda v`. I nomi di funzione noti (`det`, `sin`, ...) restano
  parole intere riservate indipendentemente da questa regola.
- `R`, `N`, `Z`, `Q`, `C` **da soli** sono riservati ai rispettivi insiemi
  numerici (ℝ, ℕ, ℤ, ℚ, ℂ): non usarli come nomi di variabile nella
  sintassi rapida. Seguiti da `(` restano invece chiamate di funzione
  normali (es. `R(x)`).

## Linguaggio naturale

Oltre ai simboli (`^`, `/`, `*`, `+`, `-`, `sqrt(...)`), alcune parole
italiane comuni sono riconosciute come alias degli stessi operatori,
**sia separate da spazi sia "incollate"** a un numero o a una singola
lettera:

| Parola | Equivale a | Esempi |
|---|---|---|
| `alla` | `^` (potenza) | `2 alla 3`, `2alla3`, `2allax` → `2^3`, `2^x` |
| `per` | `*` (moltiplicazione) | `2 per x`, `2perx` → `2*x` |
| `fratto`, `diviso` | `/` (divisione) | `3 fratto 4`, `3fratto4` → `3/4` |
| `piu` | `+` | `3 piu 4` → `3+4` |
| `meno` | `-` (binario e unario) | `5 meno 2` → `5-2`; `meno 3` → `-3` |
| `radice` | `sqrt(...)` | `radice x`, `radice(x)`, `radicex` → `√x`; `2radice3` → `2√3` (moltiplicazione implicita, come `2sqrt(3)`) |

**Come funziona la forma "incollata"** (es. `2allax`): il tokenizzatore
tratta normalmente una sequenza di lettere consecutive come un unico
identificatore, quindi `allax` da solo diventerebbe la variabile "allax".
Per evitare questo, quando incontra una sequenza di lettere cerca al suo
interno una di queste parole chiave, ma **solo se preceduta da zero o un
carattere** (un nome di variabile plausibile, come la `x` in `xallay`).
Questo:
- copre i casi naturali: `2allax` (preceduta da un numero, già separato),
  `xallay` (preceduta da una singola lettera);
- evita quasi tutti i falsi positivi su parole più lunghe che contengono
  per caso una di queste parole chiave: `sopera` **non** viene spezzata
  (il prefisso "so" ha 2 caratteri, sopra la soglia).

Di conseguenza queste parole sono **riservate**: non usarle come nome di
variabile nella sintassi rapida (non è comunque una convenzione comune in
matematica). `radice` senza parentesi prende un solo atomo come radicando
(`radice x`, `radice9`); per un radicando composto serve la parentesi:
`radice(x+1)`.

### Numeri scritti in italiano (0-999)

Anche i numeri possono essere scritti a parole, da soli o dentro
espressioni più ampie — sono sostituiti dalla cifra corrispondente prima
ancora che il parser li veda, quindi si comportano in tutto e per tutto
come se fossero stati digitati direttamente:

```
quarantadue              → 42
quarantadue alla due     → 42^2
tre piu quattro          → 3+4
novantanove fratto tre   → 99/3
duecentotrentaquattro    → 234
```

Coperto l'intervallo **0-999**, con le regole di composizione standard
dell'italiano (elisioni: `ventuno`, `ventotto`, non `ventiuno`; accento su
"tré" nei composti: `ventitré`, `centotré`, ma il 3 da solo resta `tre`).
Come per le parole-operatore sopra, ogni numero è riservato come parola
intera: non serve preoccuparsene per i nomi di variabile abituali (lettere
singole), ma non usare per esempio `quarantadue` come nome. Non sono
invece riconosciute forme "incollate" tra un numero a parole e un'altra
parola (es. `quarantaduex`): per quello serve uno spazio o l'equivalente
in cifre (`42x`).

## Potenze e pedici

| Sintassi | Risultato | Nodo AST |
|---|---|---|
| `x^2` | x² | `PowerNode` |
| `x^(n+1)` | x^(n+1) | `PowerNode` con esponente composto |
| `x_1` | x₁ | `SubscriptNode` |
| `a_(i,j)` | a_{i,j} | `SubscriptNode` |

## Radici e frazioni

| Sintassi | Risultato |
|---|---|
| `sqrt(x)` | √x |
| `root(3,x)` | ³√x |
| `frac(a,b)` | a/b (frazione verticale) |
| `(a+b)/(c+d)` | frazione, riconosciuta anche da `/` tra gruppi |

## Lettere greche

Si scrivono per nome, minuscolo = minuscola, iniziale maiuscola = maiuscola:

```
alpha beta gamma delta epsilon
theta lambda mu pi
rho sigma tau phi omega

Gamma Delta Theta Lambda
Pi Sigma Phi Omega
```

## Relazioni e operatori

| Sintassi | Simbolo |
|---|---|
| `=` | = |
| `!=` | ≠ |
| `<`, `>`, `<=`, `>=` | <, >, ≤, ≥ |
| `~~` | ≈ |
| `equiv` | ≡ |
| `in` | ∈ |
| `notin` | ∉ |
| `\|x\|` | valore assoluto \|x\| (`FunctionNode` `abs`) |

## Insiemi (M2)

| Sintassi | Simbolo |
|---|---|
| `R`, `N`, `Z`, `Q`, `C` (da soli, non come nome funzione) | ℝ, ℕ, ℤ, ℚ, ℂ |
| `union`, `intersect` | ∪, ∩ |
| `subset`, `subseteq` | ⊂, ⊆ |
| `emptyset` | ∅ |

## Vettori e matrici (M2)

```
v = [1,2,3]              vettore riga
A = [[1,2],[3,4]]         matrice 2x2, righe separate da [ ], colonne da virgole
```

Algebra lineare:

```
dot(v,w)          prodotto scalare v · w
norm(v)           ‖v‖
det(A)            determinante
transpose(A)      A^T  (anche direttamente: A^T)
inv(A)            A^-1 (anche direttamente: A^-1)
rank(A)           rango
A^T
A^-1
I_n               matrice identità n×n (notazione: pedice generico, non un nodo AST dedicato)
```

Segnatura di funzione / applicazione lineare:

```
T:R^2 -> R^3      T: ℝ² → ℝ³  (RelationNode ':' che contiene una RelationNode 'to')
```

## Calcolo (M3)

```
int x^2 dx                integrale indefinito
int_0^1 x^2 dx             integrale definito
sum_i=1^n i                 sommatoria
prod_i=1^n i                produttoria
lim_x->0 sin(x)/x            limite
d/dx x^2                    derivata
d^2/dx^2 x^2                derivata seconda
partial f/partial x          derivata parziale
```

## Esempi end-to-end

| Digitato | AST prodotto | LaTeX derivato |
|---|---|---|
| `x^2` | `PowerNode(base=x, exponent=2)` | `x^2` |
| `sqrt(x)` | `RootNode(radicand=x)` | `\sqrt{x}` |
| `frac(a,b)` | `FractionNode(a,b)` | `\frac{a}{b}` |
| `[[1,2],[3,4]]` | `MatrixNode(rows=[[1,2],[3,4]])` | `\begin{pmatrix}1&2\\3&4\end{pmatrix}` |
| `int_0^1 x^2 dx` | `IntegralNode(x^2, x, 0, 1)` | `\int_{0}^{1} x^2\,dx` |
| `lim_x->0 sin(x)/x` | `LimitNode(...)` | `\lim_{x \to 0} \frac{\sin(x)}{x}` |

La stessa tabella vale al contrario per la palette: cliccando il template
corrispondente si ottiene lo stesso nodo AST, semplicemente con
`PlaceholderNode` al posto dei valori concreti.
