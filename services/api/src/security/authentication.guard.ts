import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import type { Request } from 'express';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { IdentityRepository } from '../identity/identity.repository.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { TokenService } from './token.service.js';

export interface AuthenticatedRequest extends Request {
  authenticatedPrincipal?: AuthenticatedPrincipal;
}

@Injectable()
export class AuthenticationGuard implements CanActivate {
  constructor(
    private readonly repository: IdentityRepository,
    private readonly tokens: TokenService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    const token = extractBearerToken(request.headers.authorization);
    if (token === null) {
      throw unauthenticated();
    }

    const principal = await this.repository.findPrincipalByAccessTokenHash(
      this.tokens.hash(token),
      new Date().toISOString(),
    );
    if (principal === null) {
      throw unauthenticated();
    }

    request.authenticatedPrincipal = principal;
    return true;
  }
}

export function extractBearerToken(header: string | undefined): string | null {
  if (header === undefined) {
    return null;
  }
  const match = /^Bearer ([A-Za-z0-9_-]{40,256})$/u.exec(header);
  return match?.[1] ?? null;
}

function unauthenticated(): ApiProblemException {
  return new ApiProblemException({
    status: 401,
    code: 'SESSION_INVALID',
    title: 'Sign in again to continue.',
  });
}
