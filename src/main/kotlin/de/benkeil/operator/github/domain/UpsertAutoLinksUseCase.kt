package de.benkeil.operator.github.domain

import de.benkeil.operator.github.domain.model.AutoLink
import de.benkeil.operator.github.domain.service.AutoLinkRequest
import de.benkeil.operator.github.domain.service.GitHubService
import io.github.oshai.kotlinlogging.KotlinLogging

class UpsertAutoLinksUseCase(
    private val gitHubService: GitHubService,
) {
  private val logger = KotlinLogging.logger {}

  suspend fun execute(controller: () -> Input) {
    val spec = controller()
    val owner = spec.owner
    val name = spec.name

    // Get existing auto links
    val existingAutoLinks = gitHubService.getAutoLinks(owner, name)
    logger.debug { "Found auto links: $existingAutoLinks" }

    // Delete existing auto links that are not in the input
    val (delete, proceed) =
        existingAutoLinks.partition { existing ->
          spec.autoLinks.none { it.keyPrefix == existing.keyPrefix }
        }
    delete.forEach { autoLink ->
      logger.info { "Deleting removed auto link with key prefix '${autoLink.keyPrefix}'" }
      gitHubService.deleteAutoLink(owner, name, autoLink.id)
    }

    // Delete auto links that differ from the input
    proceed
        .filter { existing ->
          spec.autoLinks.any {
            it.keyPrefix == existing.keyPrefix &&
                (it.urlTemplate != existing.urlTemplate ||
                    it.isAlphanumeric != existing.isAlphanumeric)
          }
        }
        .forEach { autoLink ->
          logger.info { "Deleting updated auto link with key prefix '${autoLink.keyPrefix}'" }
          gitHubService.deleteAutoLink(owner, name, autoLink.id)
          logger.info { "Creating auto link with key prefix '${autoLink.keyPrefix}'" }
          gitHubService.createAutoLink(
              owner,
              name,
              spec.autoLinks.first { it.keyPrefix == autoLink.keyPrefix }.toAutoLinkRequest(),
          )
        }

    // Create new or updated auto links based on the input
    spec.autoLinks
        .filter { autoLink -> proceed.none { it.keyPrefix == autoLink.keyPrefix } }
        .forEach { autoLink ->
          logger.info { "Creating auto link with key prefix '${autoLink.keyPrefix}'" }
          gitHubService.createAutoLink(owner, name, autoLink.toAutoLinkRequest())
        }
  }

  data class Input(
      val owner: String,
      val name: String,
      val autoLinks: List<AutoLink>,
  )
}

fun AutoLink.toAutoLinkRequest(): AutoLinkRequest =
    AutoLinkRequest(
        keyPrefix = keyPrefix,
        urlTemplate = urlTemplate,
        isAlphanumeric = isAlphanumeric,
    )
