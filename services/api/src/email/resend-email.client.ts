import { Injectable } from '@nestjs/common';

import { readApiEnvironment } from '../config/environment.js';
import type { ClaimedVerificationEmail } from './verification-email.types.js';

const RESEND_EMAIL_ENDPOINT = 'https://api.resend.com/emails';

export class EmailDeliveryError extends Error {
  constructor(
    readonly safeCode: string,
    readonly retryable: boolean,
  ) {
    super(safeCode);
    this.name = 'EmailDeliveryError';
  }
}

@Injectable()
export class ResendEmailClient {
  private readonly apiKey: string | null;
  private readonly from: string | null;

  constructor() {
    const environment = readApiEnvironment(process.env);
    this.apiKey = environment.resendApiKey;
    this.from = environment.emailFrom;
  }

  async sendVerification(message: ClaimedVerificationEmail, code: string): Promise<string> {
    if (this.apiKey === null || this.from === null) {
      throw new EmailDeliveryError('email_provider_unconfigured', false);
    }

    let response: Response;
    try {
      response = await fetch(RESEND_EMAIL_ENDPOINT, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${this.apiKey}`,
          'Content-Type': 'application/json',
          'Idempotency-Key': message.id,
        },
        body: JSON.stringify({
          from: this.from,
          to: [message.recipientEmail],
          subject:
            message.locale === 'ro'
              ? 'Codul tău de verificare SharedHouse'
              : 'Your SharedHouse verification code',
          text: verificationText(message.locale, code),
          html: verificationHtml(message.locale, code),
          tags: [{ name: 'message_type', value: 'email_verification' }],
        }),
        signal: AbortSignal.timeout(10_000),
      });
    } catch {
      throw new EmailDeliveryError('resend_network_error', true);
    }

    if (!response.ok) {
      throw new EmailDeliveryError(
        `resend_http_${String(response.status)}`,
        response.status === 408 || response.status === 429 || response.status >= 500,
      );
    }

    const payload: unknown = await response.json();
    if (!isProviderResponse(payload)) {
      throw new EmailDeliveryError('resend_invalid_response', true);
    }
    return payload.id;
  }
}

function verificationText(locale: ClaimedVerificationEmail['locale'], code: string): string {
  return locale === 'ro'
    ? `Codul tău SharedHouse este ${code}. Expiră în 15 minute. Dacă nu ai creat acest cont, ignoră mesajul.`
    : `Your SharedHouse code is ${code}. It expires in 15 minutes. If you did not create this account, ignore this message.`;
}

function verificationHtml(locale: ClaimedVerificationEmail['locale'], code: string): string {
  const heading = locale === 'ro' ? 'Confirmă adresa de email' : 'Confirm your email address';
  const instruction =
    locale === 'ro'
      ? 'Introdu acest cod în aplicația SharedHouse:'
      : 'Enter this code in the SharedHouse app:';
  const expiry =
    locale === 'ro'
      ? 'Codul expiră în 15 minute. Dacă nu ai creat acest cont, ignoră mesajul.'
      : 'The code expires in 15 minutes. If you did not create this account, ignore this message.';

  return `<!doctype html><html><body style="margin:0;background:#f4f7f4;color:#17251c;font-family:Arial,sans-serif"><div style="max-width:560px;margin:0 auto;padding:32px 20px"><div style="background:#ffffff;border:1px solid #d8e2da;border-radius:20px;padding:32px"><h1 style="font-size:24px;margin:0 0 16px">${heading}</h1><p style="font-size:16px;line-height:1.5">${instruction}</p><div style="font-size:34px;font-weight:700;letter-spacing:8px;color:#174f35;padding:20px 0">${code}</div><p style="font-size:14px;line-height:1.5;color:#536158">${expiry}</p></div></div></body></html>`;
}

function isProviderResponse(value: unknown): value is { readonly id: string } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    typeof value.id === 'string' &&
    value.id.length > 0 &&
    value.id.length <= 128
  );
}
