import {
  Body,
  Controller,
  Delete,
  Get,
  Header,
  HttpCode,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import type {
  AccountDeletionResult,
  AccountSummary,
  RegistrationAccepted,
  SessionResponse,
} from '@sharedhouse/contracts';
import type { Request } from 'express';

import {
  parseRefreshRequest,
  parseDeleteAccountRequest,
  parsePublicDeleteAccountRequest,
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
  constructor(private readonly identity: IdentityService) {}

  @Get()
  @UseGuards(AuthenticationGuard)
  getAccount(@CurrentPrincipal() principal: AuthenticatedPrincipal): AccountSummary {
    return principal.account;
  }

  @Delete()
  @UseGuards(AuthenticationGuard)
  @Header('Cache-Control', 'no-store')
  @Throttle({ default: { limit: 3, ttl: 60 * 60_000 } })
  deleteAccount(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Body() body: unknown,
  ): Promise<AccountDeletionResult> {
    const request = parseDeleteAccountRequest(body);
    return this.identity.deleteAccount(principal.userId, request.password);
  }
}

@Controller()
export class PublicAccountDeletionController {
  constructor(private readonly identity: IdentityService) {}

  @Get('account-deletion')
  @Header('Content-Type', 'text/html; charset=utf-8')
  @Header('Cache-Control', 'no-store')
  deletionPage(): string {
    return accountDeletionPage();
  }

  @Get('account-deletion.css')
  @Header('Content-Type', 'text/css; charset=utf-8')
  @Header('Cache-Control', 'public, max-age=3600')
  deletionStyles(): string {
    return accountDeletionStyles;
  }

  @Post('account-deletion')
  @Header('Content-Type', 'text/html; charset=utf-8')
  @Header('Cache-Control', 'no-store')
  @Throttle({ default: { limit: 3, ttl: 60 * 60_000 } })
  async deleteFromWeb(@Body() body: unknown): Promise<string> {
    const request = parsePublicDeleteAccountRequest(body);
    await this.identity.deleteAccountByCredentials(request.email, request.password);
    return accountDeletionPage(
      'Your SharedHouse account was deleted. You can now close this page.',
    );
  }
}

function accountDeletionPage(message?: string): string {
  const notice = message === undefined ? '' : `<p role="status" class="success">${message}</p>`;
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Delete SharedHouse account</title><link rel="stylesheet" href="/account-deletion.css"></head><body><main><section><small>SharedHouse privacy</small><h1>Delete your account</h1><p>This permanently removes sign-in credentials and anonymises your profile. A household is closed automatically when you are its only active member. If other members remain, transfer ownership or remove them first. Store subscriptions must be cancelled separately in Google Play.</p>${notice}<form method="post" action="/account-deletion"><label>Email<input name="email" type="email" autocomplete="email" required maxlength="254"></label><label>Current password<input name="password" type="password" autocomplete="current-password" required maxlength="128"></label><label>Type DELETE<input name="confirmation" required pattern="DELETE" maxlength="6"></label><button type="submit">Permanently delete account</button></form></section></main></body></html>`;
}

const accountDeletionStyles = `body{margin:0;background:#f2f5f2;color:#17231d;font:16px system-ui,sans-serif}main{max-width:38rem;margin:auto;padding:3rem 1.25rem}section{background:#fff;border:1px solid #cbd5cd;border-radius:24px;padding:clamp(1.5rem,5vw,2.5rem);box-shadow:0 18px 50px #17372214}h1{font-size:clamp(2rem,8vw,3.5rem);line-height:1;margin:.4rem 0 1rem}p{line-height:1.6;color:#435248}label{display:grid;gap:.4rem;margin:1rem 0;font-weight:650}input{font:inherit;padding:.9rem 1rem;border:1px solid #738078;border-radius:12px}button{width:100%;margin-top:1rem;padding:1rem;border:0;border-radius:14px;background:#8b1e2d;color:#fff;font:700 1rem system-ui}.success{padding:1rem;border-radius:12px;background:#dcefe1;color:#163d24}`;
