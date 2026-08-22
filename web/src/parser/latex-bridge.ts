// Bridge LaTeX -> Math AST. Usato per rileggere il contenuto del
// <math-field> di MathLive dopo un editing diretto dell'utente (vedi
// ARCHITECTURE.md, sezione "Pipeline unificata").
//
// Copre il sottoinsieme di LaTeX prodotto da AstToLatex (round-trip) e le
// convenzioni standard usate di default da MathLive (\frac, ^{}, _{},
// \sqrt, \begin{pmatrix}, \cdot, macro di relazione, \placeholder{}).
// Non è un parser LaTeX generale.

import type { MathNode, RelationOp, SetOp } from '../ast/types';
import {
  binary,
  fraction,
  func,
  group,
  ident,
  matrix,
  num,
  placeholder,
  power,
  relation,
  root,
  subscript,
  sym,
  unary,
  vector,
} from '../ast/build';
import { GREEK_LETTERS } from '../ast/symbols';

type LTokenType =
  | 'COMMAND'
  | 'LBRACE'
  | 'RBRACE'
  | 'LBRACKET'
  | 'RBRACKET'
  | 'LPAREN'
  | 'RPAREN'
  | 'CARET'
  | 'UNDERSCORE'
  | 'PLUS'
  | 'MINUS'
  | 'STAR'
  | 'SLASH'
  | 'AMP'
  | 'ROWSEP'
  | 'COMMA'
  | 'COLON'
  | 'EQ'
  | 'LT'
  | 'GT'
  | 'PIPE'
  | 'NUMBER'
  | 'LETTER'
  | 'EOF';

interface LToken {
  type: LTokenType;
  text: string;
}

const GREEK_COMMAND_TO_CHAR: Record<string, string> = Object.fromEntries(
  Object.entries(GREEK_LETTERS).map(([name, char]) => [name, char])
);

