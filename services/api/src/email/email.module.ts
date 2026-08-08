import { Module } from '@nestjs/common';

import { ResendEmailClient } from './resend-email.client.js';
import { VerificationEmailCodec } from './verification-email-codec.js';
import { VerificationEmailDispatcher } from './verification-email.dispatcher.js';

@Module({
  providers: [VerificationEmailCodec, ResendEmailClient, VerificationEmailDispatcher],
  exports: [VerificationEmailCodec],
})
export class EmailModule {}
