import { useCallback, type KeyboardEvent } from 'react';
import { useNotebookStore } from '../notebook/store';
import { CellShell } from './CellShell';
import { TextCell } from './cells/TextCell';
import { MathCell } from './cells/MathCell';

// Comportamento da tastiera comune alle celle. Vedi UI_SPEC.md, sezione
// "Comportamento comune". Le combinazioni con modificatori (Ctrl/Shift)
// vengono intercettate anche quando il focus è dentro MathLive/textarea
// (non sono scorciatoie native di editing testo); Tab/Enter/Esc "nudi"
// vengono intercettati solo quando ha senso farlo, per non rubare la
// navigazione tra placeholder nativa di MathLive.
export function NotebookView() {
  const doc = useNotebookStore((s) => s.doc);
  const selectedCellId = useNotebookStore((s) => s.selectedCellId);
  const editingCellId = useNotebookStore((s) => s.editingCellId);
  const selectCell = useNotebookStore((s) => s.selectCell);
  const setEditing = useNotebookStore((s) => s.setEditing);
  const insertTextCellAfter = useNotebookStore((s) => s.insertTextCellAfter);
  const insertMathCellAfter = useNotebookStore((s) => s.insertMathCellAfter);
  const deleteCellById = useNotebookStore((s) => s.deleteCellById);
  const moveCellSelection = useNotebookStore((s) => s.moveCellSelection);
  const moveCell = useNotebookStore((s) => s.moveCell);
  const duplicateCellById = useNotebookStore((s) => s.duplicateCellById);

  const confirmAndAdvance = useCallback(
    (cellId: string, stay: boolean) => {
      const idx = doc.cells.findIndex((c) => c.id === cellId);
      if (stay) {
        setEditing(cellId);
        return;
      }
      const isLast = idx === doc.cells.length - 1;
      if (isLast) {
        const newId = insertTextCellAfter(cellId);
        selectCell(newId);
        setEditing(newId);
      } else {
        const nextId = doc.cells[idx + 1].id;
        selectCell(nextId);
        setEditing(null);
      }
    },
    [doc.cells, insertTextCellAfter, selectCell, setEditing]
  );

  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLDivElement>) => {
      if (!selectedCellId) return;
      const cell = doc.cells.find((c) => c.id === selectedCellId);
      if (!cell) return;
      const isEditing = editingCellId === selectedCellId;

      // --- combinazioni con modificatori: sempre intercettate ---
      if (e.ctrlKey && e.shiftKey && e.key === 'ArrowUp') {
        e.preventDefault();
        moveCell(selectedCellId, 'up');
        return;
      }
      if (e.ctrlKey && e.shiftKey && e.key === 'ArrowDown') {
        e.preventDefault();
        moveCell(selectedCellId, 'down');
        return;
      }
      if (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === 'd') {
        e.preventDefault();
        duplicateCellById(selectedCellId);
        return;
      }
      if (e.ctrlKey && !e.shiftKey && e.key === 'ArrowUp') {
        e.preventDefault();
        setEditing(null);
        moveCellSelection(selectedCellId, 'up');
        return;
      }
      if (e.ctrlKey && !e.shiftKey && e.key === 'ArrowDown') {
        e.preventDefault();
        setEditing(null);
        moveCellSelection(selectedCellId, 'down');
        return;
      }
      if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault();
        confirmAndAdvance(selectedCellId, true);
        return;
      }
      if (e.ctrlKey && e.key === 'Backspace' && !isEditing) {
        const isEmpty = cell.type === 'text' ? cell.markdown.trim() === '' : cell.type === 'math' ? cell.ast.type === 'PlaceholderNode' : false;
        if (isEmpty) {
          e.preventDefault();
          deleteCellById(selectedCellId);
        }
        return;
      }

      // --- Shift+Enter: conferma e avanza (vale sia editing sia selezionata) ---
      if (e.shiftKey && e.key === 'Enter') {
        e.preventDefault();
        confirmAndAdvance(selectedCellId, false);
        return;
      }

      if (isEditing) {
        // Enter "nudo": per le celle Math conferma e avanza come Shift+Enter;
        // per le Text Cell lascia il comportamento nativo (newline).
        if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && cell.type !== 'text') {
          e.preventDefault();
          confirmAndAdvance(selectedCellId, false);
          return;
        }
        if (e.key === 'Escape') {
          e.preventDefault();
          setEditing(null);
          return;
        }
        // Tab/Shift+Tab: lasciati passare a MathLive/textarea (navigazione
        // nativa tra placeholder, vedi UI_SPEC.md).
        return;
      }

      // --- cella selezionata, non in editing ---
      if (e.key === 'Enter' || e.key === 'Tab') {
        e.preventDefault();
        setEditing(selectedCellId);
        return;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        moveCellSelection(selectedCellId, 'up');
        return;
      }
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        moveCellSelection(selectedCellId, 'down');
        return;
      }
    },
    [selectedCellId, editingCellId, doc.cells, confirmAndAdvance, deleteCellById, duplicateCellById, moveCell, moveCellSelection, setEditing]
  );

  return (
    <div className="notebook-view" onKeyDown={handleKeyDown}>
      {doc.cells.map((cell) => {
        const isSelected = cell.id === selectedCellId;
        const isEditing = cell.id === editingCellId;
        return (
          <CellShell key={cell.id} cell={cell} isSelected={isSelected} isEditing={isEditing} onSelect={() => selectCell(cell.id)}>
            {cell.type === 'text' && (
              <TextCell cell={cell} isEditing={isEditing} onFocus={() => (selectCell(cell.id), setEditing(cell.id))} />
            )}
            {(cell.type === 'math' || cell.type === 'calculation') && (
              <MathCell cell={cell} isEditing={isEditing} onFocus={() => (selectCell(cell.id), setEditing(cell.id))} />
            )}
          </CellShell>
        );
      })}
      <div className="notebook-add-row">
        <button type="button" onClick={() => insertTextCellAfter(doc.cells[doc.cells.length - 1]?.id ?? null)}>
          + Text
        </button>
        <button type="button" onClick={() => insertMathCellAfter(doc.cells[doc.cells.length - 1]?.id ?? null)}>
          + Math
        </button>
      </div>
    </div>
  );
}