function tokenizeLatex(input: string): LToken[] {
  const tokens: LToken[] = [];
  let i = 0;
  const n = input.length;
  while (i < n) {
    const ch = input[i];
    if (/\s/.test(ch)) {
      i += 1;
      continue;
    }
    if (ch === '\\') {
      // "\\" (due backslash consecutivi) è il separatore di riga delle
      // matrici. NB: qui controlliamo il carattere SUCCESSIVO al primo
      // backslash già individuato da `ch`, non due caratteri dopo.
      if (input[i + 1] === '\\') {
        tokens.push({ type: 'ROWSEP', text: '\\\\' });
        i += 2;
        continue;
      }
      let j = i + 1;
      // "\\" seguito da lettere: nome di comando
      const start = j;
      while (j < n && /[a-zA-Z]/.test(input[j])) j += 1;
      if (j === start) {
        // comando di un solo carattere non alfabetico, es. "\,"
        tokens.push({ type: 'COMMAND', text: input[j] ?? '' });
        i = j + 1;
        continue;
      }
      tokens.push({ type: 'COMMAND', text: input.slice(start, j) });
      i = j;
      continue;
    }
    if (ch === '{') {
      tokens.push({ type: 'LBRACE', text: ch });
      i += 1;
      continue;
    }
    if (ch === '}') {
      tokens.push({ type: 'RBRACE', text: ch });
      i += 1;
      continue;
    }
    if (ch === '[') {
      tokens.push({ type: 'LBRACKET', text: ch });
      i += 1;
      continue;
    }
    if (ch === ']') {
      tokens.push({ type: 'RBRACKET', text: ch });
      i += 1;
      continue;
    }
    if (ch === '(') {
      tokens.push({ type: 'LPAREN', text: ch });
      i += 1;
      continue;
    }
    if (ch === ')') {
      tokens.push({ type: 'RPAREN', text: ch });
      i += 1;
      continue;
    }
    if (ch === '^') {
      tokens.push({ type: 'CARET', text: ch });
      i += 1;
      continue;
    }
    if (ch === '_') {
      tokens.push({ type: 'UNDERSCORE', text: ch });
      i += 1;
      continue;
    }
    if (ch === '+') {
      tokens.push({ type: 'PLUS', text: ch });
      i += 1;
      continue;
    }
    if (ch === '-') {
      tokens.push({ type: 'MINUS', text: ch });
      i += 1;
      continue;
    }
    if (ch === '*') {
      tokens.push({ type: 'STAR', text: ch });
      i += 1;
      continue;
    }
    if (ch === '/') {
      tokens.push({ type: 'SLASH', text: ch });
      i += 1;
      continue;
    }
    if (ch === '&') {
      tokens.push({ type: 'AMP', text: ch });
      i += 1;
      continue;
    }
    if (ch === ',') {
      tokens.push({ type: 'COMMA', text: ch });
      i += 1;
      continue;
    }
    if (ch === ':') {
      tokens.push({ type: 'COLON', text: ch });
      i += 1;
      continue;
    }
    if (/[0-9]/.test(ch)) {
      let j = i + 1;
      while (j < n && /[0-9]/.test(input[j])) j += 1;
      if (input[j] === '.' && /[0-9]/.test(input[j + 1] ?? '')) {
        j += 1;
        while (j < n && /[0-9]/.test(input[j])) j += 1;
      }
      tokens.push({ type: 'NUMBER', text: input.slice(i, j) });
      i = j;
      continue;
    }
    if (/[a-zA-Z]/.test(ch)) {
      tokens.push({ type: 'LETTER', text: ch });
      i += 1;
      continue;
    }
    if (ch === '=') {
      tokens.push({ type: 'EQ', text: '=' });
      i += 1;
      continue;
    }
    if (ch === '<') {
      tokens.push({ type: 'LT', text: '<' });
      i += 1;
      continue;
    }
    if (ch === '>') {
      tokens.push({ type: 'GT', text: '>' });
      i += 1;
      continue;
    }
    if (ch === '|') {
      tokens.push({ type: 'PIPE', text: '|' });
      i += 1;
      continue;
    }
    // carattere sconosciuto: lo saltiamo silenziosamente (spazi tipografici, ecc.)
    i += 1;
  }
  tokens.push({ type: 'EOF', text: '' });
  return tokens;
}

const RELATION_COMMANDS: Record<string, RelationOp> = {
  neq: '!=',
  leq: '<=',
  geq: '>=',
  approx: '~~',
  equiv: 'equiv',
};

const SET_COMMANDS: Record<string, SetOp> = {
  in: 'in',
  notin: 'notin',
  subset: 'subset',
  subseteq: 'subseteq',
  cup: 'union',
  cap: 'intersect',
};

const IGNORED_COMMANDS = new Set(['left', 'right', ',', '!', ';', 'quad', 'qquad']);

class LatexParser {
  private tokens: LToken[];
  private pos = 0;

  constructor(input: string) {
    this.tokens = tokenizeLatex(input);
  }

  private peek(offset = 0): LToken {
    return this.tokens[Math.min(this.pos + offset, this.tokens.length - 1)];
  }

  private advance(): LToken {
    const tok = this.peek();
    if (this.pos < this.tokens.length - 1) this.pos += 1;
    return tok;
  }

  private check(type: LTokenType): boolean {
    return this.peek().type === type;
  }

  private match(type: LTokenType): LToken | null {
    return this.check(type) ? this.advance() : null;
  }

  private expect(type: LTokenType, message: string): LToken {
    const tok = this.match(type);
    if (!tok) throw new SyntaxError(`${message} (trovato "${this.peek().text || 'EOF'}")`);
    return tok;
  }

  /** Salta comandi ignorati (\left, \right, \,, ...) di fila. */
  private skipIgnored(): void {
    while (this.check('COMMAND') && IGNORED_COMMANDS.has(this.peek().text)) {
      this.advance();
    }
  }

