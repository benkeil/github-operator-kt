package de.benkeil.operator.github.application.kubernetes

import de.benkeil.operator.github.adapter.HttpGitHubService
import de.benkeil.operator.github.adapter.KubernetesPresenter
import de.benkeil.operator.github.domain.UpsertRepositoryUseCase
import de.benkeil.operator.github.domain.model.AutoLink
import de.benkeil.operator.github.domain.model.Repository
import de.benkeil.operator.github.domain.model.RuleSet
import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.AutoLinkRequest
import de.benkeil.operator.github.domain.service.MergeCommitMessage
import de.benkeil.operator.github.domain.service.MergeCommitTitle
import de.benkeil.operator.github.domain.service.RuleSetRequest
import de.benkeil.operator.github.domain.service.SecurityAndAnalysis
import de.benkeil.operator.github.domain.service.SquashMergeCommitMessage
import de.benkeil.operator.github.domain.service.SquashMergeCommitTitle
import de.benkeil.operator.github.domain.service.Visibility
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Version
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.reconciler.BaseControl
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl
import io.javaoperatorsdk.operator.monitoring.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import java.time.OffsetDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking

fun main() {
  val gitHubToken = System.getenv("GITHUB_TOKEN")
  val gitHubService = HttpGitHubService(gitHubToken)
  val useCase = UpsertRepositoryUseCase(gitHubService)
  val reconciler = GitHubRepositoryReconciler(useCase)

  val operator = Operator {
    it.withMetrics(MicrometerMetrics.withoutPerResourceMetrics(LoggingMeterRegistry()))
  }
  operator.register(reconciler)
  operator.start()

  // com.sun.net.httpserver.HttpServer
  // HttpServer.create(InetSocketAddress(8080), 0)
}

class GitHubRepositoryReconciler(val useCase: UpsertRepositoryUseCase) :
    Reconciler<GitHubRepositoryResource> {
  companion object {
    val presenter = KubernetesPresenter()

    fun controller(schema: GitHubRepositoryResource): Repository = schema.toRepository()
  }

  override fun reconcile(
      resource: GitHubRepositoryResource,
      context: Context<GitHubRepositoryResource>
  ): UpdateControl<GitHubRepositoryResource> = runBlocking {
    val newStatus = useCase.execute({ controller(resource) }, presenter)
    resource.status = newStatus
    UpdateControl.patchStatus(resource).rescheduleAfter(12.hours)
  }
}

fun <T> BaseControl<T>.rescheduleAfter(duration: Duration): T where T : BaseControl<T> =
    rescheduleAfter(duration.toJavaDuration())

@Version("v1alpha1")
@Group("github.platform.benkeil.de")
class GitHubRepositoryResource :
    CustomResource<GitHubRepositorySpec, GitHubRepositoryStatus>(), Namespaced {}

data class GitHubRepositorySpec(
    val owner: String,
    val fullName: String? = null,
    val ownerTeam: String,
    val ownerRole: String = "admin",
    val description: String? = null,
    val private: Boolean? = null,
    val visibility: Visibility? = null,
    val autoInit: Boolean? = null,
    val deleteBranchOnMerge: Boolean? = null,
    val allowAutoMerge: Boolean? = null,
    val allowSquashMerge: Boolean? = null,
    val allowMergeCommit: Boolean? = null,
    val allowRebaseMerge: Boolean? = null,
    val allowUpdateBranch: Boolean? = null,
    val useSquashPrTitleAsDefault: Boolean? = null,
    val squashMergeCommitTitle: SquashMergeCommitTitle? = null,
    val squashMergeCommitMessage: SquashMergeCommitMessage? = null,
    val mergeCommitTitle: MergeCommitTitle? = null,
    val mergeCommitMessage: MergeCommitMessage? = null,
    val securityAndAnalysis: SecurityAndAnalysis? = null,
    val defaultBranch: String? = null,
    val automatedSecurityFixes: Boolean? = null,
    val autoLinks: List<AutoLinkRequest>? = null,
    val teamPermissions: Map<String, String>? = null,
    val collaborators: Map<String, String>? = null,
    val rulesets: List<RuleSetRequest>? = null,
)

data class GitHubRepositoryStatus(
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val autoLinkKeyPrefixes: List<String>? = null,
    val teamPermissionSlugs: List<String>? = null,
    val collaboratorLogins: List<String>? = null,
    val error: String? = null,
)

fun GitHubRepositoryResource.toRepository(): Repository =
    Repository(
        owner = spec.owner,
        ownerTeam = spec.ownerTeam,
        ownerRole = spec.ownerRole,
        name = "${metadata.namespace}_${metadata.name}",
        description = spec.description,
        private = spec.private,
        visibility = spec.visibility,
        autoInit = spec.autoInit,
        deleteBranchOnMerge = spec.deleteBranchOnMerge,
        allowAutoMerge = spec.allowAutoMerge,
        allowSquashMerge = spec.allowSquashMerge,
        allowMergeCommit = spec.allowMergeCommit,
        allowRebaseMerge = spec.allowRebaseMerge,
        allowUpdateBranch = spec.allowUpdateBranch,
        useSquashPrTitleAsDefault = spec.useSquashPrTitleAsDefault,
        squashMergeCommitTitle = spec.squashMergeCommitTitle,
        squashMergeCommitMessage = spec.squashMergeCommitMessage,
        mergeCommitTitle = spec.mergeCommitTitle,
        mergeCommitMessage = spec.mergeCommitMessage,
        securityAndAnalysis = spec.securityAndAnalysis,
        defaultBranch = spec.defaultBranch,
        automatedSecurityFixes = spec.automatedSecurityFixes ?: false,
        autoLinks =
            spec.autoLinks?.map {
              AutoLink(
                  keyPrefix = it.keyPrefix,
                  urlTemplate = it.urlTemplate,
                  isAlphanumeric = it.isAlphanumeric,
                  delete = false,
              )
            },
        teamPermissions =
            spec.teamPermissions?.map { (slug, role) ->
              Permission(
                  slug = slug,
                  role = role,
                  delete = false,
              )
            },
        collaborators =
            spec.collaborators?.map { (slug, role) ->
              Permission(
                  slug = slug,
                  role = role,
                  delete = false,
              )
            },
        rulesets =
            spec.rulesets?.map { ruleSetRequest ->
              RuleSet(
                  name = ruleSetRequest.name,
                  target = ruleSetRequest.target,
                  enforcement = ruleSetRequest.enforcement,
                  conditions = ruleSetRequest.conditions,
                  rules = ruleSetRequest.rules,
                  delete = false,
              )
            },
    )
