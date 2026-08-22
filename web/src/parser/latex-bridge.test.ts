import { describe, expect, it } from 'vitest';
import { latexToAst } from './latex-bridge';
import { toLatex } from '../render/toLatex';
import { parseMathInput } from './parser';

// Il bridge LaTeX -> AST non era mai stato testato direttamente: questi
// test coprono in particolare il round-trip di un'equazione semplice, che
// fino a poco fa produceva un AST corrotto (vedi commit/nota in
// ARCHITECTURE.md: '=' , '<', '>' erano tokenizzati come LETTER generico
// invece che con un tipo di token dedicato).
describe('latexToAst — round-trip di base', () => {
  it('"A=B" produce una RelationNode, non un prodotto con un identificatore fittizio "="', () => {
    const ast = latexToAst('A=B');
    expect(ast).toMatchObject({ type: 'RelationNode', ops: ['='], terms: [{ name: 'A' }, { name: 'B' }] });
  });

  it('"x < 3" e "x > 3" producono RelationNode con l\'operatore corretto', () => {
    expect(latexToAst('x<3')).toMatchObject({ type: 'RelationNode', ops: ['<'] });
    expect(latexToAst('x>3')).toMatchObject({ type: 'RelationNode', ops: ['>'] });
  });

  it('round-trip AST -> LaTeX -> AST per una potenza e una frazione', () => {
    const original = parseMathInput('x^2');
    const roundTripped = latexToAst(toLatex(original));
    expect(roundTripped).toMatchObject({ type: 'PowerNode', base: { name: 'x' }, exponent: { value: '2' } });

    const frac = parseMathInput('frac(a,b)');
    expect(latexToAst(toLatex(frac))).toMatchObject({ type: 'FractionNode' });
  });

  it('round-trip di una matrice 2x2', () => {
    const original = parseMathInput('[[1,2],[3,4]]');
    const roundTripped = latexToAst(toLatex(original));
    expect(roundTripped.type).toBe('MatrixNode');
    if (roundTripped.type !== 'MatrixNode') throw new Error('unreachable');
    expect(roundTripped.rows[0].map((n) => (n.type === 'NumberNode' ? n.value : null))).toEqual(['1', '2']);
  });

  it('"|x|" (\\left|x\\right|) torna a essere abs(x), non un carattere "|" scartato silenziosamente', () => {
    const ast = latexToAst('\\left|x\\right|');
    expect(ast).toMatchObject({ type: 'FunctionNode', name: 'abs', args: [{ name: 'x' }] });
  });

  it('"\\|v\\|" (norma) round-trip completo AST -> LaTeX -> AST', () => {
    const original = parseMathInput('norm(v)');
    const roundTripped = latexToAst(toLatex(original));
    expect(roundTripped).toMatchObject({ type: 'FunctionNode', name: 'norm', args: [{ name: 'v' }] });
  });

  it('\\mathbb{R} torna a essere il simbolo insieme ℝ', () => {
    expect(latexToAst('\\mathbb{R}')).toMatchObject({ type: 'SymbolNode', symbol: 'ℝ', kind: 'set' });
  });

  it('"T\\colon\\mathbb{R}^2 \\to \\mathbb{R}^3" produce la segnatura di funzione attesa', () => {
    const ast = latexToAst('T\\colon\\mathbb{R}^2 \\to \\mathbb{R}^3');
    expect(ast).toMatchObject({ type: 'RelationNode', ops: [':'] });
    if (ast.type !== 'RelationNode') throw new Error('unreachable');
    expect(ast.terms[0]).toMatchObject({ name: 'T' });
    expect(ast.terms[1]).toMatchObject({ type: 'RelationNode', ops: ['to'] });
  });

  it('round-trip di una catena di relazioni "2+2=4=2*2"', () => {
    const original = parseMathInput('2+2 = 4 = 2*2');
    const roundTripped = latexToAst(toLatex(original));
    expect(roundTripped).toMatchObject({ type: 'RelationNode', ops: ['=', '='] });
    if (roundTripped.type !== 'RelationNode') throw new Error('unreachable');
    expect(roundTripped.terms).toHaveLength(3);
  });

  it('round-trip di affermazioni multiple separate da virgola (SystemNode non racchiuso)', () => {
    const original = parseMathInput('A = [[1,2],[3,4]], B = [[5,6],[7,8]]');
    const roundTripped = latexToAst(toLatex(original));
    expect(roundTripped).toMatchObject({ type: 'SystemNode', bracketed: false });
    if (roundTripped.type !== 'SystemNode') throw new Error('unreachable');
    expect(roundTripped.equations).toHaveLength(2);
  });
});