  parseDocument(): MathNode {
    this.skipIgnored();
    if (this.check('EOF')) return placeholder();
    return this.parseTypeAnnotation();
  }

  // ---- Segnatura di funzione: "T\colon\mathbb{R}^2 \to \mathbb{R}^3" ----
  private parseTypeAnnotation(): MathNode {
    const left = this.parseArrowChain();
    this.skipIgnored();
    if (this.check('COLON') || (this.check('COMMAND') && this.peek().text === 'colon')) {
      this.advance();
      return relation(':', left, this.parseArrowChain());
    }
    return left;
  }

  private parseArrowChain(): MathNode {
    let left = this.parseRelational();
    for (;;) {
      this.skipIgnored();
      if (this.check('COMMAND') && this.peek().text === 'to') {
        this.advance();
        left = relation('to', left, this.parseRelational());
      } else {
        break;
      }
    }
    return left;
  }

  private parseRelational(): MathNode {
    const left = this.parseAdditive();
    this.skipIgnored();
    const tok = this.peek();
    if (tok.type === 'EQ') {
      this.advance();
      return relation('=', left, this.parseAdditive());
    }
    if (tok.type === 'LT' || tok.type === 'GT') {
      this.advance();
      return relation(tok.text as RelationOp, left, this.parseAdditive());
    }
    if (tok.type === 'COMMAND' && RELATION_COMMANDS[tok.text]) {
      this.advance();
      return relation(RELATION_COMMANDS[tok.text], left, this.parseAdditive());
    }
    if (tok.type === 'COMMAND' && SET_COMMANDS[tok.text]) {
      this.advance();
      const right = this.parseAdditive();
      return { id: `s${Date.now().toString(36)}`, type: 'SetNode', op: SET_COMMANDS[tok.text], operands: [left, right] };
    }
    return left;
  }

  private parseAdditive(): MathNode {
    let left = this.parseMultiplicative();
    for (;;) {
      this.skipIgnored();
      if (this.check('PLUS')) {
        this.advance();
        left = binary('+', left, this.parseMultiplicative());
      } else if (this.check('MINUS')) {
        this.advance();
        left = binary('-', left, this.parseMultiplicative());
      } else {
        break;
      }
    }
    return left;
  }

  private parseMultiplicative(): MathNode {
    let left = this.parseUnary();
    for (;;) {
      this.skipIgnored();
      if (this.check('STAR')) {
        this.advance();
        left = binary('*', left, this.parseUnary());
      } else if (this.check('SLASH')) {
        this.advance();
        left = binary('/', left, this.parseUnary());
      } else if (this.check('COMMAND') && this.peek().text === 'cdot') {
        this.advance();
        left = binary('cdot', left, this.parseUnary());
      } else if (this.canStartImplicitFactor()) {
        left = binary('*', left, this.parseUnary());
      } else {
        break;
      }
    }
    return left;
  }

  private canStartImplicitFactor(): boolean {
    const tok = this.peek();
    // NB: PIPE non è incluso qui deliberatamente. "|" apre e chiude il
    // valore assoluto con lo stesso token: se venisse trattato come inizio
    // di un fattore implicito, il "|" di chiusura verrebbe scambiato per
    // l'apertura di un nuovo valore assoluto (vedi parsePrimary/PIPE).
    if (tok.type === 'NUMBER' || tok.type === 'LETTER' || tok.type === 'LPAREN' || tok.type === 'LBRACE') return true;
    if (
      tok.type === 'COMMAND' &&
      !IGNORED_COMMANDS.has(tok.text) &&
      !RELATION_COMMANDS[tok.text] &&
      !SET_COMMANDS[tok.text] &&
      tok.text !== 'to' &&
      tok.text !== 'colon' &&
      tok.text !== '|' // "\|" (norma): stesso motivo del PIPE nudo, vedi sopra
    ) {
      return tok.text !== 'end' && tok.text !== 'right';
    }
    return false;
  }

