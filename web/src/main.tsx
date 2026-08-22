import 'mathlive';
import 'katex/dist/katex.min.css';
import './styles/index.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './components/App';

const rootEl = document.getElementById('root');
if (!rootEl) throw new Error('#root non trovato');

createRoot(rootEl).render(
  <StrictMode>
    <App />
  </StrictMode>
);
