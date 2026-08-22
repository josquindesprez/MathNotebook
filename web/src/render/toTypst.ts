import type { MathNode } from '../ast/types';
import { GREEK_CHAR_TO_NAME, RELATION_TYPST, SET_CHAR_TO_TYPST, SET_OP_TYPST } from './symbolMaps';
import { needsBinaryChildParens } from './precedence';

const TRIG_LOG_FUNCTIONS = new Set(['sin', 'cos', 'tan', 'cot', 'sec', 'csc', 'ln', 'log', 'exp']);

function needsParens(node: MathNode): boolean {
  return node.type === 'BinaryOperationNode' || node.type === 'UnaryOperationNode' || node.type === 'RelationNode';
}

function renderGrouped(node: MathNode): string {
  const inner = toTypst(node);
  return needsParens(node) ? `(${inner})` : inner;
}

export function toTypst(node: MathNode): string {
  switch (node.type) {
    case 'NumberNode':
      return node.value;

    case 'IdentifierNode':
      return node.name;

    case 'SymbolNode': {
      if (node.kind === 'greek') return GREEK_CHAR_TO_NAME[node.symbol] ?? node.symbol;
      return SET_CHAR_TO_TYPST[node.symbol] ?? node.symbol;
    }

    case 'BinaryOperationNode': {
      if (node.op === '/') {
        return `frac(${toTypst(node.left)}, ${toTypst(node.right)})`;
      }
      const left = needsBinaryChildParens(node.left, node.op, false) ? `(${toTypst(node.left)})` : toTypst(node.left);
      const right = needsBinaryChildParens(node.right, node.op, true) ? `(${toTypst(node.right)})` : toTypst(node.right);
      switch (node.op) {
        case '+':
          return `${left} + ${right}`;
        case '-':
          return `${left} - ${right}`;
        case '*':
        case 'cdot':
          return `${left} dot ${right}`;
        case 'circ':
          return `${left} compose ${right}`;
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
      return `frac(${toTypst(node.numerator)}, ${toTypst(node.denominator)})`;

    case 'PowerNode':
      return `${renderGrouped(node.base)}^(${toTypst(node.exponent)})`;

    case 'SubscriptNode':
      return `${renderGrouped(node.base)}_(${toTypst(node.subscript)})`;

    case 'RootNode':
      return node.index ? `root(${toTypst(node.index)}, ${toTypst(node.radicand)})` : `sqrt(${toTypst(node.radicand)})`;

    case 'FunctionNode':
      return renderFunction(node.name, node.args);

    case 'VectorNode':
      return `vec(${node.entries.map(toTypst).join(', ')})`;

    case 'MatrixNode':
      return `mat(${node.rows.map((row) => row.map(toTypst).join(', ')).join('; ')})`;

    case 'IntegralNode': {
      const bounds = node.lower && node.upper ? `_(${toTypst(node.lower)})^(${toTypst(node.upper)})` : '';
      return `integral${bounds} ${toTypst(node.integrand)} dif ${toTypst(node.variable)}`;
    }

    case 'DerivativeNode': {
      const expr = renderGrouped(node.expression);
      const v = toTypst(node.variable);
      if (node.order === 1) return `frac(dif, dif ${v}) ${expr}`;
      return `frac(dif^${node.order}, dif ${v}^${node.order}) ${expr}`;
    }

    case 'PartialDerivativeNode': {
      const expr = toTypst(node.expression);
      const v = toTypst(node.variable);
      if (node.order === 1) return `frac(diff ${expr}, diff ${v})`;
      return `frac(diff^${node.order} ${expr}, diff ${v}^${node.order})`;
    }

    case 'SumNode':
      return `sum_(${toTypst(node.index)}=${toTypst(node.lower)})^(${toTypst(node.upper)}) ${toTypst(node.expression)}`;

    case 'ProductNode':
      return `product_(${toTypst(node.index)}=${toTypst(node.lower)})^(${toTypst(node.upper)}) ${toTypst(node.expression)}`;

    case 'LimitNode':
      return `lim_(${toTypst(node.variable)} -> ${toTypst(node.approaches)}) ${toTypst(node.expression)}`;

    case 'RelationNode':
      return `${toTypst(node.left)} ${RELATION_TYPST[node.op] ?? node.op} ${toTypst(node.right)}`;

    case 'SetNode': {
      const op = SET_OP_TYPST[node.op] ?? node.op;
      return node.operands.map(toTypst).join(` ${op} `);
    }

    case 'PiecewiseNode':
      return `cases(${node.cases.map((c) => `${toTypst(c.expression)} & ${toTypst(c.condition)}`).join(', ')})`;

    case 'SystemNode':
      return `cases(${node.equations.map(toTypst).join(', ')})`;

    case 'PlaceholderNode':
      return '#[]';

    case 'GroupNode':
      return `(${toTypst(node.expression)})`;

    default: {
      const exhaustive: never = node;
      throw new Error(`Nodo AST non gestito da AstToTypst: ${(exhaustive as MathNode).type}`);
    }
  }
}

function renderFunction(name: string, args: MathNode[]): string {
  const rendered = args.map(toTypst);
  switch (name) {
    case 'transpose':
      return `${rendered[0]}^T`;
    case 'inv':
      return `${rendered[0]}^(-1)`;
    case 'det':
      return `det(${rendered[0]})`;
    case 'norm':
      return `norm(${rendered[0]})`;
    case 'abs':
      return `abs(${rendered[0]})`;
    case 'dot':
      return `${rendered[0]} dot ${rendered[1]}`;
    case 'rank':
      return `"rank"(${rendered[0]})`;
    default:
      if (TRIG_LOG_FUNCTIONS.has(name)) return `${name}(${rendered.join(', ')})`;
      return `${name}(${rendered.join(', ')})`;
  }
}

export const AstToTypst = { render: toTypst };
