# Team config setup

Alohomora can share **journeys**, **mock sets**, and **deep links** with your team through a git
repository, under review and version control. This is entirely optional: local config needs zero setup
— clone the app, record, save locally. The repository enters only when a team wants shared, reviewed
config.

## What you get

- **Local scope** — a personal store on disk (`~/.alohomora/local/`). No git, instant, private.
- **Team scope** — the same artifacts in a git repo, promoted with **Share with team**, which pushes a
  branch and links out to your forge for review. Alohomora never merges to `main`.

## One-time bootstrap (one admin, once)

1. **Create the repo.** Either:
   - Use the `alohomora-config-template` (see [`config-repo-template/README.md`](config-repo-template/README.md))
     via your forge's "Use this template" / fork, or
   - Point Alohomora's **Initialize empty repo** action (Settings → Team config) at an empty repo. It
     scaffolds `journeys/`, `mocks/`, `deeplinks/`, a `config.json` marker, and a `README`, then pushes
     `main`.
2. **Protect `main`.** This is load-bearing and forge-specific — an API/UI setting, not a git
   operation, so the app cannot do it portably. Require a pull/merge request and review on `main`:
   - **GitHub:** Settings → Branches → add a branch protection rule for `main`, require a PR + review.
   - **GitLab:** Settings → Repository → Protected branches + approval rules.
   - **Bitbucket / Gitea / Azure DevOps / self-hosted:** follow that forge's branch-protection docs.
   - A forge with no protection concept cannot offer this guarantee — the app cannot enforce it there.

The app cannot verify protection is on without a forge API. This OSS build documents it; a private
host-API layer can check-and-warn.

## Per-user connect

Settings → **Team config** → paste the repository URL → **Connect**. The app clones to
`~/.alohomora/config-repo`; team config appears immediately. The URL and clone path persist per user in
`java.util.prefs` — nothing is baked into the binary.

## Authentication

- **SSH** via your system agent (`git@host:…` URLs) works out of the box — Alohomora registers the
  Apache MINA sshd transport and uses your `~/.ssh` keys / agent.
- **HTTPS with a personal access token**: paste the token in Settings → Team config → Access token
  when connecting an `https://` repo. It is stored in your **OS keychain** (macOS Keychain, Windows
  Credential Store, or libsecret), keyed by host — never in plain preferences — and reused on later
  launches. Leave it blank if your HTTPS credentials are already cached by git.
- A one-tap OAuth device flow is a later milestone. For now, team mode assumes each user has normal
  git access to the forge (an admin sets up the repo; PMs connect with a token).

## Proposal creation is forge-neutral

Sharing pushes a branch to any git remote (the generic floor). Where an adapter matches the remote,
you also get one-click review creation and the right label — GitLab "merge request", GitHub "pull
request", others "review". An unrecognised host degrades to the floor: the branch is pushed and you
open the review in your tool. No forge adapter is ever required.
