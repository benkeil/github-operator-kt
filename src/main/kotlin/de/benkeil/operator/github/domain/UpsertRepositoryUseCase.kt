package de.benkeil.operator.github.domain

import de.benkeil.operator.github.application.kubernetes.GitHubRepositoryResource
import de.benkeil.operator.github.application.kubernetes.GitHubRepositoryStatus
import de.benkeil.operator.github.domain.service.AutoLinkRequest
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
      presenter: Presenter<GitHubRepositoryStatus, T>,
  ): T {
    val resource = controller()
    val repository = resource.spec
    val status = resource.status ?: GitHubRepositoryStatus(createdAt = OffsetDateTime.now())

    val owner = repository.owner
    val name = repository.name

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

    /*
     * Repository
     */
    gitHubService.updateRepository(resource.toUpdateGitHubRepositoryRequest())

    /*
     * Additional Team permissions
     */
    status.teamPermissionSlugs
        .filter { !repository.teamPermissions.containsKey(it) }
        .forEach {
          logger.info { "Deleting team permission for $it" }
          gitHubService.deleteTeamPermission(owner, name, owner, it)
        }
    repository.teamPermissions
        .filter { (slug, role) -> repository.ownerTeam != slug }
        .map { (slug, role) ->
          logger.info { "Setting team permission for $slug with role $role" }
          gitHubService.upsertTeamPermission(
              owner,
              name,
              TeamPermissionRequest(organization = owner, slug = slug, role = role),
          )
          slug
        }
        .also { list -> status.teamPermissionSlugs = list.toSet() }

    /*
     * Adding collaborators
     */
    status.collaboratorLogins
        .filter { !repository.collaborators.containsKey(it) }
        .forEach {
          logger.info { "Deleting collaborator $it" }
          gitHubService.deleteCollaborator(owner, name, it)
        }
    val existingCollaborators = gitHubService.getCollaborators(owner, name).toSet()
    repository.collaborators
        .map { (login, role) ->
          logger.info { "Setting collaborator $login with role $role" }
          gitHubService.upsertCollaborators(
              owner, name, CollaboratorRequest(login = login, role = role))
              ?: existingCollaborators.first { it.login == login }
        }
        .also { list -> status.collaboratorLogins = list.map { it.login }.toSet() }

    /*
     * Advances Security
     */
    when (repository.automatedSecurityFixes) {
      true -> {
        logger.info { "Enabling automated security fixes" }
        gitHubService.enableAutomatedSecurityFixes(owner, name)
      }
      false -> {
        logger.info { "Disabling automated security fixes" }
        gitHubService.disableAutomatedSecurityFixes(owner, name)
      }
      null -> Unit
    }

    /*
     * Auto Links
     */
    status.autoLinkKeyPrefixes
        .filter { (keyPrefix) -> repository.autoLinks.none { it.keyPrefix == keyPrefix } }
        .forEach { (keyPrefix, id) ->
          logger.info { "Deleting auto link for key prefix $keyPrefix" }
          gitHubService.deleteAutoLink(owner, name, id)
        }
    val existingAutoLinks = gitHubService.getAutoLinks(owner, name)
    val createdAutoLinks =
        repository.autoLinks
            .filter { autolink -> existingAutoLinks.none { it.keyPrefix == autolink.keyPrefix } }
            .map {
              gitHubService.createAutoLink(
                  owner = owner,
                  name = name,
                  autoLink =
                      AutoLinkRequest(
                          keyPrefix = it.keyPrefix,
                          urlTemplate = it.urlTemplate,
                          isAlphanumeric = it.isAlphanumeric,
                      ),
              )
            }
    status.autoLinkKeyPrefixes =
        createdAutoLinks.plus(existingAutoLinks).associate { it.keyPrefix to it.id }

    val existingRuleSets = gitHubService.getRuleSets(owner, name)
    status.ruleSetNames
        .filter { (name) -> repository.rulesets.none { it.name == name } }
        .forEach { (name, id) ->
          logger.info { "Deleting rule set $name with id $id" }
          gitHubService.deleteRuleSet(owner, name, id)
        }
    repository.rulesets
        .map { ruleSet ->
          val existingRuleSet = existingRuleSets.firstOrNull { it.name == ruleSet.name }
          if (existingRuleSet != null) {
            logger.info { "Updating rule set ${ruleSet.name} with id ${existingRuleSet.id}" }
            gitHubService.updateRuleSet(
                owner = owner, name = name, id = existingRuleSet.id, ruleSet = ruleSet)
          } else {
            logger.info { "Creating rule set ${ruleSet.name}" }
            gitHubService.createRuleSet(owner = owner, name = name, ruleSet = ruleSet)
          }
        }
        .also { list -> status.ruleSetNames = list.associate { it.name to it.id } }

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
