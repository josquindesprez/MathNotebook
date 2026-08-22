import { useState, type ReactNode } from 'react';
import type { Cell } from '../notebook/model';
import { useNotebookStore } from '../notebook/store';

const TYPE_LABEL: Record<Cell['type'], string> = {
  text: 'Text',
  math: 'Math',
  calculation: 'Calculation',
  graph: 'Graph',
};

interface Props {
  cell: Cell;
  isSelected: boolean;
  isEditing: boolean;
  onSelect: () => void;
  children: ReactNode;
}

export function CellShell({ cell, isSelected, isEditing, onSelect, children }: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  const duplicateCellById = useNotebookStore((s) => s.duplicateCellById);
  const deleteCellById = useNotebookStore((s) => s.deleteCellById);
  const moveCell = useNotebookStore((s) => s.moveCell);
  const changeCellType = useNotebookStore((s) => s.changeCellType);

  return (
    <div
      className={`cell-shell ${isSelected ? 'is-selected' : ''} ${isEditing ? 'is-editing' : ''}`}
      onMouseDown={(e) => {
        // evita di rubare il focus al menu quando si clicca sul gutter
        if ((e.target as HTMLElement).closest('.cell-gutter')) return;
        onSelect();
      }}
      data-cell-id={cell.id}
    >
      <div className="cell-gutter">
        <span className="cell-type-badge">{TYPE_LABEL[cell.type]}</span>
        <div className="cell-menu-anchor">
          <button
            type="button"
            className="cell-menu-button"
            aria-label="Menu cella"
            onClick={() => setMenuOpen((v) => !v)}
          >
            ⋯
          </button>
          {menuOpen && (
            <div className="cell-menu" onMouseLeave={() => setMenuOpen(false)}>
              <button onClick={() => (duplicateCellById(cell.id), setMenuOpen(false))}>Duplicate cell</button>
              <button onClick={() => (moveCell(cell.id, 'up'), setMenuOpen(false))}>Move up</button>
              <button onClick={() => (moveCell(cell.id, 'down'), setMenuOpen(false))}>Move down</button>
              <div className="cell-menu-separator" />
              <span className="cell-menu-label">Change to</span>
              {(['text', 'math'] as const).map((t) => (
                <button key={t} disabled={cell.type === t} onClick={() => (changeCellType(cell.id, t), setMenuOpen(false))}>
                  {TYPE_LABEL[t]}
                </button>
              ))}
              <div className="cell-menu-separator" />
              <button className="cell-menu-danger" onClick={() => (deleteCellById(cell.id), setMenuOpen(false))}>
                Delete cell
              </button>
            </div>
          )}
        </div>
      </div>
      <div className="cell-content">{children}</div>
    </div>
  );
}
