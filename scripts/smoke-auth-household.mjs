import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { mkdir, mkdtemp } from 'node:fs/promises';
import { createServer } from 'node:net';
import { resolve } from 'node:path';
import process from 'node:process';
import { setTimeout as delay } from 'node:timers/promises';

const repositoryRoot = process.cwd();
const runtimeRoot = resolve(repositoryRoot, 'tmp');
await mkdir(runtimeRoot, { recursive: true });
const dataDirectory = await mkdtemp(resolve(runtimeRoot, 'auth-household-smoke-'));
const port = await findAvailablePort();
const baseUrl = `http://127.0.0.1:${port}`;
const stamp = Date.now();
const email = `runtime-smoke-${stamp}@example.test`;
const password = 'A synthetic runtime smoke passphrase 2026';
let apiProcess = null;

try {
  apiProcess = await startApi();

  const registration = await request('/v1/auth/register', {
    method: 'POST',
    expectedStatus: 202,
    body: {
      email,
      password,
      displayName: 'Runtime Smoke User',
      preferredLocale: 'ro',
      ageConfirmed: true,
      termsAccepted: true,
      marketingConsent: false,
    },
  });
  const verificationCode = readString(registration, 'developmentVerificationCode');
  const verifiedSession = await request('/v1/auth/verify-email', {
    method: 'POST',
    expectedStatus: 200,
    body: {
      email,
      code: verificationCode,
      deviceName: 'Runtime persistence smoke',
    },
  });
  const initialAccessToken = readString(verifiedSession, 'accessToken');

  await request('/v1/households', {
    method: 'POST',
    expectedStatus: 201,
    accessToken: initialAccessToken,
    headers: { 'Idempotency-Key': randomUUID() },
    body: {
      name: 'Casa Smoke Persistenta',
      countryCode: 'RO',
      timezone: 'Europe/Bucharest',
      currency: 'RON',
      firstDayOfWeek: 1,
      cycleType: 'calendar_month',
      cycleAnchor: '2026-08-01',
    },
  });

  await stopApi(apiProcess);
  apiProcess = await startApi();

  const signedInSession = await request('/v1/auth/sign-in', {
    method: 'POST',
    expectedStatus: 200,
    body: {
      email,
      password,
      deviceName: 'Runtime persistence smoke restart',
    },
  });
  const account = readObject(signedInSession, 'account');
  const accessToken = readString(signedInSession, 'accessToken');
  const households = await request('/v1/households', {
    method: 'GET',
    expectedStatus: 200,
    accessToken,
  });

  if (!Array.isArray(households) || households.length !== 1) {
    throw new Error('Expected exactly one persisted household after restart.');
  }
  const household = households[0];
  if (typeof household !== 'object' || household === null) {
    throw new Error('Persisted household response is invalid.');
  }

  process.stdout.write(
    `${JSON.stringify(
      {
        status: 'passed',
        accountVerified: account.emailVerified === true,
        householdCountAfterRestart: households.length,
        householdName: household.name,
        householdVersion: household.version,
        dataDirectory,
      },
      null,
      2,
    )}\n`,
  );
} finally {
  if (apiProcess !== null) {
    await stopApi(apiProcess);
  }
}

async function startApi() {
  const child = spawn(process.execPath, [resolve('services/api/dist/main.js')], {
    cwd: repositoryRoot,
    env: {
      ...process.env,
      AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE: 'true',
      DATABASE_URL: '',
      NODE_ENV: 'development',
      PORT: String(port),
      SHAREDHOUSE_PGLITE_DATA_DIR: dataDirectory,
    },
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true,
  });
  let diagnostics = '';
  child.stdout.setEncoding('utf8');
  child.stderr.setEncoding('utf8');
  child.stdout.on('data', (chunk) => {
    diagnostics = appendDiagnostics(diagnostics, chunk);
  });
  child.stderr.on('data', (chunk) => {
    diagnostics = appendDiagnostics(diagnostics, chunk);
  });

  for (let attempt = 0; attempt < 120; attempt += 1) {
    if (child.exitCode !== null) {
      throw new Error(`API exited during startup (${child.exitCode}). ${diagnostics}`);
    }
    try {
      const response = await globalThis.fetch(`${baseUrl}/v1/health`, {
        signal: globalThis.AbortSignal.timeout(1_000),
      });
      if (response.ok) {
        return child;
      }
    } catch {
      // Startup polling is expected to fail until migrations and the HTTP listener are ready.
    }
    await delay(250);
  }

  await stopApi(child);
  throw new Error(`API did not become ready within 30 seconds. ${diagnostics}`);
}

async function stopApi(child) {
  if (child.exitCode !== null) {
    return;
  }
  child.kill('SIGTERM');
  await Promise.race([
    new Promise((resolveExit) => child.once('exit', resolveExit)),
    delay(10_000, undefined, { ref: false }).then(() => {
      throw new Error('API did not stop within 10 seconds.');
    }),
  ]);
}

async function request(path, { method, expectedStatus, body, accessToken, headers = {} }) {
  const response = await globalThis.fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...headers,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(accessToken === undefined ? {} : { Authorization: `Bearer ${accessToken}` }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    signal: globalThis.AbortSignal.timeout(20_000),
  });
  const responseBody = await response.json();
  if (response.status !== expectedStatus) {
    const safeCode = readOptionalString(responseBody, 'code') ?? 'UNKNOWN_RESPONSE';
    throw new Error(`${method} ${path} returned ${response.status} (${safeCode}).`);
  }
  return responseBody;
}

function readObject(value, property) {
  if (
    typeof value !== 'object' ||
    value === null ||
    typeof value[property] !== 'object' ||
    value[property] === null
  ) {
    throw new Error(`Response is missing object property ${property}.`);
  }
  return value[property];
}

function readString(value, property) {
  const result = readOptionalString(value, property);
  if (result === null) {
    throw new Error(`Response is missing string property ${property}.`);
  }
  return result;
}

function readOptionalString(value, property) {
  if (typeof value !== 'object' || value === null || typeof value[property] !== 'string') {
    return null;
  }
  return value[property];
}

function appendDiagnostics(current, chunk) {
  return `${current}${String(chunk)}`.slice(-4_000);
}

async function findAvailablePort() {
  const server = createServer();
  await new Promise((resolveListen, rejectListen) => {
    server.once('error', rejectListen);
    server.listen(0, '127.0.0.1', resolveListen);
  });
  const address = server.address();
  await new Promise((resolveClose, rejectClose) => {
    server.close((error) => (error === undefined ? resolveClose() : rejectClose(error)));
  });
  if (typeof address !== 'object' || address === null) {
    throw new Error('Could not allocate a local smoke-test port.');
  }
  return address.port;
}
