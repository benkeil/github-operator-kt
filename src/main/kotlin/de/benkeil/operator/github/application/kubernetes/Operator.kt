package de.benkeil.operator.github.application.kubernetes

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.benkeil.operator.github.adapter.HttpGitHubService
import de.benkeil.operator.github.adapter.KubernetesPresenter
import de.benkeil.operator.github.domain.UpdateAutomatedSecurityFixesUseCase
import de.benkeil.operator.github.domain.UpsertAutoLinksUseCase
import de.benkeil.operator.github.domain.UpsertCollaboratorsUseCase
import de.benkeil.operator.github.domain.UpsertRepositoryUseCase
import de.benkeil.operator.github.domain.UpsertRuleSetsUseCase
import de.benkeil.operator.github.domain.UpsertTeamsUseCase
import de.benkeil.operator.github.domain.model.GitHubRepositoryResource
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.config.ConfigurationService.DEFAULT_MAX_CONCURRENT_REQUEST
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
  val upsertTeamsUseCase = UpsertTeamsUseCase(gitHubService)
  val upsertCollaboratorsUseCase = UpsertCollaboratorsUseCase(gitHubService)
  val updateAutomatedSecurityFixesUseCase = UpdateAutomatedSecurityFixesUseCase(gitHubService)
  val upsertAutoLinksUseCase = UpsertAutoLinksUseCase(gitHubService)
  val upsertRuleSetsUseCase = UpsertRuleSetsUseCase(gitHubService)
  val useCase =
      UpsertRepositoryUseCase(
          gitHubService,
          upsertTeamsUseCase,
          upsertCollaboratorsUseCase,
          updateAutomatedSecurityFixesUseCase,
          upsertAutoLinksUseCase,
          upsertRuleSetsUseCase,
      )
  val presenter = KubernetesPresenter()
  val reconciler = GitHubRepositoryReconciler(useCase, presenter)
  val mapper: ObjectMapper =
      jacksonObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .registerModule(JavaTimeModule())
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)

  val operator = Operator {
    it.withMetrics(MicrometerMetrics.withoutPerResourceMetrics(LoggingMeterRegistry()))
    it.withKubernetesClient(
        KubernetesClientBuilder()
            .withConfig(
                ConfigBuilder(Config.autoConfigure(null))
                    .withMaxConcurrentRequests(DEFAULT_MAX_CONCURRENT_REQUEST)
                    .build())
            .withKubernetesSerialization(KubernetesSerialization(mapper, true))
            .build())
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
  private val logger = KotlinLogging.logger {}

  override fun reconcile(
      resource: GitHubRepositoryResource,
      context: Context<GitHubRepositoryResource>
  ): UpdateControl<GitHubRepositoryResource> = runBlocking {
    withLoggingContext(
        "namespace" to resource.metadata.namespace,
        "name" to resource.metadata.name,
        "repository" to resource.spec.name,
    ) {
      logger.info { "Reconciling ${resource.spec.name}" }
      val newStatus = useCase.execute({ resource }, presenter)
      resource.status = newStatus
      UpdateControl.patchStatus(resource).rescheduleAfter(12.hours).also {
        logger.info { "Successfully reconciled ${resource.spec.name}" }
      }
    }
  }
}

fun <T> BaseControl<T>.rescheduleAfter(duration: Duration): T where T : BaseControl<T> =
    rescheduleAfter(duration.toJavaDuration())
