import { describe, expect, it } from 'vitest';
import { parseMathInput } from './parser';
import { toLatex } from '../render/toLatex';

describe('parseMathInput — potenze e pedici', () => {
  it('x^2', () => {
    const ast = parseMathInput('x^2');
    expect(ast).toMatchObject({ type: 'PowerNode', base: { type: 'IdentifierNode', name: 'x' }, exponent: { type: 'NumberNode', value: '2' } });
    expect(toLatex(ast)).toBe('x^{2}');
  });

  it('x_1', () => {
    const ast = parseMathInput('x_1');
    expect(ast).toMatchObject({ type: 'SubscriptNode', base: { type: 'IdentifierNode', name: 'x' }, subscript: { type: 'NumberNode', value: '1' } });
  });

  it('x^(n+1)', () => {
    const ast = parseMathInput('x^(n+1)');
    expect(ast).toMatchObject({
      type: 'PowerNode',
      base: { type: 'IdentifierNode', name: 'x' },
      exponent: { type: 'BinaryOperationNode', op: '+' },
    });
    expect(toLatex(ast)).toBe('x^{n + 1}');
  });
});

describe('parseMathInput — radici e frazioni', () => {
  it('sqrt(x)', () => {
    const ast = parseMathInput('sqrt(x)');
    expect(ast).toMatchObject({ type: 'RootNode', radicand: { type: 'IdentifierNode', name: 'x' } });
    expect(toLatex(ast)).toBe('\\sqrt{x}');
  });

  it('(a+b)/(c+d)', () => {
    const ast = parseMathInput('(a+b)/(c+d)');
    expect(ast.type).toBe('BinaryOperationNode');
    expect(toLatex(ast)).toBe('\\frac{a + b}{c + d}');
  });

  it('frac(a,b)', () => {
    const ast = parseMathInput('frac(a,b)');
    expect(ast).toMatchObject({ type: 'FractionNode' });
    expect(toLatex(ast)).toBe('\\frac{a}{b}');
  });
});

describe('parseMathInput — vettori e matrici', () => {
  it('[1,2,3]', () => {
    const ast = parseMathInput('[1,2,3]');
    expect(ast).toMatchObject({
      type: 'VectorNode',
      orientation: 'row',
      entries: [{ value: '1' }, { value: '2' }, { value: '3' }],
    });
  });

  it('[[1,2],[3,4]] produce un MatrixNode con le righe attese', () => {
    const ast = parseMathInput('[[1,2],[3,4]]');
    expect(ast.type).toBe('MatrixNode');
    if (ast.type !== 'MatrixNode') throw new Error('unreachable');
    expect(ast.rows).toHaveLength(2);
    expect(ast.rows[0].map((n) => (n.type === 'NumberNode' ? n.value : null))).toEqual(['1', '2']);
    expect(ast.rows[1].map((n) => (n.type === 'NumberNode' ? n.value : null))).toEqual(['3', '4']);
  });

  it('LaTeX di [[1,2],[3,4]] è \\begin{pmatrix}...\\end{pmatrix}', () => {
    const ast = parseMathInput('[[1,2],[3,4]]');
    expect(toLatex(ast)).toBe('\\begin{pmatrix}\n1 & 2 \\\\\n3 & 4\n\\end{pmatrix}');
  });
});

describe('parseMathInput — algebra lineare', () => {
  it('A^T -> transpose(A)', () => {
    const ast = parseMathInput('A^T');
    expect(ast).toMatchObject({ type: 'FunctionNode', name: 'transpose', args: [{ type: 'IdentifierNode', name: 'A' }] });
    expect(toLatex(ast)).toBe('A^T');
  });

  it('A^-1 -> inv(A)', () => {
    const ast = parseMathInput('A^-1');
    expect(ast).toMatchObject({ type: 'FunctionNode', name: 'inv', args: [{ type: 'IdentifierNode', name: 'A' }] });
    expect(toLatex(ast)).toBe('A^{-1}');
  });

  it('dot(v,w)', () => {
    const ast = parseMathInput('dot(v,w)');
    expect(ast).toMatchObject({ type: 'FunctionNode', name: 'dot' });
    expect(toLatex(ast)).toBe('v \\cdot w');
  });

  it('norm(v)', () => {
    const ast = parseMathInput('norm(v)');
    expect(toLatex(ast)).toBe('\\left\\|v\\right\\|');
  });

  it('|x| (valore assoluto tramite pipe)', () => {
    const ast = parseMathInput('|x|');
    expect(ast).toMatchObject({ type: 'FunctionNode', name: 'abs', args: [{ name: 'x' }] });
    expect(toLatex(ast)).toBe('\\left|x\\right|');
  });

  it('det(A)', () => {
    const ast = parseMathInput('det(A)');
    expect(toLatex(ast)).toBe('\\det(A)');
  });
});

