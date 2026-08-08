const DEFAULT_PORT = 3000;

export type RuntimeEnvironment = 'development' | 'test' | 'production';
export type EmailProvider = 'disabled' | 'resend';

export interface ApiEnvironment {
  readonly port: number;
  readonly runtimeEnvironment: RuntimeEnvironment;
  readonly databaseUrl: string | null;
  readonly databasePassword: string | null;
  readonly pgliteDataDirectory: string;
  readonly exposeDevelopmentVerificationCode: boolean;
  readonly emailProvider: EmailProvider;
  readonly emailFrom: string | null;
  readonly resendApiKey: string | null;
  readonly emailOutboxEncryptionKeyBase64: string | null;
}

export function readApiEnvironment(environment: NodeJS.ProcessEnv): ApiEnvironment {
  const rawPort = environment.PORT;
  const runtimeEnvironment = readRuntimeEnvironment(environment.NODE_ENV);
  const databaseUrl = readOptionalString(environment.DATABASE_URL);
  const databasePassword = readOptionalString(environment.DATABASE_PASSWORD);
  const emailProvider = readEmailProvider(environment.EMAIL_PROVIDER);
  const emailFrom = readOptionalString(environment.EMAIL_FROM);
  const resendApiKey = readOptionalString(environment.RESEND_API_KEY);
  const emailOutboxEncryptionKeyBase64 = readOptionalString(
    environment.EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64,
  );
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

  if (runtimeEnvironment === 'production' && emailProvider !== 'resend') {
    throw new Error('EMAIL_PROVIDER=resend is required in production.');
  }

  if (emailProvider === 'resend') {
    if (resendApiKey === null) {
      throw new Error('RESEND_API_KEY is required when EMAIL_PROVIDER=resend.');
    }
    if (emailFrom === null || !isEmailSender(emailFrom)) {
      throw new Error('EMAIL_FROM must contain a valid sender address.');
    }
    if (
      emailOutboxEncryptionKeyBase64 === null ||
      !isBase64Key(emailOutboxEncryptionKeyBase64, 32)
    ) {
      throw new Error('EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64 must decode to exactly 32 bytes.');
    }
  }

  const base = {
    runtimeEnvironment,
    databaseUrl,
    databasePassword,
    pgliteDataDirectory:
      readOptionalString(environment.SHAREDHOUSE_PGLITE_DATA_DIR) ??
      (runtimeEnvironment === 'test' ? 'memory://' : './tmp/sharedhouse-pglite'),
    exposeDevelopmentVerificationCode,
    emailProvider,
    emailFrom,
    resendApiKey,
    emailOutboxEncryptionKeyBase64,
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

function readEmailProvider(value: string | undefined): EmailProvider {
  if (value === undefined || value.trim() === '') {
    return 'disabled';
  }
  if (value === 'disabled' || value === 'resend') {
    return value;
  }
  throw new Error('EMAIL_PROVIDER must be disabled or resend.');
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

function isEmailSender(value: string): boolean {
  const match = /<([^<>]+)>$/u.exec(value);
  const address = (match?.[1] ?? value).trim();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(address);
}

function isBase64Key(value: string, expectedBytes: number): boolean {
  try {
    if (value.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/u.test(value)) {
      return false;
    }
    const decoded = Buffer.from(value, 'base64');
    return decoded.length === expectedBytes && decoded.toString('base64') === value;
  } catch {
    return false;
  }
}
