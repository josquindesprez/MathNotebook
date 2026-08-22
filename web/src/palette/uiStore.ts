// Stato UI della Math Palette (categoria espansa, collassata/aperta).
// Separato dal notebook store perché è puro stato di presentazione, non
// parte del documento. Vive qui (non nel componente) così anche le
// scorciatoie globali Alt+1..Alt+9 (vedi UI_SPEC.md) possono controllarlo.
import { create } from 'zustand';
import type { PaletteCategoryName } from './categories';

interface PaletteUiState {
  collapsed: boolean;
  expandedCategory: PaletteCategoryName | '';
  toggleCollapsed: () => void;
  expandCategory: (category: PaletteCategoryName | '') => void;
}

// Sotto i 768px (vedi index.css) la palette diventa un pannello a comparsa
// a piena larghezza: se partisse aperta come su desktop coprirebbe subito
// tutto il notebook su un telefono. Parte quindi collassata su schermi
// stretti, aperta altrove — coerente in entrambi i contesti senza
// hardcodare la piattaforma (funziona anche solo ridimensionando la
// finestra su desktop).
const startsCollapsed = typeof window !== 'undefined' && window.innerWidth <= 768;

export const usePaletteUiStore = create<PaletteUiState>((set) => ({
  collapsed: startsCollapsed,
  expandedCategory: 'Basic',
  toggleCollapsed: () => set((s) => ({ collapsed: !s.collapsed })),
  expandCategory: (category) => set({ expandedCategory: category, collapsed: false }),
}));

// Ordine delle scorciatoie Alt+1..Alt+9 (vedi UI_SPEC.md).
export const ALT_SHORTCUT_CATEGORIES: PaletteCategoryName[] = [
  'Basic',
  'Algebra',
  'Linear Algebra',
  'Calculus',
  'Greek',
  'Sets',
  'Logic',
  'Functions',
  'Misc',
];
