import { Module } from '@nestjs/common';

import { AuthenticationGuard } from '../security/authentication.guard.js';
import { EmailModule } from '../email/email.module.js';
import { PasswordService } from '../security/password.service.js';
import { TokenService } from '../security/token.service.js';
import { AccountController, AuthController } from './auth.controller.js';
import { IdentityRepository } from './identity.repository.js';
import { IdentityService } from './identity.service.js';

@Module({
  imports: [EmailModule],
  controllers: [AuthController, AccountController],
  providers: [
    IdentityRepository,
    IdentityService,
    PasswordService,
    TokenService,
    AuthenticationGuard,
  ],
  exports: [IdentityRepository, TokenService, AuthenticationGuard],
})
export class IdentityModule {}
