import { createParamDecorator, type ExecutionContext } from '@nestjs/common';

import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import type { AuthenticatedRequest } from './authentication.guard.js';

export const CurrentPrincipal = createParamDecorator(
  (_data: unknown, context: ExecutionContext): AuthenticatedPrincipal => {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    if (request.authenticatedPrincipal === undefined) {
      throw new Error('Authenticated principal is unavailable.');
    }
    return request.authenticatedPrincipal;
  },
);
