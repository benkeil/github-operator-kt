# Proof of Ownership

For existing repositories, you can prove ownership by adding a specific environment variable to your repository
settings.

Add the environment `github-operator` and inside the variable `MANAGED_BY_NAMESPACE` with the namespace of your team
which is allowed to manage the repository.

For new repositories, the operator adds this variable automatically.

If you want to move ownership of a repository to another team, you must change the value of the variable to the
namespace of the other team.
