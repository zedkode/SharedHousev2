import 'reflect-metadata';

import { NestFactory } from '@nestjs/core';
import helmet from 'helmet';

import { AppModule } from './app.module.js';
import { readApiEnvironment } from './config/environment.js';
import { loadFileSecrets } from './config/file-secrets.js';

async function bootstrap(): Promise<void> {
  loadFileSecrets(process.env);
  const environment = readApiEnvironment(process.env);
  const app = await NestFactory.create(AppModule);

  if (environment.runtimeEnvironment === 'production') {
    const httpAdapter = app.getHttpAdapter().getInstance() as {
      set(name: string, value: number): void;
    };
    // The production container is reachable only through the adjacent cloudflared connector.
    // Trust exactly that single proxy hop so rate limits use the originating client address.
    httpAdapter.set('trust proxy', 1);
  }
  app.use(helmet());
  app.enableShutdownHooks();
  await app.listen(environment.port);
}

void bootstrap();
