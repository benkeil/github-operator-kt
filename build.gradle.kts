import io.fabric8.crd.generator.collector.CustomResourceCollector
import io.fabric8.crdv2.generator.CRDGenerationInfo
import io.fabric8.crdv2.generator.CRDGenerator
import java.nio.file.Files
import org.gradle.api.internal.tasks.JvmConstants

plugins { kotlin("jvm") version "2.2.0" }

group = "de.benkeil"

version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

buildscript { dependencies { classpath(libs.bundles.buildscript) } }

dependencies {
  libs.bundles.platform.get().forEach { platform(it) }
  implementation(libs.bundles.implementation)
  testImplementation(libs.bundles.testImplementation)
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(21) }

tasks.register("generateCrds") {
  description = "Generate CRDs from compiled custom resource classes"
  group = "crd"

  val sourceSet = project.sourceSets["main"]

  val compileClasspathElements = sourceSet.compileClasspath.map { e -> e.absolutePath }

  val outputClassesDirs =
      sourceSet.output.classesDirs.filter { !it.absolutePath.endsWith("/classes/java/main") }
  val outputClasspathElements = outputClassesDirs.map { d -> d.absolutePath }
  val classpathElements = listOf(outputClasspathElements, compileClasspathElements).flatten()
  val filesToScan = listOf(outputClassesDirs).flatten()
  val outputDir =
      rootDir.resolve("crd").also {
        if (it.exists()) it.deleteRecursively()
        it.mkdirs()
      }

  doLast {
    Files.createDirectories(outputDir!!.toPath())

    val collector =
        CustomResourceCollector()
            .withParentClassLoader(Thread.currentThread().contextClassLoader)
            .withClasspathElements(classpathElements)
            .withFilesToScan(filesToScan)

    val crdGenerator =
        CRDGenerator()
            .customResourceClasses(collector.findCustomResourceClasses())
            .inOutputDir(outputDir)

    val crdGenerationInfo: CRDGenerationInfo = crdGenerator.detailedGenerate()

    crdGenerationInfo.crdDetailsPerNameAndVersion.forEach { (crdName, versionToInfo) ->
      println("Generated CRD $crdName:")
      versionToInfo.forEach { (version, info) -> println(" $version -> ${info.filePath}") }
    }
  }

  dependsOn(tasks.named(JvmConstants.CLASSES_TASK_NAME))
}

tasks.named(JvmConstants.CLASSES_TASK_NAME) { finalizedBy("generateCrds") }
