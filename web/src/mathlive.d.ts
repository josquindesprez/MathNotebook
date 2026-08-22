// Dichiarazione JSX per il custom element <math-field> di MathLive.
// Import "mathlive" in main.tsx registra l'elemento (side effect); qui
// dichiariamo solo la sua forma per il type-checker di React/JSX.
import type { MathfieldElement } from 'mathlive';
import type { DetailedHTMLProps, HTMLAttributes } from 'react';

declare global {
  namespace JSX {
    interface IntrinsicElements {
      'math-field': DetailedHTMLProps<HTMLAttributes<MathfieldElement>, MathfieldElement> & {
        'virtual-keyboard-mode'?: 'auto' | 'manual' | 'off';
        'read-only'?: boolean;
      };
    }
  }
}

export {};
