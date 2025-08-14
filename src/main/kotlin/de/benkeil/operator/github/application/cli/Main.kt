package de.benkeil.operator.github.application.cli

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.benkeil.operator.github.adapter.HttpGitHubService
import de.benkeil.operator.github.adapter.KubernetesPresenter
import de.benkeil.operator.github.domain.UpsertRepositoryUseCase
import de.benkeil.operator.github.domain.model.AutoLink
import de.benkeil.operator.github.domain.model.Repository
import de.benkeil.operator.github.domain.model.RuleSet
import de.benkeil.operator.github.domain.model.Permission
import de.benkeil.operator.github.domain.service.AutoLinkRequest
import de.benkeil.operator.github.domain.service.MergeCommitMessage
import de.benkeil.operator.github.domain.service.MergeCommitTitle
import de.benkeil.operator.github.domain.service.RuleSetRequest
import de.benkeil.operator.github.domain.service.SecurityAndAnalysis
import de.benkeil.operator.github.domain.service.SquashMergeCommitMessage
import de.benkeil.operator.github.domain.service.SquashMergeCommitTitle
import de.benkeil.operator.github.domain.service.Visibility
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.runBlocking

fun main() {
  runBlocking {
    val gitHubToken = System.getenv("GITHUB_TOKEN")
    val gitHubService = HttpGitHubService(gitHubToken)
    val useCase = UpsertRepositoryUseCase(gitHubService)
    val presenter = KubernetesPresenter()

    resource2file("/manifests")
        .walk()
        .filter { it.isFile }
        .filter { it.extension == "yaml" }
        .forEach { file ->
          println("Processing file: ${file.absolutePath}")
          val status = useCase.execute({ controller(file) }, presenter)
          println(status)
        }
  }
}

fun resource2file(path: String): File {
  val resourceURL = object {}.javaClass.getResource(path)
  return File(checkNotNull(resourceURL, { "Path not found: '$path'" }).file)
}

val mapper: ObjectMapper =
    ObjectMapper(YAMLFactory())
        .registerKotlinModule()
        .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)

fun readResourceAsString(path: String): InputStream =
    object {}.javaClass.getResourceAsStream(path) ?: error("Resource not found: $path")

fun controller(file: File): Repository {
  val manifest =
      mapper.readValue(file.readBytes(), object : TypeReference<Manifest<RepositoryManifest>>() {})
  return manifest.toRepository()
}

data class Manifest<Spec>(
    val apiVersion: String,
    val kind: String,
    val metadata: Metadata,
    val spec: Spec,
)

data class Metadata(
    val name: String,
    val namespace: String,
)

data class RepositoryManifest(
    val owner: String,
    val ownerTeam: String,
    val ownerRole: String? = null,
    val description: String? = null,
    val private: Boolean? = null,
    val visibility: Visibility? = null,
    val autoInit: Boolean? = null,
    val deleteBranchOnMerge: Boolean? = null,
    val allowAutoMerge: Boolean? = null,
    val allowSquashMerge: Boolean? = null,
    val allowMergeCommit: Boolean? = null,
    val allowRebaseMerge: Boolean? = null,
    val allowUpdateBranch: Boolean? = null,
    val useSquashPrTitleAsDefault: Boolean? = null,
    val squashMergeCommitTitle: SquashMergeCommitTitle? = null,
    val squashMergeCommitMessage: SquashMergeCommitMessage? = null,
    val mergeCommitTitle: MergeCommitTitle? = null,
    val mergeCommitMessage: MergeCommitMessage? = null,
    val securityAndAnalysis: SecurityAndAnalysis? = null,
    val defaultBranch: String? = null,
    val automatedSecurityFixes: Boolean? = null,
    val autoLinks: List<AutoLinkRequest>? = null,
    val teamPermissions: Map<String, String>? = null,
    val collaborators: Map<String, String>? = null,
    val rulesets: List<RuleSetRequest>? = null,
)

fun Manifest<RepositoryManifest>.toRepository(): Repository =
    Repository(
        owner = spec.owner,
        ownerTeam = spec.ownerTeam,
        ownerRole = spec.ownerRole ?: "admin",
        name = "${metadata.namespace}_${metadata.name}",
        description = spec.description,
        private = spec.private,
        visibility = spec.visibility,
        autoInit = spec.autoInit,
        deleteBranchOnMerge = spec.deleteBranchOnMerge,
        allowAutoMerge = spec.allowAutoMerge,
        allowSquashMerge = spec.allowSquashMerge,
        allowMergeCommit = spec.allowMergeCommit,
        allowRebaseMerge = spec.allowRebaseMerge,
        allowUpdateBranch = spec.allowUpdateBranch,
        useSquashPrTitleAsDefault = spec.useSquashPrTitleAsDefault,
        squashMergeCommitTitle = spec.squashMergeCommitTitle,
        squashMergeCommitMessage = spec.squashMergeCommitMessage,
        mergeCommitTitle = spec.mergeCommitTitle,
        mergeCommitMessage = spec.mergeCommitMessage,
        securityAndAnalysis = spec.securityAndAnalysis,
        defaultBranch = spec.defaultBranch,
        automatedSecurityFixes = spec.automatedSecurityFixes,
        autoLinks =
            spec.autoLinks?.map {
              AutoLink(
                  keyPrefix = it.keyPrefix,
                  urlTemplate = it.urlTemplate,
                  isAlphanumeric = it.isAlphanumeric,
                  delete = false,
              )
            },
        teamPermissions =
            spec.teamPermissions?.map { (slug, role) ->
              Permission(
                  slug = slug,
                  role = role,
                  delete = false,
              )
            },
        collaborators =
            spec.collaborators?.map { (slug, role) ->
              Permission(
                  slug = slug,
                  role = role,
                  delete = false,
              )
            },
        rulesets =
            spec.rulesets?.map { ruleSetRequest ->
              RuleSet(
                  name = ruleSetRequest.name,
                  target = ruleSetRequest.target,
                  enforcement = ruleSetRequest.enforcement,
                  conditions = ruleSetRequest.conditions,
                  rules = ruleSetRequest.rules,
                  delete = false,
              )
            },
    )
