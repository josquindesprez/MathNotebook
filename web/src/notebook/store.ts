// Stato applicativo del notebook attivo: celle, selezione, undo/redo,
// stato di salvataggio. Vedi UI_SPEC.md per il comportamento atteso delle
// celle e DOCUMENT_FORMAT.md per il documento persistito.

import { create } from 'zustand';
import type { MathNode } from '../ast/types';
import {
  type Cell,
  type NotebookDocument,
  createEmptyNotebook,
  createMathCell,
  createTextCell,
  duplicateCell,
} from './model';

export type SaveState = 'saved' | 'unsaved' | 'saving';

interface HistoryEntry {
  cells: Cell[];
}

interface NotebookState {
  doc: NotebookDocument;
  filePath: string | null;
  selectedCellId: string | null;
  editingCellId: string | null;
  saveState: SaveState;
  past: HistoryEntry[];
  future: HistoryEntry[];

  loadDocument: (doc: NotebookDocument, filePath: string | null) => void;
  setFilePath: (path: string | null) => void;
  markSaved: () => void;
  markDirty: () => void;

  selectCell: (id: string | null) => void;
  setEditing: (id: string | null) => void;

  updateCell: (id: string, updater: (cell: Cell) => Cell) => void;
  setMathCellAst: (id: string, ast: MathNode) => void;
  setTextCellMarkdown: (id: string, markdown: string) => void;

  insertTextCellAfter: (id: string | null) => string;
  insertMathCellAfter: (id: string | null, ast?: MathNode) => string;
  duplicateCellById: (id: string) => void;
  deleteCellById: (id: string) => void;
  moveCellSelection: (id: string, direction: 'up' | 'down') => void;
  moveCell: (id: string, direction: 'up' | 'down') => void;
  changeCellType: (id: string, type: Cell['type']) => void;

  undo: () => void;
  redo: () => void;
}

function snapshot(cells: Cell[]): HistoryEntry {
  return { cells: structuredClone(cells) };
}

const HISTORY_LIMIT = 100;

