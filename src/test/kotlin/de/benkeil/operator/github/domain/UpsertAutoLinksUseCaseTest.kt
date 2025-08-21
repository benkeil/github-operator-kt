package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.service.AutoLink
import de.benkeil.operator.github.domain.service.AutoLinkResponse
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.support.everySuspending
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify

class UpsertAutoLinksUseCaseTest :
    FunSpec({
      val owner = "otto"
      val name = "operator"

      val gitHubService = mockk<GitHubService>()
      val useCase = UpsertAutoLinksUseCase(gitHubService)

      afterTest { clearAllMocks() }

      test("execute") {
        // given
        everySuspending { gitHubService.getAutoLinks(owner, name) } returns
            listOf(
                AutoLinkResponse(
                    id = 1,
                    keyPrefix = "to-delete-",
                    urlTemplate = "https://example.com/issues/{number}",
                    isAlphanumeric = false,
                ),
                AutoLinkResponse(
                    id = 2,
                    keyPrefix = "to-update-",
                    urlTemplate = "https://example.com/issues/{number}",
                    isAlphanumeric = true,
                ),
                AutoLinkResponse(
                    id = 3,
                    keyPrefix = "to-ignore-",
                    urlTemplate = "https://example.com/issues/{number}",
                    isAlphanumeric = false,
                ),
            )
        everySuspending { gitHubService.deleteAutoLink(owner, name, any()) } returns Unit
        everySuspending { gitHubService.createAutoLink(owner, name, any()) } returns
            AutoLinkResponse(
                id = 1,
                keyPrefix = "ignored",
                urlTemplate = "ignored",
                isAlphanumeric = false,
            )

        // when
        useCase.execute {
          UpsertAutoLinksUseCase.Input(
              owner = owner,
              name = name,
              autoLinks =
                  listOf(
                      AutoLink(
                          keyPrefix = "to-update-",
                          urlTemplate = "https://example.com/issues/{number}",
                          isAlphanumeric = false,
                      ),
                      AutoLink(
                          keyPrefix = "to-create-",
                          urlTemplate = "https://example.com/issues/{number}",
                          isAlphanumeric = false,
                      ),
                      AutoLink(
                          keyPrefix = "to-ignore-",
                          urlTemplate = "https://example.com/issues/{number}",
                          isAlphanumeric = false,
                      ),
                  ),
          )
        }

        // then
        verify(exactly = 2) { runBlocking { gitHubService.deleteAutoLink(owner, name, any()) } }
        verify(exactly = 1) { runBlocking { gitHubService.deleteAutoLink(owner, name, 1) } }
        verify(exactly = 1) { runBlocking { gitHubService.deleteAutoLink(owner, name, 2) } }

        verify(exactly = 2) { runBlocking { gitHubService.createAutoLink(owner, name, any()) } }
        verify(exactly = 1) {
          runBlocking {
            gitHubService.createAutoLink(
                owner,
                name,
                AutoLink(
                    keyPrefix = "to-update-",
                    urlTemplate = "https://example.com/issues/{number}",
                    isAlphanumeric = false,
                ))
          }
        }
        verify(exactly = 1) {
          runBlocking {
            gitHubService.createAutoLink(
                owner,
                name,
                AutoLink(
                    keyPrefix = "to-create-",
                    urlTemplate = "https://example.com/issues/{number}",
                    isAlphanumeric = false,
                ))
          }
        }
      }
    })
