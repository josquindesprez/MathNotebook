import type { BinaryOp, MathNode } from '../ast/types';

// Precedenza usata dai pretty-printer (LaTeX/Typst/PlainText) per decidere
// quando un operando binario necessita di parentesi visibili. La divisione
// ha precedenza "massima" perché viene sempre resa come frazione
// (\frac{}{}, frac(), (a)/(b)): è visivamente già raggruppata e non
// richiede mai parentesi aggiuntive attorno a sé.
function precedence(op: BinaryOp): number {
  if (op === '+' || op === '-') return 1;
  if (op === '/') return 3;
  return 2; // '*', 'cdot', 'circ'
}

export function needsBinaryChildParens(child: MathNode, parentOp: BinaryOp, isRightChild: boolean): boolean {
  if (child.type !== 'BinaryOperationNode') return false;
  const childPrec = precedence(child.op);
  const parentPrec = precedence(parentOp);
  if (childPrec < parentPrec) return true;
  if (childPrec === parentPrec && isRightChild && parentOp !== '+' && parentOp !== '*' && parentOp !== 'cdot') {
    return true;
  }
  return false;
}
