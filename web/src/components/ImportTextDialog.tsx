import { useState } from 'react';

interface Props {
  onImport: (text: string) => void;
  onCancel: () => void;
}

// "Import text…" (vedi UI_SPEC.md): incolla un foglio di testo/LaTeX (es.
// generato da ChatGPT) e lo spezza in celle — vedi notebook/importText.ts
// per l'euristica di classificazione Text/Math.
export function ImportTextDialog({ onImport, onCancel }: Props) {
  const [text, setText] = useState('');

  function handleImport() {
    if (text.trim()) onImport(text);
  }

  return (
    <div className="dialog-overlay" onMouseDown={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="dialog-box dialog-box-wide">
        <h3>Import text</h3>
        <p className="dialog-hint">
          Incolla qui un foglio di esercizi o di formule (testo con matematica tra $...$, o righe di solo LaTeX).
          Verrà spezzato in celle e aggiunto in fondo al notebook — potrai correggere il tipo di ogni cella dal suo
          menu "..." se qualcosa non viene classificato come previsto.
        </p>
        <textarea
          className="dialog-textarea"
          autoFocus
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={'Dati i vettori $\\mathbf a = ...$ e $\\mathbf b = ...$, calcola la loro somma.\n\\mathbf a+\\mathbf b'}
        />
        <div className="dialog-actions">
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className="dialog-primary" onClick={handleImport} disabled={!text.trim()}>
            Import
          </button>
        </div>
      </div>
    </div>
  );
}
