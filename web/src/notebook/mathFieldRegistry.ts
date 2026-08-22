// Registro delle istanze <math-field> attualmente montate, indicizzate per
// id di cella. Permette alla Math Palette di inserire un template
// direttamente nel campo attivo (vedi UI_SPEC.md, "Math Palette").
import type { MathfieldElement } from 'mathlive';

const registry = new Map<string, MathfieldElement>();

export function registerMathField(cellId: string, element: MathfieldElement): void {
  registry.set(cellId, element);
}

export function unregisterMathField(cellId: string): void {
  registry.delete(cellId);
}

export function getMathField(cellId: string): MathfieldElement | undefined {
  return registry.get(cellId);
}
