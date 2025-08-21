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
    private val upsertTeamUseCase: UpsertTeamUseCase,
    private val upsertCollaboratorsUseCase: UpsertCollaboratorsUseCase,
    private val updateAutomatedSecurityFixesUseCase: UpdateAutomatedSecurityFixesUseCase,
    private val upsertAutoLinksUseCase: UpsertAutoLinksUseCase,
    private val upsertRuleSetsUseCase: UpsertRuleSetsUseCase,
) {
  private val logger = KotlinLogging.logger {}

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
      // TODO poll and wait until the repository is created
      delay(5.seconds)
    }

    // TODO check if different from existing repository
    gitHubService.updateRepository(resource.toUpdateGitHubRepositoryRequest())

    upsertTeamUseCase.execute {
      UpsertTeamUseCase.Input(
          owner = owner,
          name = name,
          teamPermissions = spec.teams,
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
