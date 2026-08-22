// Definizione delle categorie e dei template della Math Palette.
// Vedi UI_SPEC.md. Il testo inserito è LaTeX con \placeholder{} per gli
// slot navigabili: è lo stesso formato nativo di MathLive, quindi
// l'inserimento da palette produce esattamente la stessa struttura
// ottenibile digitando la sintassi rapida (vedi ARCHITECTURE.md, "Pipeline
// unificata").

export interface PaletteItem {
  id: string;
  category: PaletteCategoryName;
  label: string;
  description: string;
  keyboardHint?: string;
  insertLatex: string;
  keywords?: string[];
  /** true per elementi che aprono un mini popup invece di inserire subito
   * (es. Custom matrix: chiede Rows/Columns, vedi UI_SPEC.md). */
  opensDialog?: true;
}

// Ordine di visualizzazione completo (vedi UI_SPEC.md). M1 popola Basic,
// Powers, Fractions, Roots, Greek; M2 aggiunge Sets, Vectors, Matrices,
// Linear Algebra. Le categorie ancora vuote (Calculus, Logic, ...)
// compariranno nelle milestone successive (vedi ROADMAP.md) e per ora
// restano nascoste (vedi nonEmptyCategories()).
export const CATEGORY_ORDER = [
  'Basic',
  'Powers',
  'Fractions',
  'Roots',
  'Greek',
  'Algebra',
  'Linear Algebra',
  'Matrices',
  'Vectors',
  'Calculus',
  'Sets',
  'Logic',
  'Functions',
  'Trigonometry',
  'Relations',
  'Arrows',
  'Probability',
  'Misc',
] as const;

export type PaletteCategoryName = (typeof CATEGORY_ORDER)[number];

/** Genera il LaTeX di una matrice rows×cols con uno \placeholder{} per
 * cella, usato sia dai template fissi (2×2, 3×3, ...) sia dal popup
 * "Custom matrix" (vedi UI_SPEC.md). */
export function matrixTemplateLatex(rows: number, cols: number): string {
  const row = Array.from({ length: cols }, () => '\\placeholder{}').join(' & ');
  const body = Array.from({ length: rows }, () => row).join(' \\\\\n');
  return `\\begin{pmatrix}\n${body}\n\\end{pmatrix}`;
}

function vectorTemplateLatex(size: number, orientation: 'row' | 'column'): string {
  const entries = Array.from({ length: size }, () => '\\placeholder{}');
  const body = orientation === 'row' ? entries.join(' & ') : entries.join(' \\\\\n');
  return `\\begin{pmatrix}\n${body}\n\\end{pmatrix}`;
}

const GREEK_NAMES: [string, string][] = [
  ['alpha', 'α'],
  ['beta', 'β'],
  ['gamma', 'γ'],
  ['delta', 'δ'],
  ['epsilon', 'ε'],
  ['theta', 'θ'],
  ['lambda', 'λ'],
  ['mu', 'μ'],
  ['pi', 'π'],
  ['rho', 'ρ'],
  ['sigma', 'σ'],
  ['tau', 'τ'],
  ['phi', 'φ'],
  ['omega', 'ω'],
  ['Gamma', 'Γ'],
  ['Delta', 'Δ'],
  ['Theta', 'Θ'],
  ['Lambda', 'Λ'],
  ['Pi', 'Π'],
  ['Sigma', 'Σ'],
  ['Phi', 'Φ'],
  ['Omega', 'Ω'],
];

