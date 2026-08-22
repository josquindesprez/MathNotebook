import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Base "./" so the built assets load correctly from a local file/virtual-host
// mapping inside WebView2 (no absolute "/..." paths, no dev server at runtime).
export default defineConfig({
  base: './',
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
});
