package de.benkeil.operator.github.application.cli

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.benkeil.operator.github.adapter.HttpGitHubService
import de.benkeil.operator.github.adapter.KubernetesPresenter
import de.benkeil.operator.github.domain.model.GitHubRepositoryResource
import de.benkeil.operator.github.domain.model.GitHubRepositoryStatus
import de.benkeil.operator.github.domain.UpsertRepositoryUseCase
import java.io.File
import java.io.InputStream
import kotlin.io.path.Path
import kotlinx.coroutines.runBlocking

fun main() {
  runBlocking {
    val gitHubToken = System.getenv("GITHUB_TOKEN")
    val gitHubService = HttpGitHubService(gitHubToken)
    // val useCase = UpsertRepositoryUseCase(gitHubService)
    val presenter = KubernetesPresenter()

    resource2file("/manifests")
        .walk()
        .filter { it.isFile }
        .filter { it.extension == "yaml" }
        .filter { !it.name.contains(".status.") }
        .forEach { file ->
          println("Processing file: ${file.absolutePath}")
          val statusFile =
              Path("")
                  .resolve("src/main/resources/manifests/${file.nameWithoutExtension}.status.json")
                  .toFile()
          // val status = useCase.execute({ controller(file, statusFile) }, presenter)
          // val newStatus = mapper.writeValueAsString(status)
          // statusFile.writeText(newStatus)
          // println(newStatus)
        }
  }
}

fun resource2file(path: String): File {
  val resourceURL = object {}.javaClass.getResource(path)
  return File(checkNotNull(resourceURL, { "Path not found: '$path'" }).file)
}

val mapper: ObjectMapper =
    jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(JavaTimeModule())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

val yamlMapper: ObjectMapper =
    ObjectMapper(YAMLFactory())
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)

fun readResourceAsString(path: String): InputStream =
    object {}.javaClass.getResourceAsStream(path) ?: error("Resource not found: $path")

fun controller(file: File, oldStatusFile: File): GitHubRepositoryResource =
    yamlMapper
        .readValue(file.readBytes(), object : TypeReference<GitHubRepositoryResource>() {})
        .apply {
          status =
              if (oldStatusFile.exists()) {
                yamlMapper.readValue(
                    oldStatusFile.readBytes(), object : TypeReference<GitHubRepositoryStatus>() {})
              } else {
                null
              }
        }
