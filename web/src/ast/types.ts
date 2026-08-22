// Math AST — fonte di verità del documento. Vedi ARCHITECTURE.md.
// Ogni nodo è dato puro, serializzabile 1:1 in JSON.

export type SymbolKind = 'greek' | 'set' | 'constant' | 'misc';

export type BinaryOp = '+' | '-' | '*' | '/' | 'cdot' | 'circ';
export type UnaryOp = '-' | '+' | '!';
// ':' e 'to' coprono la notazione di segnatura di funzione (es. "T:R^2 -> R^3",
// vedi SYNTAX.md); riusano RelationNode invece di un nodo dedicato perché
// semanticamente sono comunque una relazione binaria tra due espressioni.
export type RelationOp = '=' | '!=' | '<' | '>' | '<=' | '>=' | '~~' | 'equiv' | ':' | 'to';
export type SetOp =
  | 'in'
  | 'notin'
  | 'subset'
  | 'subseteq'
  | 'union'
  | 'intersect';

interface NodeBase {
  id: string;
}

export interface NumberNode extends NodeBase {
  type: 'NumberNode';
  value: string;
}

export interface IdentifierNode extends NodeBase {
  type: 'IdentifierNode';
  name: string;
}

export interface SymbolNode extends NodeBase {
  type: 'SymbolNode';
  symbol: string;
  kind: SymbolKind;
}

export interface BinaryOperationNode extends NodeBase {
  type: 'BinaryOperationNode';
  op: BinaryOp;
  left: MathNode;
  right: MathNode;
}

export interface UnaryOperationNode extends NodeBase {
  type: 'UnaryOperationNode';
  op: UnaryOp;
  operand: MathNode;
}

export interface FractionNode extends NodeBase {
  type: 'FractionNode';
  numerator: MathNode;
  denominator: MathNode;
}

export interface PowerNode extends NodeBase {
  type: 'PowerNode';
  base: MathNode;
  exponent: MathNode;
}

export interface SubscriptNode extends NodeBase {
  type: 'SubscriptNode';
  base: MathNode;
  subscript: MathNode;
}

export interface RootNode extends NodeBase {
  type: 'RootNode';
  radicand: MathNode;
  index?: MathNode;
}

export interface FunctionNode extends NodeBase {
  type: 'FunctionNode';
  name: string;
  args: MathNode[];
}

export interface VectorNode extends NodeBase {
  type: 'VectorNode';
  orientation: 'row' | 'column';
  entries: MathNode[];
}

export interface MatrixNode extends NodeBase {
  type: 'MatrixNode';
  rows: MathNode[][];
}

export interface IntegralNode extends NodeBase {
  type: 'IntegralNode';
  integrand: MathNode;
  variable: MathNode;
  lower?: MathNode;
  upper?: MathNode;
}

export interface DerivativeNode extends NodeBase {
  type: 'DerivativeNode';
  expression: MathNode;
  variable: MathNode;
  order: number;
}

export interface PartialDerivativeNode extends NodeBase {
  type: 'PartialDerivativeNode';
  expression: MathNode;
  variable: MathNode;
  order: number;
}

export interface SumNode extends NodeBase {
  type: 'SumNode';
  expression: MathNode;
  index: MathNode;
  lower: MathNode;
  upper: MathNode;
}

export interface ProductNode extends NodeBase {
  type: 'ProductNode';
  expression: MathNode;
  index: MathNode;
  lower: MathNode;
  upper: MathNode;
}

export interface LimitNode extends NodeBase {
  type: 'LimitNode';
  expression: MathNode;
  variable: MathNode;
  approaches: MathNode;
  direction?: '+' | '-';
}

// Una catena di relazioni: "a = b = c" (passaggi di calcolo) o "a < b < c"
// (disuguaglianze incatenate). terms.length === ops.length + 1 sempre;
// il caso più comune (una sola relazione) è terms=[left,right], ops=[op].
export interface RelationNode extends NodeBase {
  type: 'RelationNode';
  terms: MathNode[];
  ops: RelationOp[];
}

export interface SetNode extends NodeBase {
  type: 'SetNode';
  op: SetOp;
  operands: MathNode[];
}

export interface PiecewiseNode extends NodeBase {
  type: 'PiecewiseNode';
  cases: { expression: MathNode; condition: MathNode }[];
}

export interface SystemNode extends NodeBase {
  type: 'SystemNode';
  equations: MathNode[];
  // true (o assente, per compatibilità) = sistema da risolvere insieme,
  // reso con la graffa "\begin{cases}"; false = più affermazioni
  // indipendenti nella stessa cella, separate da virgola, senza graffa
  // (es. "A = [[1,2],[3,4]], B = [[5,6],[7,8]]" — vedi SYNTAX.md).
  bracketed?: boolean;
}

export interface PlaceholderNode extends NodeBase {
  type: 'PlaceholderNode';
}

export interface GroupNode extends NodeBase {
  type: 'GroupNode';
  expression: MathNode;
}

export type MathNode =
  | NumberNode
  | IdentifierNode
  | SymbolNode
  | BinaryOperationNode
  | UnaryOperationNode
  | FractionNode
  | PowerNode
  | SubscriptNode
  | RootNode
  | FunctionNode
  | VectorNode
  | MatrixNode
  | IntegralNode
  | DerivativeNode
  | PartialDerivativeNode
  | SumNode
  | ProductNode
  | LimitNode
  | RelationNode
  | SetNode
  | PiecewiseNode
  | SystemNode
  | PlaceholderNode
  | GroupNode;

export type MathNodeType = MathNode['type'];
