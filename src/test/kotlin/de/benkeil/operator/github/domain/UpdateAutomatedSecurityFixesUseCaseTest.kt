package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.support.everySuspending
import de.benkeil.support.verifySuspending
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk

class UpdateAutomatedSecurityFixesUseCaseTest :
    FunSpec({
      val owner = "otto"
      val name = "operator"

      val gitHubService = mockk<GitHubService>()
      val useCase = UpdateAutomatedSecurityFixesUseCase(gitHubService)

      afterTest { clearAllMocks() }

      test("enable") {
        // given
        everySuspending { gitHubService.enableAutomatedSecurityFixes(owner, name) } returns Unit

        // when
        useCase.execute {
          UpdateAutomatedSecurityFixesUseCase.Input(
              owner = owner,
              name = name,
              enabled = true,
          )
        }

        // then
        verifySuspending(exactly = 1) { gitHubService.enableAutomatedSecurityFixes(owner, name) }
        verifySuspending(exactly = 0) { gitHubService.disableAutomatedSecurityFixes(owner, name) }
      }

      test("disable") {
        // given
        everySuspending { gitHubService.disableAutomatedSecurityFixes(owner, name) } returns Unit

        // when
        useCase.execute {
          UpdateAutomatedSecurityFixesUseCase.Input(
              owner = owner,
              name = name,
              enabled = false,
          )
        }

        // then
        verifySuspending(exactly = 1) { gitHubService.disableAutomatedSecurityFixes(owner, name) }
        verifySuspending(exactly = 0) { gitHubService.enableAutomatedSecurityFixes(owner, name) }
      }

      test("ignore") {
        // when
        useCase.execute {
          UpdateAutomatedSecurityFixesUseCase.Input(
              owner = owner,
              name = name,
              enabled = null,
          )
        }

        // then
        verifySuspending(exactly = 0) { gitHubService.disableAutomatedSecurityFixes(owner, name) }
        verifySuspending(exactly = 0) { gitHubService.enableAutomatedSecurityFixes(owner, name) }
      }
    })
