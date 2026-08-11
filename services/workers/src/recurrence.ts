export type ExpenseCadence = 'weekly' | 'monthly' | 'quarterly' | 'yearly';

export function nextOccurrenceDate(
  currentDate: string,
  cadence: ExpenseCadence,
  anchorDay: number,
  anchorMonth: number,
): string {
  const current = parseDate(currentDate);
  assertIntegerInRange(anchorDay, 1, 31, 'anchorDay');
  assertIntegerInRange(anchorMonth, 1, 12, 'anchorMonth');

  if (cadence === 'weekly') {
    const value = new Date(Date.UTC(current.year, current.month - 1, current.day + 7));
    return formatDate(value.getUTCFullYear(), value.getUTCMonth() + 1, value.getUTCDate());
  }

  if (cadence === 'yearly') {
    return anchoredDate(current.year + 1, anchorMonth, anchorDay);
  }

  const addedMonths = cadence === 'monthly' ? 1 : 3;
  const zeroBasedTargetMonth = current.year * 12 + current.month - 1 + addedMonths;
  const targetYear = Math.floor(zeroBasedTargetMonth / 12);
  const targetMonth = (zeroBasedTargetMonth % 12) + 1;
  return anchoredDate(targetYear, targetMonth, anchorDay);
}

export function equalAllocationMinorUnits(
  totalMinor: string | number | bigint,
  memberCount: number,
): readonly bigint[] {
  if (!Number.isSafeInteger(memberCount) || memberCount < 1) {
    throw new Error('memberCount must be a positive integer.');
  }
  const total = BigInt(totalMinor);
  if (total < 1n) throw new Error('totalMinor must be positive.');
  const count = BigInt(memberCount);
  const base = total / count;
  const remainder = total % count;
  return Array.from(
    { length: memberCount },
    (_, index) => base + (BigInt(index) < remainder ? 1n : 0n),
  );
}

function anchoredDate(year: number, month: number, anchorDay: number): string {
  const finalDay = Math.min(anchorDay, daysInMonth(year, month));
  return formatDate(year, month, finalDay);
}

function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

function parseDate(value: string): {
  readonly year: number;
  readonly month: number;
  readonly day: number;
} {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/u.exec(value);
  if (match === null) throw new Error(`Invalid local date: ${value}`);
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const reconstructed = formatDate(year, month, day);
  if (
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > daysInMonth(year, month) ||
    reconstructed !== value
  ) {
    throw new Error(`Invalid local date: ${value}`);
  }
  return { year, month, day };
}

function formatDate(year: number, month: number, day: number): string {
  return `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day
    .toString()
    .padStart(2, '0')}`;
}

function assertIntegerInRange(value: number, minimum: number, maximum: number, name: string): void {
  if (!Number.isSafeInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} must be between ${minimum.toString()} and ${maximum.toString()}.`);
  }
}
