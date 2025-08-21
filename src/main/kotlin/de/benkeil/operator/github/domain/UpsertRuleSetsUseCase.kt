package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.RuleSet

class UpsertRuleSetsUseCase(
    private val gitHubService: GitHubService,
) {
  suspend fun execute(controller: () -> Input) {
    val input = controller()
    val owner = input.owner
    val name = input.name

    val existingRuleSets = gitHubService.getRuleSets(owner, name)

    // Delete rule sets that are no longer present in the input
    existingRuleSets
        .filter { ruleSet -> input.rulesets.none { ruleSet.name == it.name } }
        .forEach { gitHubService.deleteRuleSet(owner, name, it.id) }

    // Create or update rule sets
    input.rulesets.forEach { ruleSet ->
      val existingRuleSet = existingRuleSets.find { it.name == ruleSet.name }
      if (existingRuleSet != null) {
        gitHubService.updateRuleSet(owner, name, existingRuleSet.id, ruleSet)
      } else {
        gitHubService.createRuleSet(owner, name, ruleSet)
      }
    }
  }

  data class Input(
      val owner: String,
      val name: String,
      val rulesets: List<RuleSet>,
  )
}
