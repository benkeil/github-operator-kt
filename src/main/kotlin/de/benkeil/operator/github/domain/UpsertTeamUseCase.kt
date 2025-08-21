package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.TeamPermissionRequest

class UpsertTeamUseCase(
    private val gitHubService: GitHubService,
) {
  suspend fun execute(controller: () -> Input) {
    val spec = controller()
    val owner = spec.owner
    val name = spec.name

    val existingTeamPermissions = gitHubService.getTeamPermissions(owner, name)

    // Delete existing team that are not in the input
    existingTeamPermissions
        .filter { teamPermission -> spec.teamPermissions.none { it.name != teamPermission.slug } }
        .forEach { permission ->
          gitHubService.deleteTeamPermission(owner, name, owner, permission.slug)
        }

    // Upsert team based on the input
    spec.teamPermissions.forEach { permission ->
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
      val teamPermissions: List<Permission>,
  )
}
