import { useEffect, useState } from 'react';
import { listNotebookFiles, readNotebookFile, pickLocalFile, type NotebookFileInfo } from '../bridge/hostBridge';

interface Props {
  onOpen: (name: string, contents: string) => void;
  onCancel: () => void;
}

// Su Capacitor (Android) non esiste un dialogo di sistema per scegliere un
// file dell'app: i notebook salvati vivono nella cartella Documenti
// dell'app, ed "Open" li elenca qui sotto (vedi ARCHITECTURE.md,
// "Mobile"). Per aprire un file "esterno" qualunque (es. un .txt scaricato
// da GPT e salvato altrove sul telefono, non ancora dentro Documenti) c'è
// invece "Sfoglia file...", che usa il selettore nativo di sistema tramite
// un <input type="file"> — funziona anche dentro la WebView di Capacitor.
export function OpenFileListDialog({ onOpen, onCancel }: Props) {
  const [files, setFiles] = useState<NotebookFileInfo[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [openingName, setOpeningName] = useState<string | null>(null);

  useEffect(() => {
    listNotebookFiles()
      .then(setFiles)
      .catch((err) => setError(err instanceof Error ? err.message : String(err)));
  }, []);

  async function handlePick(name: string) {
    setOpeningName(name);
    try {
      const contents = await readNotebookFile(name);
      onOpen(name, contents);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setOpeningName(null);
    }
  }

  async function handleBrowse() {
    setError(null);
    try {
      const result = await pickLocalFile('.mathnb,.txt,application/json,text/plain');
      if (result.canceled || result.contents === undefined) return;
      onOpen(result.path ?? 'file', result.contents);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  return (
    <div className="dialog-overlay" onMouseDown={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="dialog-box dialog-box-wide">
        <h3>Open</h3>
        {error && <p className="dialog-hint">Errore: {error}</p>}
        {!error && files === null && <p className="dialog-hint">Caricamento…</p>}
        {!error && files !== null && files.length === 0 && (
          <p className="dialog-hint">Nessun notebook salvato ancora su questo dispositivo.</p>
        )}
        {!error && files !== null && files.length > 0 && (
          <ul className="file-list">
            {files.map((f) => (
              <li key={f.name}>
                <button type="button" className="file-list-item" onClick={() => handlePick(f.name)} disabled={openingName !== null}>
                  {f.name}
                  {openingName === f.name && ' — apro…'}
                </button>
              </li>
            ))}
          </ul>
        )}
        <div className="dialog-actions">
          <button type="button" onClick={handleBrowse}>
            Sfoglia file…
          </button>
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
