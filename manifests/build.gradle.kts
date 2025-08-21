plugins {
  kotlin("jvm") version "2.2.0"
  application
}

group = "de.benkeil"

version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
  libs.bundles.platform.get().forEach { platform(it) }
  implementation(libs.bundles.bootstrap)
  testImplementation(libs.bundles.testImplementation)
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(21) }

application { mainClass.set("de.benkeil.ApplicationKt") }
