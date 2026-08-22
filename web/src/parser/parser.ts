// Parser (discesa ricorsiva) della sintassi rapida -> Math AST.
// Vedi SYNTAX.md per la grammatica informale e ARCHITECTURE.md per il ruolo
// nella pipeline unificata (stessa AST prodotta dalla palette e da MathLive).

import { tokenize, type Token, type TokenType } from './tokenizer';
import type { MathNode, RelationNode, RelationOp, SetOp } from '../ast/types';
import {
  binary,
  extendRelationChain,
  func,
  fraction,
  ident,
  matrix,
  num,
  power,
  relation,
  root,
  subscript,
  sym,
  unary,
  vector,
} from '../ast/build';
import { GREEK_LETTERS, SET_SYMBOLS } from '../ast/symbols';
import { NATURAL_LANGUAGE_KEYWORDS } from './naturalLanguage';

export class ParseError extends Error {}

const RELATION_TOKENS: Partial<Record<TokenType, RelationOp>> = {
  EQ: '=',
  NE: '!=',
  LT: '<',
  GT: '>',
  LE: '<=',
  GE: '>=',
  APPROX: '~~',
};

const SET_KEYWORDS: Record<string, SetOp> = {
  in: 'in',
  notin: 'notin',
  subset: 'subset',
  subseteq: 'subseteq',
  union: 'union',
  intersect: 'intersect',
};

const STRUCTURAL_KEYWORDS = new Set(['int', 'sum', 'prod', 'lim', 'd', 'partial']);
// Solo le parole "operatore" (piu/meno/per/fratto/diviso/alla) vanno escluse
// dalla moltiplicazione implicita: sono già intercettate al loro livello di
// precedenza. "radice" invece si comporta come "sqrt" — un valore, non un
// operatore — quindi "2radice3" deve poter moltiplicare implicitamente
// (2 * radice(3)) esattamente come già succede con "2sqrt(3)".
const NL_OPERATOR_KEYWORDS = new Set<string>(NATURAL_LANGUAGE_KEYWORDS.filter((k) => k !== 'radice'));

class Parser {
  private tokens: Token[];
  private pos = 0;
  private limitStack: number[] = [];

  constructor(input: string) {
    this.tokens = tokenize(input);
  }

  private get limit(): number {
    return this.limitStack.length > 0 ? this.limitStack[this.limitStack.length - 1] : this.tokens.length;
  }

  private peek(offset = 0): Token {
    const idx = Math.min(this.pos + offset, this.limit);
    if (idx >= this.limit) return { type: 'EOF', text: '', pos: -1 };
    return this.tokens[idx];
  }

  private atEnd(): boolean {
    return this.pos >= this.limit || this.peek().type === 'EOF';
  }

  private advance(): Token {
    const tok = this.peek();
    this.pos = Math.min(this.pos + 1, this.limit);
    return tok;
  }

  private check(type: TokenType): boolean {
    return this.peek().type === type;
  }

  private checkIdent(text: string): boolean {
    const tok = this.peek();
    return tok.type === 'IDENT' && tok.text === text;
  }

  private match(type: TokenType): Token | null {
    if (this.check(type)) return this.advance();
    return null;
  }

  private expect(type: TokenType, message: string): Token {
    const tok = this.match(type);
    if (!tok) {
      throw new ParseError(`${message} (trovato "${this.peek().text || 'EOF'}" alla posizione ${this.peek().pos})`);
    }
    return tok;
  }

  private withLimit<T>(newLimit: number, fn: () => T): T {
    this.limitStack.push(newLimit);
    try {
      return fn();
    } finally {
      this.limitStack.pop();
    }
  }

  /** Trova, a partire dalla posizione corrente, il primo indice (a profondità
   * di parentesi 0) di un token IDENT che sembra un differenziale "dx". */
  private findDifferentialIndex(): number | null {
    let depth = 0;
    for (let idx = this.pos; idx < this.limit; idx += 1) {
      const tok = this.tokens[idx];
      if (tok.type === 'LPAREN' || tok.type === 'LBRACKET') depth += 1;
      else if (tok.type === 'RPAREN' || tok.type === 'RBRACKET') depth -= 1;
      else if (depth === 0 && tok.type === 'IDENT' && /^d[a-zA-Z]$/.test(tok.text) && idx > this.pos) {
        return idx;
      }
    }
    return null;
  }

