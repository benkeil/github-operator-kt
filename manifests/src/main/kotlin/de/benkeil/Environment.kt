package de.benkeil

data class Environment(
    val repositories: List<String> =
        listOf(
            "pdh-da_github-operator-example",
            "pdh-da_kubernetes-cluster-platform",
            // "pdh-da_product-partner-journey",
        ),
    val owner: String = "otto-ec",
    val ownerTeam: String = "pdh-distribution-analytics",
    val functionalTeamUser: String = "pdh-da-fkt-github",
)
