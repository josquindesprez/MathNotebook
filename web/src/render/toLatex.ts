import type { MathNode } from '../ast/types';
import { GREEK_CHAR_TO_NAME, RELATION_LATEX, SET_CHAR_TO_LATEX, SET_OP_LATEX } from './symbolMaps';
import { needsBinaryChildParens } from './precedence';

const TRIG_LOG_FUNCTIONS = new Set(['sin', 'cos', 'tan', 'cot', 'sec', 'csc', 'ln', 'exp']);

function needsParens(node: MathNode): boolean {
  return (
    node.type === 'BinaryOperationNode' ||
    node.type === 'UnaryOperationNode' ||
    node.type === 'RelationNode' ||
    node.type === 'SetNode'
  );
}

function renderGrouped(node: MathNode): string {
  const inner = toLatex(node);
  return needsParens(node) ? `\\left(${inner}\\right)` : inner;
}

export function toLatex(node: MathNode): string {
  switch (node.type) {
    case 'NumberNode':
      return node.value;

    case 'IdentifierNode':
      return node.name;

    case 'SymbolNode': {
      if (node.kind === 'greek') {
        const name = GREEK_CHAR_TO_NAME[node.symbol];
        return name ? `\\${name}` : node.symbol;
      }
      return SET_CHAR_TO_LATEX[node.symbol] ?? node.symbol;
    }

    case 'BinaryOperationNode': {
      if (node.op === '/') {
        return `\\frac{${toLatex(node.left)}}{${toLatex(node.right)}}`;
      }
      const left = needsBinaryChildParens(node.left, node.op, false) ? `\\left(${toLatex(node.left)}\\right)` : toLatex(node.left);
      const right = needsBinaryChildParens(node.right, node.op, true) ? `\\left(${toLatex(node.right)}\\right)` : toLatex(node.right);
      switch (node.op) {
        case '+':
          return `${left} + ${right}`;
        case '-':
          return `${left} - ${right}`;
        case '*':
        case 'cdot':
          return `${left} \\cdot ${right}`;
        case 'circ':
          return `${left} \\circ ${right}`;
        default:
          return `${left} ${node.op} ${right}`;
      }
    }

    case 'UnaryOperationNode': {
      const operand = renderGrouped(node.operand);
      if (node.op === '!') return `${operand}!`;
      return `${node.op}${operand}`;
    }

    case 'FractionNode':
      return `\\frac{${toLatex(node.numerator)}}{${toLatex(node.denominator)}}`;

    case 'PowerNode':
      return `${renderGrouped(node.base)}^{${toLatex(node.exponent)}}`;

    case 'SubscriptNode':
      return `${renderGrouped(node.base)}_{${toLatex(node.subscript)}}`;

    case 'RootNode':
      return node.index ? `\\sqrt[${toLatex(node.index)}]{${toLatex(node.radicand)}}` : `\\sqrt{${toLatex(node.radicand)}}`;

    case 'FunctionNode':
      return renderFunction(node.name, node.args);

    case 'VectorNode': {
      const entries = node.entries.map(toLatex);
      const body = node.orientation === 'row' ? entries.join(' & ') : entries.join(' \\\\ ');
      return `\\begin{pmatrix}${body}\\end{pmatrix}`;
    }

    case 'MatrixNode': {
      const rows = node.rows.map((row) => row.map(toLatex).join(' & '));
      return `\\begin{pmatrix}\n${rows.join(' \\\\\n')}\n\\end{pmatrix}`;
    }

    case 'IntegralNode': {
      const bounds = node.lower && node.upper ? `_{${toLatex(node.lower)}}^{${toLatex(node.upper)}}` : '';
      return `\\int${bounds} ${toLatex(node.integrand)}\\,d${toLatex(node.variable)}`;
    }

    case 'DerivativeNode': {
      const expr = renderGrouped(node.expression);
      const varLatex = toLatex(node.variable);
      if (node.order === 1) return `\\frac{d}{d${varLatex}}${expr}`;
      return `\\frac{d^${node.order}}{d${varLatex}^${node.order}}${expr}`;
    }

    case 'PartialDerivativeNode': {
      const expr = toLatex(node.expression);
      const varLatex = toLatex(node.variable);
      if (node.order === 1) return `\\frac{\\partial ${expr}}{\\partial ${varLatex}}`;
      return `\\frac{\\partial^${node.order} ${expr}}{\\partial ${varLatex}^${node.order}}`;
    }

    case 'SumNode':
      return `\\sum_{${toLatex(node.index)}=${toLatex(node.lower)}}^{${toLatex(node.upper)}} ${toLatex(node.expression)}`;

    case 'ProductNode':
      return `\\prod_{${toLatex(node.index)}=${toLatex(node.lower)}}^{${toLatex(node.upper)}} ${toLatex(node.expression)}`;

    case 'LimitNode': {
      const dir = node.direction ?? '';
      return `\\lim_{${toLatex(node.variable)} \\to ${toLatex(node.approaches)}${dir}} ${toLatex(node.expression)}`;
    }

    case 'RelationNode': {
      const parts = [toLatex(node.terms[0])];
      for (let i = 0; i < node.ops.length; i += 1) {
        parts.push(RELATION_LATEX[node.ops[i]] ?? node.ops[i], toLatex(node.terms[i + 1]));
      }
      return parts.join(' ');
    }

    case 'SetNode': {
      const op = SET_OP_LATEX[node.op] ?? node.op;
      return node.operands.map(toLatex).join(` ${op} `);
    }

    case 'PiecewiseNode': {
      const cases = node.cases.map((c) => `${toLatex(c.expression)} & ${toLatex(c.condition)}`).join(' \\\\\n');
      return `\\begin{cases}\n${cases}\n\\end{cases}`;
    }

    case 'SystemNode': {
      if (node.bracketed === false) {
        return node.equations.map(toLatex).join(', ');
      }
      const eqs = node.equations.map(toLatex).join(' \\\\\n');
      return `\\begin{cases}\n${eqs}\n\\end{cases}`;
    }

    case 'PlaceholderNode':
      return '\\placeholder{}';

    case 'GroupNode':
      return `\\left(${toLatex(node.expression)}\\right)`;

    default: {
      const exhaustive: never = node;
      throw new Error(`Nodo AST non gestito da AstToLatex: ${(exhaustive as MathNode).type}`);
    }
  }
}

function renderFunction(name: string, args: MathNode[]): string {
  const rendered = args.map(toLatex);
  switch (name) {
    case 'transpose':
      return `${rendered[0]}^T`;
    case 'inv':
      return `${rendered[0]}^{-1}`;
    case 'det':
      return `\\det(${rendered[0]})`;
    case 'norm':
      return `\\left\\|${rendered[0]}\\right\\|`;
    case 'abs':
      return `\\left|${rendered[0]}\\right|`;
    case 'dot':
      return `${rendered[0]} \\cdot ${rendered[1]}`;
    case 'rank':
      return `\\operatorname{rank}(${rendered[0]})`;
    default:
      if (TRIG_LOG_FUNCTIONS.has(name)) {
        return `\\${name}(${rendered.join(', ')})`;
      }
      if (name === 'log') return `\\log(${rendered.join(', ')})`;
      return `${name}(${rendered.join(', ')})`;
  }
}

export const AstToLatex = { render: toLatex };
