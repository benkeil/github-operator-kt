package de.benkeil

import imports.de.benkeil.platform.github.Repository
import imports.de.benkeil.platform.github.RepositorySpec
import imports.de.benkeil.platform.github.RepositorySpecAutoLinks
import imports.de.benkeil.platform.github.RepositorySpecCollaborators
import imports.de.benkeil.platform.github.RepositorySpecRulesets
import imports.de.benkeil.platform.github.RepositorySpecRulesetsConditions
import imports.de.benkeil.platform.github.RepositorySpecRulesetsConditionsRefName
import imports.de.benkeil.platform.github.RepositorySpecRulesetsRules
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysis
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisAdvancedSecurity
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisAdvancedSecurityStatus
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisCodeSecurity
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisCodeSecurityStatus
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanning
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningAiDetection
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningAiDetectionStatus
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningNonProviderPatterns
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningNonProviderPatternsStatus
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningPushProtection
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningPushProtectionStatus
import imports.de.benkeil.platform.github.RepositorySpecSecurityAndAnalysisSecretScanningStatus
import imports.de.benkeil.platform.github.RepositorySpecTeams
import imports.de.benkeil.platform.github.RepositorySpecVisibility
import org.cdk8s.ApiObjectMetadata
import org.cdk8s.Chart
import org.cdk8s.ChartProps
import org.cdk8s.plus32.Namespace
import software.constructs.Construct

class RepositoriesChart(scope: Construct, environment: Environment) :
    Chart(
        scope,
        "repositories",
        ChartProps.builder().namespace("pdh-da").disableResourceNameHashes(true).build(),
    ) {
  init {
    Namespace.Builder.create(this, "pdh-da")
        .metadata(ApiObjectMetadata.builder().name("pdh-da").build())
        .build()

    environment.repositories.forEach { repo ->
      Repository.Builder.create(this, repo)
          .metadata(ApiObjectMetadata.builder().name(repo.replace("_", "-")).build())
          .spec(
              RepositorySpec.builder()
                  .name(repo)
                  .owner(environment.owner)
                  .visibility(RepositorySpecVisibility.INTERNAL)
                  .privateValue(true)
                  .autoInit(true)
                  .defaultBranch("main")
                  .collaborators(
                      listOf(
                          RepositorySpecCollaborators.builder()
                              .name(environment.functionalTeamUser)
                              .permission("admin")
                              .build(),
                      ))
                  .teams(
                      listOf(
                          RepositorySpecTeams.builder()
                              .name(environment.ownerTeam)
                              .permission("admin")
                              .build(),
                      ))
                  .securityAndAnalysis(
                      RepositorySpecSecurityAndAnalysis.builder()
                          .advancedSecurity(
                              RepositorySpecSecurityAndAnalysisAdvancedSecurity.builder()
                                  .status(
                                      RepositorySpecSecurityAndAnalysisAdvancedSecurityStatus
                                          .ENABLED)
                                  .build())
                          .codeSecurity(
                              RepositorySpecSecurityAndAnalysisCodeSecurity.builder()
                                  .status(
                                      RepositorySpecSecurityAndAnalysisCodeSecurityStatus.ENABLED)
                                  .build())
                          .secretScanning(
                              RepositorySpecSecurityAndAnalysisSecretScanning.builder()
                                  .status(
                                      RepositorySpecSecurityAndAnalysisSecretScanningStatus.ENABLED)
                                  .build())
                          .secretScanningAiDetection(
                              RepositorySpecSecurityAndAnalysisSecretScanningAiDetection.builder()
                                  .status(
                                      RepositorySpecSecurityAndAnalysisSecretScanningAiDetectionStatus
                                          .DISABLED)
                                  .build())
                          .secretScanningNonProviderPatterns(
                              RepositorySpecSecurityAndAnalysisSecretScanningNonProviderPatterns
                                  .builder()
                                  .status(
                                      RepositorySpecSecurityAndAnalysisSecretScanningNonProviderPatternsStatus
                                          .ENABLED)
                                  .build())
                          .secretScanningPushProtection(
                              RepositorySpecSecurityAndAnalysisSecretScanningPushProtection
                                  .builder()
                                  .status(
                                      RepositorySpecSecurityAndAnalysisSecretScanningPushProtectionStatus
                                          .ENABLED)
                                  .build())
                          .build())
                  .automatedSecurityFixes(true)
                  .autoLinks(
                      listOf(
                          RepositorySpecAutoLinks.builder()
                              .keyPrefix("DV-")
                              .urlTemplate("https://otto-eg.atlassian.net/browse/DV-<num>")
                              .isAlphanumeric(false)
                              .build()))
                  .rulesets(
                      listOf(
                          RepositorySpecRulesets.builder()
                              .name("Default")
                              .target("branch")
                              .enforcement("active")
                              .conditions(
                                  RepositorySpecRulesetsConditions.builder()
                                      .refName(
                                          RepositorySpecRulesetsConditionsRefName.builder()
                                              .include(listOf("~DEFAULT_BRANCH"))
                                              .exclude(emptyList())
                                              .build())
                                      .build())
                              .rules(
                                  listOf(
                                      RepositorySpecRulesetsRules.builder()
                                          .type("deletion")
                                          .build(),
                                      RepositorySpecRulesetsRules.builder()
                                          .type("non_fast_forward")
                                          .build(),
                                  ))
                              .build()))
                  .build())
          .build()
    }
  }
}
