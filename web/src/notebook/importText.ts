// Importazione di un foglio di testo/LaTeX (es. generato da ChatGPT) in
// celle del notebook. Vedi UI_SPEC.md, "Import text". Euristica
// deliberatamente semplice e "best effort": ogni cella risultante può
// comunque essere cambiata di tipo a mano dal menu "..." della cella se la
// classificazione automatica sbaglia (Text ↔ Math), quindi non serve un
// riconoscimento perfetto.

import { latexToAst } from '../parser/latex-bridge';
import { createMathCell, createTextCell, type Cell } from './model';

/** Una riga è considerata "LaTeX puro" (quindi candidata a Math Cell) se:
 * - non contiene "$" (altrimenti è testo con matematica inline, per una Text Cell);
 * - contiene un comando LaTeX ("\" seguito da una lettera, es. \mathbf, \sqrt, \begin), oppure
 * - non contiene spazi ed è fatta solo di caratteri tipici della notazione matematica
 *   (lettere, cifre, operatori) — copre casi come "AB" o "2A+B".
 * Una riga di prosa con spazi ma senza comandi LaTeX (es. "Esercizio 3",
 * "1. Somma di vettori") non passa nessuna delle due condizioni: resta prosa. */
function looksLikeLatex(trimmed: string): boolean {
  if (trimmed.includes('$')) return false;
  if (/\\[a-zA-Z]/.test(trimmed)) return true;
  if (/\s/.test(trimmed)) return false;
  return /^[A-Za-z0-9+\-*/^_(){}[\].,=<>|]+$/.test(trimmed);
}

export function parseWorksheetText(text: string): Cell[] {
  const lines = text.split(/\r\n|\r|\n/);
  const cells: Cell[] = [];
  let buffer: string[] = [];

  function flushText() {
    if (buffer.length === 0) return;
    const markdown = buffer.join('\n').trim();
    if (markdown) cells.push(createTextCell(markdown));
    buffer = [];
  }

  for (const rawLine of lines) {
    const trimmed = rawLine.trim();
    if (trimmed === '') {
      flushText();
      continue;
    }

    if (looksLikeLatex(trimmed)) {
      try {
        const ast = latexToAst(trimmed);
        flushText();
        cells.push(createMathCell(ast));
        continue;
      } catch {
        // Sembrava LaTeX ma non lo era davvero: trattala come prosa.
      }
    }

    buffer.push(rawLine);
  }
  flushText();

  return cells;
}