  // Più affermazioni indipendenti nella stessa cella, separate da virgola
  // ("A = [[1,2],[3,4]], B = [[5,6],[7,8]]" — vedi SYNTAX.md): diventano un
  // SystemNode non racchiuso in graffa (bracketed:false), a differenza del
  // "sistema di equazioni" da risolvere insieme (quello sì con la graffa).
  parseDocument(): MathNode {
    if (this.atEnd()) return { id: 'empty', type: 'PlaceholderNode' };
    const first = this.parseTypeAnnotation();
    if (this.atEnd()) return first;

    if (this.check('COMMA')) {
      const statements = [first];
      while (this.match('COMMA')) {
        statements.push(this.parseTypeAnnotation());
      }
      if (!this.atEnd()) {
        throw new ParseError(`Token inatteso "${this.peek().text}" alla posizione ${this.peek().pos}`);
      }
      return { id: `sys${Date.now().toString(36)}`, type: 'SystemNode', equations: statements, bracketed: false };
    }

    throw new ParseError(`Token inatteso "${this.peek().text}" alla posizione ${this.peek().pos}`);
  }

  // ---- Segnatura di funzione: "T:R^2 -> R^3" (vedi SYNTAX.md) ----
  private parseTypeAnnotation(): MathNode {
    const left = this.parseArrowChain();
    if (this.check('COLON')) {
      this.advance();
      const right = this.parseArrowChain();
      return relation(':', left, right);
    }
    return left;
  }

  private parseArrowChain(): MathNode {
    let left = this.parseRelational();
    while (this.check('ARROW')) {
      this.advance();
      left = relation('to', left, this.parseRelational());
    }
    return left;
  }

  // ---- Relazioni / insiemi (precedenza più bassa) ----
  // "a = b = c" o "a < b < c" (catena di passaggi/disuguaglianze, vedi
  // SYNTAX.md) restano UNA sola RelationNode con più termini, non relazioni
  // annidate: continuiamo a incatenare finché troviamo un altro operatore
  // di relazione.
  private parseRelational(): MathNode {
    const first = this.parseAdditive();
    let chain: RelationNode | undefined;

    for (;;) {
      const tok = this.peek();
      const relOp: RelationOp | null = RELATION_TOKENS[tok.type] ?? (tok.type === 'IDENT' && tok.text === 'equiv' ? 'equiv' : null);
      if (!relOp) break;
      this.advance();
      const next = this.parseAdditive();
      chain = chain ? extendRelationChain(chain, relOp, next) : relation(relOp, first, next);
    }
    if (chain) return chain;

    const tok = this.peek();
    if (tok.type === 'IDENT' && SET_KEYWORDS[tok.text]) {
      this.advance();
      const right = this.parseAdditive();
      return { id: `s${Date.now().toString(36)}`, type: 'SetNode', op: SET_KEYWORDS[tok.text], operands: [first, right] };
    }

    return first;
  }

  // ---- Additiva ----
  // "piu"/"meno" sono alias in linguaggio naturale di +/- (vedi SYNTAX.md,
  // "Linguaggio naturale"): stessa precedenza, stesso comportamento.
  private parseAdditive(): MathNode {
    let left = this.parseMultiplicative();
    for (;;) {
      if (this.check('PLUS') || this.checkIdent('piu')) {
        this.advance();
        left = binary('+', left, this.parseMultiplicative());
      } else if (this.check('MINUS') || this.checkIdent('meno')) {
        this.advance();
        left = binary('-', left, this.parseMultiplicative());
      } else {
        break;
      }
    }
    return left;
  }

