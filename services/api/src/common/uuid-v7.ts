import { randomBytes } from 'node:crypto';

export function newUuidV7(timestamp = Date.now()): string {
  if (!Number.isSafeInteger(timestamp) || timestamp < 0 || timestamp > 0xffff_ffff_ffff) {
    throw new Error('UUIDv7 timestamp is outside the 48-bit range.');
  }

  const bytes = randomBytes(16);
  let remaining = BigInt(timestamp);

  for (let index = 5; index >= 0; index -= 1) {
    bytes[index] = Number(remaining & 0xffn);
    remaining >>= 8n;
  }

  bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x70;
  bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80;

  const hex = bytes.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
