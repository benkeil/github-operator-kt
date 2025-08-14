plugins { kotlin("jvm") version "2.2.0" }

group = "de.benkeil"

version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
  libs.bundles.platform.get().forEach { platform(it) }
  implementation(libs.bundles.implementation)
  testImplementation(libs.bundles.testImplementation)
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(21) }
