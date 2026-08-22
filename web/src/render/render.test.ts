import { describe, expect, it } from 'vitest';
import { parseMathInput } from '../parser/parser';
import { toLatex } from './toLatex';
import { toTypst } from './toTypst';
import { toPlainText } from './toPlainText';
import { toMathMLDocument } from './toMathML';

describe('AstToLatex — precedenza tra operatori binari', () => {
  it('non aggiunge parentesi superflue per + concatenati', () => {
    expect(toLatex(parseMathInput('a+b+c'))).toBe('a + b + c');
  });

  it('aggiunge parentesi quando la precedenza lo richiede', () => {
    expect(toLatex(parseMathInput('(a+b)*c'))).toBe('\\left(a + b\\right) \\cdot c');
    expect(toLatex(parseMathInput('a-(b-c)'))).toBe('a - \\left(b - c\\right)');
  });

  it('non serve avvolgere una frazione in parentesi: si autoraggruppa', () => {
    expect(toLatex(parseMathInput('(a+b)/(c+d)'))).toBe('\\frac{a + b}{c + d}');
  });
});

describe('Renderer indipendenti producono output plausibile senza eccezioni', () => {
  const samples = ['x^2', 'sqrt(x)', 'frac(a,b)', '[[1,2],[3,4]]', 'A^T', 'det(A)', 'a != b'];

  it.each(samples)('%s viene renderizzato in tutti i formati', (input) => {
    const ast = parseMathInput(input);
    expect(() => toLatex(ast)).not.toThrow();
    expect(() => toTypst(ast)).not.toThrow();
    expect(() => toPlainText(ast)).not.toThrow();
    expect(() => toMathMLDocument(ast)).not.toThrow();
  });

  it('AstToPlainText usa unicode per potenze semplici', () => {
    expect(toPlainText(parseMathInput('x^2'))).toBe('x²');
  });

  it('AstToMathML produce un documento <math> valido come stringa XML', () => {
    const xml = toMathMLDocument(parseMathInput('x^2'));
    expect(xml.startsWith('<math')).toBe(true);
    expect(xml).toContain('<msup>');
  });
});
