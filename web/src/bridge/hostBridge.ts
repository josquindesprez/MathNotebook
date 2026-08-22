// Bridge verso la shell nativa. Due shell native supportate:
// - .NET/WPF + WebView2 (desktop Windows, vedi ARCHITECTURE.md "Bridge nativo");
// - Capacitor (Android — vedi ARCHITECTURE.md "Mobile"): niente dialoghi di
//   sistema, i file .mathnb vivono nella cartella Documenti dell'app
//   tramite @capacitor/filesystem; l'elenco per "Open" lo mostra
//   OpenFileListDialog.tsx, che usa listNotebookFiles/readNotebookFile qui sotto.
// In sviluppo browser puro (`npm run dev` senza alcuna shell) usa un
// fallback locale così l'app resta utilizzabile.

export interface OpenFileResult {
  canceled: boolean;
  path?: string;
  contents?: string;
}

export interface SaveFileResult {
  canceled: boolean;
  path?: string;
}

interface HostWebView {
  postMessage(message: string): void;
  addEventListener(type: 'message', listener: (ev: MessageEvent) => void): void;
}

declare global {
  interface Window {
    chrome?: { webview?: HostWebView };
  }
}

function getHostWebView(): HostWebView | null {
  return window.chrome?.webview ?? null;
}

let nextRequestId = 1;
const pending = new Map<number, { resolve: (v: any) => void; reject: (e: any) => void }>();

function ensureListenerInstalled(webview: HostWebView) {
  if ((webview as any).__mathNotebookListenerInstalled) return;
  (webview as any).__mathNotebookListenerInstalled = true;
  webview.addEventListener('message', (ev) => {
    const data = typeof ev.data === 'string' ? JSON.parse(ev.data) : ev.data;
    const entry = pending.get(data.id);
    if (!entry) return;
    pending.delete(data.id);
    if ('error' in data) entry.reject(new Error(data.error));
    else entry.resolve(data.result);
  });
}

function callHost<T>(method: string, params?: unknown): Promise<T> {
  const webview = getHostWebView();
  if (!webview) return Promise.reject(new Error('Nessuna shell nativa disponibile (host bridge non trovato).'));
  ensureListenerInstalled(webview);
  const id = nextRequestId++;
  return new Promise<T>((resolve, reject) => {
    pending.set(id, { resolve, reject });
    webview.postMessage(JSON.stringify({ id, method, params }));
  });
}

export function isNativeHostAvailable(): boolean {
  return getHostWebView() !== null;
}

// ---- Capacitor (Android) ----
// Import dinamico: @capacitor/core/filesystem non devono impedire il
// funzionamento della shell WPF o del fallback browser se per qualunque
// motivo non fossero disponibili a runtime.

export function isCapacitorNative(): boolean {
  const cap = (window as any).Capacitor;
  return typeof cap !== 'undefined' && typeof cap.isNativePlatform === 'function' && cap.isNativePlatform();
}

const MATHNB_EXTENSION = '.mathnb';

async function getFilesystemModule() {
  return import('@capacitor/filesystem');
}

export interface NotebookFileInfo {
  name: string;
  modifiedAt: number;
}

/** Elenca i file .mathnb nella cartella Documenti dell'app (Capacitor),
 * più recenti prima — usato da OpenFileListDialog.tsx. */
export async function listNotebookFiles(): Promise<NotebookFileInfo[]> {
  const { Filesystem, Directory } = await getFilesystemModule();
  const result = await Filesystem.readdir({ path: '', directory: Directory.Documents });
  return result.files
    .filter((f) => f.name.endsWith(MATHNB_EXTENSION) && f.type === 'file')
    .map((f) => ({ name: f.name, modifiedAt: f.mtime ?? 0 }))
    .sort((a, b) => b.modifiedAt - a.modifiedAt);
}

