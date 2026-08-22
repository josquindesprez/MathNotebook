import { describe, expect, it } from 'vitest';
import { computeGroupColorIndices, createMathCell, createTextCell, GROUP_COLOR_COUNT, type Cell } from './model';

describe('computeGroupColorIndices', () => {
  it('tutte le celle sono nel gruppo 0 se nessuna ha groupStart', () => {
    const cells: Cell[] = [createTextCell(''), createMathCell(), createMathCell()];
    expect(computeGroupColorIndices(cells)).toEqual([0, 0, 0]);
  });

  it('ogni cella con groupStart incrementa il colore (ciclicamente)', () => {
    const cells: Cell[] = [createTextCell(''), createMathCell(), createMathCell(), createMathCell()];
    cells[2].groupStart = true;
    expect(computeGroupColorIndices(cells)).toEqual([0, 0, 1, 1]);
  });

  it('groupStart sulla prima cella non conta (è già l\'inizio del primo gruppo)', () => {
    const cells: Cell[] = [createTextCell('')];
    cells[0].groupStart = true;
    expect(computeGroupColorIndices(cells)).toEqual([0]);
  });

  it('il colore torna a 0 dopo GROUP_COLOR_COUNT gruppi', () => {
    const cells: Cell[] = Array.from({ length: GROUP_COLOR_COUNT + 1 }, () => createMathCell());
    for (let i = 1; i < cells.length; i += 1) cells[i].groupStart = true;
    const colors = computeGroupColorIndices(cells);
    expect(colors[0]).toBe(0);
    expect(colors[GROUP_COLOR_COUNT]).toBe(0); // ha fatto un giro completo
  });
});
