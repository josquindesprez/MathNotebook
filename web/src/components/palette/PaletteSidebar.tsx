import { useMemo, useState } from 'react';
import { PALETTE_ITEMS, itemsByCategory, matrixTemplateLatex, nonEmptyCategories, type PaletteItem } from '../../palette/categories';
import { searchPaletteItems } from '../../palette/search';
import { useNotebookStore } from '../../notebook/store';
import { getMathField } from '../../notebook/mathFieldRegistry';
import { latexToAst } from '../../parser/latex-bridge';
import { usePaletteUiStore } from '../../palette/uiStore';
import { CustomMatrixDialog } from './CustomMatrixDialog';

export function PaletteSidebar() {
  const [query, setQuery] = useState('');
  const [customMatrixOpen, setCustomMatrixOpen] = useState(false);
  const collapsed = usePaletteUiStore((s) => s.collapsed);
  const toggleCollapsed = usePaletteUiStore((s) => s.toggleCollapsed);
  const expanded = usePaletteUiStore((s) => s.expandedCategory);
  const expandCategory = usePaletteUiStore((s) => s.expandCategory);

  const doc = useNotebookStore((s) => s.doc);
  const editingCellId = useNotebookStore((s) => s.editingCellId);
  const selectedCellId = useNotebookStore((s) => s.selectedCellId);
  const insertMathCellAfter = useNotebookStore((s) => s.insertMathCellAfter);
  const selectCell = useNotebookStore((s) => s.selectCell);
  const setEditing = useNotebookStore((s) => s.setEditing);

  const searchResults = useMemo(() => (query.trim() ? searchPaletteItems(PALETTE_ITEMS, query) : null), [query]);
  const categories = nonEmptyCategories();

  function insertLatexTemplate(latex: string) {
    const activeId = editingCellId ?? selectedCellId;
    const activeCell = activeId ? doc.cells.find((c) => c.id === activeId) : null;
    const field = activeId ? getMathField(activeId) : undefined;

    if (activeCell && (activeCell.type === 'math' || activeCell.type === 'calculation') && field) {
      field.focus();
      field.insert(latex, { selectionMode: 'placeholder' });
      return;
    }

    let ast;
    try {
      ast = latexToAst(latex);
    } catch {
      ast = undefined;
    }
    const newId = insertMathCellAfter(activeId ?? null, ast);
    selectCell(newId);
    setEditing(newId);
  }

  function insertItem(item: PaletteItem) {
    if (item.opensDialog) {
      setCustomMatrixOpen(true);
      return;
    }
    insertLatexTemplate(item.insertLatex);
  }

  if (collapsed) {
    return (
      <div className="palette-sidebar palette-collapsed">
        <button type="button" className="palette-expand-button" onClick={toggleCollapsed} aria-label="Mostra Math Palette">
          ⟨
        </button>
      </div>
    );
  }

  return (
    <div className="palette-sidebar">
      <div className="palette-header">
        <input
          type="text"
          className="palette-search"
          placeholder="Cerca simboli, template…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="button" className="palette-collapse-button" onClick={toggleCollapsed} aria-label="Nascondi Math Palette">
          ⟩
        </button>
      </div>

      <div className="palette-body">
        {searchResults ? (
          <PaletteGrid items={searchResults} onInsert={insertItem} />
        ) : (
          categories.map((category) => (
            <div key={category} className="palette-category">
              <button type="button" className="palette-category-header" onClick={() => expandCategory(expanded === category ? '' : category)}>
                <span>{category}</span>
                <span className="palette-category-chevron">{expanded === category ? '▾' : '▸'}</span>
              </button>
              {expanded === category && <PaletteGrid items={itemsByCategory(category)} onInsert={insertItem} />}
            </div>
          ))
        )}
      </div>

      {customMatrixOpen && (
        <CustomMatrixDialog
          onCancel={() => setCustomMatrixOpen(false)}
          onInsert={(rows, cols) => {
            insertLatexTemplate(matrixTemplateLatex(rows, cols));
            setCustomMatrixOpen(false);
          }}
        />
      )}
    </div>
  );
}

function PaletteGrid({ items, onInsert }: { items: PaletteItem[]; onInsert: (item: PaletteItem) => void }) {
  return (
    <div className="palette-grid">
      {items.map((item) => (
        <button key={item.id} type="button" className="palette-item" onClick={() => onInsert(item)} title={item.description}>
          <span className="palette-item-glyph">{item.label}</span>
          <span className="palette-item-tooltip">
            <strong>{item.description}</strong>
            {item.keyboardHint && (
              <>
                <br />
                <span className="palette-item-hint">Keyboard: {item.keyboardHint}</span>
              </>
            )}
          </span>
        </button>
      ))}
    </div>
  );
}
