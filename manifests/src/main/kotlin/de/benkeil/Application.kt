package de.benkeil

import org.cdk8s.App
import org.cdk8s.AppProps
import org.cdk8s.YamlOutputType

fun main() {
  val app = App(AppProps.builder().yamlOutputType(YamlOutputType.FILE_PER_RESOURCE).build())
  val env = Environment()
  RepositoriesChart(app, env)
  app.synth()
}