  private parseUnary(): MathNode {
    this.skipIgnored();
    if (this.check('MINUS')) {
      this.advance();
      return unary('-', this.parseUnary());
    }
    if (this.check('PLUS')) {
      this.advance();
      return unary('+', this.parseUnary());
    }
    return this.parsePowerChain();
  }

  private parsePowerChain(): MathNode {
    let base = this.parsePrimary();
    for (;;) {
      if (this.check('CARET')) {
        this.advance();
        base = this.parseExponent(base);
      } else if (this.check('UNDERSCORE')) {
        this.advance();
        base = subscript(base, this.parseGroupOrAtom());
      } else {
        break;
      }
    }
    return base;
  }

  private parseExponent(base: MathNode): MathNode {
    if (this.check('LETTER') && this.peek().text === 'T' && this.peek(1).type !== 'LETTER' && this.peek(1).type !== 'NUMBER') {
      this.advance();
      return func('transpose', [base]);
    }
    if (this.check('LBRACE') && this.peek(1).type === 'MINUS' && this.peek(2).type === 'NUMBER' && this.peek(2).text === '1' && this.peek(3).type === 'RBRACE') {
      this.advance();
      this.advance();
      this.advance();
      this.advance();
      return func('inv', [base]);
    }
    return power(base, this.parseGroupOrAtom());
  }

  /** { ... } oppure un singolo atomo (numero/lettera/comando semplice). */
  private parseGroupOrAtom(): MathNode {
    if (this.check('LBRACE')) {
      this.advance();
      const inner = this.parseAdditive();
      this.expect('RBRACE', 'Attesa "}"');
      return inner;
    }
    if (this.check('MINUS')) {
      this.advance();
      return unary('-', this.parseGroupOrAtom());
    }
    return this.parsePrimary();
  }

  private parsePrimary(): MathNode {
    this.skipIgnored();
    const tok = this.peek();

    if (tok.type === 'NUMBER') {
      this.advance();
      return num(tok.text);
    }

    if (tok.type === 'LETTER') {
      this.advance();
      return ident(tok.text);
    }

    if (tok.type === 'LPAREN') {
      this.advance();
      const inner = this.parseAdditive();
      this.skipIgnored();
      this.expect('RPAREN', 'Attesa ")"');
      return group(inner);
    }

    if (tok.type === 'LBRACE') {
      this.advance();
      const inner = this.parseAdditive();
      this.expect('RBRACE', 'Attesa "}"');
      return inner;
    }

    // "|x|" (valore assoluto). "\left"/"\right" davanti a "|" sono già
    // stati scartati da skipIgnored().
    if (tok.type === 'PIPE') {
      this.advance();
      const inner = this.parseAdditive();
      this.skipIgnored();
      this.expect('PIPE', 'Attesa "|" di chiusura per il valore assoluto');
      return func('abs', [inner]);
    }

    if (tok.type === 'COMMAND') {
      return this.parseCommand();
    }

    throw new SyntaxError(`Token LaTeX inatteso "${tok.text || 'EOF'}"`);
  }

