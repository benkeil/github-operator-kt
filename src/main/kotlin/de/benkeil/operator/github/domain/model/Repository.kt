package de.benkeil.operator.github.domain.model

import de.benkeil.operator.github.domain.service.MergeCommitMessage
import de.benkeil.operator.github.domain.service.MergeCommitTitle
import de.benkeil.operator.github.domain.service.Rule
import de.benkeil.operator.github.domain.service.RuleSetCondition
import de.benkeil.operator.github.domain.service.SecurityAndAnalysis
import de.benkeil.operator.github.domain.service.SquashMergeCommitMessage
import de.benkeil.operator.github.domain.service.SquashMergeCommitTitle
import de.benkeil.operator.github.domain.service.Visibility

data class Repository(
    val owner: String,
    val ownerTeam: String,
    val ownerRole: String,
    val name: String,
    val defaultBranch: String?,
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
    val automatedSecurityFixes: Boolean?,
    val autoLinks: List<AutoLink>?,
    val teamPermissions: List<Permission>?,
    val collaborators: List<Permission>?,
    val rulesets: List<RuleSet>?,
)

data class AutoLink(
    val keyPrefix: String,
    val urlTemplate: String,
    val isAlphanumeric: Boolean,
    val delete: Boolean,
)

data class Permission(
    val slug: String,
    val role: String,
    val delete: Boolean,
)

data class RuleSet(
    val name: String,
    val target: String,
    val enforcement: String,
    val conditions: RuleSetCondition,
    val rules: List<Rule>,
    val delete: Boolean,
)
