// Tabella nomi -> simbolo, condivisa da parser e palette. Vedi SYNTAX.md.

export const GREEK_LETTERS: Record<string, string> = {
  alpha: 'α',
  beta: 'β',
  gamma: 'γ',
  delta: 'δ',
  epsilon: 'ε',
  zeta: 'ζ',
  eta: 'η',
  theta: 'θ',
  iota: 'ι',
  kappa: 'κ',
  lambda: 'λ',
  mu: 'μ',
  nu: 'ν',
  xi: 'ξ',
  omicron: 'ο',
  pi: 'π',
  rho: 'ρ',
  sigma: 'σ',
  tau: 'τ',
  upsilon: 'υ',
  phi: 'φ',
  chi: 'χ',
  psi: 'ψ',
  omega: 'ω',
  Gamma: 'Γ',
  Delta: 'Δ',
  Theta: 'Θ',
  Lambda: 'Λ',
  Xi: 'Ξ',
  Pi: 'Π',
  Sigma: 'Σ',
  Upsilon: 'Υ',
  Phi: 'Φ',
  Psi: 'Ψ',
  Omega: 'Ω',
};

export const LATEX_GREEK_NAMES: Record<string, string> = Object.fromEntries(
  Object.entries(GREEK_LETTERS).map(([name]) => [name, `\\${name}`])
);

export const SET_SYMBOLS: Record<string, string> = {
  R: 'ℝ',
  N: 'ℕ',
  Z: 'ℤ',
  Q: 'ℚ',
  C: 'ℂ',
};

export const LINEAR_ALGEBRA_FUNCTIONS = new Set([
  'dot',
  'norm',
  'det',
  'transpose',
  'inv',
  'rank',
]);

export const KNOWN_FUNCTIONS = new Set([
  'sin',
  'cos',
  'tan',
  'cot',
  'sec',
  'csc',
  'ln',
  'log',
  'exp',
  ...LINEAR_ALGEBRA_FUNCTIONS,
]);
