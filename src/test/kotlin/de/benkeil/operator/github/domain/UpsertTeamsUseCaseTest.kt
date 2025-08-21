package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.TeamPermission
import de.benkeil.operator.github.domain.service.TeamPermissionRequest
import de.benkeil.support.everySuspending
import de.benkeil.support.verifySuspending
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk

class UpsertTeamsUseCaseTest :
    FunSpec({
      val owner = "otto"
      val name = "operator"

      val gitHubService = mockk<GitHubService>()

      val useCase = UpsertTeamsUseCase(gitHubService)
      afterTest { clearAllMocks() }

      test("execute") {
        // given
        everySuspending { gitHubService.getTeamPermissions(owner, name) } returns
            listOf(
                TeamPermission(id = 1, slug = "to-delete", permission = "admin"),
                TeamPermission(id = 1, slug = "to-update", permission = "old"),
                TeamPermission(id = 1, slug = "to-ignore", permission = "read"),
            )
        everySuspending { gitHubService.deleteTeamPermission(owner, name, owner, any()) } returns
            Unit
        everySuspending { gitHubService.upsertTeamPermission(owner, name, any()) } returns Unit

        // when
        useCase.execute {
          UpsertTeamsUseCase.Input(
              owner = owner,
              name = name,
              teams =
                  listOf(
                      Permission(name = "to-update", permission = "new"),
                      Permission(name = "to-create", permission = "admin"),
                      Permission(name = "to-ignore", permission = "read"),
                  ),
          )
        }

        // then
        verifySuspending(exactly = 1) {
          gitHubService.deleteTeamPermission(owner, name, any(), any())
        }
        verifySuspending(exactly = 1) {
          gitHubService.deleteTeamPermission(owner, name, owner, "to-delete")
        }
        verifySuspending(exactly = 2) { gitHubService.upsertTeamPermission(owner, name, any()) }
        verifySuspending(exactly = 1) {
          gitHubService.upsertTeamPermission(
              owner,
              name,
              TeamPermissionRequest(
                  organization = owner,
                  slug = "to-update",
                  role = "new",
              ))
        }
        verifySuspending(exactly = 1) {
          gitHubService.upsertTeamPermission(
              owner,
              name,
              TeamPermissionRequest(
                  organization = owner,
                  slug = "to-create",
                  role = "admin",
              ))
        }
      }
    })