export const useNotebookStore = create<NotebookState>((set, get) => ({
  doc: createEmptyNotebook(),
  filePath: null,
  selectedCellId: null,
  editingCellId: null,
  saveState: 'saved',
  past: [],
  future: [],

  loadDocument: (doc, filePath) =>
    set({
      doc,
      filePath,
      selectedCellId: doc.cells[0]?.id ?? null,
      editingCellId: null,
      saveState: 'saved',
      past: [],
      future: [],
    }),

  setFilePath: (path) => set({ filePath: path }),
  markSaved: () => set({ saveState: 'saved' }),
  markDirty: () => set((s) => (s.saveState === 'unsaved' ? s : { saveState: 'unsaved' })),

  selectCell: (id) => set({ selectedCellId: id }),
  setEditing: (id) => set({ editingCellId: id, selectedCellId: id ?? get().selectedCellId }),

  updateCell: (id, updater) => {
    const { doc, past } = get();
    const nextPast = [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT);
    const cells = doc.cells.map((c) => (c.id === id ? updater(c) : c));
    set({ doc: { ...doc, cells }, past: nextPast, future: [], saveState: 'unsaved' });
  },

  setMathCellAst: (id, ast) => {
    get().updateCell(id, (cell) => (cell.type === 'math' || cell.type === 'calculation' ? { ...cell, ast } : cell));
  },

  setTextCellMarkdown: (id, markdown) => {
    get().updateCell(id, (cell) => (cell.type === 'text' ? { ...cell, markdown } : cell));
  },

  insertTextCellAfter: (id) => {
    const { doc, past } = get();
    const cell = createTextCell('');
    const cells = insertAfter(doc.cells, id, cell);
    set({ doc: { ...doc, cells }, past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT), future: [], saveState: 'unsaved', selectedCellId: cell.id, editingCellId: cell.id });
    return cell.id;
  },

  insertMathCellAfter: (id, ast) => {
    const { doc, past } = get();
    const cell = createMathCell(ast);
    const cells = insertAfter(doc.cells, id, cell);
    set({ doc: { ...doc, cells }, past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT), future: [], saveState: 'unsaved', selectedCellId: cell.id, editingCellId: cell.id });
    return cell.id;
  },

  duplicateCellById: (id) => {
    const { doc, past } = get();
    const idx = doc.cells.findIndex((c) => c.id === id);
    if (idx === -1) return;
    const copy = duplicateCell(doc.cells[idx]);
    const cells = [...doc.cells.slice(0, idx + 1), copy, ...doc.cells.slice(idx + 1)];
    set({ doc: { ...doc, cells }, past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT), future: [], saveState: 'unsaved', selectedCellId: copy.id });
  },

  deleteCellById: (id) => {
    const { doc, past, selectedCellId } = get();
    const idx = doc.cells.findIndex((c) => c.id === id);
    if (idx === -1) return;
    const cells = doc.cells.filter((c) => c.id !== id);
    const nextSelected = selectedCellId === id ? cells[Math.max(0, idx - 1)]?.id ?? null : selectedCellId;
    set({
      doc: { ...doc, cells },
      past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT),
      future: [],
      saveState: 'unsaved',
      selectedCellId: nextSelected,
      editingCellId: null,
    });
  },

  moveCellSelection: (id, direction) => {
    const { doc } = get();
    const idx = doc.cells.findIndex((c) => c.id === id);
    if (idx === -1) return;
    const nextIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (nextIdx < 0 || nextIdx >= doc.cells.length) return;
    set({ selectedCellId: doc.cells[nextIdx].id });
  },

  moveCell: (id, direction) => {
    const { doc, past } = get();
    const idx = doc.cells.findIndex((c) => c.id === id);
    if (idx === -1) return;
    const swapWith = direction === 'up' ? idx - 1 : idx + 1;
    if (swapWith < 0 || swapWith >= doc.cells.length) return;
    const cells = [...doc.cells];
    [cells[idx], cells[swapWith]] = [cells[swapWith], cells[idx]];
    set({ doc: { ...doc, cells }, past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT), future: [], saveState: 'unsaved' });
  },

  changeCellType: (id, type) => {
    const { doc, past } = get();
    const cells = doc.cells.map((c) => {
      if (c.id !== id || c.type === type) return c;
      if (type === 'text') return createRetyped(c, createTextCell(''));
      if (type === 'math' || type === 'calculation') return createRetyped(c, { ...createMathCell(), type });
      return c;
    });
    set({ doc: { ...doc, cells }, past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT), future: [], saveState: 'unsaved' });
  },

  undo: () => {
    const { doc, past, future } = get();
    if (past.length === 0) return;
    const previous = past[past.length - 1];
    set({
      doc: { ...doc, cells: previous.cells },
      past: past.slice(0, -1),
      future: [snapshot(doc.cells), ...future].slice(0, HISTORY_LIMIT),
      saveState: 'unsaved',
    });
  },

  redo: () => {
    const { doc, past, future } = get();
    if (future.length === 0) return;
    const next = future[0];
    set({
      doc: { ...doc, cells: next.cells },
      past: [...past, snapshot(doc.cells)].slice(-HISTORY_LIMIT),
      future: future.slice(1),
      saveState: 'unsaved',
    });
  },
}));

function insertAfter(cells: Cell[], id: string | null, cell: Cell): Cell[] {
  if (id === null) return [...cells, cell];
  const idx = cells.findIndex((c) => c.id === id);
  if (idx === -1) return [...cells, cell];
  return [...cells.slice(0, idx + 1), cell, ...cells.slice(idx + 1)];
}

function createRetyped(original: Cell, replacement: Cell): Cell {
  return { ...replacement, id: original.id };
}
