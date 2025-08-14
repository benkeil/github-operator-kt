package de.benkeil.operator.github.application.kubernetes

import com.google.gson.reflect.TypeToken
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Namespace
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.Watch
import java.util.concurrent.TimeUnit

class Plain(
    private val client: ApiClient = Config.defaultClient(),
) {
  private val api = CoreV1Api(client)

  init {
    val httpClient = client.httpClient.newBuilder().readTimeout(0, TimeUnit.SECONDS).build()
    client.httpClient = httpClient
  }

  fun run() {
    val watch: Watch<V1Namespace> =
        Watch.createWatch(
            client,
            api.listNamespace().watch(true).buildCall(null),
            object : TypeToken<Watch.Response<V1Namespace>>() {}.type)

    try {
      watch.forEach { item ->
        System.out.printf("%s : %s%n", item.type, item.`object`?.metadata?.name)
      }
    } finally {
      watch.close()
    }
  }
}

fun main() {
  val operator = Plain()
  operator.run()
}
