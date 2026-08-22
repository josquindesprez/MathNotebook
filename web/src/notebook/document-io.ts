// Serializzazione/caricamento del documento .mathnb con versioning dello
// schema. Vedi DOCUMENT_FORMAT.md.

import { CURRENT_SCHEMA_VERSION, type NotebookDocument } from './model';

export class UnsupportedSchemaVersionError extends Error {
  constructor(public foundVersion: number) {
    super(
      `Il file è stato salvato con una versione dello schema (${foundVersion}) più recente di quella supportata da questa versione dell'app (${CURRENT_SCHEMA_VERSION}). Apertura in sola lettura non disponibile in questa build.`
    );
  }
}

/** Cammina ricorsivamente qualunque struttura JSON (indipendentemente dai
 * nomi dei campi: left/right, base/exponent, args, rows, ...) e converte
 * ogni RelationNode dalla forma v1 {op,left,right} alla forma v2
 * {terms[],ops[]} (vedi ast/types.ts). Generico apposta: non deve sapere
 * nulla della forma degli altri nodi AST per raggiungere quelli annidati. */
function migrateRelationNodeShapeV1toV2(node: unknown): unknown {
  if (Array.isArray(node)) return node.map(migrateRelationNodeShapeV1toV2);
  if (node && typeof node === 'object') {
    const obj = node as Record<string, unknown>;
    let working: Record<string, unknown> = obj;
    if (obj.type === 'RelationNode' && 'left' in obj && 'right' in obj && 'op' in obj) {
      const { op, left, right, ...rest } = obj;
      working = { ...rest, terms: [left, right], ops: [op] };
    }
    const migrated: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(working)) {
      migrated[key] = migrateRelationNodeShapeV1toV2(value);
    }
    return migrated;
  }
  return node;
}

// Migrazioni pure (docV_N) => docV_{N+1}.
const MIGRATIONS: Record<number, (doc: any) => any> = {
  1: (doc) => ({
    ...doc,
    schemaVersion: 2,
    cells: migrateRelationNodeShapeV1toV2(doc.cells),
  }),
};

export function serializeNotebook(doc: NotebookDocument): string {
  return JSON.stringify(doc, null, 2);
}

export function deserializeNotebook(json: string): NotebookDocument {
  const raw = JSON.parse(json);
  if (typeof raw.schemaVersion !== 'number') {
    throw new Error('File .mathnb non valido: manca "schemaVersion".');
  }
  if (raw.schemaVersion > CURRENT_SCHEMA_VERSION) {
    throw new UnsupportedSchemaVersionError(raw.schemaVersion);
  }
  let doc = raw;
  let version = raw.schemaVersion;
  while (version < CURRENT_SCHEMA_VERSION) {
    const migrate = MIGRATIONS[version];
    if (!migrate) {
      throw new Error(`Manca la migrazione dalla versione dello schema ${version}.`);
    }
    doc = migrate(doc);
    version += 1;
  }
  return doc as NotebookDocument;
}

export function touchModified(doc: NotebookDocument): NotebookDocument {
  return { ...doc, modifiedAt: new Date().toISOString() };
}
