import 'reflect-metadata';

import { NestFactory } from '@nestjs/core';
import helmet from 'helmet';

import { AppModule } from './app.module.js';
import { readApiEnvironment } from './config/environment.js';

async function bootstrap(): Promise<void> {
  const environment = readApiEnvironment(process.env);
  const app = await NestFactory.create(AppModule);

  app.use(helmet());
  app.enableShutdownHooks();
  await app.listen(environment.port);
}

void bootstrap();
