import { describe, expect, it } from 'vitest';
import { PALETTE_ITEMS } from './categories';
import { searchPaletteItems } from './search';

describe('searchPaletteItems', () => {
  it('trova la frazione cercando "frac"', () => {
    const results = searchPaletteItems(PALETTE_ITEMS, 'frac');
    expect(results.some((r) => r.id === 'fraction-basic')).toBe(true);
  });

  it('trova i template di radice cercando "root"', () => {
    const results = searchPaletteItems(PALETTE_ITEMS, 'root');
    expect(results.map((r) => r.id)).toContain('root-nth');
  });

  it('trova le lettere greche per nome', () => {
    const results = searchPaletteItems(PALETTE_ITEMS, 'alpha');
    expect(results[0]?.id).toBe('greek-alpha');
  });

  it('query vuota restituisce tutti gli elementi', () => {
    expect(searchPaletteItems(PALETTE_ITEMS, '')).toHaveLength(PALETTE_ITEMS.length);
  });

  it('"matrix" trova i template di matrice (M2)', () => {
    const results = searchPaletteItems(PALETTE_ITEMS, 'matrix').map((r) => r.id);
    expect(results).toEqual(expect.arrayContaining(['matrix-2x2', 'matrix-3x3', 'matrix-custom']));
  });

  it('"transpose" trova la trasposta', () => {
    const results = searchPaletteItems(PALETTE_ITEMS, 'transpose');
    expect(results[0]?.id).toBe('la-transpose');
  });
});
