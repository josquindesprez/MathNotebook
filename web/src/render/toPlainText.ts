import type { MathNode } from '../ast/types';
import { RELATION_SYMBOL, SET_OP_SYMBOL, toUnicodeSubscript, toUnicodeSuperscript } from './symbolMaps';
import { needsBinaryChildParens } from './precedence';

function needsParens(node: MathNode): boolean {
  return node.type === 'BinaryOperationNode' || node.type === 'UnaryOperationNode' || node.type === 'RelationNode';
}

function renderGrouped(node: MathNode): string {
  const inner = toPlainText(node);
  return needsParens(node) ? `(${inner})` : inner;
}

export function toPlainText(node: MathNode): string {
  switch (node.type) {
    case 'NumberNode':
      return node.value;

    case 'IdentifierNode':
      return node.name;

    case 'SymbolNode':
      return node.symbol;

    case 'BinaryOperationNode': {
      if (node.op === '/') {
        return `(${toPlainText(node.left)})/(${toPlainText(node.right)})`;
      }
      const left = needsBinaryChildParens(node.left, node.op, false) ? `(${toPlainText(node.left)})` : toPlainText(node.left);
      const right = needsBinaryChildParens(node.right, node.op, true) ? `(${toPlainText(node.right)})` : toPlainText(node.right);
      const opText = node.op === '*' || node.op === 'cdot' ? '·' : node.op === 'circ' ? '∘' : node.op;
      return `${left} ${opText} ${right}`;
    }

    case 'UnaryOperationNode': {
      const operand = renderGrouped(node.operand);
      if (node.op === '!') return `${operand}!`;
      return `${node.op}${operand}`;
    }

    case 'FractionNode':
      return `(${toPlainText(node.numerator)})/(${toPlainText(node.denominator)})`;

    case 'PowerNode': {
      const base = renderGrouped(node.base);
      if (node.exponent.type === 'NumberNode') {
        const sup = toUnicodeSuperscript(node.exponent.value);
        if (sup) return `${base}${sup}`;
      }
      return `${base}^(${toPlainText(node.exponent)})`;
    }

    case 'SubscriptNode': {
      const base = renderGrouped(node.base);
      if (node.subscript.type === 'NumberNode') {
        const sub = toUnicodeSubscript(node.subscript.value);
        if (sub) return `${base}${sub}`;
      }
      return `${base}_(${toPlainText(node.subscript)})`;
    }

    case 'RootNode':
      return node.index ? `${toPlainText(node.index)}√(${toPlainText(node.radicand)})` : `√(${toPlainText(node.radicand)})`;

    case 'FunctionNode':
      return renderFunction(node.name, node.args);

    case 'VectorNode':
      return node.orientation === 'row'
        ? `[${node.entries.map(toPlainText).join(', ')}]`
        : `[${node.entries.map(toPlainText).join('; ')}]`;

    case 'MatrixNode':
      return `[${node.rows.map((row) => `[${row.map(toPlainText).join(', ')}]`).join(', ')}]`;

    case 'IntegralNode': {
      const bounds = node.lower && node.upper ? `_${toPlainText(node.lower)}^${toPlainText(node.upper)}` : '';
      return `∫${bounds} ${toPlainText(node.integrand)} d${toPlainText(node.variable)}`;
    }

    case 'DerivativeNode': {
      const expr = renderGrouped(node.expression);
      const v = toPlainText(node.variable);
      if (node.order === 1) return `d/d${v} ${expr}`;
      return `d${toUnicodeSuperscript(String(node.order)) ?? `^${node.order}`}/d${v}${toUnicodeSuperscript(String(node.order)) ?? `^${node.order}`} ${expr}`;
    }

    case 'PartialDerivativeNode':
      return `∂${toPlainText(node.expression)}/∂${toPlainText(node.variable)}`;

    case 'SumNode':
      return `∑_(${toPlainText(node.index)}=${toPlainText(node.lower)})^${toPlainText(node.upper)} ${toPlainText(node.expression)}`;

    case 'ProductNode':
      return `∏_(${toPlainText(node.index)}=${toPlainText(node.lower)})^${toPlainText(node.upper)} ${toPlainText(node.expression)}`;

    case 'LimitNode':
      return `lim_(${toPlainText(node.variable)}→${toPlainText(node.approaches)}) ${toPlainText(node.expression)}`;

    case 'RelationNode': {
      const parts = [toPlainText(node.terms[0])];
      for (let i = 0; i < node.ops.length; i += 1) {
        parts.push(RELATION_SYMBOL[node.ops[i]] ?? node.ops[i], toPlainText(node.terms[i + 1]));
      }
      return parts.join(' ');
    }

    case 'SetNode': {
      const op = SET_OP_SYMBOL[node.op] ?? node.op;
      return node.operands.map(toPlainText).join(` ${op} `);
    }

    case 'PiecewiseNode':
      return node.cases.map((c) => `${toPlainText(c.expression)} se ${toPlainText(c.condition)}`).join('; ');

    case 'SystemNode':
      return node.equations.map(toPlainText).join(node.bracketed === false ? ', ' : '; ');

    case 'PlaceholderNode':
      return '▢';

    case 'GroupNode':
      return `(${toPlainText(node.expression)})`;

    default: {
      const exhaustive: never = node;
      throw new Error(`Nodo AST non gestito da AstToPlainText: ${(exhaustive as MathNode).type}`);
    }
  }
}

function renderFunction(name: string, args: MathNode[]): string {
  const rendered = args.map(toPlainText);
  switch (name) {
    case 'transpose':
      return `${rendered[0]}ᵀ`;
    case 'inv':
      return `${rendered[0]}⁻¹`;
    case 'det':
      return `det(${rendered[0]})`;
    case 'norm':
      return `‖${rendered[0]}‖`;
    case 'abs':
      return `|${rendered[0]}|`;
    case 'dot':
      return `${rendered[0]} · ${rendered[1]}`;
    case 'rank':
      return `rank(${rendered[0]})`;
    default:
      return `${name}(${rendered.join(', ')})`;
  }
}

export const AstToPlainText = { render: toPlainText };
