import { HttpException } from '@nestjs/common';

export interface FieldViolation {
  readonly field: string;
  readonly message: string;
}

export interface ApiProblemDefinition {
  readonly status: number;
  readonly code: string;
  readonly title: string;
  readonly detail?: string;
  readonly violations?: readonly FieldViolation[];
}

export class ApiProblemException extends HttpException {
  constructor(readonly problem: ApiProblemDefinition) {
    super(problem.title, problem.status);
  }
}

export function validationProblem(violations: readonly FieldViolation[]): ApiProblemException {
  return new ApiProblemException({
    status: 400,
    code: 'VALIDATION_FAILED',
    title: 'The request contains invalid fields.',
    violations,
  });
}
