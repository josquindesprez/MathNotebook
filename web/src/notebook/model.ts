// Modello dati del notebook e formato documento .mathnb. Vedi DOCUMENT_FORMAT.md.

import type { MathNode } from '../ast/types';
import { placeholder } from '../ast/build';

// v2: RelationNode è passata da {op,left,right} a {terms[],ops[]} per
// supportare le catene di relazioni ("a = b = c"). Vedi
// document-io.ts (migrazione 1 -> 2) e DOCUMENT_FORMAT.md.
export const CURRENT_SCHEMA_VERSION = 2;

// true = questa cella apre un nuovo gruppo visuale: il colore del bordo
// sinistro cambia rispetto al gruppo precedente (ciclico, non scelto
// dall'utente — vedi UI_SPEC.md, "Raggruppamento celle"). Campo opzionale
// e additivo: non richiede una nuova versione di schema.
interface GroupableCell {
  groupStart?: boolean;
}

export interface TextCell extends GroupableCell {
  id: string;
  type: 'text';
  markdown: string;
}

export interface MathCell extends GroupableCell {
  id: string;
  type: 'math';
  ast: MathNode;
}

export interface CalculationCell extends GroupableCell {
  id: string;
  type: 'calculation';
  ast: MathNode;
  lastResult?: {
    ast: MathNode;
    computedAt: string;
    engine: 'sympy';
  };
}

export interface GraphCell extends GroupableCell {
  id: string;
  type: 'graph';
  expressions: { id: string; ast: MathNode; color?: string; visible: boolean }[];
  viewport?: { xMin: number; xMax: number; yMin: number; yMax: number };
}

export type Cell = TextCell | MathCell | CalculationCell | GraphCell;

export interface NotebookSettings {
  theme: 'system' | 'light' | 'dark';
}

export interface NotebookDocument {
  schemaVersion: number;
  id: string;
  title: string;
  createdAt: string;
  modifiedAt: string;
  settings: NotebookSettings;
  cells: Cell[];
}

function uuid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

export function createEmptyNotebook(title = 'Nuovo notebook'): NotebookDocument {
  const now = new Date().toISOString();
  return {
    schemaVersion: CURRENT_SCHEMA_VERSION,
    id: uuid(),
    title,
    createdAt: now,
    modifiedAt: now,
    settings: { theme: 'system' },
    cells: [createTextCell(''), createMathCell()],
  };
}

export function createTextCell(markdown = ''): TextCell {
  return { id: uuid(), type: 'text', markdown };
}

export function createMathCell(ast?: MathNode): MathCell {
  return { id: uuid(), type: 'math', ast: ast ?? placeholder() };
}

export function createCalculationCell(ast?: MathNode): CalculationCell {
  return { id: uuid(), type: 'calculation', ast: ast ?? placeholder() };
}

export function createGraphCell(): GraphCell {
  return { id: uuid(), type: 'graph', expressions: [] };
}

export function duplicateCell(cell: Cell): Cell {
  return { ...structuredClone(cell), id: uuid() };
}

export const GROUP_COLOR_COUNT = 6;

/** Indice colore di gruppo (0..GROUP_COLOR_COUNT-1) per ogni cella, nello
 * stesso ordine di `cells`. La prima cella è sempre l'inizio del primo
 * gruppo; ogni cella con `groupStart: true` incrementa il colore
 * (ciclicamente) rispetto al gruppo precedente. Vedi UI_SPEC.md. */
export function computeGroupColorIndices(cells: Cell[]): number[] {
  let current = 0;
  let seenFirst = false;
  return cells.map((cell) => {
    if (!seenFirst) {
      seenFirst = true;
    } else if (cell.groupStart) {
      current = (current + 1) % GROUP_COLOR_COUNT;
    }
    return current;
  });
}
