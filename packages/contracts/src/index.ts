export const API_VERSION = 'v1' as const;

export interface ServiceHealth {
  readonly status: 'ok';
  readonly service: 'api' | 'workers';
  readonly apiVersion: typeof API_VERSION;
  readonly checkedAt: string;
}

export interface ProblemDetails {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly code: string;
  readonly correlationId: string;
  readonly detail?: string;
  readonly violations?: readonly {
    readonly field: string;
    readonly message: string;
  }[];
}

export interface Money {
  readonly minorUnits: number;
  readonly currency: string;
}
