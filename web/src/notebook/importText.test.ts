import { describe, expect, it } from 'vitest';
import { parseWorksheetText } from './importText';
import { toLatex } from '../render/toLatex';

describe('parseWorksheetText', () => {
  it('un paragrafo con matematica inline diventa una Text Cell', () => {
    const cells = parseWorksheetText('Dati i vettori $\\mathbf a = 1$ e $\\mathbf b = 2$, calcola la somma.');
    expect(cells).toHaveLength(1);
    expect(cells[0].type).toBe('text');
  });

  it('una riga di solo LaTeX diventa una Math Cell', () => {
    const cells = parseWorksheetText('\\mathbf a+\\mathbf b');
    expect(cells).toHaveLength(1);
    expect(cells[0].type).toBe('math');
    if (cells[0].type !== 'math') throw new Error('unreachable');
    expect(toLatex(cells[0].ast)).toContain('mathbf');
  });

  it('righe consecutive di solo LaTeX diventano Math Cell separate (senza righe vuote tra loro)', () => {
    const cells = parseWorksheetText('AB\n2A+B\nA\\in\\mathbb R^{m\\times n}');
    expect(cells.map((c) => c.type)).toEqual(['math', 'math', 'math']);
  });

  it('righe corte di sola prosa (titoli tipo "Esercizio 3") non diventano Math Cell', () => {
    const cells = parseWorksheetText('Esercizio 3\n1. Somma di vettori');
    expect(cells).toHaveLength(1);
    expect(cells[0].type).toBe('text');
  });

  it('un vero foglio esercizi in stile GPT produce l\'alternanza Text/Math attesa', () => {
    const worksheet = [
      '1. Somma di vettori',
      'Dati i vettori $\\mathbf a = 1$ e $\\mathbf b = 2$, calcola la loro somma.',
      '\\mathbf a+\\mathbf b',
      '',
      '2. Moltiplicazione per scalare',
      'Dato $\\mathbf v = 1$, calcola:',
      '3\\mathbf v',
      '',
      '6. Ortogonalità',
      'Dati $\\mathbf a = 1$ e $\\mathbf b = 2$, stabilisci se sono ortogonali.',
    ].join('\n');

    const cells = parseWorksheetText(worksheet);
    expect(cells.map((c) => c.type)).toEqual(['text', 'math', 'text', 'math', 'text']);
  });

  it('un foglio formule (una per riga, senza righe vuote) produce tutte Math Cell', () => {
    const worksheet = [
      '\\mathbf a+\\mathbf b=\\begin{pmatrix}a_1+b_1\\\\a_2+b_2\\end{pmatrix}',
      '\\|\\mathbf v\\|=\\sqrt{v_1^2+v_2^2}',
      'A\\in\\mathbb R^{m\\times n}',
      '(m\\times n)(n\\times p)\\rightarrow(m\\times p)',
    ].join('\n');

    const cells = parseWorksheetText(worksheet);
    expect(cells).toHaveLength(4);
    expect(cells.every((c) => c.type === 'math')).toBe(true);
  });

  it('testo vuoto o solo spazi non produce celle', () => {
    expect(parseWorksheetText('')).toHaveLength(0);
    expect(parseWorksheetText('   \n\n  ')).toHaveLength(0);
  });
});
