// Fuzzy search sulla Math Palette. Vedi UI_SPEC.md — subsequence match con
// punteggio (bonus inizio parola, penalità distanza tra i caratteri).

import type { PaletteItem } from './categories';

function fuzzyScore(query: string, target: string): number | null {
  const q = query.toLowerCase();
  const t = target.toLowerCase();
  if (q.length === 0) return 0;

  let qi = 0;
  let score = 0;
  let lastMatchIndex = -1;

  for (let ti = 0; ti < t.length && qi < q.length; ti += 1) {
    if (t[ti] === q[qi]) {
      let charScore = 10;
      if (ti === 0 || t[ti - 1] === ' ') charScore += 8; // bonus inizio parola
      if (lastMatchIndex >= 0) {
        const gap = ti - lastMatchIndex - 1;
        charScore -= Math.min(gap, 5); // penalità distanza
      }
      score += charScore;
      lastMatchIndex = ti;
      qi += 1;
    }
  }

  return qi === q.length ? score : null;
}

function bestScoreForItem(query: string, item: PaletteItem): number | null {
  const candidates = [item.label, item.description, item.keyboardHint ?? '', ...(item.keywords ?? [])];
  let best: number | null = null;
  for (const candidate of candidates) {
    const score = fuzzyScore(query, candidate);
    if (score !== null && (best === null || score > best)) best = score;
  }
  return best;
}

export function searchPaletteItems(items: PaletteItem[], query: string): PaletteItem[] {
  if (!query.trim()) return items;
  return items
    .map((item) => ({ item, score: bestScoreForItem(query, item) }))
    .filter((entry): entry is { item: PaletteItem; score: number } => entry.score !== null)
    .sort((a, b) => b.score - a.score)
    .map((entry) => entry.item);
}
