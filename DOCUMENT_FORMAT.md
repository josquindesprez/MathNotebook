# DOCUMENT_FORMAT.md — formato `.mathnb`

File locale, JSON, versionabile (adatto a essere messo in git da chi vuole).
Estensione `.mathnb`.

## Struttura di alto livello

```json
{
  "schemaVersion": 1,
  "id": "b7e2...",
  "title": "Algebra lineare",
  "createdAt": "2026-08-21T10:00:00.000Z",
  "modifiedAt": "2026-08-21T10:32:00.000Z",
  "settings": {
    "theme": "system"
  },
  "cells": []
}
```

## Campi

- `schemaVersion` (number, obbligatorio): intero incrementale. Il loader
  applica migrazioni sequenziali `migrateFrom<N>To<N+1>` fino alla versione
  corrente. Un file con `schemaVersion` maggiore della versione supportata
  dall'app viene aperto in sola lettura con avviso, mai troncato/riscritto.
- `id`: UUID del notebook, stabile nel tempo (permette rename senza perdere l'identità nei "recent").
- `title`: stringa mostrata nella tab e nella finestra.
- `createdAt` / `modifiedAt`: ISO 8601.
- `settings`: preferenze per-documento (tema, eventualmente unità di misura in futuro).
- `cells`: array ordinato di `Cell`.

## Cell

```ts
interface CellBase {
  id: string;          // uuid
  type: 'text' | 'math' | 'calculation' | 'graph';
}

interface TextCell extends CellBase {
  type: 'text';
  markdown: string;     // markdown di base + math inline delimitato da $...$
}

interface MathCell extends CellBase {
  type: 'math';
  ast: MathNode;        // radice dell'espressione (spesso una RelationNode o GroupNode)
}

interface CalculationCell extends CellBase {
  type: 'calculation';
  ast: MathNode;
  lastResult?: {
    ast: MathNode;
    computedAt: string;
    engine: 'sympy';
  };
}

interface GraphCell extends CellBase {
  type: 'graph';
  expressions: { id: string; ast: MathNode; color?: string; visible: boolean }[];
  viewport?: { xMin: number; xMax: number; yMin: number; yMax: number };
}
```

`ast` è sempre un nodo del Math AST descritto in ARCHITECTURE.md, serializzato
1:1 come JSON (nessuna stringa LaTeX salvata come fonte di verità). Questo
garantisce che riaprendo il file la struttura resti pienamente editabile
(placeholder, Tab-navigation) e non un semplice testo formattato.

## Esempio completo

```json
{
  "schemaVersion": 1,
  "id": "b7e2f6b0-1c2d-4e3a-9f0a-000000000001",
  "title": "Esercizi matrici",
  "createdAt": "2026-08-21T10:00:00.000Z",
  "modifiedAt": "2026-08-21T10:05:00.000Z",
  "settings": { "theme": "system" },
  "cells": [
    {
      "id": "c1",
      "type": "text",
      "markdown": "Calcoliamo il determinante di $A$."
    },
    {
      "id": "c2",
      "type": "math",
      "ast": {
        "id": "n1",
        "type": "RelationNode",
        "op": "=",
        "left": { "id": "n2", "type": "IdentifierNode", "name": "A" },
        "right": {
          "id": "n3",
          "type": "MatrixNode",
          "rows": [
            [ { "id": "n4", "type": "NumberNode", "value": "1" }, { "id": "n5", "type": "NumberNode", "value": "2" } ],
            [ { "id": "n6", "type": "NumberNode", "value": "3" }, { "id": "n7", "type": "NumberNode", "value": "4" } ]
          ]
        }
      }
    }
  ]
}
```

## Versioning dello schema

- `schemaVersion` parte da `1` (Milestone 1: solo `text`/`math`).
- Ogni cambio non retro-compatibile della struttura (nuovo tipo di cella
  obbligatorio, rename di un campo, cambio di forma di un nodo AST) incrementa
  la versione e aggiunge una funzione di migrazione pura
  `(doc: DocV_N) => DocV_{N+1}` in `web/src/notebook/migrations/`.
- Aggiunte puramente additive e opzionali (nuovo campo opzionale, nuovo tipo
  di nodo AST non ancora usato da vecchi file) **non** richiedono bump di
  versione.
- Il file non viene mai riscritto silenziosamente a uno schema più vecchio:
  il salvataggio scrive sempre l'ultima versione supportata dall'app in uso.

## Autosave

- Autosave periodico (debounce ~2s dopo l'ultima modifica) sullo stesso
  percorso del file se già salvato una volta; per notebook non ancora
  salvati, autosave in una cartella locale di recovery
  (`%LOCALAPPDATA%/MathNotebook/autosave/<id>.mathnb`) recuperabile
  all'avvio se l'app si chiude senza salvataggio esplicito.
- `Ctrl+S`: salva sul percorso corrente (o apre "Salva con nome" se nuovo).
- `Ctrl+Shift+S`: "Salva con nome" esplicito.
