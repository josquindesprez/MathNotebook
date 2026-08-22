// Numeri scritti in italiano, da 0 a 999 (vedi SYNTAX.md, "Linguaggio
// naturale"). Invece di un dizionario scritto a mano (soggetto a errori e
// difficile da mantenere), generiamo la forma italiana corretta per ogni
// numero nell'intervallo con le regole di composizione standard, e da
// quella costruiamo il dizionario inverso (parola -> valore) usato sia dal
// tokenizzatore della sintassi rapida sia dalle scorciatoie dal vivo di
// MathLive (vedi components/cells/MathCell.tsx).

const UNITS = ['zero', 'uno', 'due', 'tre', 'quattro', 'cinque', 'sei', 'sette', 'otto', 'nove'];
const TEENS = ['dieci', 'undici', 'dodici', 'tredici', 'quattordici', 'quindici', 'sedici', 'diciassette', 'diciotto', 'diciannove'];
// Indicizzato per cifra delle decine (2-9); 0 e 1 non usati direttamente.
const TENS = ['', '', 'venti', 'trenta', 'quaranta', 'cinquanta', 'sessanta', 'settanta', 'ottanta', 'novanta'];

/** Da 0 a 99. Gestisce le elisioni ("ventuno", non "ventiuno"; "ventotto",
 * non "ventiotto") e l'accento su "tré" quando non è il numero 3 da solo. */
function wordsForTens(n: number): string {
  if (n < 10) return UNITS[n];
  if (n < 20) return TEENS[n - 10];
  const tensWord = TENS[Math.floor(n / 10)];
  const unit = n % 10;
  if (unit === 0) return tensWord;
  if (unit === 1 || unit === 8) return tensWord.slice(0, -1) + UNITS[unit];
  if (unit === 3) return `${tensWord}tré`;
  return tensWord + UNITS[unit];
}

/** Da 0 a 999. */
export function numberToItalianWords(n: number): string {
  if (n < 0 || n > 999 || !Number.isInteger(n)) {
    throw new RangeError(`numberToItalianWords supporta solo interi 0-999, ricevuto ${n}`);
  }
  if (n < 100) return wordsForTens(n);
  const hundredsDigit = Math.floor(n / 100);
  const remainder = n % 100;
  const hundredsWord = hundredsDigit === 1 ? 'cento' : UNITS[hundredsDigit] + 'cento';
  if (remainder === 0) return hundredsWord;
  if (remainder === 3) return `${hundredsWord}tré`;
  return hundredsWord + wordsForTens(remainder);
}

/** parola italiana -> valore, per l'intero intervallo 0-999. */
export const ITALIAN_NUMBER_WORDS: ReadonlyMap<string, number> = (() => {
  const map = new Map<string, number>();
  for (let n = 0; n <= 999; n += 1) {
    map.set(numberToItalianWords(n), n);
  }
  return map;
})();

export function italianWordToNumber(word: string): number | undefined {
  return ITALIAN_NUMBER_WORDS.get(word);
}

// Stessa tabella, in forma di scorciatoie MathLive (parola -> stringa di
// cifre) per il riconoscimento dal vivo dentro il campo <math-field>: vedi
// components/cells/MathCell.tsx e naturalLanguage.ts.
export const ITALIAN_NUMBER_INLINE_SHORTCUTS: Record<string, string> = Object.fromEntries(
  [...ITALIAN_NUMBER_WORDS.entries()].map(([word, value]) => [word, String(value)])
);
