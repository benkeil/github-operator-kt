package de.benkeil.operator.github.adapter

import de.benkeil.operator.github.application.kubernetes.GitHubRepositoryStatus
import de.benkeil.operator.github.domain.service.Presenter

class KubernetesPresenter : Presenter<GitHubRepositoryStatus, GitHubRepositoryStatus> {
  override fun ok(status: GitHubRepositoryStatus): GitHubRepositoryStatus = status

  override fun error(status: GitHubRepositoryStatus, error: Throwable): GitHubRepositoryStatus =
      status.apply { errors.add(error.message ?: "Unknown error, see log for details") }
}
