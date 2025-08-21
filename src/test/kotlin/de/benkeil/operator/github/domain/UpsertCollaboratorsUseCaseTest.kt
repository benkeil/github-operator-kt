package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.Collaborator
import de.benkeil.operator.github.domain.service.CollaboratorRequest
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.support.everySuspending
import de.benkeil.support.verifySuspending
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk

class UpsertCollaboratorsUseCaseTest :
    FunSpec({
      val owner = "otto"
      val name = "operator"

      val gitHubService = mockk<GitHubService>()
      val useCase = UpsertCollaboratorsUseCase(gitHubService)

      afterTest { clearAllMocks() }

      test("execute") {
        // given
        everySuspending { gitHubService.getCollaborators(owner, name) } returns
            listOf(
                Collaborator(id = 1, login = "to-delete", roleName = "admin"),
                Collaborator(id = 1, login = "to-update", roleName = "old"),
                Collaborator(id = 1, login = "to-ignore", roleName = "read"),
            )
        everySuspending { gitHubService.deleteCollaborator(owner, name, any()) } returns Unit
        everySuspending { gitHubService.upsertCollaborators(owner, name, any()) } returns
            Collaborator(
                id = 1,
                login = "ignored",
                roleName = "ignored",
            )

        // when
        useCase.execute {
          UpsertCollaboratorsUseCase.Input(
              owner = owner,
              name = name,
              collaborators =
                  listOf(
                      Permission(name = "to-update", permission = "new"),
                      Permission(name = "to-create", permission = "admin"),
                      Permission(name = "to-ignore", permission = "read"),
                  ),
          )
        }

        // then
        verifySuspending(exactly = 1) { gitHubService.deleteCollaborator(owner, name, "to-delete") }
        verifySuspending(exactly = 2) { gitHubService.upsertCollaborators(owner, name, any()) }
        verifySuspending(exactly = 1) {
          gitHubService.upsertCollaborators(
              owner,
              name,
              CollaboratorRequest(
                  login = "to-update",
                  role = "new",
              ))
        }
        verifySuspending(exactly = 1) {
          gitHubService.upsertCollaborators(
              owner,
              name,
              CollaboratorRequest(
                  login = "to-create",
                  role = "admin",
              ))
        }
      }
    })
