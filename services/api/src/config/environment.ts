const DEFAULT_PORT = 3000;

export type RuntimeEnvironment = 'development' | 'test' | 'production';

export interface ApiEnvironment {
  readonly port: number;
  readonly runtimeEnvironment: RuntimeEnvironment;
  readonly databaseUrl: string | null;
  readonly pgliteDataDirectory: string;
  readonly exposeDevelopmentVerificationCode: boolean;
}

export function readApiEnvironment(environment: NodeJS.ProcessEnv): ApiEnvironment {
  const rawPort = environment.PORT;
  const runtimeEnvironment = readRuntimeEnvironment(environment.NODE_ENV);
  const databaseUrl = readOptionalString(environment.DATABASE_URL);
  const exposeDevelopmentVerificationCode = readBoolean(
    environment.AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE,
    runtimeEnvironment !== 'production',
    'AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE',
  );

  if (runtimeEnvironment === 'production' && databaseUrl === null) {
    throw new Error('DATABASE_URL is required in production.');
  }

  if (runtimeEnvironment === 'production' && exposeDevelopmentVerificationCode) {
    throw new Error('Development verification codes cannot be exposed in production.');
  }

  const base = {
    runtimeEnvironment,
    databaseUrl,
    pgliteDataDirectory:
      readOptionalString(environment.SHAREDHOUSE_PGLITE_DATA_DIR) ??
      (runtimeEnvironment === 'test' ? 'memory://' : './tmp/sharedhouse-pglite'),
    exposeDevelopmentVerificationCode,
  };

  if (rawPort === undefined || rawPort.trim() === '') {
    return { port: DEFAULT_PORT, ...base };
  }

  const port = Number(rawPort);

  if (!Number.isSafeInteger(port) || port < 1 || port > 65_535) {
    throw new Error('PORT must be an integer between 1 and 65535.');
  }

  return { port, ...base };
}

function readRuntimeEnvironment(value: string | undefined): RuntimeEnvironment {
  if (value === undefined || value.trim() === '') {
    return 'development';
  }

  if (value === 'development' || value === 'test' || value === 'production') {
    return value;
  }

  throw new Error('NODE_ENV must be development, test, or production.');
}

function readOptionalString(value: string | undefined): string | null {
  if (value === undefined || value.trim() === '') {
    return null;
  }

  return value.trim();
}

function readBoolean(value: string | undefined, fallback: boolean, name: string): boolean {
  if (value === undefined || value.trim() === '') {
    return fallback;
  }

  if (value === 'true') {
    return true;
  }

  if (value === 'false') {
    return false;
  }

  throw new Error(`${name} must be true or false.`);
}
