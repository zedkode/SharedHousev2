import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';

import { loadFileSecrets } from '../src/config/file-secrets.js';

const temporaryDirectories: string[] = [];

afterEach(() => {
  for (const directory of temporaryDirectories.splice(0)) {
    rmSync(directory, { recursive: true, force: true });
  }
});

describe('file-backed secrets', () => {
  it('loads an absolute secret file without retaining the file path as the value', () => {
    const directory = mkdtempSync(join(tmpdir(), 'sharedhouse-secrets-'));
    temporaryDirectories.push(directory);
    const path = join(directory, 'database-url');
    writeFileSync(path, 'postgresql://sharedhouse:synthetic@postgres/sharedhouse\n', 'utf8');
    const environment: NodeJS.ProcessEnv = { DATABASE_URL_FILE: path };

    loadFileSecrets(environment);

    expect(environment.DATABASE_URL).toBe(
      'postgresql://sharedhouse:synthetic@postgres/sharedhouse',
    );
  });

  it('rejects ambiguous, relative and empty secret configuration', () => {
    expect(() =>
      loadFileSecrets({ DATABASE_URL: 'direct', DATABASE_URL_FILE: 'also-file' }),
    ).toThrow('DATABASE_URL and DATABASE_URL_FILE cannot both be configured.');
    expect(() => loadFileSecrets({ DATABASE_URL_FILE: 'relative-secret' })).toThrow(
      'DATABASE_URL_FILE must be an absolute path.',
    );

    const directory = mkdtempSync(join(tmpdir(), 'sharedhouse-secrets-'));
    temporaryDirectories.push(directory);
    const path = join(directory, 'empty');
    writeFileSync(path, '\n', 'utf8');
    expect(() => loadFileSecrets({ DATABASE_URL_FILE: path })).toThrow(
      'DATABASE_URL_FILE points to an empty secret.',
    );
  });
});
