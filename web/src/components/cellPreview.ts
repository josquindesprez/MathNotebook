// Anteprima statica e in sola lettura di una cella qualunque, usata dal
// "peek" sulla cella precedente quando una Math/Text cell è aperta a
// schermo intero su mobile (vedi CellShell.tsx). Riusa i renderer già
// esistenti (KaTeX per la matematica, lo stesso mini-markdown delle Text
// Cell) invece di introdurne di nuovi.
import katex from 'katex';
import type { Cell } from '../notebook/model';
import { toLatex } from '../render/toLatex';
import { renderMarkdown } from './cells/miniMarkdown';

export function renderCellPreviewHtml(cell: Cell): string {
  if (cell.type === 'text') {
    return cell.markdown.trim() === '' ? '<span class="cell-peek-empty">(vuoto)</span>' : renderMarkdown(cell.markdown);
  }
  if (cell.type === 'math' || cell.type === 'calculation') {
    try {
      return katex.renderToString(toLatex(cell.ast), { throwOnError: false, displayMode: false });
    } catch {
      return '<span class="cell-peek-empty">(non renderizzabile)</span>';
    }
  }
  return '';
}
