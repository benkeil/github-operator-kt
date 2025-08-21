package de.benkeil.operator.github.domain.model

import com.fasterxml.jackson.annotation.JsonProperty
import de.benkeil.operator.github.domain.service.MergeCommitMessage
import de.benkeil.operator.github.domain.service.MergeCommitTitle
import de.benkeil.operator.github.domain.service.RuleSet
import de.benkeil.operator.github.domain.service.SecurityAndAnalysis
import de.benkeil.operator.github.domain.service.SquashMergeCommitMessage
import de.benkeil.operator.github.domain.service.SquashMergeCommitTitle
import de.benkeil.operator.github.domain.service.Visibility
import io.fabric8.generator.annotation.Required
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version
import java.time.OffsetDateTime

@Version(GitHubRepositoryResource.API_VERSION)
@Group(GitHubRepositoryResource.GROUP)
@Kind(GitHubRepositoryResource.KIND)
class GitHubRepositoryResource :
    CustomResource<GitHubRepositorySpec, GitHubRepositoryStatus>(), Namespaced {
  companion object {
    const val GROUP = "github.platform.benkeil.de"
    const val API_VERSION = "v1alpha1"
    const val KIND = "Repository"
  }
}

data class GitHubRepositorySpec(
    @Required val owner: String,
    @Required val name: String,
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
    val autoLinks: List<AutoLink> = emptyList(),
    val teams: List<Permission> = emptyList(),
    val collaborators: List<Permission> = emptyList(),
    val rulesets: List<RuleSet> = emptyList(),
)

data class Permission(
    @Required val name: String,
    @Required val permission: String,
)

data class GitHubRepositoryStatus(
    val createdAt: OffsetDateTime,
    var updatedAt: OffsetDateTime? = null,
    val errorMessages: MutableList<String> = mutableListOf(),
)

data class AutoLink(
    val keyPrefix: String,
    val urlTemplate: String,
    @get:JsonProperty("isAlphanumeric") val isAlphanumeric: Boolean,
)
