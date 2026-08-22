// Parole "in linguaggio naturale" (italiano) riconosciute dal tokenizzatore
// come alternativa ai simboli (^, /, *, +, -) e a sqrt(). Vedi SYNTAX.md,
// sezione "Linguaggio naturale".
//
// Funzionano sia separate da spazi ("2 alla 3") sia "incollate" a un
// singolo carattere prima e dopo ("2alla3", "2allax", "xallay"): vedi
// splitNaturalLanguageWord per i limiti di questa seconda forma.

export const NATURAL_LANGUAGE_KEYWORDS = ['fratto', 'diviso', 'radice', 'alla', 'piu', 'meno', 'per'] as const;

export type NaturalLanguageKeyword = (typeof NATURAL_LANGUAGE_KEYWORDS)[number];

// Ordinate dalla più lunga alla più corta: in una ricerca "quale parola
// chiave inizia qui" vogliamo il match più lungo (evita che "per" rubi i
// primi caratteri di un'ipotetica parola chiave più lunga che iniziasse
// allo stesso modo — oggi non capita, ma tiene la regola robusta).
const KEYWORDS_BY_LENGTH_DESC = [...NATURAL_LANGUAGE_KEYWORDS].sort((a, b) => b.length - a.length);

/**
 * Spezza una sequenza di lettere incollate cercando parole chiave del
 * linguaggio naturale al suo interno. Per evitare falsi positivi su nomi
 * di variabile più lunghi che contengono per caso una parola chiave come
 * sottostringa (es. "sopera" contiene "per"), il match scatta solo se la
 * parte PRIMA della parola chiave è vuota o di un solo carattere (un nome
 * di variabile plausibile, come "x" in "xallay"). Il resto dopo la parola
 * chiave viene rielaborato con la stessa regola (gestisce anche i casi
 * incatenati, es. "xperyallaz").
 *
 * Se non trova nessun match plausibile, restituisce l'intera parola come
 * un solo identificatore (comportamento invariato).
 */
export function splitNaturalLanguageWord(word: string): string[] {
  if (word.length === 0) return [];

  // Solo le posizioni 0 e 1 sono ammesse (nessun prefisso, o un solo
  // carattere prima): controllate in quest'ordine, preferendo a parità di
  // posizione la parola chiave più lunga (KEYWORDS_BY_LENGTH_DESC).
  for (const idx of [0, 1]) {
    if (idx >= word.length) break;
    const keyword = KEYWORDS_BY_LENGTH_DESC.find((k) => word.startsWith(k, idx));
    if (keyword) {
      const before = word.slice(0, idx);
      const after = word.slice(idx + keyword.length);
      const result: string[] = [];
      if (before) result.push(before);
      result.push(keyword);
      result.push(...splitNaturalLanguageWord(after));
      return result;
    }
  }

  return [word];
}

// Le stesse parole, ma per l'editing dal vivo dentro MathLive (non passa
// dal nostro tokenizer: vedi components/cells/MathCell.tsx). MathLive
// riconosce le sequenze di tasti carattere per carattere e sostituisce il
// testo digitato con questo LaTeX; "#@" è il segnaposto nativo di MathLive
// per "l'atomo appena prima" (usato anche dai suoi tasti ^2 e frazione),
// "#?" per uno slot vuoto navigabile in cui il cursore entra subito dopo.
// Coerente con la stessa pipeline unificata usata da palette e sintassi
// rapida (vedi ARCHITECTURE.md).
export const NATURAL_LANGUAGE_INLINE_SHORTCUTS: Record<NaturalLanguageKeyword, string> = {
  alla: '#@^{#?}',
  per: '\\cdot',
  fratto: '\\frac{#@}{#?}',
  diviso: '\\frac{#@}{#?}',
  piu: '+',
  meno: '-',
  radice: '\\sqrt{#?}',
};
