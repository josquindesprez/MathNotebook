import type { MathNode } from '../ast/types';
import { RELATION_SYMBOL, SET_OP_SYMBOL } from './symbolMaps';

const TRIG_LOG_FUNCTIONS = new Set(['sin', 'cos', 'tan', 'cot', 'sec', 'csc', 'ln', 'log', 'exp']);

function mrow(children: string): string {
  return `<mrow>${children}</mrow>`;
}

function mo(text: string): string {
  return `<mo>${escapeXml(text)}</mo>`;
}

function escapeXml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

export function toMathML(node: MathNode): string {
  switch (node.type) {
    case 'NumberNode':
      return `<mn>${escapeXml(node.value)}</mn>`;

    case 'IdentifierNode':
      return `<mi>${escapeXml(node.name)}</mi>`;

    case 'SymbolNode':
      return `<mi>${escapeXml(node.symbol)}</mi>`;

    case 'BinaryOperationNode': {
      const opSymbol = node.op === '*' || node.op === 'cdot' ? '·' : node.op === 'circ' ? '∘' : node.op === '/' ? '/' : node.op;
      return mrow(`${toMathML(node.left)}${mo(opSymbol)}${toMathML(node.right)}`);
    }

    case 'UnaryOperationNode':
      return mrow(`${mo(node.op === '!' ? '' : node.op)}${toMathML(node.operand)}${node.op === '!' ? mo('!') : ''}`);

    case 'FractionNode':
      return `<mfrac>${toMathML(node.numerator)}${toMathML(node.denominator)}</mfrac>`;

    case 'PowerNode':
      return `<msup>${toMathML(node.base)}${toMathML(node.exponent)}</msup>`;

    case 'SubscriptNode':
      return `<msub>${toMathML(node.base)}${toMathML(node.subscript)}</msub>`;

    case 'RootNode':
      return node.index
        ? `<mroot>${toMathML(node.radicand)}${toMathML(node.index)}</mroot>`
        : `<msqrt>${toMathML(node.radicand)}</msqrt>`;

    case 'FunctionNode': {
      const name = latinFunctionName(node.name);
      const args = node.args.map(toMathML).join(mo(','));
      return mrow(`<mi>${escapeXml(name)}</mi>${mo('(')}${args}${mo(')')}`);
    }

    case 'VectorNode': {
      const cells = node.entries.map((e) => `<mtd>${toMathML(e)}</mtd>`);
      const rows = node.orientation === 'row' ? [`<mtr>${cells.join('')}</mtr>`] : cells.map((c) => `<mtr>${c}</mtr>`);
      return `<mrow>${mo('(')}<mtable>${rows.join('')}</mtable>${mo(')')}</mrow>`;
    }

    case 'MatrixNode': {
      const rows = node.rows.map((row) => `<mtr>${row.map((e) => `<mtd>${toMathML(e)}</mtd>`).join('')}</mtr>`);
      return `<mrow>${mo('(')}<mtable>${rows.join('')}</mtable>${mo(')')}</mrow>`;
    }

    case 'IntegralNode': {
      const bounds = node.lower && node.upper ? `<msubsup>${mo('∫')}${toMathML(node.lower)}${toMathML(node.upper)}</msubsup>` : mo('∫');
      return mrow(`${bounds}${toMathML(node.integrand)}<mo>d</mo>${toMathML(node.variable)}`);
    }

    case 'DerivativeNode': {
      const orderSup = node.order > 1 ? `<msup><mi>d</mi><mn>${node.order}</mn></msup>` : `<mi>d</mi>`;
      const denomBase = node.order > 1 ? `<msup>${toMathML(node.variable)}<mn>${node.order}</mn></msup>` : toMathML(node.variable);
      const denom = mrow(`<mi>d</mi>${denomBase}`);
      return mrow(`<mfrac>${orderSup}${denom}</mfrac>${toMathML(node.expression)}`);
    }

    case 'PartialDerivativeNode': {
      const num = mrow(`<mo>∂</mo>${toMathML(node.expression)}`);
      const denom = mrow(`<mo>∂</mo>${toMathML(node.variable)}`);
      return `<mfrac>${num}${denom}</mfrac>`;
    }

    case 'SumNode':
    case 'ProductNode': {
      const symbol = node.type === 'SumNode' ? '∑' : '∏';
      const under = mrow(`${toMathML(node.index)}${mo('=')}${toMathML(node.lower)}`);
      return mrow(`<munderover>${mo(symbol)}${under}${toMathML(node.upper)}</munderover>${toMathML(node.expression)}`);
    }

    case 'LimitNode': {
      const under = mrow(`${toMathML(node.variable)}${mo('→')}${toMathML(node.approaches)}`);
      return mrow(`<munder><mo>lim</mo>${under}</munder>${toMathML(node.expression)}`);
    }

    case 'RelationNode':
      return mrow(`${toMathML(node.left)}${mo(RELATION_SYMBOL[node.op] ?? node.op)}${toMathML(node.right)}`);

    case 'SetNode': {
      const symbol = SET_OP_SYMBOL[node.op] ?? node.op;
      return mrow(node.operands.map(toMathML).join(mo(symbol)));
    }

    case 'PiecewiseNode':
    case 'SystemNode': {
      const equations = node.type === 'PiecewiseNode' ? node.cases.map((c) => c.expression) : node.equations;
      const rows = equations.map((eq) => `<mtr><mtd>${toMathML(eq)}</mtd></mtr>`);
      return `<mrow>${mo('{')}<mtable>${rows.join('')}</mtable></mrow>`;
    }

    case 'PlaceholderNode':
      return '<mi>&#9633;</mi>';

    case 'GroupNode':
      return mrow(`${mo('(')}${toMathML(node.expression)}${mo(')')}`);

    default: {
      const exhaustive: never = node;
      throw new Error(`Nodo AST non gestito da AstToMathML: ${(exhaustive as MathNode).type}`);
    }
  }
}

function latinFunctionName(name: string): string {
  if (TRIG_LOG_FUNCTIONS.has(name)) return name;
  const specialNames: Record<string, string> = { det: 'det', norm: 'norm', rank: 'rank', dot: 'dot', transpose: 'transpose', inv: 'inv' };
  return specialNames[name] ?? name;
}

export function toMathMLDocument(node: MathNode): string {
  return `<math xmlns="http://www.w3.org/1998/Math/MathML">${toMathML(node)}</math>`;
}

export const AstToMathML = { render: toMathMLDocument };
