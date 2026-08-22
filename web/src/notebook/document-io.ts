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

// Migrazioni pure (docV_N) => docV_{N+1}. Vuoto finché esiste solo la v1.
const MIGRATIONS: Record<number, (doc: any) => any> = {};

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
