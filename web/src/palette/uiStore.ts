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

export const usePaletteUiStore = create<PaletteUiState>((set) => ({
  collapsed: false,
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
