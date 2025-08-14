package de.benkeil.operator.github.domain.service

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

interface GitHubService {
  suspend fun getRepository(owner: String, name: String): GitHubRepositoryResponse?

  suspend fun createRepository(
      gitHubRepository: CreateGitHubRepositoryRequest
  ): GitHubRepositoryResponse

  suspend fun updateRepository(
      gitHubRepository: UpdateGitHubRepositoryRequest
  ): GitHubRepositoryResponse

  suspend fun enableAutomatedSecurityFixes(owner: String, name: String)

  suspend fun disableAutomatedSecurityFixes(owner: String, name: String)

  suspend fun getAutoLinks(owner: String, name: String): List<AutoLinkResponse>

  suspend fun createAutoLink(owner: String, name: String, autoLink: AutoLinkRequest)

  suspend fun deleteAutoLink(owner: String, name: String, autoLinkId: Int)

  suspend fun getTeamPermissions(owner: String, name: String): List<TeamPermission>

  suspend fun upsertTeamPermission(owner: String, name: String, team: TeamPermissionRequest)

  suspend fun deleteTeamPermission(owner: String, name: String, teamSlug: String)

  suspend fun getCollaborators(owner: String, name: String): List<Collaborator>

  suspend fun upsertCollaborators(owner: String, name: String, team: CollaboratorRequest)

  suspend fun deleteCollaborator(owner: String, name: String, login: String)

  suspend fun getRuleSets(owner: String, name: String): List<RuleSetResponse>

  suspend fun createRuleSet(owner: String, name: String, ruleSet: RuleSetRequest)

  suspend fun updateRuleSet(owner: String, name: String, id: String, ruleSet: RuleSetRequest)

  suspend fun deleteRuleSet(owner: String, name: String, id: String)
}

data class RuleSetRequest(
    val name: String,
    val target: String,
    val enforcement: String,
    val conditions: RuleSetCondition,
    val rules: List<Rule>,
)

data class RuleSetCondition(val refName: RefName)

data class RefName(val include: List<String>, val exclude: List<String>)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Rule {
  @JsonTypeName("deletion") class Deletion : Rule

  @JsonTypeName("non_fast_forward") class NonFastForward : Rule

  @JsonTypeName("pull_request")
  data class PullRequest(val parameters: Parameters) : Rule {
    data class Parameters(
        val allowedMergeMethods: List<MergeMethods>?,
        val automaticCopilotCodeReviewEnabled: Boolean?,
        val dismissStaleReviewsOnPush: Boolean,
        val requireCodeOwnerReview: Boolean,
        val requireLastPushApproval: Boolean,
        val requiredApprovingReviewCount: Int,
        val requiredReviewThreadResolution: Boolean,
    )
  }
}

enum class MergeMethods {
  SQUASH,
  MERGE,
  REBASE,
  ;

  override fun toString(): String = name.lowercase()
}

data class RuleDeletion(val type: String)

data class RuleNonFastForward(val type: String = "non_fast_forward")

data class RuleSetResponse(
    val id: String,
    val name: String,
    val target: String,
    val enforcement: String,
    val conditions: List<RuleSetCondition>?,
    val rules: List<Rule>?,
)

data class CreateGitHubRepositoryRequest(
    val name: String,
    val owner: String,
    val description: String?,
    val private: Boolean?,
    val visibility: Visibility?,
    val autoInit: Boolean?,
)

data class UpdateGitHubRepositoryRequest(
    val name: String,
    val owner: String,
    val description: String?,
    val private: Boolean?,
    val visibility: Visibility?,
    val autoInit: Boolean?,
    val defaultBranch: String?,
    val deleteBranchOnMerge: Boolean?,
    val allowAutoMerge: Boolean?,
    val allowSquashMerge: Boolean?,
    val allowMergeCommit: Boolean?,
    val allowRebaseMerge: Boolean?,
    val allowUpdateBranch: Boolean?,
    val useSquashPrTitleAsDefault: Boolean?,
    val squashMergeCommitTitle: SquashMergeCommitTitle?,
    val squashMergeCommitMessage: SquashMergeCommitMessage?,
    val mergeCommitTitle: MergeCommitTitle?,
    val mergeCommitMessage: MergeCommitMessage?,
    val securityAndAnalysis: SecurityAndAnalysis?,
)

enum class MergeCommitTitle {
  PR_TITLE,
  MERGE_MESSAGE,
}

enum class MergeCommitMessage {
  PR_TITLE,
  PR_BODY,
  BLANK,
}

enum class SquashMergeCommitTitle {
  PR_TITLE,
  COMMIT_OR_PR_TITLE,
}

enum class SquashMergeCommitMessage {
  PR_BODY,
  COMMIT_MESSAGES,
  BLANK,
}

data class SecurityAndAnalysis(
    val advancedSecurity: AdvancedSecurity?,
    val codeSecurity: CodeSecurity?,
    val secretScanning: SecretScanning?,
    val secretScanningPushProtection: SecretScanningPushProtection?,
    val secretScanningAiDetection: SecretScanningAiDetection?,
    val secretScanningNonProviderPatterns: SecretScanningNonProviderPatterns?,
)

data class AdvancedSecurity(val status: Status)

data class CodeSecurity(val status: Status)

data class SecretScanning(val status: Status)

data class SecretScanningPushProtection(val status: Status)

data class SecretScanningAiDetection(val status: Status)

data class SecretScanningNonProviderPatterns(val status: Status)

enum class Status {
  ENABLED,
  DISABLED,
  ;

  override fun toString(): String = name.lowercase()
}

data class GitHubRepositoryResponse(
    val owner: User,
    val name: String,
    val fullName: String?,
    val description: String?,
    val private: Boolean?,
    val visibility: Visibility?,
    val autoInit: Boolean?,
    val deleteBranchOnMerge: Boolean?,
    val allowAutoMerge: Boolean?,
    val allowSquashMerge: Boolean?,
    val allowMergeCommit: Boolean?,
    val allowRebaseMerge: Boolean?,
    val allowUpdateBranch: Boolean?,
    val useSquashPrTitleAsDefault: Boolean?,
    val squashMergeCommitTitle: SquashMergeCommitTitle?,
    val squashMergeCommitMessage: SquashMergeCommitMessage?,
    val mergeCommitTitle: MergeCommitTitle?,
    val mergeCommitMessage: MergeCommitMessage?,
    val securityAndAnalysis: SecurityAndAnalysis?,
)

enum class Visibility {
  PUBLIC,
  PRIVATE,
  INTERNAL,
  ;

  override fun toString(): String = name.lowercase()
}

data class User(
    val id: Int,
    val login: String,
)

data class Team(
    val id: Int,
    val name: String,
    val slug: String,
    val organization: User,
)

data class AutoLinkRequest(
    val keyPrefix: String,
    val urlTemplate: String,
    val isAlphanumeric: Boolean,
)

data class AutoLinkResponse(
    val id: Int,
    val keyPrefix: String,
    val urlTemplate: String,
    val isAlphanumeric: Boolean,
)

data class TeamPermission(
    val id: Int,
    val slug: String,
    val permission: String,
)

data class TeamPermissionRequest(
    val organization: String,
    val slug: String,
    val role: String,
)

data class Collaborator(
    val id: Int,
    val login: String,
    val roleName: String,
)

data class CollaboratorRequest(
    val login: String,
    val role: String,
)