export const PALETTE_ITEMS: PaletteItem[] = [
  // ---- Basic ----
  { id: 'basic-eq', category: 'Basic', label: '=', description: 'Uguale', keyboardHint: '=', insertLatex: '=' },
  { id: 'basic-neq', category: 'Basic', label: '≠', description: 'Diverso', keyboardHint: 'a != b', insertLatex: '\\neq' },
  { id: 'basic-lt', category: 'Basic', label: '<', description: 'Minore', keyboardHint: 'a < b', insertLatex: '<' },
  { id: 'basic-gt', category: 'Basic', label: '>', description: 'Maggiore', keyboardHint: 'a > b', insertLatex: '>' },
  { id: 'basic-le', category: 'Basic', label: '≤', description: 'Minore o uguale', keyboardHint: 'a <= b', insertLatex: '\\leq' },
  { id: 'basic-ge', category: 'Basic', label: '≥', description: 'Maggiore o uguale', keyboardHint: 'a >= b', insertLatex: '\\geq' },
  { id: 'basic-approx', category: 'Basic', label: '≈', description: 'Circa uguale', keyboardHint: 'a ~~ b', insertLatex: '\\approx' },
  {
    id: 'basic-abs',
    category: 'Basic',
    label: '|x|',
    description: 'Valore assoluto',
    keyboardHint: 'abs(x)',
    insertLatex: '\\left|\\placeholder{}\\right|',
    keywords: ['absolute value', 'modulo'],
  },

  // ---- Powers ----
  {
    id: 'power-square',
    category: 'Powers',
    label: 'x²',
    description: 'Quadrato',
    keyboardHint: 'x^2',
    insertLatex: '\\placeholder{}^{2}',
    keywords: ['square', 'quadrato', 'power 2'],
  },
  {
    id: 'power-generic',
    category: 'Powers',
    label: 'xⁿ',
    description: 'Potenza generica',
    keyboardHint: 'x^n',
    insertLatex: '\\placeholder{}^{\\placeholder{}}',
    keywords: ['power', 'esponente', 'exponent'],
  },
  {
    id: 'power-subscript',
    category: 'Powers',
    label: 'x₁',
    description: 'Pedice',
    keyboardHint: 'x_1',
    insertLatex: '\\placeholder{}_{\\placeholder{}}',
    keywords: ['subscript', 'index', 'indice'],
  },

  // ---- Fractions ----
  {
    id: 'fraction-basic',
    category: 'Fractions',
    label: 'a/b',
    description: 'Frazione',
    keyboardHint: 'frac(a,b)',
    insertLatex: '\\frac{\\placeholder{}}{\\placeholder{}}',
  },

  // ---- Roots ----
  {
    id: 'root-square',
    category: 'Roots',
    label: '√x',
    description: 'Radice quadrata',
    keyboardHint: 'sqrt(x)',
    insertLatex: '\\sqrt{\\placeholder{}}',
  },
  {
    id: 'root-nth',
    category: 'Roots',
    label: 'ⁿ√x',
    description: 'Radice ennesima',
    keyboardHint: 'root(n,x)',
    insertLatex: '\\sqrt[\\placeholder{}]{\\placeholder{}}',
    keywords: ['nth root', 'radice n-esima'],
  },

  // ---- Greek ----
  ...GREEK_NAMES.map(
    ([name, char]): PaletteItem => ({
      id: `greek-${name}`,
      category: 'Greek',
      label: char,
      description: name.charAt(0).toUpperCase() + name.slice(1),
      keyboardHint: name,
      insertLatex: `\\${name}`,
      keywords: [name],
    })
  ),

  // ---- Sets (M2) ----
  { id: 'set-R', category: 'Sets', label: 'ℝ', description: 'Numeri reali', keyboardHint: 'R', insertLatex: '\\mathbb{R}' },
  { id: 'set-N', category: 'Sets', label: 'ℕ', description: 'Numeri naturali', keyboardHint: 'N', insertLatex: '\\mathbb{N}' },
  { id: 'set-Z', category: 'Sets', label: 'ℤ', description: 'Numeri interi', keyboardHint: 'Z', insertLatex: '\\mathbb{Z}' },
  { id: 'set-Q', category: 'Sets', label: 'ℚ', description: 'Numeri razionali', keyboardHint: 'Q', insertLatex: '\\mathbb{Q}' },
  { id: 'set-C', category: 'Sets', label: 'ℂ', description: 'Numeri complessi', keyboardHint: 'C', insertLatex: '\\mathbb{C}' },
  { id: 'set-in', category: 'Sets', label: '∈', description: 'Appartiene', keyboardHint: 'x in R', insertLatex: '\\in' },
  { id: 'set-notin', category: 'Sets', label: '∉', description: 'Non appartiene', keyboardHint: 'x notin R', insertLatex: '\\notin' },
  { id: 'set-subset', category: 'Sets', label: '⊂', description: 'Sottoinsieme proprio', keyboardHint: 'subset', insertLatex: '\\subset' },
  { id: 'set-subseteq', category: 'Sets', label: '⊆', description: 'Sottoinsieme o uguale', keyboardHint: 'subseteq', insertLatex: '\\subseteq' },
  { id: 'set-union', category: 'Sets', label: '∪', description: 'Unione', keyboardHint: 'union', insertLatex: '\\cup' },
  { id: 'set-intersect', category: 'Sets', label: '∩', description: 'Intersezione', keyboardHint: 'intersect', insertLatex: '\\cap' },
  { id: 'set-emptyset', category: 'Sets', label: '∅', description: 'Insieme vuoto', keyboardHint: 'emptyset', insertLatex: '\\emptyset' },

  // ---- Vectors (M2) ----
  {
    id: 'vector-row',
    category: 'Vectors',
    label: '[a b]',
    description: 'Vettore riga',
    keyboardHint: '[1,2,3]',
    insertLatex: vectorTemplateLatex(3, 'row'),
  },
  {
    id: 'vector-column',
    category: 'Vectors',
    label: '[a;b]',
    description: 'Vettore colonna',
    keyboardHint: 'palette',
    insertLatex: vectorTemplateLatex(3, 'column'),
  },

  // ---- Matrices (M2) ----
  { id: 'matrix-2x2', category: 'Matrices', label: '2×2', description: 'Matrice 2×2', keyboardHint: '[[1,2],[3,4]]', insertLatex: matrixTemplateLatex(2, 2), keywords: ['matrix'] },
  { id: 'matrix-2x3', category: 'Matrices', label: '2×3', description: 'Matrice 2×3', insertLatex: matrixTemplateLatex(2, 3), keywords: ['matrix'] },
  { id: 'matrix-3x2', category: 'Matrices', label: '3×2', description: 'Matrice 3×2', insertLatex: matrixTemplateLatex(3, 2), keywords: ['matrix'] },
  { id: 'matrix-3x3', category: 'Matrices', label: '3×3', description: 'Matrice 3×3', insertLatex: matrixTemplateLatex(3, 3), keywords: ['matrix'] },
  {
    id: 'matrix-custom',
    category: 'Matrices',
    label: 'n×m…',
    description: 'Matrice personalizzata',
    keyboardHint: 'scegli righe e colonne',
    insertLatex: '',
    opensDialog: true,
    keywords: ['matrix', 'custom matrix'],
  },
  {
    id: 'matrix-identity',
    category: 'Matrices',
    label: 'I',
    description: 'Matrice identità 2×2',
    keyboardHint: 'I_n',
    insertLatex: '\\begin{pmatrix}\n1 & 0 \\\\\n0 & 1\n\\end{pmatrix}',
    keywords: ['identity', 'identità'],
  },
  {
    id: 'matrix-zero',
    category: 'Matrices',
    label: '𝟎',
    description: 'Matrice nulla 2×2',
    keyboardHint: 'zero matrix',
    insertLatex: '\\begin{pmatrix}\n0 & 0 \\\\\n0 & 0\n\\end{pmatrix}',
    keywords: ['zero', 'nulla'],
  },

  // ---- Linear Algebra (M2) ----
  { id: 'la-transpose', category: 'Linear Algebra', label: 'Aᵀ', description: 'Trasposta', keyboardHint: 'transpose(A) / A^T', insertLatex: '\\placeholder{}^T' },
  { id: 'la-inverse', category: 'Linear Algebra', label: 'A⁻¹', description: 'Inversa', keyboardHint: 'inv(A) / A^-1', insertLatex: '\\placeholder{}^{-1}' },
  { id: 'la-det', category: 'Linear Algebra', label: 'det', description: 'Determinante', keyboardHint: 'det(A)', insertLatex: '\\det(\\placeholder{})' },
  { id: 'la-rank', category: 'Linear Algebra', label: 'rank', description: 'Rango', keyboardHint: 'rank(A)', insertLatex: '\\operatorname{rank}(\\placeholder{})' },
  { id: 'la-dot', category: 'Linear Algebra', label: 'a·b', description: 'Prodotto scalare', keyboardHint: 'dot(v,w)', insertLatex: '\\placeholder{} \\cdot \\placeholder{}' },
  { id: 'la-norm', category: 'Linear Algebra', label: '‖v‖', description: 'Norma', keyboardHint: 'norm(v)', insertLatex: '\\left\\|\\placeholder{}\\right\\|' },
  { id: 'la-identity-n', category: 'Linear Algebra', label: 'Iₙ', description: 'Identità n×n (notazione)', keyboardHint: 'I_n', insertLatex: 'I_{\\placeholder{}}' },
  { id: 'la-span', category: 'Linear Algebra', label: 'span', description: 'Span (sottospazio generato)', keyboardHint: 'span(v1,v2)', insertLatex: '\\operatorname{span}(\\placeholder{})' },
  { id: 'la-kernel', category: 'Linear Algebra', label: 'ker', description: 'Nucleo (kernel)', keyboardHint: 'ker(A)', insertLatex: '\\operatorname{ker}(\\placeholder{})' },
  { id: 'la-image', category: 'Linear Algebra', label: 'im', description: 'Immagine', keyboardHint: 'im(A)', insertLatex: '\\operatorname{im}(\\placeholder{})' },
  {
    id: 'la-linear-map',
    category: 'Linear Algebra',
    label: 'T:ℝⁿ→ℝᵐ',
    description: 'Applicazione lineare (segnatura)',
    keyboardHint: 'T:R^2 -> R^3',
    insertLatex: '\\placeholder{}\\colon\\mathbb{R}^{\\placeholder{}} \\to \\mathbb{R}^{\\placeholder{}}',
  },
  {
    id: 'la-eigen',
    category: 'Linear Algebra',
    label: 'Av=λv',
    description: 'Equazione agli autovalori',
    keyboardHint: 'A v = lambda v',
    insertLatex: 'Av = \\lambda v',
  },
];

export function itemsByCategory(category: PaletteCategoryName): PaletteItem[] {
  return PALETTE_ITEMS.filter((item) => item.category === category);
}

export function nonEmptyCategories(): PaletteCategoryName[] {
  const withItems = new Set(PALETTE_ITEMS.map((item) => item.category));
  return CATEGORY_ORDER.filter((c) => withItems.has(c));
}
