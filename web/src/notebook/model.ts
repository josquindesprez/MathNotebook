// Modello dati del notebook e formato documento .mathnb. Vedi DOCUMENT_FORMAT.md.

import type { MathNode } from '../ast/types';
import { placeholder } from '../ast/build';

export const CURRENT_SCHEMA_VERSION = 1;

export interface TextCell {
  id: string;
  type: 'text';
  markdown: string;
}

export interface MathCell {
  id: string;
  type: 'math';
  ast: MathNode;
}

export interface CalculationCell {
  id: string;
  type: 'calculation';
  ast: MathNode;
  lastResult?: {
    ast: MathNode;
    computedAt: string;
    engine: 'sympy';
  };
}

export interface GraphCell {
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
