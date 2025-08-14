package de.benkeil.operator.github.application.kubernetes

import de.benkeil.operator.github.adapter.HttpGitHubService
import de.benkeil.operator.github.adapter.KubernetesPresenter
import de.benkeil.operator.github.domain.UpsertRepositoryUseCase
import de.benkeil.operator.github.domain.model.GitHubRepositoryResource
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.reconciler.BaseControl
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl
import io.javaoperatorsdk.operator.monitoring.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking

fun main() {
  val gitHubToken = System.getenv("GITHUB_TOKEN")
  val gitHubService = HttpGitHubService(gitHubToken)
  val useCase = UpsertRepositoryUseCase(gitHubService)
  val presenter = KubernetesPresenter()
  val reconciler = GitHubRepositoryReconciler(useCase, presenter)

  val operator = Operator {
    it.withMetrics(MicrometerMetrics.withoutPerResourceMetrics(LoggingMeterRegistry()))
  }
  operator.register(reconciler)
  operator.start()

  // com.sun.net.httpserver.HttpServer
  // HttpServer.create(InetSocketAddress(8080), 0)
}

class GitHubRepositoryReconciler(
    val useCase: UpsertRepositoryUseCase,
    val presenter: KubernetesPresenter,
) : Reconciler<GitHubRepositoryResource> {

  override fun reconcile(
      resource: GitHubRepositoryResource,
      context: Context<GitHubRepositoryResource>
  ): UpdateControl<GitHubRepositoryResource> = runBlocking {
    val newStatus = useCase.execute({ resource }, presenter)
    resource.status = newStatus
    UpdateControl.patchStatus(resource).rescheduleAfter(12.hours)
  }
}

fun <T> BaseControl<T>.rescheduleAfter(duration: Duration): T where T : BaseControl<T> =
    rescheduleAfter(duration.toJavaDuration())
