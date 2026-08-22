// Tokenizzatore della sintassi rapida. Vedi SYNTAX.md.

import { splitNaturalLanguageWord } from './naturalLanguage';
import { italianWordToNumber } from './italianNumbers';

export type TokenType =
  | 'NUMBER'
  | 'IDENT'
  | 'PLUS'
  | 'MINUS'
  | 'STAR'
  | 'SLASH'
  | 'CARET'
  | 'UNDERSCORE'
  | 'BANG'
  | 'LPAREN'
  | 'RPAREN'
  | 'LBRACKET'
  | 'RBRACKET'
  | 'COMMA'
  | 'COLON'
  | 'EQ'
  | 'NE'
  | 'LE'
  | 'GE'
  | 'LT'
  | 'GT'
  | 'APPROX'
  | 'ARROW'
  | 'PIPE'
  | 'DOUBLE_PIPE'
  | 'EOF';

export interface Token {
  type: TokenType;
  text: string;
  pos: number;
}

const SIMPLE: Record<string, TokenType> = {
  '+': 'PLUS',
  '*': 'STAR',
  '/': 'SLASH',
  '^': 'CARET',
  '_': 'UNDERSCORE',
  '!': 'BANG', // '!=' è gestito prima come caso a due caratteri
  '(': 'LPAREN',
  ')': 'RPAREN',
  '[': 'LBRACKET',
  ']': 'RBRACKET',
  ',': 'COMMA',
  ':': 'COLON',
  '|': 'PIPE',
  '=': 'EQ',
  '<': 'LT',
  '>': 'GT',
};

function isDigit(ch: string): boolean {
  return ch >= '0' && ch <= '9';
}

function isLetter(ch: string): boolean {
  return /[a-zA-Zα-ωΑ-Ω]/.test(ch);
}

export function tokenize(input: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;
  const n = input.length;

  while (i < n) {
    const ch = input[i];

    if (ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r') {
      i += 1;
      continue;
    }

    // operatori a due caratteri
    const two = input.slice(i, i + 2);
    if (two === '!=') {
      tokens.push({ type: 'NE', text: two, pos: i });
      i += 2;
      continue;
    }
    if (two === '<=') {
      tokens.push({ type: 'LE', text: two, pos: i });
      i += 2;
      continue;
    }
    if (two === '>=') {
      tokens.push({ type: 'GE', text: two, pos: i });
      i += 2;
      continue;
    }
    if (two === '~~') {
      tokens.push({ type: 'APPROX', text: two, pos: i });
      i += 2;
      continue;
    }
    if (two === '->') {
      tokens.push({ type: 'ARROW', text: two, pos: i });
      i += 2;
      continue;
    }
    if (two === '||') {
      tokens.push({ type: 'DOUBLE_PIPE', text: two, pos: i });
      i += 2;
      continue;
    }
    if (ch === '-') {
      tokens.push({ type: 'MINUS', text: '-', pos: i });
      i += 1;
      continue;
    }

    if (isDigit(ch)) {
      let j = i + 1;
      while (j < n && isDigit(input[j])) j += 1;
      if (input[j] === '.' && isDigit(input[j + 1] ?? '')) {
        j += 1;
        while (j < n && isDigit(input[j])) j += 1;
      }
      tokens.push({ type: 'NUMBER', text: input.slice(i, j), pos: i });
      i = j;
      continue;
    }

    if (isLetter(ch)) {
      let j = i + 1;
      while (j < n && isLetter(input[j])) j += 1;
      const run = input.slice(i, j);

      // Un numero scritto in italiano ("quarantadue", 0-999, vedi
      // italianNumbers.ts) diventa direttamente un NUMBER, utilizzabile
      // ovunque un numero è valido — anche dentro espressioni più ampie
      // ("quarantadue alla due"), perché da qui in poi il parser non vede
      // alcuna differenza rispetto ad aver digitato "42".
      const italianValue = italianWordToNumber(run);
      if (italianValue !== undefined) {
        tokens.push({ type: 'NUMBER', text: String(italianValue), pos: i });
        i = j;
        continue;
      }

      // Spezza parole "incollate" tipo "2allax" in alla + x (vedi
      // naturalLanguage.ts); per un identificatore normale non cambia
      // nulla: restituisce [run] così com'è.
      for (const part of splitNaturalLanguageWord(run)) {
        tokens.push({ type: 'IDENT', text: part, pos: i });
      }
      i = j;
      continue;
    }

    const simple = SIMPLE[ch];
    if (simple) {
      tokens.push({ type: simple, text: ch, pos: i });
      i += 1;
      continue;
    }

    throw new SyntaxError(`Carattere inatteso "${ch}" alla posizione ${i}`);
  }

  tokens.push({ type: 'EOF', text: '', pos: n });
  return tokens;
}
