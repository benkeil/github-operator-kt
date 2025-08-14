package de.benkeil.operator.github.domain

import de.benkeil.operator.github.application.kubernetes.GitHubRepositoryResource
import de.benkeil.operator.github.application.kubernetes.GitHubRepositoryStatus
import de.benkeil.operator.github.domain.model.AutoLink
import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.model.RuleSet
import de.benkeil.operator.github.domain.service.AutoLinkRequest
import de.benkeil.operator.github.domain.service.AutoLinkResponse
import de.benkeil.operator.github.domain.service.CollaboratorRequest
import de.benkeil.operator.github.domain.service.CreateGitHubRepositoryRequest
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.Presenter
import de.benkeil.operator.github.domain.service.TeamPermissionRequest
import de.benkeil.operator.github.domain.service.UpdateGitHubRepositoryRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class UpsertRepositoryUseCase(
    private val gitHubService: GitHubService,
) {
  private val logger = KotlinLogging.logger {}

  suspend fun <T> execute(
      controller: () -> GitHubRepositoryResource,
      presenter: Presenter<GitHubRepositoryStatus, T>
  ): T {
    val resource = controller()
    val repository = resource.spec

    val owner = repository.owner
    val name = resource.name

    if (gitHubService.getRepository(owner, name) == null) {
      gitHubService.createRepository(resource.toCreateGitHubRepositoryRequest())
      // TODO poll and wait until the repository is created
      delay(5.seconds)
      gitHubService.upsertTeamPermission(
          owner,
          name,
          TeamPermissionRequest(
              organization = owner,
              slug = repository.ownerTeam,
              role = repository.ownerRole,
          ),
      )
    }

    gitHubService.updateRepository(resource.toUpdateGitHubRepositoryRequest())

    val existingTeamPermission = gitHubService.getTeamPermissions(owner, name)
    repository.teamPermissions
        ?.map { (slug, role) ->
          Permission(
              slug = slug,
              role = role,
              delete = false,
          )
        }
        ?.filter { repository.ownerTeam != it.slug }
        ?.forEach { teamPermission ->
          when (teamPermission.delete) {
            true -> {
              logger.info {
                "Remove team permission for ${teamPermission.slug} with role ${teamPermission.role}"
              }
              gitHubService.deleteTeamPermission(owner, name, teamPermission.slug)
            }
            false -> {
              logger.info {
                "Setting team permission for ${teamPermission.slug} with role ${teamPermission.role}"
              }
              gitHubService.upsertTeamPermission(
                  owner,
                  name,
                  TeamPermissionRequest(
                      organization = owner,
                      slug = teamPermission.slug,
                      role = teamPermission.role,
                  ),
              )
            }
          }
        }

    val existingCollaborators = gitHubService.getCollaborators(owner, name)
    repository.collaborators
        ?.map { (slug, role) ->
          Permission(
              slug = slug,
              role = role,
              delete = false,
          )
        }
        ?.forEach { collaborator ->
          when (collaborator.delete) {
            true -> gitHubService.deleteCollaborator(owner, name, collaborator.slug)
            false ->
                gitHubService.upsertCollaborators(
                    owner,
                    name,
                    CollaboratorRequest(
                        login = collaborator.slug,
                        role = collaborator.role,
                    ),
                )
          }
        }

    when (repository.automatedSecurityFixes) {
      true -> gitHubService.enableAutomatedSecurityFixes(owner, name)
      false -> gitHubService.disableAutomatedSecurityFixes(owner, name)
      null -> Unit
    }

    val existingAutoLinks = gitHubService.getAutoLinks(owner, name)
    val existingAutoLinkKeys = existingAutoLinks.map { it.keyPrefix }
    repository.autoLinks
        ?.map {
          AutoLink(
              keyPrefix = it.keyPrefix,
              urlTemplate = it.urlTemplate,
              isAlphanumeric = it.isAlphanumeric,
              delete = false,
          )
        }
        ?.forEach { autoLink ->
          when (autoLink.delete) {
            true ->
                if (existingAutoLinkKeys.contains(autoLink.keyPrefix)) {
                  gitHubService.deleteAutoLink(
                      owner,
                      name,
                      existingAutoLinks.firstByKeyPrefix(autoLink.keyPrefix).id,
                  )
                }

            false ->
                if (!existingAutoLinkKeys.contains(autoLink.keyPrefix)) {
                  gitHubService.createAutoLink(
                      owner = owner,
                      name = name,
                      autoLink =
                          AutoLinkRequest(
                              keyPrefix = autoLink.keyPrefix,
                              urlTemplate = autoLink.urlTemplate,
                              isAlphanumeric = autoLink.isAlphanumeric,
                          ),
                  )
                }
          }
        }

    val existingRuleSets = gitHubService.getRuleSets(owner, name)
    println("existingRuleSets: $existingRuleSets")
    repository.rulesets
        ?.map {
          RuleSet(
              name = it.name,
              target = it.target,
              enforcement = it.enforcement,
              conditions = it.conditions,
              rules = it.rules,
              delete = false,
          )
        }
        ?.forEach { ruleSet ->
          when (ruleSet.delete) {
            true ->
                existingRuleSets
                    .firstOrNull { it.name == ruleSet.name }
                    ?.also { gitHubService.deleteRuleSet(owner, name, it.id) }
            false -> {
              val existingRuleSet = existingRuleSets.firstOrNull { it.name == ruleSet.name }
              if (existingRuleSet != null) {
                gitHubService.updateRuleSet(
                    owner = owner,
                    name = name,
                    id = existingRuleSet.id,
                    ruleSet = ruleSet.toRuleSetRequest(),
                )
              } else {
                gitHubService.createRuleSet(
                    owner = owner,
                    name = name,
                    ruleSet = ruleSet.toRuleSetRequest(),
                )
              }
            }
          }
        }

    return presenter.ok(GitHubRepositoryStatus(updatedAt = OffsetDateTime.now()))
  }
}

val GitHubRepositoryResource.name: String
  get() = spec.fullName ?: "${metadata.namespace}_${metadata.name}"

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

fun List<AutoLinkResponse>.firstByKeyPrefix(keyPrefix: String) = first { it.keyPrefix == keyPrefix }

fun RuleSet.toRuleSetRequest(): de.benkeil.operator.github.domain.service.RuleSetRequest {
  return de.benkeil.operator.github.domain.service.RuleSetRequest(
      name = name,
      target = target,
      enforcement = enforcement,
      conditions = conditions,
      rules = rules,
  )
}
