// Dichiarazione JSX per il custom element <math-field> di MathLive.
// Import "mathlive" in main.tsx registra l'elemento (side effect); qui
// dichiariamo solo la sua forma per il type-checker di React/JSX.
import type { MathfieldElement } from 'mathlive';
import type { DetailedHTMLProps, HTMLAttributes } from 'react';

declare global {
  namespace JSX {
    interface IntrinsicElements {
      'math-field': DetailedHTMLProps<HTMLAttributes<MathfieldElement>, MathfieldElement> & {
        // "virtual-keyboard-mode" (usato qui fino a M4) non è un attributo
        // reale di MathLive — non ha mai avuto alcun effetto. L'attributo
        // vero è questo, e il suo default ("auto": tastiera virtuale solo
        // su device touch) è già il comportamento che vogliamo sia su
        // desktop (WPF) sia su mobile (Capacitor): non serve impostarlo.
        'math-virtual-keyboard-policy'?: 'auto' | 'manual' | 'sandboxed';
        'read-only'?: boolean;
      };
    }
  }
}

export {};
