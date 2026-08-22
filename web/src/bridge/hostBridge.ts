// Bridge verso la shell nativa (.NET/WPF + WebView2). Vedi ARCHITECTURE.md,
// sezione "Bridge nativo". In sviluppo browser puro (`npm run dev` senza la
// shell WPF) usa un fallback locale così l'app resta utilizzabile.

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

// ---- Fallback browser (sviluppo senza shell WPF) ----

const LOCAL_STORAGE_KEY = 'mathnotebook.devFallback.lastDocument';

async function browserOpenFile(): Promise<OpenFileResult> {
  return new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.mathnb,application/json';
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

export async function openFile(): Promise<OpenFileResult> {
  if (isNativeHostAvailable()) return callHost<OpenFileResult>('file.open');
  return browserOpenFile();
}

export async function saveFile(path: string | undefined, suggestedName: string, contents: string): Promise<SaveFileResult> {
  if (isNativeHostAvailable()) return callHost<SaveFileResult>('file.save', { path, suggestedName, contents });
  return browserSaveFile(suggestedName, contents);
}

export async function saveFileAs(suggestedName: string, contents: string): Promise<SaveFileResult> {
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
