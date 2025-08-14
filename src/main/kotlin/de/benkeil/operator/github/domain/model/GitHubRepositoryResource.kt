package de.benkeil.operator.github.domain.model

import de.benkeil.operator.github.domain.service.AutoLinkRequest
import de.benkeil.operator.github.domain.service.MergeCommitMessage
import de.benkeil.operator.github.domain.service.MergeCommitTitle
import de.benkeil.operator.github.domain.service.RuleSetRequest
import de.benkeil.operator.github.domain.service.SecurityAndAnalysis
import de.benkeil.operator.github.domain.service.SquashMergeCommitMessage
import de.benkeil.operator.github.domain.service.SquashMergeCommitTitle
import de.benkeil.operator.github.domain.service.Visibility
import io.fabric8.generator.annotation.Default
import io.fabric8.generator.annotation.ValidationRule
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
    const val KIND = "GitHubRepository"
  }
}

object Rules {
  const val IMMUTABLE = "oldSelf == null || self == oldSelf"
}

data class GitHubRepositorySpec(
    val owner: String,
    val name: String,
    @ValidationRule(Rules.IMMUTABLE) val ownerTeam: String,
    @ValidationRule(Rules.IMMUTABLE) @Default("admin") val ownerRole: String = "admin",
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
    val autoLinks: List<AutoLinkRequest> = emptyList(),
    val teamPermissions: Map<String, String> = emptyMap(),
    val collaborators: Map<String, String> = emptyMap(),
    val rulesets: List<RuleSetRequest> = emptyList(),
)

data class GitHubRepositoryStatus(
    val createdAt: OffsetDateTime,
    var updatedAt: OffsetDateTime? = null,
    var autoLinkKeyPrefixes: Map<String, Int> = mapOf(),
    var ruleSetNames: Map<String, Int> = mapOf(),
    var teamPermissionSlugs: Set<String> = setOf(),
    var collaboratorLogins: Set<String> = setOf(),
    val errors: MutableList<String> = mutableListOf(),
)
