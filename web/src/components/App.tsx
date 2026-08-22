import { useCallback, useEffect, useState } from 'react';
import { useNotebookStore } from '../notebook/store';
import { createEmptyNotebook } from '../notebook/model';
import { deserializeNotebook, serializeNotebook } from '../notebook/document-io';
import { parseWorksheetText } from '../notebook/importText';
import { openFile, saveFile, saveFileAs, setWindowTitle, copyToClipboard } from '../bridge/hostBridge';
import { getMathField } from '../notebook/mathFieldRegistry';
import { toLatex } from '../render/toLatex';
import { usePaletteUiStore, ALT_SHORTCUT_CATEGORIES } from '../palette/uiStore';
import { NotebookView } from './NotebookView';
import { PaletteSidebar } from './palette/PaletteSidebar';
import { StatusBar } from './StatusBar';
import { ImportTextDialog } from './ImportTextDialog';

export function App() {
  const doc = useNotebookStore((s) => s.doc);
  const filePath = useNotebookStore((s) => s.filePath);
  const saveState = useNotebookStore((s) => s.saveState);
  const editingCellId = useNotebookStore((s) => s.editingCellId);
  const loadDocument = useNotebookStore((s) => s.loadDocument);
  const markSaved = useNotebookStore((s) => s.markSaved);
  const undo = useNotebookStore((s) => s.undo);
  const redo = useNotebookStore((s) => s.redo);
  const togglePaletteCollapsed = usePaletteUiStore((s) => s.toggleCollapsed);
  const expandPaletteCategory = usePaletteUiStore((s) => s.expandCategory);

  const importCells = useNotebookStore((s) => s.importCells);

  const [focusMode, setFocusMode] = useState(false);
  const [importOpen, setImportOpen] = useState(false);

  const handleImportText = useCallback(
    (text: string) => {
      const cells = parseWorksheetText(text);
      importCells(cells);
      setImportOpen(false);
    },
    [importCells]
  );

  useEffect(() => {
    setWindowTitle(`${doc.title}${saveState === 'unsaved' ? ' •' : ''} — Math Notebook`);
  }, [doc.title, saveState]);

  const handleSave = useCallback(
    async (forcePickPath: boolean) => {
      const suggestedName = `${doc.title || 'notebook'}.mathnb`;
      const contents = serializeNotebook(doc);
      const result = forcePickPath || !filePath ? await saveFileAs(suggestedName, contents) : await saveFile(filePath, suggestedName, contents);
      if (!result.canceled) {
        markSaved();
        if (result.path) useNotebookStore.getState().setFilePath(result.path);
      }
    },
    [doc, filePath, markSaved]
  );

  const handleOpen = useCallback(async () => {
    const result = await openFile();
    if (result.canceled || !result.contents) return;
    try {
      const loaded = deserializeNotebook(result.contents);
      loadDocument(loaded, result.path ?? null);
    } catch (err) {
      // Prima d'ora un file non valido (es. un .txt scambiato per .mathnb)
      // falliva qui senza alcun avviso: sembrava che "non succedesse
      // niente". Vedi conversazione: due fogli di testo di GPT non si
      // aprivano per questo.
      window.alert(
        `Impossibile aprire il file: non è un notebook Math Notebook valido (.mathnb).\n\n${err instanceof Error ? err.message : String(err)}`
      );
    }
  }, [loadDocument]);

  const handleNew = useCallback(() => {
    loadDocument(createEmptyNotebook(), null);
  }, [loadDocument]);

  const handleCopyLatex = useCallback(() => {
    if (!editingCellId) return;
    const cell = doc.cells.find((c) => c.id === editingCellId);
    if (!cell || (cell.type !== 'math' && cell.type !== 'calculation')) return;
    const field = getMathField(editingCellId);
    const latex = field ? field.value : toLatex(cell.ast);
    void copyToClipboard(latex);
  }, [doc.cells, editingCellId]);

  // Autosave: se il notebook ha già un percorso su disco, salva in modo
  // silenzioso ~2s dopo l'ultima modifica (vedi DOCUMENT_FORMAT.md).
  useEffect(() => {
    if (saveState !== 'unsaved' || !filePath) return;
    const timer = window.setTimeout(() => {
      void handleSave(false);
    }, 2000);
    return () => window.clearTimeout(timer);
  }, [doc, saveState, filePath, handleSave]);

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      const ctrl = e.ctrlKey || e.metaKey;
      if (ctrl && e.key.toLowerCase() === 's') {
        e.preventDefault();
        void handleSave(e.shiftKey);
      } else if (ctrl && e.key.toLowerCase() === 'o') {
        e.preventDefault();
        void handleOpen();
      } else if (ctrl && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        handleNew();
      } else if (ctrl && !e.shiftKey && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        undo();
      } else if (ctrl && (e.key.toLowerCase() === 'y' || (e.shiftKey && e.key.toLowerCase() === 'z'))) {
        e.preventDefault();
        redo();
      } else if (e.altKey && e.key.toLowerCase() === 'p') {
        e.preventDefault();
        togglePaletteCollapsed();
      } else if (e.altKey && /^[1-9]$/.test(e.key)) {
        const category = ALT_SHORTCUT_CATEGORIES[Number(e.key) - 1];
        if (category) {
          e.preventDefault();
          expandPaletteCategory(category);
        }
      } else if (ctrl && e.key === '.') {
        e.preventDefault();
        setFocusMode((v) => !v);
      } else if (ctrl && e.shiftKey && e.key.toLowerCase() === 'c') {
        e.preventDefault();
        handleCopyLatex();
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [handleSave, handleOpen, handleNew, undo, redo, handleCopyLatex, togglePaletteCollapsed, expandPaletteCategory]);

  return (
    <div className={`app-shell ${focusMode ? 'focus-mode' : ''}`}>
      {!focusMode && (
        <div className="app-toolbar">
          <span className="app-title">{doc.title}</span>
          <div className="app-toolbar-actions">
            <button onClick={handleNew}>New</button>
            <button onClick={handleOpen}>Open</button>
            <button onClick={() => void handleSave(false)}>Save</button>
            <button onClick={() => void handleSave(true)}>Save As</button>
            <button onClick={() => setImportOpen(true)}>Import text…</button>
            <button onClick={undo}>Undo</button>
            <button onClick={redo}>Redo</button>
          </div>
        </div>
      )}
      <div className="app-main">
        <div className="notebook-scroll-area">
          <NotebookView />
        </div>
        {!focusMode && <PaletteSidebar />}
      </div>
      {!focusMode && <StatusBar />}
      {importOpen && <ImportTextDialog onImport={handleImportText} onCancel={() => setImportOpen(false)} />}
    </div>
  );
}
