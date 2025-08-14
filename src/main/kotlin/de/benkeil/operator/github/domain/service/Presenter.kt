package de.benkeil.operator.github.domain.service

interface Presenter<UseCaseResult, Result> {
  fun ok(status: UseCaseResult): Result

  fun error(error: Throwable): Result
}
