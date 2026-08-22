import { useState, type KeyboardEvent } from 'react';

const MIN_SIZE = 1;
const MAX_SIZE = 8;

interface Props {
  onInsert: (rows: number, cols: number) => void;
  onCancel: () => void;
}

// Mini popup "Custom matrix" (vedi UI_SPEC.md): chiede Rows/Columns e
// genera un template rows×cols con uno \placeholder{} per cella.
export function CustomMatrixDialog({ onInsert, onCancel }: Props) {
  const [rows, setRows] = useState(2);
  const [cols, setCols] = useState(2);

  function clamp(value: number): number {
    return Math.min(MAX_SIZE, Math.max(MIN_SIZE, Math.round(value) || MIN_SIZE));
  }

  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      e.preventDefault();
      onInsert(rows, cols);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onCancel();
    }
  }

  return (
    <div className="dialog-overlay" onMouseDown={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="dialog-box" onKeyDown={handleKeyDown}>
        <h3>Custom matrix</h3>
        <div className="dialog-row">
          <label htmlFor="matrix-rows">Rows</label>
          <input
            id="matrix-rows"
            type="number"
            min={MIN_SIZE}
            max={MAX_SIZE}
            value={rows}
            autoFocus
            onChange={(e) => setRows(clamp(Number(e.target.value)))}
          />
        </div>
        <div className="dialog-row">
          <label htmlFor="matrix-cols">Columns</label>
          <input
            id="matrix-cols"
            type="number"
            min={MIN_SIZE}
            max={MAX_SIZE}
            value={cols}
            onChange={(e) => setCols(clamp(Number(e.target.value)))}
          />
        </div>
        <div className="dialog-actions">
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className="dialog-primary" onClick={() => onInsert(rows, cols)}>
            Insert
          </button>
        </div>
      </div>
    </div>
  );
}
