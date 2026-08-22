import { useCallback, useEffect, useState } from 'react';
import { useNotebookStore } from '../notebook/store';
import { createEmptyNotebook } from '../notebook/model';
import { deserializeNotebook, serializeNotebook } from '../notebook/document-io';
import { parseWorksheetText } from '../notebook/importText';

/** Nome del file senza percorso ed estensione, usato come titolo quando si
 * apre un foglio di testo/LaTeX (non un .mathnb, che ha già un suo titolo). */
function titleFromPath(path: string | null): string {
  if (!path) return 'Nuovo notebook';
  const base = path.replace(/\\/g, '/').split('/').pop() ?? path;
  return base.replace(/\.[^./]+$/, '') || base;
}
import { openFile, saveFile, saveFileAs, setWindowTitle, copyToClipboard, isCapacitorNative } from '../bridge/hostBridge';
import { getMathField } from '../notebook/mathFieldRegistry';
import { toLatex } from '../render/toLatex';
import { usePaletteUiStore, ALT_SHORTCUT_CATEGORIES } from '../palette/uiStore';
import { NotebookView } from './NotebookView';
import { PaletteSidebar } from './palette/PaletteSidebar';
import { StatusBar } from './StatusBar';
import { ImportTextDialog } from './ImportTextDialog';
import { OpenFileListDialog } from './OpenFileListDialog';

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
  const [openListOpen, setOpenListOpen] = useState(false);

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

  const openFromContents = useCallback(
    (path: string | null, contents: string) => {
      try {
        const loaded = deserializeNotebook(contents);
        loadDocument(loaded, path);
        return;
      } catch (mathnbErr) {
        // Non è un .mathnb valido: prova a interpretarlo come foglio di
        // testo/LaTeX (lo stesso formato di "Import text...", vedi
        // importText.ts) così un .txt già pronto (es. da GPT) si apre
        // direttamente con "Open", senza dover incollare a mano nella
        // textarea di Import. Sostituisce il notebook corrente (come ogni
        // Open), non lo aggiunge in coda come fa Import.
        const cells = parseWorksheetText(contents);
        if (cells.length === 0) {
          window.alert(
            `Impossibile aprire il file: non è un notebook Math Notebook valido (.mathnb) né un testo riconoscibile.\n\n${mathnbErr instanceof Error ? mathnbErr.message : String(mathnbErr)}`
          );
          return;
        }
        loadDocument({ ...createEmptyNotebook(titleFromPath(path)), cells }, null);
      }
    },
    [loadDocument]
  );

  const handleOpen = useCallback(async () => {
    // Su Capacitor non c'è un dialogo di sistema: mostriamo l'elenco dei
    // notebook salvati nella cartella Documenti dell'app (vedi
    // OpenFileListDialog.tsx e ARCHITECTURE.md "Mobile").
    if (isCapacitorNative()) {
      setOpenListOpen(true);
      return;
    }
    const result = await openFile();
    if (result.canceled || !result.contents) return;
    openFromContents(result.path ?? null, result.contents);
  }, [openFromContents]);

  const handleOpenFromList = useCallback(
    (name: string, contents: string) => {
      setOpenListOpen(false);
      openFromContents(name, contents);
    },
    [openFromContents]
  );

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
      {/* Visibile solo sotto i 768px (vedi index.css): su desktop la
          palette è sempre affiancata, non serve un pulsante per aprirla. */}
      {!focusMode && (
        <button type="button" className="palette-fab" onClick={togglePaletteCollapsed} aria-label="Mostra/nascondi Math Palette">
          Σ
        </button>
      )}
      {importOpen && <ImportTextDialog onImport={handleImportText} onCancel={() => setImportOpen(false)} />}
      {openListOpen && <OpenFileListDialog onOpen={handleOpenFromList} onCancel={() => setOpenListOpen(false)} />}
    </div>
  );
}
