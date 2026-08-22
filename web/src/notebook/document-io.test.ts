import { describe, expect, it } from 'vitest';
import { createEmptyNotebook, createMathCell, createTextCell, CURRENT_SCHEMA_VERSION } from './model';
import { deserializeNotebook, serializeNotebook, UnsupportedSchemaVersionError } from './document-io';
import { parseMathInput } from '../parser/parser';
import { toLatex } from '../render/toLatex';

describe('serializeNotebook / deserializeNotebook', () => {
  it('round-trip preserva celle e AST', () => {
    const doc = createEmptyNotebook('Esercizi matrici');
    doc.cells = [createTextCell('Calcoliamo il determinante di $A$.'), createMathCell(parseMathInput('det(A)'))];

    const json = serializeNotebook(doc);
    const reloaded = deserializeNotebook(json);

    expect(reloaded.title).toBe('Esercizi matrici');
    expect(reloaded.cells).toHaveLength(2);
    expect(reloaded.cells[0]).toMatchObject({ type: 'text', markdown: 'Calcoliamo il determinante di $A$.' });
    const mathCell = reloaded.cells[1];
    expect(mathCell.type).toBe('math');
    if (mathCell.type === 'math') {
      expect(toLatex(mathCell.ast)).toBe('\\det(A)');
    }
  });

  it("rifiuta un file con schemaVersion mancante", () => {
    expect(() => deserializeNotebook(JSON.stringify({ title: 'x' }))).toThrow();
  });

  it('rifiuta (con errore dedicato) uno schemaVersion futuro non supportato', () => {
    const doc = createEmptyNotebook();
    const future = { ...doc, schemaVersion: CURRENT_SCHEMA_VERSION + 1 };
    expect(() => deserializeNotebook(JSON.stringify(future))).toThrow(UnsupportedSchemaVersionError);
  });

  it('migra un file v1 con RelationNode nella vecchia forma {op,left,right}', () => {
    const v1Json = JSON.stringify({
      schemaVersion: 1,
      id: 'doc1',
      title: 'Vecchio file',
      createdAt: new Date().toISOString(),
      modifiedAt: new Date().toISOString(),
      settings: { theme: 'system' },
      cells: [
        {
          id: 'c1',
          type: 'math',
          ast: {
            id: 'n1',
            type: 'RelationNode',
            op: '=',
            left: { id: 'n2', type: 'IdentifierNode', name: 'A' },
            right: { id: 'n3', type: 'NumberNode', value: '1' },
          },
        },
      ],
    });

    const migrated = deserializeNotebook(v1Json);
    expect(migrated.schemaVersion).toBe(CURRENT_SCHEMA_VERSION);
    const cell = migrated.cells[0];
    if (cell.type !== 'math') throw new Error('unreachable');
    expect(cell.ast).toMatchObject({
      type: 'RelationNode',
      ops: ['='],
      terms: [{ name: 'A' }, { value: '1' }],
    });
    expect(toLatex(cell.ast)).toBe('A = 1');
  });
});
