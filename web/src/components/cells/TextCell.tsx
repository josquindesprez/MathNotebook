import { useRef, useEffect } from 'react';
import type { TextCell as TextCellModel } from '../../notebook/model';
import { useNotebookStore } from '../../notebook/store';
import { renderMarkdown } from './miniMarkdown';

interface Props {
  cell: TextCellModel;
  isEditing: boolean;
  onFocus: () => void;
}

export function TextCell({ cell, isEditing, onFocus }: Props) {
  const setMarkdown = useNotebookStore((s) => s.setTextCellMarkdown);
  const ref = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (isEditing) {
      const el = ref.current;
      el?.focus();
      el?.setSelectionRange(el.value.length, el.value.length);
    }
  }, [isEditing]);

  if (isEditing) {
    return (
      <textarea
        ref={ref}
        className="text-cell-editor"
        value={cell.markdown}
        placeholder="Scrivi testo, **markdown** e matematica inline con $x^2$…"
        onFocus={onFocus}
        onChange={(e) => setMarkdown(cell.id, e.target.value)}
        rows={Math.max(1, cell.markdown.split('\n').length)}
      />
    );
  }

  if (cell.markdown.trim() === '') {
    return (
      <div className="text-cell-preview text-cell-empty" onClick={onFocus} tabIndex={-1}>
        Testo vuoto — clicca per scrivere
      </div>
    );
  }

  return (
    <div
      className="text-cell-preview"
      onClick={onFocus}
      tabIndex={-1}
      dangerouslySetInnerHTML={{ __html: renderMarkdown(cell.markdown) }}
    />
  );
}