export async function readNotebookFile(name: string): Promise<string> {
  const { Filesystem, Directory, Encoding } = await getFilesystemModule();
  const result = await Filesystem.readFile({ path: name, directory: Directory.Documents, encoding: Encoding.UTF8 });
  return typeof result.data === 'string' ? result.data : await (result.data as Blob).text();
}

async function writeNotebookFile(name: string, contents: string): Promise<void> {
  const { Filesystem, Directory, Encoding } = await getFilesystemModule();
  await Filesystem.writeFile({ path: name, directory: Directory.Documents, data: contents, encoding: Encoding.UTF8 });
}

function sanitizeFileName(name: string): string {
  const base = name.endsWith(MATHNB_EXTENSION) ? name : `${name}${MATHNB_EXTENSION}`;
  // niente separatori di percorso: restiamo dentro Directory.Documents.
  return base.replace(/[/\\]/g, '_');
}

// ---- Fallback browser (sviluppo senza shell WPF) ----

const LOCAL_STORAGE_KEY = 'mathnotebook.devFallback.lastDocument';

/** Apre il selettore file nativo del sistema operativo tramite un
 * <input type="file"> nascosto e legge il contenuto scelto come testo.
 * Funziona non solo nel fallback browser di sviluppo, ma anche dentro la
 * WebView di Capacitor su Android (che supporta input file e apre il
 * selettore nativo di Storage Access Framework) — vedi
 * OpenFileListDialog.tsx, pulsante "Sfoglia file...". */
export async function pickLocalFile(accept: string): Promise<OpenFileResult> {
  return new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) {
        resolve({ canceled: true });
        return;
      }
      const reader = new FileReader();
      reader.onload = () => resolve({ canceled: false, path: file.name, contents: String(reader.result) });
      reader.readAsText(file);
    };
    input.click();
  });
}

async function browserOpenFile(): Promise<OpenFileResult> {
  return pickLocalFile('.mathnb,.txt,application/json,text/plain');
}

function browserDownload(filename: string, contents: string) {
  const blob = new Blob([contents], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

async function browserSaveFile(suggestedName: string, contents: string): Promise<SaveFileResult> {
  localStorage.setItem(LOCAL_STORAGE_KEY, contents);
  browserDownload(suggestedName, contents);
  return { canceled: false, path: suggestedName };
}

// ---- API pubblica ----

/** Su Capacitor "Open" usa un elenco in-app (OpenFileListDialog.tsx via
 * listNotebookFiles/readNotebookFile), non questa funzione: niente
 * dialogo di sistema su Android per scegliere un file. Chi chiama
 * openFile() deve controllare prima isCapacitorNative(). */
export async function openFile(): Promise<OpenFileResult> {
  if (isNativeHostAvailable()) return callHost<OpenFileResult>('file.open');
  return browserOpenFile();
}

export async function saveFile(path: string | undefined, suggestedName: string, contents: string): Promise<SaveFileResult> {
  if (isCapacitorNative()) {
    const name = sanitizeFileName(path ?? suggestedName);
    await writeNotebookFile(name, contents);
    return { canceled: false, path: name };
  }
  if (isNativeHostAvailable()) return callHost<SaveFileResult>('file.save', { path, suggestedName, contents });
  return browserSaveFile(suggestedName, contents);
}

export async function saveFileAs(suggestedName: string, contents: string): Promise<SaveFileResult> {
  if (isCapacitorNative()) {
    const name = sanitizeFileName(suggestedName);
    await writeNotebookFile(name, contents);
    return { canceled: false, path: name };
  }
  if (isNativeHostAvailable()) return callHost<SaveFileResult>('file.saveAs', { suggestedName, contents });
  return browserSaveFile(suggestedName, contents);
}

export async function setWindowTitle(title: string): Promise<void> {
  if (isNativeHostAvailable()) {
    await callHost('window.setTitle', { title });
    return;
  }
  document.title = title;
}

export async function copyToClipboard(text: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    // fallback silenzioso: alcuni contesti WebView senza focus negano l'accesso
  }
}
