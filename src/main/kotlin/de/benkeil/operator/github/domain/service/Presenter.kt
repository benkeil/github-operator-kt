package de.benkeil.operator.github.domain.service

interface Presenter<UseCaseResult, Result> {
  fun ok(status: UseCaseResult): Result

  fun error(status: UseCaseResult, error: Throwable): Result
}
