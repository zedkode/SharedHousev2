import { Body, Controller, Get, Header, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import type { AccountSummary, RegistrationAccepted, SessionResponse } from '@sharedhouse/contracts';
import type { Request } from 'express';

import {
  parseRefreshRequest,
  parseRegisterRequest,
  parseResendEmailVerificationRequest,
  parseSignInRequest,
  parseVerifyEmailRequest,
} from '../http/request-validation.js';
import { AuthenticationGuard, extractBearerToken } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import type { AuthenticatedPrincipal } from './identity.types.js';
import { IdentityService } from './identity.service.js';

@Controller('v1/auth')
export class AuthController {
  constructor(private readonly identity: IdentityService) {}

  @Post('register')
  @HttpCode(202)
  @Header('Cache-Control', 'no-store')
  @Header('Pragma', 'no-cache')
  @Throttle({ default: { limit: 10, ttl: 15 * 60_000 } })
  async register(@Body() body: unknown): Promise<RegistrationAccepted> {
    return this.identity.register(parseRegisterRequest(body));
  }

  @Post('verify-email')
  @HttpCode(200)
  @Header('Cache-Control', 'no-store')
  @Header('Pragma', 'no-cache')
  @Throttle({ default: { limit: 8, ttl: 60_000 } })
  async verifyEmail(@Body() body: unknown): Promise<SessionResponse> {
    return this.identity.verifyEmail(parseVerifyEmailRequest(body));
  }

  @Post('resend-verification')
  @HttpCode(202)
  @Header('Cache-Control', 'no-store')
  @Header('Pragma', 'no-cache')
  @Throttle({ default: { limit: 3, ttl: 15 * 60_000 } })
  async resendVerification(@Body() body: unknown): Promise<RegistrationAccepted> {
    return this.identity.resendVerification(parseResendEmailVerificationRequest(body));
  }

  @Post('sign-in')
  @HttpCode(200)
  @Header('Cache-Control', 'no-store')
  @Header('Pragma', 'no-cache')
  @Throttle({ default: { limit: 8, ttl: 60_000 } })
  async signIn(@Body() body: unknown): Promise<SessionResponse> {
    return this.identity.signIn(parseSignInRequest(body));
  }

  @Post('refresh')
  @HttpCode(200)
  @Header('Cache-Control', 'no-store')
  @Header('Pragma', 'no-cache')
  @Throttle({ default: { limit: 20, ttl: 60_000 } })
  async refresh(@Body() body: unknown): Promise<SessionResponse> {
    return this.identity.refresh(parseRefreshRequest(body));
  }

  @Post('sign-out')
  @HttpCode(204)
  @UseGuards(AuthenticationGuard)
  async signOut(@Req() request: Request): Promise<void> {
    const token = extractBearerToken(request.headers.authorization);
    if (token !== null) {
      await this.identity.signOut(token);
    }
  }
}

@Controller('v1/account')
export class AccountController {
  @Get()
  @UseGuards(AuthenticationGuard)
  getAccount(@CurrentPrincipal() principal: AuthenticatedPrincipal): AccountSummary {
    return principal.account;
  }
}
