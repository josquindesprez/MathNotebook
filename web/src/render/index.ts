import type { MathNode } from '../ast/types';
import { toLatex, AstToLatex } from './toLatex';
import { toTypst, AstToTypst } from './toTypst';
import { toMathMLDocument, AstToMathML } from './toMathML';
import { toPlainText, AstToPlainText } from './toPlainText';

export { toLatex, AstToLatex, toTypst, AstToTypst, toMathMLDocument, AstToMathML, toPlainText, AstToPlainText };

export type OutputFormat = 'latex' | 'typst' | 'mathml' | 'plaintext';

export function render(node: MathNode, format: OutputFormat): string {
  switch (format) {
    case 'latex':
      return toLatex(node);
    case 'typst':
      return toTypst(node);
    case 'mathml':
      return toMathMLDocument(node);
    case 'plaintext':
      return toPlainText(node);
    default: {
      const exhaustive: never = format;
      throw new Error(`Formato di output sconosciuto: ${exhaustive}`);
    }
  }
}
