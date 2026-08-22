import { useState, type ReactNode } from 'react';
import type { MathfieldElement } from 'mathlive';
import type { Cell } from '../notebook/model';
import { useNotebookStore } from '../notebook/store';
import { renderCellPreviewHtml } from './cellPreview';
import { getMathField } from '../notebook/mathFieldRegistry';

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
  /** Indice colore di gruppo (0..GROUP_COLOR_COUNT-1) già calcolato da
   * NotebookView su tutto l'elenco celle, vedi model.ts/computeGroupColorIndices. */
  groupColorIndex: number;
  /** false solo per la prima cella del notebook: non ha senso "iniziare un
   * gruppo" lì, è già l'inizio del primo. */
  canToggleGroup: boolean;
  /** true quando questa cella è sia in editing sia su schermo mobile: si
   * apre a schermo intero invece che inline (vedi index.css,
   * .cell-shell--zoomed) — sulla tastiera del telefono 1.15rem lascia
   * troppo poco spazio, meglio dedicare tutto lo schermo alla cella attiva. */
  isMobileZoomed: boolean;
  /** Cella precedente nel notebook, per il "peek" di riferimento rapido
   * mostrato in cima alla vista a schermo intero (null se questa è la
   * prima cella, o se non si è in modalità zoom). */
  previousCell: Cell | null;
}

export function CellShell({
  cell,
  isSelected,
  isEditing,
  onSelect,
  children,
  groupColorIndex,
  canToggleGroup,
  isMobileZoomed,
  previousCell,
}: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  const duplicateCellById = useNotebookStore((s) => s.duplicateCellById);
  const deleteCellById = useNotebookStore((s) => s.deleteCellById);
  const moveCell = useNotebookStore((s) => s.moveCell);
  const changeCellType = useNotebookStore((s) => s.changeCellType);
  const toggleGroupStart = useNotebookStore((s) => s.toggleGroupStart);
  const setEditing = useNotebookStore((s) => s.setEditing);

  return (
    <div
      className={`cell-shell ${isSelected ? 'is-selected' : ''} ${isEditing ? 'is-editing' : ''} ${isMobileZoomed ? 'cell-shell--zoomed' : ''}`}
      onMouseDown={(e) => {
        // evita di rubare il focus al menu quando si clicca sul gutter
        if ((e.target as HTMLElement).closest('.cell-gutter')) return;
        onSelect();
      }}
      data-cell-id={cell.id}
      data-group-color={groupColorIndex}
    >
      {isMobileZoomed ? (
        <div className="cell-zoom-header">
          {previousCell && (
            <div className="cell-zoom-peek" dangerouslySetInnerHTML={{ __html: renderCellPreviewHtml(previousCell) }} />
          )}
          {/* I pulsanti nativi di MathLive (tastiera virtuale, menu) sono
             nascosti dentro il campo su mobile (vedi index.css, ::part) e
             ricreati qui fuori, dove c'è spazio ed è chiaro cosa fanno. */}
          {(cell.type === 'math' || cell.type === 'calculation') && (
            <>
              <button
                type="button"
                className="cell-zoom-tool-button"
                aria-label="Tastiera virtuale"
                // "toggleVirtualKeyboard" è un comando reale di MathLive
                // (vedi virtual-keyboard.d.ts, VirtualKeyboardCommands) ma i
                // .d.ts pubblicati non lo includono nel tipo Selector di
                // executeCommand: cast necessario, non un errore nostro.
                onClick={() => getMathField(cell.id)?.executeCommand('toggleVirtualKeyboard' as unknown as Parameters<MathfieldElement['executeCommand']>[0])}
              >
                ⌨
              </button>
              <button
                type="button"
                className="cell-zoom-tool-button"
                aria-label="Menu formula"
                onClick={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect();
                  getMathField(cell.id)?.showMenu({
                    location: { x: rect.left, y: rect.bottom },
                    modifiers: { alt: false, control: false, shift: false, meta: false },
                  });
                }}
              >
                ⋯
              </button>
            </>
          )}
          <button type="button" className="cell-zoom-done-button" onClick={() => setEditing(null)}>
            Fatto
          </button>
        </div>
      ) : (
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
                {canToggleGroup && (
                  <button onClick={() => (toggleGroupStart(cell.id), setMenuOpen(false))}>
                    {cell.groupStart ? 'Merge with previous group' : 'Start new group here'}
                  </button>
                )}
                <button className="cell-menu-danger" onClick={() => (deleteCellById(cell.id), setMenuOpen(false))}>
                  Delete cell
                </button>
              </div>
            )}
          </div>
        </div>
      )}
      <div className="cell-content">{children}</div>
    </div>
  );
}
