// Stessa soglia usata in index.css (@media max-width: 768px) e in
// palette/uiStore.ts, ma reattiva al resize/rotazione — serve per decidere
// a runtime (non solo al primo render) se una cella in editing deve aprirsi
// a schermo intero (vedi CellShell.tsx).
import { useEffect, useState } from 'react';

const QUERY = '(max-width: 768px)';

export function useIsMobileLayout(): boolean {
  const [isMobile, setIsMobile] = useState(() => typeof window !== 'undefined' && window.matchMedia(QUERY).matches);

  useEffect(() => {
    const mql = window.matchMedia(QUERY);
    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches);
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, []);

  return isMobile;
}
