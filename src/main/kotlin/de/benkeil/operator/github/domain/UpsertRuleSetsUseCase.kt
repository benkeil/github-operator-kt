package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.RuleSet
import de.benkeil.operator.github.domain.service.RuleSetResponse
import io.github.oshai.kotlinlogging.KotlinLogging

class UpsertRuleSetsUseCase(
    private val gitHubService: GitHubService,
) {
  private val logger = KotlinLogging.logger {}

  suspend fun execute(controller: () -> Input) {
    val input = controller()
    val owner = input.owner
    val name = input.name

    val existingRuleSets = gitHubService.getRuleSets(owner, name)

    // Delete rule sets that are no longer present in the input
    existingRuleSets
        .filter { ruleSet -> input.rulesets.none { ruleSet.name == it.name } }
        .forEach {
          logger.info { "Deleting rule set ${it.name}" }
          gitHubService.deleteRuleSet(owner, name, it.id)
        }

    // Create or update rule sets
    input.rulesets.forEach { ruleSet ->
      val existingRuleSet = existingRuleSets.find { it.name == ruleSet.name }
      println("matches")
      println(existingRuleSet?.toRuleSet())
      println(ruleSet)
      println(existingRuleSet?.toRuleSet()?.equals(ruleSet))
      if (existingRuleSet == null) {
        logger.info { "Create rule set ${ruleSet.name}" }
        gitHubService.createRuleSet(owner, name, ruleSet)
      } else if (!existingRuleSet.isEqualTo(ruleSet)) {
        logger.info { "Updating rule set ${ruleSet.name}" }
        gitHubService.updateRuleSet(owner, name, existingRuleSet.id, ruleSet)
      }
    }
  }

  data class Input(
      val owner: String,
      val name: String,
      val rulesets: List<RuleSet>,
  )
}

fun RuleSetResponse.toRuleSet(): RuleSet =
    RuleSet(
        name = name,
        target = target,
        enforcement = enforcement,
        conditions = conditions,
        rules = rules,
    )

fun RuleSetResponse.isEqualTo(other: RuleSet): Boolean {
  return this.name == other.name &&
      this.target == other.target &&
      this.enforcement == other.enforcement &&
      this.conditions == other.conditions &&
      this.rules == other.rules
}
