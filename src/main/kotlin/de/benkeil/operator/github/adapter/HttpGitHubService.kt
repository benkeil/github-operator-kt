package de.benkeil.operator.github.adapter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.benkeil.operator.github.domain.service.AutoLinkRequest
import de.benkeil.operator.github.domain.service.AutoLinkResponse
import de.benkeil.operator.github.domain.service.Collaborator
import de.benkeil.operator.github.domain.service.CollaboratorRequest
import de.benkeil.operator.github.domain.service.CreateGitHubRepositoryRequest
import de.benkeil.operator.github.domain.service.GitHubRepositoryResponse
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.RuleSet
import de.benkeil.operator.github.domain.service.RuleSetResponse
import de.benkeil.operator.github.domain.service.TeamPermission
import de.benkeil.operator.github.domain.service.TeamPermissionRequest
import de.benkeil.operator.github.domain.service.UpdateGitHubRepositoryRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class HttpGitHubService(
    private val gitHubToken: String,
    private val baseUrl: String = "https://api.github.com/"
) : GitHubService {

  private val logger = KotlinLogging.logger {}

  private val mapper =
      jacksonObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
          .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())

  private val client =
      HttpClient(CIO) {
        expectSuccess = false
        defaultRequest { url(baseUrl) }
        install(Auth) { bearer { loadTokens { BearerTokens(gitHubToken, null) } } }
      }

  override suspend fun getRepository(owner: String, name: String): GitHubRepositoryResponse? =
      client.get("repos/$owner/$name").let { response ->
        if (response.status.isSuccess()) {
          mapper.readValue(response.bodyAsBytes(), GitHubRepositoryResponse::class.java)
        } else if (response.status == HttpStatusCode.NotFound) {
          null
        } else {
          error("Failed to get repository: ${response.status.value}")
        }
      }

  override suspend fun createRepository(
      gitHubRepository: CreateGitHubRepositoryRequest
  ): GitHubRepositoryResponse =
      client
          .post("orgs/${gitHubRepository.owner}/repos") {
            contentType(Application.Json)
            setBody(mapper.writeValueAsString(gitHubRepository))
          }
          .let { response ->
            if (response.status.isSuccess()) {
              mapper.readValue(response.bodyAsBytes(), GitHubRepositoryResponse::class.java)
            } else {
              error("Failed to create repository: ${response.status.value}")
            }
          }

  override suspend fun updateRepository(
      gitHubRepository: UpdateGitHubRepositoryRequest
  ): GitHubRepositoryResponse =
      client
          .patch("repos/${gitHubRepository.owner}/${gitHubRepository.name}") {
            contentType(Application.Json)
            setBody(mapper.writeValueAsString(gitHubRepository))
            parameter("per_page", 100)
            parameter("affiliation", "direct")
          }
          .let { response ->
            if (response.status.isSuccess()) {
              mapper.readValue(response.bodyAsBytes(), GitHubRepositoryResponse::class.java)
            } else {
              error("Failed to updates repository: ${response.status.value}")
            }
          }

  override suspend fun enableAutomatedSecurityFixes(owner: String, name: String) {
    client.put("repos/$owner/$name/automated-security-fixes").let { response ->
      if (!response.status.isSuccess()) {
        error("Failed to enable automated security fixes: ${response.status.value}")
      }
    }
  }

  override suspend fun disableAutomatedSecurityFixes(owner: String, name: String) {
    client.delete("repos/$owner/$name/automated-security-fixes").let { response ->
      if (!response.status.isSuccess()) {
        error("Failed to disable automated security fixes: ${response.status.value}")
      }
    }
  }

  override suspend fun getAutoLinks(owner: String, name: String): List<AutoLinkResponse> =
      client.get("repos/$owner/$name/autolinks").let { response ->
        if (response.status.isSuccess()) {
          return mapper.readValue(
              response.bodyAsBytes(),
              object : TypeReference<List<AutoLinkResponse>>() {},
          )
        } else {
          error("Failed to get auto links: ${response.status.value}")
        }
      }

  override suspend fun createAutoLink(
      owner: String,
      name: String,
      autoLink: AutoLinkRequest
  ): AutoLinkResponse =
      client
          .post("repos/$owner/$name/autolinks") {
            contentType(Application.Json)
            setBody(mapper.writeValueAsString(autoLink))
          }
          .let { response ->
            if (!response.status.isSuccess()) {
              error("Failed to create auto link: ${response.status.value}")
            }
            mapper.readValue(response.bodyAsBytes(), AutoLinkResponse::class.java)
          }

  override suspend fun deleteAutoLink(owner: String, name: String, autoLinkId: Int) {
    client.delete("repos/$owner/$name/autolinks/$autoLinkId").let { response ->
      if (!response.status.isSuccess()) {
        error("Failed to delete auto link: ${response.status.value}")
      }
    }
  }

  override suspend fun getTeamPermissions(
      owner: String,
      name: String,
  ): List<TeamPermission> =
      client.get("repos/$owner/$name/teams").let { response ->
        if (response.status.isSuccess()) {
          return mapper.readValue(
              response.bodyAsBytes(),
              object : TypeReference<List<TeamPermission>>() {},
          )
        } else {
          error("Failed to get team permissions: ${response.status.value}")
        }
      }

  override suspend fun upsertTeamPermission(
      owner: String,
      name: String,
      team: TeamPermissionRequest,
  ) {
    client
        .put("orgs/${team.organization}/teams/${team.slug}/repos/$owner/$name") {
          contentType(Application.Json)
          setBody(mapper.writeValueAsString(UpdateTeamPermissionRequest(team.role)))
        }
        .let { response ->
          if (!response.status.isSuccess()) {
            error(
                "Failed to update team permission: ${response.status.value} - ${response.bodyAsText()}")
          }
        }
  }

  override suspend fun deleteTeamPermission(
      owner: String,
      name: String,
      organization: String,
      teamSlug: String
  ) {
    client.delete("orgs/$organization/teams/$teamSlug/repos/$owner/$name").let { response ->
      if (!response.status.isSuccess()) {
        error(
            "Failed to delete team permission: ${response.status.value} - ${response.bodyAsText()}")
      }
    }
  }

  override suspend fun getCollaborators(owner: String, name: String): List<Collaborator> =
      client
          .get("repos/$owner/$name/collaborators") {
            parameter("per_page", 100)
            parameter("affiliation", "direct")
          }
          .let { response ->
            if (response.status.isSuccess()) {
              mapper.readValue(
                  response.bodyAsBytes(),
                  object : TypeReference<List<Collaborator>>() {},
              )
            } else {
              error("Failed to get collaborators: ${response.status.value}")
            }
          }

  override suspend fun upsertCollaborators(
      owner: String,
      name: String,
      team: CollaboratorRequest
  ): Collaborator? =
      client
          .put("repos/$owner/$name/collaborators/${team.login}") {
            contentType(Application.Json)
            setBody(mapper.writeValueAsString(UpdateTeamPermissionRequest(team.role)))
          }
          .let { response ->
            when {
              response.status == HttpStatusCode.NoContent -> null
              !response.status.isSuccess() ->
                  error(
                      "Failed to update collaborator: ${response.status.value} - ${response.bodyAsText()}")
              response.status == HttpStatusCode.Created ->
                  mapper.readValue(response.bodyAsBytes(), Collaborator::class.java)
              else ->
                  error(
                      "Unexpected response when updating collaborator: ${response.status.value} - ${response.bodyAsText()}")
            }
          }

  override suspend fun deleteCollaborator(owner: String, name: String, login: String) {
    client.delete("repos/$owner/$name/collaborators/$login").let { response ->
      if (!response.status.isSuccess()) {
        error("Failed to delete collaborator: ${response.status.value} - ${response.bodyAsText()}")
      }
    }
  }

  override suspend fun getRuleSets(owner: String, name: String): List<RuleSetResponse> =
      client
          .get("repos/$owner/$name/rulesets")
          .let { response ->
            if (response.status.isSuccess()) {
              mapper.readValue(
                  response.bodyAsBytes(),
                  object : TypeReference<List<RuleSetResponse>>() {},
              )
            } else {
              error("Failed to get rule sets: ${response.status.value}")
            }
          }
          .map { getRuleSetById(owner, name, it.id) }

  private suspend fun getRuleSetById(owner: String, name: String, id: Int): RuleSetResponse =
      client.get("repos/$owner/$name/rulesets/$id").let { response ->
        if (response.status.isSuccess()) {
          mapper.readValue(response.bodyAsBytes(), RuleSetResponse::class.java)
        } else {
          error("Failed to get rule set by ID: ${response.status.value}")
        }
      }

  override suspend fun createRuleSet(
      owner: String,
      name: String,
      ruleSet: RuleSet
  ): RuleSetResponse =
      client
          .post("repos/$owner/$name/rulesets") {
            contentType(Application.Json)
            setBody(mapper.writeValueAsString(ruleSet))
          }
          .let { response ->
            if (!response.status.isSuccess()) {
              error(
                  "Failed to create rule set: ${response.status.value} - ${response.bodyAsText()}")
            }
            mapper.readValue(response.bodyAsBytes(), RuleSetResponse::class.java)
          }

  override suspend fun updateRuleSet(
      owner: String,
      name: String,
      id: Int,
      ruleSet: RuleSet
  ): RuleSetResponse =
      client
          .put("repos/$owner/$name/rulesets/$id") {
            contentType(Application.Json)
            setBody(mapper.writeValueAsString(ruleSet))
          }
          .let { response ->
            if (!response.status.isSuccess()) {
              error(
                  "Failed to update rule set: ${response.status.value} - ${response.bodyAsText()}")
            }
            mapper.readValue(response.bodyAsBytes(), RuleSetResponse::class.java)
          }

  override suspend fun deleteRuleSet(owner: String, name: String, id: Int) {
    client.delete("repos/$owner/$name/rulesets/$id").let { response ->
      if (!response.status.isSuccess()) {
        error("Failed to delete rule set: ${response.status.value} - ${response.bodyAsText()}")
      }
    }
  }
}

internal data class UpdateTeamPermissionRequest(val permission: String)
