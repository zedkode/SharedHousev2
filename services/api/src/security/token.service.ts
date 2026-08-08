import { Injectable } from '@nestjs/common';
import { createHash, randomBytes, randomInt } from 'node:crypto';

@Injectable()
export class TokenService {
  createAccessToken(): string {
    return `sh_at_${randomBytes(32).toString('base64url')}`;
  }

  createRefreshToken(): string {
    return `sh_rt_${randomBytes(48).toString('base64url')}`;
  }

  createVerificationCode(): string {
    return randomInt(0, 100_000_000).toString().padStart(8, '0');
  }

  createInvitationToken(): string {
    return `sh_inv_${randomBytes(32).toString('base64url')}`;
  }

  hash(value: string): string {
    return createHash('sha256').update(value, 'utf8').digest('hex');
  }
}
