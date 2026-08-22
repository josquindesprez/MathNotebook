import { describe, expect, it } from 'vitest';
import { splitNaturalLanguageWord } from './naturalLanguage';
import { parseMathInput } from './parser';
import { toLatex } from '../render/toLatex';

describe('splitNaturalLanguageWord', () => {
  it('lascia invariati gli identificatori normali', () => {
    expect(splitNaturalLanguageWord('x')).toEqual(['x']);
    expect(splitNaturalLanguageWord('alpha')).toEqual(['alpha']);
    expect(splitNaturalLanguageWord('sopera')).toEqual(['sopera']); // "per" a metà parola: non spezzato
  });

  it('spezza una parola chiave incollata a un solo carattere prima/dopo', () => {
    expect(splitNaturalLanguageWord('allax')).toEqual(['alla', 'x']);
    expect(splitNaturalLanguageWord('xallay')).toEqual(['x', 'alla', 'y']);
    expect(splitNaturalLanguageWord('perx')).toEqual(['per', 'x']);
  });

  it('gestisce catene multiple', () => {
    expect(splitNaturalLanguageWord('xperyallaz')).toEqual(['x', 'per', 'y', 'alla', 'z']);
  });
});

describe('parseMathInput — linguaggio naturale', () => {
  it('"2 alla 3" e "2alla3" producono lo stesso PowerNode di "2^3"', () => {
    const spaced = parseMathInput('2 alla 3');
    const glued = parseMathInput('2alla3');
    const symbolic = parseMathInput('2^3');
    expect(toLatex(spaced)).toBe(toLatex(symbolic));
    expect(toLatex(glued)).toBe(toLatex(symbolic));
  });

  it('"2allax" produce 2^x', () => {
    const ast = parseMathInput('2allax');
    expect(ast).toMatchObject({ type: 'PowerNode', base: { value: '2' }, exponent: { name: 'x' } });
  });

  it('"3 fratto 4" e "3 diviso 4" equivalgono a "3/4"', () => {
    const expected = toLatex(parseMathInput('3/4'));
    expect(toLatex(parseMathInput('3 fratto 4'))).toBe(expected);
    expect(toLatex(parseMathInput('3 diviso 4'))).toBe(expected);
    expect(toLatex(parseMathInput('3fratto4'))).toBe(expected);
  });

  it('"2 per x" e "2perx" equivalgono a "2*x" (moltiplicazione implicita)', () => {
    const expected = toLatex(parseMathInput('2x'));
    expect(toLatex(parseMathInput('2 per x'))).toBe(expected);
    expect(toLatex(parseMathInput('2perx'))).toBe(expected);
  });

  it('"3 piu 4" e "5 meno 2" equivalgono a +/-', () => {
    expect(toLatex(parseMathInput('3 piu 4'))).toBe(toLatex(parseMathInput('3+4')));
    expect(toLatex(parseMathInput('5 meno 2'))).toBe(toLatex(parseMathInput('5-2')));
  });

  it('"meno 3" è un meno unario, come "-3"', () => {
    expect(toLatex(parseMathInput('meno 3'))).toBe(toLatex(parseMathInput('-3')));
  });

  it('"radice x", "radice(x)" e "radicex" equivalgono a "sqrt(x)"', () => {
    const expected = toLatex(parseMathInput('sqrt(x)'));
    expect(toLatex(parseMathInput('radice x'))).toBe(expected);
    expect(toLatex(parseMathInput('radice(x)'))).toBe(expected);
    expect(toLatex(parseMathInput('radicex'))).toBe(expected);
  });

  it('"2radice3" moltiplica implicitamente come "2sqrt(3)"', () => {
    const expected = toLatex(parseMathInput('2sqrt(3)'));
    expect(toLatex(parseMathInput('2radice3'))).toBe(expected);
  });

  it('combinazioni realistiche: "2 alla x piu 1" = "2^x + 1"', () => {
    const expected = toLatex(parseMathInput('2^x + 1'));
    expect(toLatex(parseMathInput('2 alla x piu 1'))).toBe(expected);
  });
});
