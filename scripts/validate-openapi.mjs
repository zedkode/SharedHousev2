import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import process from 'node:process';
import { parse } from 'yaml';

const contractPath = resolve('packages/contracts/openapi/sharedhouse-v1.yaml');
const source = await readFile(contractPath, 'utf8');
const contract = parse(source);

const failures = [];

if (contract?.openapi !== '3.1.0') {
  failures.push('The contract must use OpenAPI 3.1.0.');
}

if (contract?.info?.version !== '1.4.0') {
  failures.push('The v1 contract must declare info.version 1.4.0.');
}

for (const requiredPath of [
  '/v1/health',
  '/v1/auth/register',
  '/v1/auth/verify-email',
  '/v1/auth/sign-in',
  '/v1/auth/refresh',
  '/v1/auth/sign-out',
  '/v1/account',
  '/v1/households',
  '/v1/households/{householdId}',
]) {
  if (contract?.paths?.[requiredPath] === undefined) {
    failures.push(`Missing required path: ${requiredPath}`);
  }
}

function findSecretLikeLiteral(value, path = []) {
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => findSecretLikeLiteral(item, [...path, String(index)]));
  }

  if (value === null || typeof value !== 'object') {
    return [];
  }

  return Object.entries(value).flatMap(([key, child]) => {
    const childPath = [...path, key];
    const keyLooksSensitive = /(password|token|secret)/iu.test(key);
    const isLiteral = typeof child === 'string' || typeof child === 'number';

    if (keyLooksSensitive && isLiteral && !['format', 'description'].includes(key)) {
      return [childPath.join('.')];
    }

    return findSecretLikeLiteral(child, childPath);
  });
}

const secretLikeLiterals = findSecretLikeLiteral(contract);
if (secretLikeLiterals.length > 0) {
  failures.push(`Secret-like literal found at: ${secretLikeLiterals.join(', ')}`);
}

if (failures.length > 0) {
  for (const failure of failures) {
    process.stderr.write(`contract-error: ${failure}\n`);
  }
  process.exitCode = 1;
} else {
  process.stdout.write(
    `Validated OpenAPI contract: ${contract.info.title} ${contract.info.version}\n`,
  );
}
