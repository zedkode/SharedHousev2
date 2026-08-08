import { Test } from '@nestjs/testing';
import type { INestApplication } from '@nestjs/common';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';

describe('GET /v1/health', () => {
  let app: INestApplication;

  beforeEach(async () => {
    const module = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = module.createNestApplication();
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  it('returns the versioned API health contract', async () => {
    const server = app.getHttpServer() as unknown as Server;
    const response = await request(server)
      .get('/v1/health')
      .expect('Content-Type', /json/u)
      .expect(200);

    const body: unknown = response.body;

    expect(body).toMatchObject({
      status: 'ok',
      service: 'api',
      apiVersion: 'v1',
    });

    if (
      typeof body !== 'object' ||
      body === null ||
      !('checkedAt' in body) ||
      typeof body.checkedAt !== 'string'
    ) {
      throw new Error('Health response is missing checkedAt.');
    }

    expect(Number.isNaN(Date.parse(body.checkedAt))).toBe(false);
  });

  it('reports database readiness separately from process liveness', async () => {
    const server = app.getHttpServer() as unknown as Server;
    await request(server)
      .get('/v1/health/ready')
      .expect('Content-Type', /json/u)
      .expect(200)
      .expect(({ body }: { body: unknown }) => {
        expect(body).toMatchObject({ status: 'ok', service: 'api', apiVersion: 'v1' });
      });
  });
});
