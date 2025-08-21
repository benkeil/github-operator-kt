package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.CollaboratorRequest
import de.benkeil.operator.github.domain.service.GitHubService
import io.github.oshai.kotlinlogging.KotlinLogging

class UpsertCollaboratorsUseCase(
    private val gitHubService: GitHubService,
) {
  private val logger = KotlinLogging.logger {}

  suspend fun execute(controller: () -> Input) {
    val spec = controller()
    val owner = spec.owner
    val name = spec.name

    val existingCollaborators = gitHubService.getCollaborators(owner, name)

    // Delete existing collaborators that are not in the input
    val (delete, proceed) =
        existingCollaborators.partition { collaborator ->
          spec.collaborators.none { it.name == collaborator.login }
        }
    delete.forEach { permission ->
      logger.info { "Deleting collaborator ${permission.login}" }
      gitHubService.deleteCollaborator(owner, name, permission.login)
    }

    // Upsert collaborators based on the input
    spec.collaborators
        .filter { permission ->
          existingCollaborators.none { it.login == permission.name } ||
              existingCollaborators.any {
                it.login == permission.name && it.roleName != permission.permission
              }
        }
        .forEach { permission ->
          logger.info {
            "Upserting collaborator ${permission.name} with role ${permission.permission}"
          }
          gitHubService.upsertCollaborators(
              owner,
              name,
              CollaboratorRequest(
                  login = permission.name,
                  role = permission.permission,
              ),
          )
        }
  }

  data class Input(
      val owner: String,
      val name: String,
      val collaborators: List<Permission>,
  )
}
