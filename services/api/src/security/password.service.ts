import { Injectable } from '@nestjs/common';
import * as crypto from 'node:crypto';

export type PasswordAlgorithm = 'argon2id-v1' | 'scrypt-v1';

export interface PasswordCredential {
  readonly algorithm: PasswordAlgorithm;
  readonly saltBase64: string;
  readonly hashBase64: string;
}

const COMMON_PASSWORDS = new Set([
  '123456789012345',
  'correcthorsebatterystaple',
  'letmeinletmeinletmein',
  'passwordpassword',
  'qwertyqwertyqwerty',
]);

@Injectable()
export class PasswordService {
  private readonly dummyCredential = this.hashPassword(
    crypto.randomBytes(48).toString('base64url'),
  );

  validatePolicy(password: string): readonly string[] {
    const length = Array.from(password).length;
    const violations: string[] = [];

    if (length < 15) {
      violations.push('Use at least 15 characters.');
    }
    if (length > 128) {
      violations.push('Use at most 128 characters.');
    }
    if (COMMON_PASSWORDS.has(password.toLocaleLowerCase('en-US'))) {
      violations.push('Choose a less common password or passphrase.');
    }

    return violations;
  }

  async hashPassword(password: string): Promise<PasswordCredential> {
    const salt = crypto.randomBytes(16);

    if (typeof crypto.argon2 === 'function') {
      const hash = await new Promise<Buffer>((resolve, reject) => {
        crypto.argon2(
          'argon2id',
          {
            message: Buffer.from(password, 'utf8'),
            nonce: salt,
            parallelism: 4,
            tagLength: 32,
            memory: 65_536,
            passes: 3,
          },
          (error, derivedKey) => {
            if (error === null) {
              resolve(derivedKey);
            } else {
              reject(error);
            }
          },
        );
      });

      return {
        algorithm: 'argon2id-v1',
        saltBase64: salt.toString('base64'),
        hashBase64: hash.toString('base64'),
      };
    }

    const hash = await scryptPassword(password, salt);
    return {
      algorithm: 'scrypt-v1',
      saltBase64: salt.toString('base64'),
      hashBase64: hash.toString('base64'),
    };
  }

  async verifyPassword(password: string, credential: PasswordCredential): Promise<boolean> {
    const salt = Buffer.from(credential.saltBase64, 'base64');
    const expected = Buffer.from(credential.hashBase64, 'base64');
    const actual =
      credential.algorithm === 'argon2id-v1'
        ? await argon2Password(password, salt)
        : await scryptPassword(password, salt);

    return expected.length === actual.length && crypto.timingSafeEqual(expected, actual);
  }

  async consumeDummyVerification(password: string): Promise<void> {
    await this.verifyPassword(password, await this.dummyCredential);
  }
}

async function argon2Password(password: string, salt: Buffer): Promise<Buffer> {
  if (typeof crypto.argon2 !== 'function') {
    throw new Error('This credential requires Node.js Argon2 support.');
  }

  return new Promise<Buffer>((resolve, reject) => {
    crypto.argon2(
      'argon2id',
      {
        message: Buffer.from(password, 'utf8'),
        nonce: salt,
        parallelism: 4,
        tagLength: 32,
        memory: 65_536,
        passes: 3,
      },
      (error, derivedKey) => {
        if (error === null) {
          resolve(derivedKey);
        } else {
          reject(error);
        }
      },
    );
  });
}

async function scryptPassword(password: string, salt: Buffer): Promise<Buffer> {
  return new Promise<Buffer>((resolve, reject) => {
    crypto.scrypt(
      password,
      salt,
      32,
      { N: 32_768, r: 8, p: 1, maxmem: 64 * 1024 * 1024 },
      (error, derivedKey) => {
        if (error === null) {
          resolve(derivedKey);
        } else {
          reject(error);
        }
      },
    );
  });
}