  private parseCommand(): MathNode {
    const tok = this.advance();
    const name = tok.text;

    if (name === 'placeholder') {
      if (this.check('LBRACE')) {
        this.advance();
        this.match('RBRACE');
      }
      return placeholder();
    }

    if (GREEK_COMMAND_TO_CHAR[name]) {
      return sym(GREEK_COMMAND_TO_CHAR[name], 'greek');
    }

    if (name === 'mathbb') {
      this.expect('LBRACE', 'Attesa "{" dopo \\mathbb');
      const letter = this.advance().text;
      this.expect('RBRACE', 'Attesa "}"');
      const map: Record<string, string> = { R: 'ℝ', N: 'ℕ', Z: 'ℤ', Q: 'ℚ', C: 'ℂ' };
      return sym(map[letter] ?? letter, 'set');
    }

    if (name === 'emptyset') return sym('∅', 'set');

    // "\begin{pmatrix} ... \end{pmatrix}" (anche bmatrix/vmatrix/matrix:
    // lo stile delle parentesi non è tracciato nell'AST, vedi ast/types.ts).
    // Una singola riga o una singola colonna sono ambigue tra vettore e
    // matrice 1×n / n×1: interpretarle come vettore è la lettura più comune.
    if (name === 'begin') {
      return this.parseMatrixEnvironment();
    }

    // "\|v\|" (norma). Il tokenizzatore emette COMMAND con testo "|" sia
    // per "\|" sia (per costruzione) qui sotto per il "\|" di chiusura.
    if (name === '|') {
      const inner = this.parseAdditive();
      this.skipIgnored();
      if (this.check('COMMAND') && this.peek().text === '|') {
        this.advance();
      } else {
        this.expect('PIPE', 'Attesa "\\|" di chiusura per la norma');
      }
      return func('norm', [inner]);
    }

    if (name === 'frac') {
      const numerator = this.parseBraceGroup();
      const denominator = this.parseBraceGroup();
      return fraction(numerator, denominator);
    }

    if (name === 'sqrt') {
      let index: MathNode | undefined;
      if (this.check('LBRACKET')) {
        this.advance();
        index = this.parseAdditive();
        this.expect('RBRACKET', 'Attesa "]"');
      }
      const radicand = this.parseBraceGroup();
      return root(radicand, index);
    }

    if (name === 'operatorname') {
      this.expect('LBRACE', 'Attesa "{" dopo \\operatorname');
      let fname = '';
      while (!this.check('RBRACE')) fname += this.advance().text;
      this.expect('RBRACE', 'Attesa "}"');
      const args = this.parseParenArgs();
      return func(fname, args);
    }

    if (name === 'int') return this.parseIntegral();
    if (name === 'sum') return this.parseSumProd('SumNode');
    if (name === 'prod') return this.parseSumProd('ProductNode');
    if (name === 'lim') return this.parseLimit();
    if (name === 'partial') return this.parsePartial();

    // funzioni note: \sin, \cos, \det, ...
    const args = this.tryParseParenArgs();
    if (args) return func(name, args);

    // fallback: comando sconosciuto trattato come identificatore/simbolo
    return sym(`\\${name}`, 'misc');
  }

  private parseMatrixEnvironment(): MathNode {
    this.expect('LBRACE', 'Attesa "{" dopo \\begin');
    while (!this.check('RBRACE')) this.advance(); // nome ambiente (pmatrix, bmatrix, ...): non tracciato nell'AST
    this.expect('RBRACE', 'Attesa "}"');

    const rows: MathNode[][] = [];
    let currentRow: MathNode[] = [];
    this.skipIgnored();
    currentRow.push(this.parseAdditive());
    for (;;) {
      this.skipIgnored();
      if (this.check('AMP')) {
        this.advance();
        this.skipIgnored();
        currentRow.push(this.parseAdditive());
      } else if (this.check('ROWSEP')) {
        this.advance();
        rows.push(currentRow);
        currentRow = [];
        this.skipIgnored();
        currentRow.push(this.parseAdditive());
      } else {
        break;
      }
    }
    rows.push(currentRow);

    this.skipIgnored();
    if (!(this.check('COMMAND') && this.peek().text === 'end')) {
      throw new SyntaxError(`Atteso "\\end{...}" a chiusura della matrice (trovato "${this.peek().text || 'EOF'}")`);
    }
    this.advance();
    this.expect('LBRACE', 'Attesa "{" dopo \\end');
    while (!this.check('RBRACE')) this.advance();
    this.expect('RBRACE', 'Attesa "}"');

    if (rows.length === 1) return vector('row', rows[0]);
    if (rows.every((row) => row.length === 1)) return vector('column', rows.map((row) => row[0]));
    return matrix(rows);
  }