  // ---- Moltiplicativa (con moltiplicazione implicita) ----
  // "per" = *, "fratto"/"diviso" = / (linguaggio naturale, vedi SYNTAX.md).
  private parseMultiplicative(): MathNode {
    let left = this.parseUnary();
    for (;;) {
      if (this.check('STAR') || this.checkIdent('per')) {
        this.advance();
        left = binary('*', left, this.parseUnary());
      } else if (this.check('SLASH') || this.checkIdent('fratto') || this.checkIdent('diviso')) {
        this.advance();
        left = binary('/', left, this.parseUnary());
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
    // NB: PIPE e DOUBLE_PIPE non sono inclusi deliberatamente: "|"/"||"
    // aprono e chiudono valore assoluto/norma con lo stesso token, e
    // trattarli come inizio di un fattore implicito farebbe scambiare la
    // chiusura per una nuova apertura (vedi parsePrimary/PIPE/DOUBLE_PIPE).
    if (tok.type === 'NUMBER' || tok.type === 'LPAREN' || tok.type === 'LBRACKET') return true;
    if (tok.type === 'IDENT') {
      // parole chiave che chiudono l'espressione corrente, o che sono già
      // gestite esplicitamente a un altro livello di precedenza (piu/meno/
      // per/fratto/diviso/alla/radice), non iniziano un fattore implicito.
      if (SET_KEYWORDS[tok.text] || tok.text === 'equiv' || NL_OPERATOR_KEYWORDS.has(tok.text)) return false;
      return true;
    }
    return false;
  }

  // ---- Unaria ----
  private parseUnary(): MathNode {
    if (this.check('MINUS') || this.checkIdent('meno')) {
      this.advance();
      return unary('-', this.parseUnary());
    }
    if (this.check('PLUS') || this.checkIdent('piu')) {
      this.advance();
      return unary('+', this.parseUnary());
    }
    return this.parsePowerChain();
  }

  // ---- Potenze / pedici (dopo un primary, in catena) ----
  // "alla" = ^ in linguaggio naturale ("2 alla 3", "2alla3", "2allax").
  private parsePowerChain(): MathNode {
    let base = this.parsePrimary();
    for (;;) {
      if (this.check('CARET') || this.checkIdent('alla')) {
        this.advance();
        base = this.parseExponent(base);
      } else if (this.check('UNDERSCORE')) {
        this.advance();
        const sub = this.parseTightAtom();
        base = subscript(base, sub);
      } else {
        break;
      }
    }
    return base;
  }

  private parseExponent(base: MathNode): MathNode {
    // A^T -> transpose(A)
    if (this.checkIdent('T') && !this.followedByPower(1)) {
      this.advance();
      return func('transpose', [base]);
    }
    // A^-1 -> inv(A)
    if (this.check('MINUS') && this.peek(1).type === 'NUMBER' && this.peek(1).text === '1' && !this.followedByPower(2)) {
      this.advance();
      this.advance();
      return func('inv', [base]);
    }
    const exponent = this.parseTightAtom();
    return power(base, exponent);
  }

  private followedByPower(offset: number): boolean {
    return this.peek(offset).type === 'CARET' || this.peek(offset).type === 'UNDERSCORE';
  }

  /** Un "atomo stretto": singolo numero/identificatore/greco/funzione o
   * gruppo tra parentesi (le parentesi vengono scartate: le graffe di
   * LaTeX/il contesto di esponente/pedice forniscono già il raggruppamento). */
  private parseTightAtom(): MathNode {
    if (this.check('MINUS')) {
      this.advance();
      return unary('-', this.parseTightAtom());
    }
    const node = this.parsePrimary();
    if (node.type === 'GroupNode') return node.expression;
    return node;
  }

  // ---- Primary ----
  private parsePrimary(): MathNode {
    const tok = this.peek();

    if (tok.type === 'NUMBER') {
      this.advance();
      return num(tok.text);
    }

    if (tok.type === 'LPAREN') {
      this.advance();
      const inner = this.parseAdditive();
      this.expect('RPAREN', 'Attesa ")"');
      // Le parentesi hanno già guidato la precedenza durante il parsing:
      // non avvolgiamo in un GroupNode, il renderer le re-inserirà da solo
      // dove semanticamente necessarie (vedi render/precedence.ts).
      return inner;
    }

    if (tok.type === 'PIPE') {
      this.advance();
      const inner = this.parseAdditive();
      this.expect('PIPE', 'Attesa "|" di chiusura per il valore assoluto');
      return func('abs', [inner]);
    }

    // "||v||" = norma, come "norm(v)" (vedi SYNTAX.md).
    if (tok.type === 'DOUBLE_PIPE') {
      this.advance();
      const inner = this.parseAdditive();
      this.expect('DOUBLE_PIPE', 'Attesa "||" di chiusura per la norma');
      return func('norm', [inner]);
    }

    if (tok.type === 'LBRACKET') {
      return this.parseBracket();
    }

    if (tok.type === 'IDENT') {
      return this.parsePrimaryIdent();
    }

    throw new ParseError(`Espressione inattesa "${tok.text || 'EOF'}" alla posizione ${tok.pos}`);
  }

  private parsePrimaryIdent(): MathNode {
    const tok = this.advance();
    const name = tok.text;

    // "d" è ambiguo con un identificatore comune (es. una distanza "d"):
    // trattalo come inizio di una derivata solo se seguito da "/" o "^"
    // (i pattern "d/dx" e "d^2/dx^2..."), altrimenti è un identificatore.
    if (name === 'd' && (this.check('SLASH') || this.check('CARET'))) {
      return this.parseStructural(name);
    }
    if (name !== 'd' && STRUCTURAL_KEYWORDS.has(name)) {
      return this.parseStructural(name);
    }

    if (GREEK_LETTERS[name]) {
      return sym(GREEK_LETTERS[name], 'greek');
    }

    // R, N, Z, Q, C sono riservati agli insiemi numerici (ℝ, ℕ, ℤ, ℚ, ℂ):
    // in questo notebook, orientato all'algebra lineare, un uso come nome di
    // variabile è raro e la notazione "v in R^3" / "T:R^2 -> R^3" molto più
    // comune (vedi SYNTAX.md). Non seguito da "(" (altrimenti sarebbe una
    // chiamata di funzione a una lettera, es. "R(x)").
    if (SET_SYMBOLS[name] && !this.check('LPAREN')) {
      return sym(SET_SYMBOLS[name], 'set');
    }

    if (name === 'sqrt' && this.check('LPAREN')) {
      const args = this.parseArgList();
      return root(args[0] ?? { id: 'ph', type: 'PlaceholderNode' });
    }

    // "radice" = alias in linguaggio naturale di sqrt, sia con parentesi
    // ("radice(x)") sia senza ("radice x", "radice9", "radicex"): vedi
    // SYNTAX.md. Senza parentesi prende un solo atomo stretto (per un
    // radicando composto serve la parentesi: "radice(x+1)").
    if (name === 'radice') {
      if (this.check('LPAREN')) {
        const args = this.parseArgList();
        return root(args[0] ?? { id: 'ph', type: 'PlaceholderNode' });
      }
      return root(this.parseTightAtom());
    }

    if (name === 'root' && this.check('LPAREN')) {
      const args = this.parseArgList();
      return root(args[1], args[0]);
    }

    if (name === 'frac' && this.check('LPAREN')) {
      const args = this.parseArgList();
      return fraction(args[0], args[1]);
    }

    if (this.check('LPAREN')) {
      const args = this.parseArgList();
      return func(name, args);
    }

    return ident(name);
  }

  private parseArgList(): MathNode[] {
    this.expect('LPAREN', 'Attesa "("');
    const args: MathNode[] = [];
    if (!this.check('RPAREN')) {
      args.push(this.parseAdditive());
      while (this.match('COMMA')) {
        args.push(this.parseAdditive());
      }
    }
    this.expect('RPAREN', 'Attesa ")"');
    return args;
  }

  private parseBracket(): MathNode {
    this.expect('LBRACKET', 'Attesa "["');
    if (this.check('LBRACKET')) {
      const rows: MathNode[][] = [this.parseRow()];
      while (this.match('COMMA')) {
        rows.push(this.parseRow());
      }
      this.expect('RBRACKET', 'Attesa "]" di chiusura matrice');
      return matrix(rows);
    }
    const entries: MathNode[] = [];
    if (!this.check('RBRACKET')) {
      entries.push(this.parseAdditive());
      while (this.match('COMMA')) {
        entries.push(this.parseAdditive());
      }
    }
    this.expect('RBRACKET', 'Attesa "]"');
    return vector('row', entries);
  }

  private parseRow(): MathNode[] {
    this.expect('LBRACKET', 'Attesa "[" di riga matrice');
    const entries: MathNode[] = [];
    if (!this.check('RBRACKET')) {
      entries.push(this.parseAdditive());
      while (this.match('COMMA')) {
        entries.push(this.parseAdditive());
      }
    }
    this.expect('RBRACKET', 'Attesa "]" di chiusura riga');
    return entries;
  }

  // ---- Costrutti strutturali: int, sum, prod, lim, d/dx, partial ----
  private parseStructural(keyword: string): MathNode {
    switch (keyword) {
      case 'int':
        return this.parseIntegral();
      case 'sum':
        return this.parseSumOrProduct('SumNode');
      case 'prod':
        return this.parseSumOrProduct('ProductNode');
      case 'lim':
        return this.parseLimit();
      case 'd':
        return this.parseDerivative();
      case 'partial':
        return this.parsePartialDerivative();
      default:
        throw new ParseError(`Costrutto sconosciuto "${keyword}"`);
    }
  }

  private parseIntegral(): MathNode {
    let lower: MathNode | undefined;
    let upper: MathNode | undefined;
    if (this.check('UNDERSCORE')) {
      this.advance();
      lower = this.parseTightAtom();
      this.expect('CARET', 'Attesa "^" dopo il limite inferiore dell\'integrale');
      upper = this.parseTightAtom();
    }

    const diffIdx = this.findDifferentialIndex();
    const integrandEnd = diffIdx ?? this.limit;
    const integrand = this.withLimit(integrandEnd, () => this.parseAdditive());

    let variable: MathNode = ident('x');
    if (diffIdx !== null) {
      const diffTok = this.tokens[diffIdx];
      variable = ident(diffTok.text.slice(1));
      this.pos = diffIdx + 1;
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

  private parseSumOrProduct(type: 'SumNode' | 'ProductNode'): MathNode {
    this.expect('UNDERSCORE', 'Attesa "_" dopo sum/prod, es. sum_i=1^n');
    const indexTok = this.expect('IDENT', 'Atteso indice, es. sum_i=1^n');
    this.expect('EQ', 'Attesa "=" nell\'intestazione di sum/prod');
    const lower = this.parseTightAtom();
    this.expect('CARET', 'Attesa "^" per il limite superiore di sum/prod');
    const upper = this.parseTightAtom();
    const expression = this.parseAdditive();
    return {
      id: `agg${Date.now().toString(36)}`,
      type,
      expression,
      index: ident(indexTok.text),
      lower,
      upper,
    } as MathNode;
  }

  private parseLimit(): MathNode {
    this.expect('UNDERSCORE', 'Attesa "_" dopo lim, es. lim_x->0');
    const varTok = this.expect('IDENT', 'Attesa variabile, es. lim_x->0');
    this.expect('ARROW', 'Attesa "->" nel limite');
    let direction: '+' | '-' | undefined;
    let sign: MathNode | null = null;
    if (this.check('MINUS')) {
      this.advance();
      sign = unary('-', this.parseTightAtom());
    }
    const approaches = sign ?? this.parseTightAtom();
    if (this.check('PLUS')) {
      this.advance();
      direction = '+';
    } else if (this.check('MINUS')) {
      this.advance();
      direction = '-';
    }
    const expression = this.parseAdditive();
    return {
      id: `lim${Date.now().toString(36)}`,
      type: 'LimitNode',
      expression,
      variable: ident(varTok.text),
      approaches,
      ...(direction ? { direction } : {}),
    };
  }

  private parseDerivative(): MathNode {
    let order = 1;
    if (this.check('CARET')) {
      this.advance();
      const orderTok = this.expect('NUMBER', 'Attesa un numero per l\'ordine della derivata');
      order = parseInt(orderTok.text, 10);
    }
    this.expect('SLASH', 'Attesa "/" in d/dx');
    const denomTok = this.expect('IDENT', 'Attesa "dx" (variabile) in d/dx');
    if (!/^d[a-zA-Z]$/.test(denomTok.text)) {
      throw new ParseError(`Atteso un simbolo tipo "dx" dopo "d/", trovato "${denomTok.text}"`);
    }
    if (this.check('CARET')) {
      this.advance();
      this.expect('NUMBER', 'Attesa il ripetere l\'ordine della derivata dopo dx^n');
    }
    const variable = ident(denomTok.text.slice(1));
    const expression = this.parseAdditive();
    return {
      id: `der${Date.now().toString(36)}`,
      type: 'DerivativeNode',
      expression,
      variable,
      order,
    };
  }

  private parsePartialDerivative(): MathNode {
    const exprTok = this.expect('IDENT', 'Attesa un\'espressione dopo "partial"');
    this.expect('SLASH', 'Attesa "/" in partial f/partial x');
    this.expect('IDENT', 'Attesa "partial" dopo "/"'); // testo "partial" ripetuto
    const varTok = this.expect('IDENT', 'Attesa la variabile dopo "partial x"');
    return {
      id: `pder${Date.now().toString(36)}`,
      type: 'PartialDerivativeNode',
      expression: ident(exprTok.text),
      variable: ident(varTok.text),
      order: 1,
    };
  }
}

export function parseMathInput(input: string): MathNode {
  return new Parser(input).parseDocument();
}
