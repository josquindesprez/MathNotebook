import { useEffect, useRef } from 'react';
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
  onFocus: () => void;
}

export function MathCell({ cell, isEditing, onFocus }: Props) {
  const ref = useRef<MathfieldElement>(null);
  const setAst = useNotebookStore((s) => s.setMathCellAst);
  const lastKnownLatex = useRef<string>('');

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
  }, [cell.ast]);

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
    };
    mf.addEventListener('input', handler);
    return () => mf.removeEventListener('input', handler);
  }, [cell.id, setAst]);

  useEffect(() => {
    if (isEditing) ref.current?.focus();
  }, [isEditing]);

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
    return () => unregisterMathField(cell.id);
  }, [cell.id]);

  return (
    <math-field
      ref={ref}
      className="math-field"
      virtual-keyboard-mode="off"
      onFocus={onFocus}
      data-cell-id={cell.id}
    />
  );
}