  private parseBraceGroup(): MathNode {
    if (this.check('LBRACE')) {
      this.advance();
      const inner = this.parseAdditive();
      this.expect('RBRACE', 'Attesa "}"');
      return inner;
    }
    return this.parsePrimary();
  }

  private tryParseParenArgs(): MathNode[] | null {
    this.skipIgnored();
    if (!this.check('LPAREN')) return null;
    return this.parseParenArgs();
  }

  private parseParenArgs(): MathNode[] {
    this.skipIgnored();
    this.expect('LPAREN', 'Attesa "("');
    const args: MathNode[] = [];
    this.skipIgnored();
    if (!this.check('RPAREN')) {
      args.push(this.parseAdditive());
      this.skipIgnored();
      while (this.match('COMMA')) {
        args.push(this.parseAdditive());
        this.skipIgnored();
      }
    }
    this.skipIgnored();
    this.expect('RPAREN', 'Attesa ")"');
    return args;
  }

  private parseIntegral(): MathNode {
    let lower: MathNode | undefined;
    let upper: MathNode | undefined;
    if (this.check('UNDERSCORE')) {
      this.advance();
      lower = this.parseGroupOrAtom();
      this.expect('CARET', 'Attesa "^" dopo il limite inferiore');
      upper = this.parseGroupOrAtom();
    }
    const integrand = this.parseAdditive();
    // consuma "d" seguito dal nome variabile, es. \,dx oppure dx
    let variable: MathNode = ident('x');
    this.skipIgnored();
    if (this.check('LETTER') && this.peek().text === 'd' && this.peek(1).type === 'LETTER') {
      this.advance();
      variable = ident(this.advance().text);
    }
    return {
      id: `int${Date.now().toString(36)}`,
      type: 'IntegralNode',
      integrand,
      variable,
      ...(lower ? { lower } : {}),
      ...(upper ? { upper } : {}),
    };
  }

  private parseSumProd(type: 'SumNode' | 'ProductNode'): MathNode {
    this.expect('UNDERSCORE', 'Attesa "_"');
    this.expect('LBRACE', 'Attesa "{"');
    const indexTok = this.expect('LETTER', 'Atteso indice');
    // "=" è tokenizzato come LETTER '='
    this.expect('LETTER', 'Attesa "=" ');
    const lower = this.parseAdditive();
    this.expect('RBRACE', 'Attesa "}"');
    this.expect('CARET', 'Attesa "^"');
    const upper = this.parseGroupOrAtom();
    const expression = this.parseAdditive();
    return { id: `agg${Date.now().toString(36)}`, type, expression, index: ident(indexTok.text), lower, upper } as MathNode;
  }

  private parseLimit(): MathNode {
    this.expect('UNDERSCORE', 'Attesa "_"');
    this.expect('LBRACE', 'Attesa "{"');
    const varTok = this.expect('LETTER', 'Attesa variabile');
    this.expect('COMMAND', 'Attesa "\\to"'); // 'to'
    const approaches = this.parseAdditive();
    this.expect('RBRACE', 'Attesa "}"');
    const expression = this.parseAdditive();
    return { id: `lim${Date.now().toString(36)}`, type: 'LimitNode', expression, variable: ident(varTok.text), approaches };
  }

  private parsePartial(): MathNode {
    const expression = this.parseAdditive();
    this.expect('SLASH', 'Attesa "/" (nota: usa la forma \\frac{\\partial f}{\\partial x})');
    this.expect('COMMAND', 'Attesa "\\partial"');
    const variable = this.parseAdditive();
    return { id: `pder${Date.now().toString(36)}`, type: 'PartialDerivativeNode', expression, variable, order: 1 };
  }
}

export function latexToAst(latex: string): MathNode {
  return new LatexParser(latex).parseDocument();
}
