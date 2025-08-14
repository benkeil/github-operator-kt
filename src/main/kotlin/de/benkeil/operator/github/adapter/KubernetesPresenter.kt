package de.benkeil.operator.github.adapter

import de.benkeil.operator.github.application.kubernetes.GitHubRepositoryStatus
import de.benkeil.operator.github.domain.service.Presenter

class KubernetesPresenter : Presenter<GitHubRepositoryStatus, GitHubRepositoryStatus> {
  override fun ok(status: GitHubRepositoryStatus): GitHubRepositoryStatus {
    return status
  }

  override fun error(error: Throwable): GitHubRepositoryStatus {
    return GitHubRepositoryStatus(error = error.message ?: "Unknown error, see log for details")
  }
}
