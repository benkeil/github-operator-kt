package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.service.GitHubService
import io.github.oshai.kotlinlogging.KotlinLogging

class UpdateAutomatedSecurityFixesUseCase(
    private val gitHubService: GitHubService,
) {
  private val logger = KotlinLogging.logger {}

  suspend fun execute(controller: () -> Input) {
    val spec = controller()
    val owner = spec.owner
    val name = spec.name
    when (spec.enabled) {
      true -> {
        logger.info { "Enabling automated security fixes" }
        gitHubService.enableAutomatedSecurityFixes(owner, name)
      }
      false -> {
        logger.info { "Disabling automated security fixes" }
        gitHubService.disableAutomatedSecurityFixes(owner, name)
      }
      null -> Unit
    }
  }

  data class Input(
      val owner: String,
      val name: String,
      val enabled: Boolean?,
  )
}
