package de.benkeil.operator.github.domain

import de.benkeil.operator.github.adapter.KubernetesPresenter
import de.benkeil.operator.github.domain.model.GitHubRepositoryResource
import de.benkeil.operator.github.domain.model.GitHubRepositorySpec
import de.benkeil.operator.github.domain.service.CreateGitHubRepositoryRequest
import de.benkeil.operator.github.domain.service.GitHubRepositoryResponse
import de.benkeil.operator.github.domain.service.GitHubService
import de.benkeil.operator.github.domain.service.User
import de.benkeil.support.everySuspending
import de.benkeil.support.verifySuspending
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk

class UpsertRepositoryUseCaseTest :
    FunSpec({
      val owner = "otto"
      val name = "operator"

      val gitHubService = mockk<GitHubService>()
      val upsertTeamsUseCase = mockk<UpsertTeamsUseCase>()
      val upsertCollaboratorsUseCase = mockk<UpsertCollaboratorsUseCase>()
      val updateAutomatedSecurityFixesUseCase = mockk<UpdateAutomatedSecurityFixesUseCase>()
      val upsertAutoLinksUseCase = mockk<UpsertAutoLinksUseCase>()
      val upsertRuleSetsUseCase = mockk<UpsertRuleSetsUseCase>()
      val useCase =
          UpsertRepositoryUseCase(
              gitHubService,
              upsertTeamsUseCase,
              upsertCollaboratorsUseCase,
              updateAutomatedSecurityFixesUseCase,
              upsertAutoLinksUseCase,
              upsertRuleSetsUseCase,
          )
      val presenter = KubernetesPresenter()

      beforeTest {
        everySuspending { upsertTeamsUseCase.execute(any()) } returns Unit
        everySuspending { upsertCollaboratorsUseCase.execute(any()) } returns Unit
        everySuspending { updateAutomatedSecurityFixesUseCase.execute(any()) } returns Unit
        everySuspending { upsertAutoLinksUseCase.execute(any()) } returns Unit
        everySuspending { upsertRuleSetsUseCase.execute(any()) } returns Unit
      }

      afterTest { clearAllMocks() }

      test("create") {
        // given
        val request =
            CreateGitHubRepositoryRequest(
                owner = owner,
                name = name,
                description = null,
                private = null,
                visibility = null,
                autoInit = null,
            )
        everySuspending { gitHubService.getRepository(owner, name) } returns null
        everySuspending { gitHubService.createRepository(any()) } returns
            GitHubRepositoryResponse(
                owner = User(login = owner, id = 1),
                name = name,
                fullName = null,
                description = null,
                private = null,
                visibility = null,
                autoInit = null,
                deleteBranchOnMerge = null,
                allowAutoMerge = null,
                allowSquashMerge = null,
                allowMergeCommit = null,
                allowRebaseMerge = null,
                allowUpdateBranch = null,
                useSquashPrTitleAsDefault = null,
                squashMergeCommitTitle = null,
                squashMergeCommitMessage = null,
                mergeCommitTitle = null,
                mergeCommitMessage = null,
                securityAndAnalysis = null,
            )
        everySuspending { gitHubService.updateRepository(any()) } returns
            GitHubRepositoryResponse(
                owner = User(login = owner, id = 1),
                name = name,
                fullName = null,
                description = null,
                private = null,
                visibility = null,
                autoInit = null,
                deleteBranchOnMerge = null,
                allowAutoMerge = null,
                allowSquashMerge = null,
                allowMergeCommit = null,
                allowRebaseMerge = null,
                allowUpdateBranch = null,
                useSquashPrTitleAsDefault = null,
                squashMergeCommitTitle = null,
                squashMergeCommitMessage = null,
                mergeCommitTitle = null,
                mergeCommitMessage = null,
                securityAndAnalysis = null,
            )

        // when
        useCase.execute(
            {
              GitHubRepositoryResource().apply {
                spec = GitHubRepositorySpec(owner = owner, name = name)
              }
            },
            presenter)

        // then
        verifySuspending(exactly = 1) { gitHubService.createRepository(request) }
        verifySuspending(exactly = 1) { gitHubService.updateRepository(any()) }
      }

      test("update") {
        // given
        val request =
            CreateGitHubRepositoryRequest(
                owner = owner,
                name = name,
                description = null,
                private = null,
                visibility = null,
                autoInit = null,
            )
        val repo =
            GitHubRepositoryResponse(
                owner = User(login = owner, id = 1),
                name = name,
                fullName = null,
                description = null,
                private = null,
                visibility = null,
                autoInit = null,
                deleteBranchOnMerge = null,
                allowAutoMerge = null,
                allowSquashMerge = null,
                allowMergeCommit = null,
                allowRebaseMerge = null,
                allowUpdateBranch = null,
                useSquashPrTitleAsDefault = null,
                squashMergeCommitTitle = null,
                squashMergeCommitMessage = null,
                mergeCommitTitle = null,
                mergeCommitMessage = null,
                securityAndAnalysis = null,
            )
        everySuspending { gitHubService.getRepository(owner, name) } returns repo
        everySuspending { gitHubService.updateRepository(any()) } returns repo

        // when
        useCase.execute(
            {
              GitHubRepositoryResource().apply {
                spec = GitHubRepositorySpec(owner = owner, name = name)
              }
            },
            presenter)

        // then
        verifySuspending(exactly = 0) { gitHubService.createRepository(any()) }
        verifySuspending(exactly = 1) { gitHubService.updateRepository(any()) }
      }
    })
