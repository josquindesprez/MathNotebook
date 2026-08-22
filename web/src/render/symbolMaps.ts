import { GREEK_LETTERS, SET_SYMBOLS } from '../ast/symbols';

export const GREEK_CHAR_TO_NAME: Record<string, string> = Object.fromEntries(
  Object.entries(GREEK_LETTERS).map(([name, char]) => [char, name])
);

export const SET_CHAR_TO_LATEX: Record<string, string> = {
  [SET_SYMBOLS.R]: '\\mathbb{R}',
  [SET_SYMBOLS.N]: '\\mathbb{N}',
  [SET_SYMBOLS.Z]: '\\mathbb{Z}',
  [SET_SYMBOLS.Q]: '\\mathbb{Q}',
  [SET_SYMBOLS.C]: '\\mathbb{C}',
  '∅': '\\emptyset',
};

export const SET_CHAR_TO_TYPST: Record<string, string> = {
  [SET_SYMBOLS.R]: 'RR',
  [SET_SYMBOLS.N]: 'NN',
  [SET_SYMBOLS.Z]: 'ZZ',
  [SET_SYMBOLS.Q]: 'QQ',
  [SET_SYMBOLS.C]: 'CC',
  '∅': 'emptyset',
};

export const RELATION_SYMBOL: Record<string, string> = {
  '=': '=',
  '!=': '≠',
  '<': '<',
  '>': '>',
  '<=': '≤',
  '>=': '≥',
  '~~': '≈',
  equiv: '≡',
  ':': ':',
  to: '→',
};

export const RELATION_LATEX: Record<string, string> = {
  '=': '=',
  '!=': '\\neq',
  '<': '<',
  '>': '>',
  '<=': '\\leq',
  '>=': '\\geq',
  '~~': '\\approx',
  equiv: '\\equiv',
  ':': '\\colon',
  to: '\\to',
};

export const RELATION_TYPST: Record<string, string> = {
  '=': '=',
  '!=': 'eq.not',
  '<': '<',
  '>': '>',
  '<=': 'lt.eq',
  '>=': 'gt.eq',
  '~~': 'approx',
  equiv: 'equiv',
  ':': ':',
  to: '->',
};

export const SET_OP_SYMBOL: Record<string, string> = {
  in: '∈',
  notin: '∉',
  subset: '⊂',
  subseteq: '⊆',
  union: '∪',
  intersect: '∩',
};

export const SET_OP_LATEX: Record<string, string> = {
  in: '\\in',
  notin: '\\notin',
  subset: '\\subset',
  subseteq: '\\subseteq',
  union: '\\cup',
  intersect: '\\cap',
};

export const SET_OP_TYPST: Record<string, string> = {
  in: 'in',
  notin: 'in.not',
  subset: 'subset',
  subseteq: 'subset.eq',
  union: 'union',
  intersect: 'sect',
};

const SUPERSCRIPT_DIGITS: Record<string, string> = {
  '0': '⁰',
  '1': '¹',
  '2': '²',
  '3': '³',
  '4': '⁴',
  '5': '⁵',
  '6': '⁶',
  '7': '⁷',
  '8': '⁸',
  '9': '⁹',
  '-': '⁻',
};

const SUBSCRIPT_DIGITS: Record<string, string> = {
  '0': '₀',
  '1': '₁',
  '2': '₂',
  '3': '₃',
  '4': '₄',
  '5': '₅',
  '6': '₆',
  '7': '₇',
  '8': '₈',
  '9': '₉',
  '-': '₋',
};

export function toUnicodeSuperscript(text: string): string | null {
  let out = '';
  for (const ch of text) {
    const mapped = SUPERSCRIPT_DIGITS[ch];
    if (!mapped) return null;
    out += mapped;
  }
  return out;
}

export function toUnicodeSubscript(text: string): string | null {
  let out = '';
  for (const ch of text) {
    const mapped = SUBSCRIPT_DIGITS[ch];
    if (!mapped) return null;
    out += mapped;
  }
  return out;
}
