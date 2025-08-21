package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.Rule
import de.benkeil.operator.github.domain.service.RuleSet
import de.benkeil.operator.github.domain.service.RuleSetResponse
import de.benkeil.support.everySuspending
import de.benkeil.support.verifySuspending
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk

class UpsertRuleSetsUseCaseTest :
    FunSpec({
      val owner = "otto"
      val name = "operator"

      val gitHubService = mockk<GitHubService>()
      val useCase = UpsertRuleSetsUseCase(gitHubService)

      afterTest { clearAllMocks() }

      test("execute") {
        // given
        everySuspending { gitHubService.getRuleSets(owner, name) } returns
            listOf(
                RuleSetResponse(
                    id = 1,
                    name = "to-delete",
                    target = "target",
                    enforcement = "enforcement",
                    conditions = null,
                    rules = null,
                ),
                RuleSetResponse(
                    id = 2,
                    name = "to-update",
                    target = "target",
                    enforcement = "enforcement",
                    conditions = null,
                    rules = listOf(Rule.Deletion),
                ),
                RuleSetResponse(
                    id = 3,
                    name = "to-ignore",
                    target = "target",
                    enforcement = "enforcement",
                    conditions = null,
                    rules = listOf(Rule.Deletion),
                ),
            )
        everySuspending { gitHubService.deleteRuleSet(owner, name, any()) } returns Unit
        everySuspending { gitHubService.updateRuleSet(owner, name, any(), any()) } returns
            RuleSetResponse(
                id = 10,
                name = "ignored",
                target = "ignored",
                enforcement = "ignored",
                conditions = null,
                rules = null,
            )
        everySuspending { gitHubService.createRuleSet(owner, name, any()) } returns
            RuleSetResponse(
                id = 10,
                name = "ignored",
                target = "ignored",
                enforcement = "ignored",
                conditions = null,
                rules = null,
            )

        // when
        useCase.execute {
          UpsertRuleSetsUseCase.Input(
              owner = owner,
              name = name,
              rulesets =
                  listOf(
                      RuleSet(
                          name = "to-update",
                          target = "target",
                          enforcement = "enforcement",
                          conditions = null,
                          rules = listOf(Rule.Deletion, Rule.NonFastForward),
                      ),
                      RuleSet(
                          name = "to-create",
                          target = "target",
                          enforcement = "enforcement",
                          conditions = null,
                          rules = listOf(Rule.Deletion),
                      ),
                      RuleSet(
                          name = "to-ignore",
                          target = "target",
                          enforcement = "enforcement",
                          conditions = null,
                          rules = listOf(Rule.Deletion),
                      ),
                  ),
          )
        }

        // then
        verifySuspending(exactly = 1) { gitHubService.deleteRuleSet(owner, name, any()) }
        verifySuspending(exactly = 1) { gitHubService.deleteRuleSet(owner, name, 1) }
        verifySuspending(exactly = 1) { gitHubService.createRuleSet(owner, name, any()) }
        verifySuspending(exactly = 1) {
          gitHubService.createRuleSet(
              owner,
              name,
              RuleSet(
                  name = "to-create",
                  target = "target",
                  enforcement = "enforcement",
                  conditions = null,
                  rules = listOf(Rule.Deletion),
              ))
        }
        verifySuspending(exactly = 1) { gitHubService.updateRuleSet(owner, name, any(), any()) }
        verifySuspending(exactly = 1) {
          gitHubService.updateRuleSet(
              owner,
              name,
              any(),
              RuleSet(
                  name = "to-update",
                  target = "target",
                  enforcement = "enforcement",
                  conditions = null,
                  rules = listOf(Rule.Deletion, Rule.NonFastForward),
              ))
        }
      }
    })
