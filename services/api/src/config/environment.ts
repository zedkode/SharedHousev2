const DEFAULT_PORT = 3000;

export interface ApiEnvironment {
  readonly port: number;
}

export function readApiEnvironment(environment: NodeJS.ProcessEnv): ApiEnvironment {
  const rawPort = environment.PORT;

  if (rawPort === undefined || rawPort.trim() === '') {
    return { port: DEFAULT_PORT };
  }

  const port = Number(rawPort);

  if (!Number.isSafeInteger(port) || port < 1 || port > 65_535) {
    throw new Error('PORT must be an integer between 1 and 65535.');
  }

  return { port };
}
