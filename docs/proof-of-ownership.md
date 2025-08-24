# Proof of Ownership

## Methods

We should proof ownership by team, so we don't need the functional user anymore.

The functional user could be completely removed if we auto manage a dedicated GitHub App for every team.

### Exactly one

> Check that exactly one team has admin permissions.

### Full match

> Check that all teams with admin permissions are identical between spec and repository.

## Example Code

> Operator runs as Organization Admin.

### Use Case: hijack a repository

```kotlin
val spec =
    GitHubRepositorySpec(
        owner = "otto-ec",
        name = "tesla-service",
        teams =
            listOf(
                Team(name = "pdh-da", permission = "admin"),
                Team(name = "tesla", permission = "admin"),
            ),
        collaborators =
            listOf(
                Collaborator(name = "fkt-pdh-da", permission = "admin"),
            ),
    )

if (gitHubService.getRepository(spec.owner, spec.name) == null) {
  gitHubService.createRepository(spec)
  setTeams(spec.owner, spec.name, spec.collaborators)
}

val repo = gitHubService.getRepository(spec.owner, spec.name)

val specOwners = spec.teams.filter { it.permission == "admin" }
val repoOwners = repo.teams.filter { it.permission == "admin" }

// proof of ownership
if (specOwners == repoOwners) {
  gitHubService.updateRepositoryTeams(repo, spec.teams)
} else {
  error("Ownership could not be verified")
}

```

## Questions

- How to change the ownership later?
- How to remove one team from ownership?
