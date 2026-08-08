import { readFileSync } from 'node:fs';
import { isAbsolute } from 'node:path';

const FILE_SECRET_NAMES = [
  'DATABASE_URL',
  'DATABASE_PASSWORD',
  'RESEND_API_KEY',
  'EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64',
] as const;

export function loadFileSecrets(environment: NodeJS.ProcessEnv): void {
  for (const name of FILE_SECRET_NAMES) {
    const direct = environment[name]?.trim();
    const fileVariable = `${name}_FILE`;
    const filePath = environment[fileVariable]?.trim();

    if (direct !== undefined && direct !== '' && filePath !== undefined && filePath !== '') {
      throw new Error(`${name} and ${fileVariable} cannot both be configured.`);
    }
    if (filePath === undefined || filePath === '') {
      continue;
    }
    if (!isAbsolute(filePath)) {
      throw new Error(`${fileVariable} must be an absolute path.`);
    }

    const value = readFileSync(filePath, 'utf8').trim();
    if (value === '') {
      throw new Error(`${fileVariable} points to an empty secret.`);
    }
    environment[name] = value;
  }
}
