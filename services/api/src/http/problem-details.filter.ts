import { ArgumentsHost, Catch, HttpException, type ExceptionFilter } from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import type { Request, Response } from 'express';

import { ApiProblemException, type ApiProblemDefinition } from './api-problem.exception.js';

@Catch()
export class ProblemDetailsFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost): void {
    const context = host.switchToHttp();
    const request = context.getRequest<Request>();
    const response = context.getResponse<Response>();
    const correlationId = readCorrelationId(request.headers['x-correlation-id']);
    const problem = mapException(exception);

    response.setHeader('X-Correlation-Id', correlationId);
    response
      .status(problem.status)
      .type('application/problem+json')
      .send({
        type: `https://sharedhouse.example/problems/${problem.code.toLocaleLowerCase('en-US')}`,
        title: problem.title,
        status: problem.status,
        code: problem.code,
        correlationId,
        ...(problem.detail === undefined ? {} : { detail: problem.detail }),
        ...(problem.violations === undefined ? {} : { violations: problem.violations }),
      });
  }
}

function mapException(exception: unknown): ApiProblemDefinition {
  if (exception instanceof ApiProblemException) {
    return exception.problem;
  }

  if (exception instanceof HttpException) {
    const status = exception.getStatus();
    if (status === 429) {
      return {
        status,
        code: 'RATE_LIMITED',
        title: 'Too many requests. Try again later.',
      };
    }

    return {
      status,
      code: 'REQUEST_REJECTED',
      title:
        status >= 500 ? 'The service could not complete the request.' : 'The request was rejected.',
    };
  }

  return {
    status: 500,
    code: 'INTERNAL_ERROR',
    title: 'The service could not complete the request.',
  };
}

function readCorrelationId(header: string | readonly string[] | undefined): string {
  const value = typeof header === 'string' ? header : header?.[0];
  if (
    typeof value === 'string' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value)
  ) {
    return value;
  }

  return randomUUID();
}