describe('parseMathInput — calcolo', () => {
  it('int x^2 dx (indefinito)', () => {
    const ast = parseMathInput('int x^2 dx');
    expect(ast.type).toBe('IntegralNode');
    if (ast.type !== 'IntegralNode') throw new Error('unreachable');
    expect(ast.lower).toBeUndefined();
    expect(ast.variable).toMatchObject({ type: 'IdentifierNode', name: 'x' });
    expect(toLatex(ast)).toBe('\\int x^{2}\\,dx');
  });

  it('int_0^1 x^2 dx (definito)', () => {
    const ast = parseMathInput('int_0^1 x^2 dx');
    expect(toLatex(ast)).toBe('\\int_{0}^{1} x^{2}\\,dx');
  });

  it('sum_i=1^n i', () => {
    const ast = parseMathInput('sum_i=1^n i');
    expect(ast.type).toBe('SumNode');
    expect(toLatex(ast)).toBe('\\sum_{i=1}^{n} i');
  });

  it('lim_x->0 sin(x)/x', () => {
    const ast = parseMathInput('lim_x->0 sin(x)/x');
    expect(ast.type).toBe('LimitNode');
    expect(toLatex(ast)).toBe('\\lim_{x \\to 0} \\frac{\\sin(x)}{x}');
  });

  it('d/dx x^2', () => {
    const ast = parseMathInput('d/dx x^2');
    expect(ast.type).toBe('DerivativeNode');
    if (ast.type !== 'DerivativeNode') throw new Error('unreachable');
    expect(ast.variable).toMatchObject({ name: 'x' });
    expect(ast.order).toBe(1);
  });
});

describe('parseMathInput — greco, relazioni, errori', () => {
  it('alpha, beta, theta risolvono ai simboli unicode', () => {
    expect(parseMathInput('alpha')).toMatchObject({ type: 'SymbolNode', symbol: 'α', kind: 'greek' });
    expect(parseMathInput('theta')).toMatchObject({ symbol: 'θ' });
  });

  it('a != b, x >= 3, x in R', () => {
    expect(parseMathInput('a != b')).toMatchObject({ type: 'RelationNode', op: '!=' });
    expect(parseMathInput('x >= 3')).toMatchObject({ type: 'RelationNode', op: '>=' });
    expect(parseMathInput('x in R')).toMatchObject({ type: 'SetNode', op: 'in' });
  });

  it('rilancia ParseError su input malformato', () => {
    expect(() => parseMathInput('x + + ')).toThrow();
    expect(() => parseMathInput('(a+b')).toThrow();
  });
});

describe('parseMathInput — algebra lineare (M2): insiemi numerici e segnature', () => {
  it('R, N, Z, Q, C sono riservati ai simboli insieme', () => {
    expect(parseMathInput('R')).toMatchObject({ type: 'SymbolNode', symbol: 'ℝ', kind: 'set' });
    expect(parseMathInput('N')).toMatchObject({ symbol: 'ℕ' });
    expect(parseMathInput('Z')).toMatchObject({ symbol: 'ℤ' });
    expect(parseMathInput('Q')).toMatchObject({ symbol: 'ℚ' });
    expect(parseMathInput('C')).toMatchObject({ symbol: 'ℂ' });
  });

  it('v in R^3 -> SetNode con R^3 come PowerNode su un SymbolNode insieme', () => {
    const ast = parseMathInput('v in R^3');
    expect(ast.type).toBe('SetNode');
    if (ast.type !== 'SetNode') throw new Error('unreachable');
    expect(ast.operands[1]).toMatchObject({ type: 'PowerNode', base: { symbol: 'ℝ' }, exponent: { value: '3' } });
    expect(toLatex(ast)).toBe('v \\in \\mathbb{R}^{3}');
  });

  it('T:R^2 -> R^3 produce una segnatura di funzione (RelationNode \':\' che contiene una RelationNode \'to\')', () => {
    const ast = parseMathInput('T:R^2 -> R^3');
    expect(ast).toMatchObject({ type: 'RelationNode', op: ':', left: { name: 'T' } });
    if (ast.type !== 'RelationNode') throw new Error('unreachable');
    expect(ast.right).toMatchObject({ type: 'RelationNode', op: 'to' });
    expect(toLatex(ast)).toBe('T \\colon \\mathbb{R}^{2} \\to \\mathbb{R}^{3}');
  });

  it('"A v" (lettere singole separate da spazio) è un prodotto A*v, non un identificatore "Av"', () => {
    const ast = parseMathInput('A v = lambda v');
    expect(ast).toMatchObject({ type: 'RelationNode', op: '=' });
    if (ast.type !== 'RelationNode') throw new Error('unreachable');
    expect(ast.left).toMatchObject({ type: 'BinaryOperationNode', op: '*', left: { name: 'A' }, right: { name: 'v' } });
  });

  it('R non è riservato quando è chiamato come funzione, es. R(x)', () => {
    expect(parseMathInput('R(x)')).toMatchObject({ type: 'FunctionNode', name: 'R' });
  });
});
