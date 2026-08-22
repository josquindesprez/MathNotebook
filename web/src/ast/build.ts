// Factory functions per costruire nodi AST con id univoco già assegnato.
import type {
  BinaryOp,
  BinaryOperationNode,
  FractionNode,
  GroupNode,
  IdentifierNode,
  MathNode,
  NumberNode,
  PlaceholderNode,
  PowerNode,
  RelationOp,
  RelationNode,
  RootNode,
  SubscriptNode,
  SymbolKind,
  SymbolNode,
  UnaryOp,
  UnaryOperationNode,
  MatrixNode,
  VectorNode,
  FunctionNode,
} from './types';

let counter = 0;
export function nextId(): string {
  counter += 1;
  return `n${Date.now().toString(36)}${counter.toString(36)}`;
}

export const num = (value: string): NumberNode => ({ id: nextId(), type: 'NumberNode', value });
export const ident = (name: string): IdentifierNode => ({ id: nextId(), type: 'IdentifierNode', name });
export const sym = (symbol: string, kind: SymbolKind = 'misc'): SymbolNode => ({ id: nextId(), type: 'SymbolNode', symbol, kind });
export const placeholder = (): PlaceholderNode => ({ id: nextId(), type: 'PlaceholderNode' });
export const group = (expression: MathNode): GroupNode => ({ id: nextId(), type: 'GroupNode', expression });

export const binary = (op: BinaryOp, left: MathNode, right: MathNode): BinaryOperationNode => ({
  id: nextId(),
  type: 'BinaryOperationNode',
  op,
  left,
  right,
});

export const unary = (op: UnaryOp, operand: MathNode): UnaryOperationNode => ({
  id: nextId(),
  type: 'UnaryOperationNode',
  op,
  operand,
});

export const fraction = (numerator: MathNode, denominator: MathNode): FractionNode => ({
  id: nextId(),
  type: 'FractionNode',
  numerator,
  denominator,
});

export const power = (base: MathNode, exponent: MathNode): PowerNode => ({
  id: nextId(),
  type: 'PowerNode',
  base,
  exponent,
});

export const subscript = (base: MathNode, sub: MathNode): SubscriptNode => ({
  id: nextId(),
  type: 'SubscriptNode',
  base,
  subscript: sub,
});

export const root = (radicand: MathNode, index?: MathNode): RootNode => ({
  id: nextId(),
  type: 'RootNode',
  radicand,
  ...(index ? { index } : {}),
});

export const relation = (op: RelationOp, left: MathNode, right: MathNode): RelationNode => ({
  id: nextId(),
  type: 'RelationNode',
  terms: [left, right],
  ops: [op],
});

/** Aggiunge un passaggio a una catena di relazioni già esistente: usato per
 * "a = b = c" o "a < b < c" (vedi SYNTAX.md, "Catene di relazioni"). */
export function extendRelationChain(chain: RelationNode, op: RelationOp, next: MathNode): RelationNode {
  return { ...chain, id: nextId(), terms: [...chain.terms, next], ops: [...chain.ops, op] };
}

export const func = (name: string, args: MathNode[]): FunctionNode => ({
  id: nextId(),
  type: 'FunctionNode',
  name,
  args,
});

export const vector = (orientation: 'row' | 'column', entries: MathNode[]): VectorNode => ({
  id: nextId(),
  type: 'VectorNode',
  orientation,
  entries,
});

export const matrix = (rows: MathNode[][]): MatrixNode => ({
  id: nextId(),
  type: 'MatrixNode',
  rows,
});
