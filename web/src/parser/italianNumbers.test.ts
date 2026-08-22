import { describe, expect, it } from 'vitest';
import { numberToItalianWords, italianWordToNumber, ITALIAN_NUMBER_WORDS } from './italianNumbers';
import { parseMathInput } from './parser';
import { toLatex } from '../render/toLatex';

describe('numberToItalianWords', () => {
  const cases: [number, string][] = [
    [0, 'zero'],
    [1, 'uno'],
    [3, 'tre'],
    [10, 'dieci'],
    [11, 'undici'],
    [13, 'tredici'],
    [16, 'sedici'],
    [19, 'diciannove'],
    [20, 'venti'],
    [21, 'ventuno'],
    [23, 'ventitré'],
    [28, 'ventotto'],
    [30, 'trenta'],
    [31, 'trentuno'],
    [33, 'trentatré'],
    [42, 'quarantadue'],
    [80, 'ottanta'],
    [88, 'ottantotto'],
    [99, 'novantanove'],
    [100, 'cento'],
    [101, 'centouno'],
    [103, 'centotré'],
    [108, 'centootto'],
    [111, 'centoundici'],
    [121, 'centoventuno'],
    [200, 'duecento'],
    [234, 'duecentotrentaquattro'],
    [300, 'trecento'],
    [555, 'cinquecentocinquantacinque'],
    [700, 'settecento'],
    [808, 'ottocentootto'],
    [900, 'novecento'],
    [999, 'novecentonovantanove'],
  ];

  it.each(cases)('%i -> "%s"', (n, expected) => {
    expect(numberToItalianWords(n)).toBe(expected);
  });

  it('rifiuta valori fuori range 0-999', () => {
    expect(() => numberToItalianWords(-1)).toThrow();
    expect(() => numberToItalianWords(1000)).toThrow();
  });

  it('genera esattamente 1000 parole distinte (0-999, nessuna collisione)', () => {
    expect(ITALIAN_NUMBER_WORDS.size).toBe(1000);
  });
});

describe('italianWordToNumber', () => {
  it('è l\'inversa di numberToItalianWords su tutto 0-999', () => {
    for (let n = 0; n <= 999; n += 1) {
      expect(italianWordToNumber(numberToItalianWords(n))).toBe(n);
    }
  });

  it('restituisce undefined per parole che non sono numeri', () => {
    expect(italianWordToNumber('alla')).toBeUndefined();
    expect(italianWordToNumber('ciao')).toBeUndefined();
  });
});

describe('parseMathInput — numeri in italiano', () => {
  it('"quarantadue" da solo produce NumberNode(42)', () => {
    expect(parseMathInput('quarantadue')).toMatchObject({ type: 'NumberNode', value: '42' });
  });

  it('funzionano dentro espressioni più ampie', () => {
    expect(toLatex(parseMathInput('quarantadue alla due'))).toBe(toLatex(parseMathInput('42^2')));
    expect(toLatex(parseMathInput('tre piu quattro'))).toBe(toLatex(parseMathInput('3+4')));
    expect(toLatex(parseMathInput('novantanove fratto tre'))).toBe(toLatex(parseMathInput('99/3')));
    expect(toLatex(parseMathInput('duecentotrentaquattro per due'))).toBe(toLatex(parseMathInput('234*2')));
  });

  it('si combinano con la sintassi simbolica nella stessa espressione', () => {
    expect(toLatex(parseMathInput('quarantadue + x'))).toBe(toLatex(parseMathInput('42 + x')));
  });
});
