import { useNotebookStore } from '../notebook/store';

const SAVE_LABEL: Record<string, string> = {
  saved: 'Saved',
  unsaved: 'Unsaved changes',
  saving: 'Saving…',
};

export function StatusBar() {
  const saveState = useNotebookStore((s) => s.saveState);
  const editingCellId = useNotebookStore((s) => s.editingCellId);
  const doc = useNotebookStore((s) => s.doc);

  const editingCell = editingCellId ? doc.cells.find((c) => c.id === editingCellId) : null;
  const isMath = editingCell?.type === 'math' || editingCell?.type === 'calculation';

  return (
    <div className="status-bar">
      <span>{isMath ? 'Math mode' : ' '}</span>
      <span className="status-bar-spacer" />
      {isMath && <span>LaTeX available</span>}
      <span>{SAVE_LABEL[saveState]}</span>
    </div>
  );
}
