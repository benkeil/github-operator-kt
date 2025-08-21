package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.TeamPermissionRequest
import io.github.oshai.kotlinlogging.KotlinLogging

class UpsertTeamsUseCase(
    private val gitHubService: GitHubService,
) {
  private val logger = KotlinLogging.logger {}

  suspend fun execute(controller: () -> Input) {
    val spec = controller()
    val owner = spec.owner
    val name = spec.name
    val ignoredTeams = listOf("ec-security-champs", "githubsecurity", "security-monitoring")

    val existingTeamPermissions =
        gitHubService.getTeamPermissions(owner, name).filter { !ignoredTeams.contains(it.slug) }

    // Delete existing team that are not in the input
    existingTeamPermissions
        .filter { teamPermission -> spec.teams.none { it.name == teamPermission.slug } }
        .forEach { permission ->
          logger.info { "Deleting team permission ${permission.slug}" }
          gitHubService.deleteTeamPermission(owner, name, owner, permission.slug)
        }

    // Upsert team based on the input
    spec.teams
        .filter { permission ->
          existingTeamPermissions.none { it.slug == permission.name } ||
              existingTeamPermissions.any {
                it.slug == permission.name && it.permission != permission.permission
              }
        }
        .forEach { permission ->
          logger.info { "Upserting team permission ${permission.name}" }
          gitHubService.upsertTeamPermission(
              owner,
              name,
              TeamPermissionRequest(
                  organization = owner,
                  slug = permission.name,
                  role = permission.permission,
              ),
          )
        }
  }

  data class Input(
      val owner: String,
      val name: String,
      val teams: List<Permission>,
  )
}
