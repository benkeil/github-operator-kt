package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.GitHubRepositoryResource
import de.benkeil.operator.github.domain.model.GitHubRepositoryStatus
import de.benkeil.operator.github.domain.service.CreateGitHubRepositoryRequest
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.Presenter
import de.benkeil.operator.github.domain.service.UpdateGitHubRepositoryRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class UpsertRepositoryUseCase(
    private val gitHubService: GitHubService,
    private val upsertTeamsUseCase: UpsertTeamsUseCase,
    private val upsertCollaboratorsUseCase: UpsertCollaboratorsUseCase,
    private val updateAutomatedSecurityFixesUseCase: UpdateAutomatedSecurityFixesUseCase,
    private val upsertAutoLinksUseCase: UpsertAutoLinksUseCase,
    private val upsertRuleSetsUseCase: UpsertRuleSetsUseCase,
) {
  private val logger = KotlinLogging.logger {}

  companion object {
    const val ENVIRONMENT = "github-operator"
    const val MANAGED_BY_VARIABLE = "MANGED_BY_NAMESPACE"
  }

  suspend fun <T> execute(
      controller: () -> GitHubRepositoryResource,
      presenter: Presenter<GitHubRepositoryStatus, T>,
  ): T {
    val resource = controller()
    val spec = resource.spec
    val status = resource.status ?: GitHubRepositoryStatus(createdAt = OffsetDateTime.now())
    val owner = spec.owner
    val name = spec.name

    logger.info { "Get repository $owner/$name" }
    val repository = gitHubService.getRepository(owner, name)
    if (repository == null) {
      logger.info { "Repository $owner/$name does not exist, creating it." }
      gitHubService.createRepository(resource.toCreateGitHubRepositoryRequest())
      waitForCreation(owner, name)
      gitHubService.createEnvironment(owner, name, ENVIRONMENT)
      gitHubService.createEnvironmentVariable(
          owner,
          name,
          ENVIRONMENT,
          MANAGED_BY_VARIABLE,
          resource.metadata.namespace,
      )
    }

    val managedByNamespace =
        gitHubService.getEnvironmentVariable(owner, name, ENVIRONMENT, MANAGED_BY_VARIABLE)
    logger.info { "Repository must be managed by namespace $managedByNamespace" }
    if (managedByNamespace != resource.metadata.namespace) {
      error("Proof of ownership failed")
    }

    // TODO check if different from existing repository
    gitHubService.updateRepository(resource.toUpdateGitHubRepositoryRequest())

    upsertTeamsUseCase.execute {
      UpsertTeamsUseCase.Input(
          owner = owner,
          name = name,
          teams = spec.teams,
      )
    }

    upsertCollaboratorsUseCase.execute {
      UpsertCollaboratorsUseCase.Input(
          owner = owner,
          name = name,
          collaborators = spec.collaborators,
      )
    }

    updateAutomatedSecurityFixesUseCase.execute {
      UpdateAutomatedSecurityFixesUseCase.Input(
          owner = owner,
          name = name,
          enabled = spec.automatedSecurityFixes,
      )
    }

    upsertAutoLinksUseCase.execute {
      UpsertAutoLinksUseCase.Input(
          owner = owner,
          name = name,
          autoLinks = spec.autoLinks,
      )
    }

    upsertRuleSetsUseCase.execute {
      UpsertRuleSetsUseCase.Input(
          owner = owner,
          name = name,
          rulesets = spec.rulesets,
      )
    }

    status.updatedAt = OffsetDateTime.now()
    return presenter.ok(status)
  }

  suspend fun waitForCreation(owner: String, name: String) {
    do {
      delay(5.seconds)
    } while (gitHubService.getRepository(owner, name)?.defaultBranch == null)
  }
}

fun GitHubRepositoryResource.toCreateGitHubRepositoryRequest(): CreateGitHubRepositoryRequest =
    with(spec) {
      CreateGitHubRepositoryRequest(
          owner = owner,
          name = name,
          description = description,
          private = private,
          visibility = visibility,
          autoInit = autoInit,
          defaultBranch = defaultBranch,
      )
    }

fun GitHubRepositoryResource.toUpdateGitHubRepositoryRequest(): UpdateGitHubRepositoryRequest =
    with(spec) {
      UpdateGitHubRepositoryRequest(
          owner = owner,
          name = name,
          description = description,
          private = private,
          visibility = visibility,
          autoInit = autoInit,
          defaultBranch = defaultBranch,
          deleteBranchOnMerge = deleteBranchOnMerge,
          allowAutoMerge = allowAutoMerge,
          allowSquashMerge = allowSquashMerge,
          allowMergeCommit = allowMergeCommit,
          allowRebaseMerge = allowRebaseMerge,
          allowUpdateBranch = allowUpdateBranch,
          useSquashPrTitleAsDefault = useSquashPrTitleAsDefault,
          squashMergeCommitTitle = squashMergeCommitTitle,
          squashMergeCommitMessage = squashMergeCommitMessage,
          mergeCommitTitle = mergeCommitTitle,
          mergeCommitMessage = mergeCommitMessage,
          securityAndAnalysis = securityAndAnalysis,
      )
    }
