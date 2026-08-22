import { useCallback, useEffect, useRef, useState } from 'react';
import type { MathfieldElement } from 'mathlive';
import type { MathCell as MathCellModel, CalculationCell } from '../../notebook/model';
import { toLatex } from '../../render/toLatex';
import { latexToAst } from '../../parser/latex-bridge';
import { useNotebookStore } from '../../notebook/store';
import { registerMathField, unregisterMathField } from '../../notebook/mathFieldRegistry';
import { NATURAL_LANGUAGE_INLINE_SHORTCUTS } from '../../parser/naturalLanguage';
import { ITALIAN_NUMBER_INLINE_SHORTCUTS } from '../../parser/italianNumbers';

interface Props {
  cell: MathCellModel | CalculationCell;
  isEditing: boolean;
  /** true quando questa cella è aperta a schermo intero su mobile (vedi
   * CellShell.tsx/.cell-shell--zoomed): qui il font resta grande e fisso
   * (leggibile) e un contenuto più largo dello schermo si scorre con i
   * pulsanti ‹ › invece di essere rimpicciolito. */
  isZoomed: boolean;
  onFocus: () => void;
}

const MIN_FIT_SCALE = 0.55;

export function MathCell({ cell, isEditing, isZoomed, onFocus }: Props) {
  const ref = useRef<MathfieldElement>(null);
  const setAst = useNotebookStore((s) => s.setMathCellAst);
  const lastKnownLatex = useRef<string>('');
  const [panOffset, setPanOffset] = useState(0);
  const [overflowRange, setOverflowRange] = useState({ natural: 0, available: 0 });

  // Misura il contenuto e decide come farlo stare nella larghezza
  // disponibile. Due modalità, perché lo scroll orizzontale nativo non
  // funziona: l'overflow di MathLive avviene dentro il suo Shadow DOM, non
  // nel nostro contenitore (vedi conversazione: font fisso -> clipping
  // invisibile su mobile).
  // - Non zoomata (elenco): rimpicciolisce il font quanto basta per farci
  //   stare tutto (fino a MIN_FIT_SCALE), per una vista compatta.
  // - Zoomata: il font resta quello grande dichiarato in CSS; se il
  //   contenuto non ci sta comunque, resta a dimensione piena e si scorre
  //   con i pulsanti ‹ › (vedi renderPanButtons sotto).
  const measure = useCallback(() => {
    const el = ref.current;
    const container = el?.parentElement;
    if (!el || !container) return;

    el.style.fontSize = '';
    el.style.transform = '';
    const available = container.clientWidth;
    if (!available) return;

    // MathLive dimensiona l'elemento al contenuto quando non è vincolato
    // da "width: 100%" (regola CSS, .math-field): rimuoverlo temporaneamente
    // per misurare la larghezza naturale non produce flicker visibile,
    // perché lettura e ripristino avvengono nello stesso tick sincrono,
    // prima del prossimo repaint.
    el.style.width = 'auto';
    el.style.display = 'inline-block';
    const natural = el.scrollWidth;

    if (isZoomed) {
      // Resta "auto": .cell-shell--zoomed .cell-content ha overflow:hidden,
      // il contenuto in eccesso si nasconde e si raggiunge col pan.
      setOverflowRange({ natural, available });
      setPanOffset((prev) => Math.min(prev, Math.max(0, natural - available)));
      return;
    }

    el.style.width = '';
    el.style.display = '';
    if (natural > available) {
      const baseSize = parseFloat(getComputedStyle(el).fontSize);
      const scale = Math.max(available / natural, MIN_FIT_SCALE);
      el.style.fontSize = `${baseSize * scale}px`;
    }
  }, [isZoomed]);

  // Uscendo dallo zoom lo scroll orizzontale non ha più senso: si torna
  // sempre all'inizio della formula.
  useEffect(() => {
    if (!isZoomed) setPanOffset(0);
  }, [isZoomed]);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.transform = isZoomed && panOffset > 0 ? `translateX(-${panOffset}px)` : '';
  }, [panOffset, isZoomed]);

  // Riflette l'AST (fonte di verità) nel campo MathLive quando cambia
  // dall'esterno (caricamento documento, inserimento da palette, undo/redo).
  useEffect(() => {
    const mf = ref.current;
    if (!mf) return;
    const latex = toLatex(cell.ast);
    if (latex !== lastKnownLatex.current) {
      mf.value = latex;
      lastKnownLatex.current = latex;
    }
    measure();
  }, [cell.ast, measure]);

  // Rilegge il LaTeX prodotto da MathLive dopo un editing diretto
  // dell'utente e lo riconverte nel nostro AST (vedi ARCHITECTURE.md).
  useEffect(() => {
    const mf = ref.current;
    if (!mf) return;
    const handler = () => {
      const latex = mf.value;
      if (latex === lastKnownLatex.current) return;
      lastKnownLatex.current = latex;
      try {
        const ast = latexToAst(latex);
        setAst(cell.id, ast);
      } catch {
        // LaTeX temporaneamente incompleto durante la digitazione: ignora,
        // il prossimo "input" valido aggiornerà l'AST.
      }
      measure();
    };
    mf.addEventListener('input', handler);
    return () => mf.removeEventListener('input', handler);
  }, [cell.id, setAst, measure]);

  useEffect(() => {
    // Cambiare isEditing/isZoomed può cambiare la larghezza disponibile
    // (apertura dello zoom a schermo intero su mobile, vedi CellShell.tsx):
    // ricalcola.
    measure();
    if (isEditing) ref.current?.focus();
  }, [isEditing, measure]);

  useEffect(() => {
    const mf = ref.current;
    const container = mf?.parentElement;
    if (!container) return;
    const ro = new ResizeObserver(measure);
    ro.observe(container);
    return () => ro.disconnect();
  }, [measure]);

  useEffect(() => {
    const mf = ref.current;
    if (!mf) return;
    registerMathField(cell.id, mf);
    // Alias in linguaggio naturale ("alla", "per", "fratto"/"diviso",
    // "piu"/"meno", "radice") digitabili direttamente nel campo, in aggiunta
    // alle scorciatoie native di MathLive (sqrt, alpha, >=, ...): vedi
    // SYNTAX.md, "Linguaggio naturale". Si aggiungono a quelle di default,
    // non le sostituiscono.
    mf.inlineShortcuts = { ...mf.inlineShortcuts, ...NATURAL_LANGUAGE_INLINE_SHORTCUTS, ...ITALIAN_NUMBER_INLINE_SHORTCUTS };
    // Di default MathLive avvolge il LaTeX copiato (Ctrl+C) in "$$ ... $$"
    // (vedi CLIPBOARD_LATEX_BEGIN/END nel suo sorgente): incollato tra due
    // "$" già scritti in una Text Cell produce "$$$ ... $$$", che KaTeX non
    // renderizza. Restituendo il LaTeX "nudo" il copia-incolla tra Math
    // Cell e math inline di una Text Cell funziona senza sorprese, e
    // Ctrl+C nativo si comporta come "Copy as LaTeX" (Ctrl+Shift+C).
    mf.onExport = (_field, latex) => latex;
    return () => unregisterMathField(cell.id);
  }, [cell.id]);

  const maxPan = Math.max(0, overflowRange.natural - overflowRange.available);
  const showPanControls = isZoomed && maxPan > 0;

  function panBy(direction: 1 | -1) {
    const step = overflowRange.available * 0.8 || 120;
    setPanOffset((prev) => Math.min(Math.max(prev + direction * step, 0), maxPan));
  }

  return (
    <>
      {/* Fratello del viewport, non genitore/figlio: sta fisso subito sotto
         l'header, in cima all'area zoomata (vedi index.css,
         .cell-shell--zoomed .cell-content), a prescindere da dove
         l'equazione finisce per centrarsi verticalmente nello spazio
         restante — affiancarlo al campo ne riduceva la larghezza
         disponibile, "risquezandolo" di nuovo (vedi conversazione). */}
      {showPanControls && (
        <div className="math-field-pan-controls">
          <button type="button" className="cell-pan-button" aria-label="Scorri a sinistra" disabled={panOffset <= 0} onClick={() => panBy(-1)}>
            ‹
          </button>
          <button type="button" className="cell-pan-button" aria-label="Scorri a destra" disabled={panOffset >= maxPan} onClick={() => panBy(1)}>
            ›
          </button>
        </div>
      )}
      <div className="math-field-viewport">
        {/* Nessun math-virtual-keyboard-policy esplicito: il default "auto" di
           MathLive mostra la tastiera virtuale solo su device touch (mobile),
           mai su desktop con tastiera fisica — esattamente il comportamento
           voluto in entrambi i casi (vedi mathlive.d.ts). */}
        <math-field ref={ref} className="math-field" onFocus={onFocus} data-cell-id={cell.id} />
      </div>
    </>
  );
}
