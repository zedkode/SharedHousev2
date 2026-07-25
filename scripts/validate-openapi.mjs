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

if (contract?.info?.version !== '1.0.0') {
  failures.push('The v1 contract must declare info.version 1.0.0.');
}

for (const requiredPath of ['/v1/health']) {
  if (contract?.paths?.[requiredPath] === undefined) {
    failures.push(`Missing required path: ${requiredPath}`);
  }
}

if (/\b(password|token|secret)\s*:/iu.test(source)) {
  failures.push('The checked-in contract appears to contain a secret-like literal.');
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
