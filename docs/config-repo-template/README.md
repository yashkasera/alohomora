# Alohomora config

Version-controlled **journeys**, **mock sets**, and **deep links** for your team, managed by the
[Alohomora](https://github.com/yashkasera/Alohomora) desktop app.

Use this repository as a template (fork it, or your forge's "Use this template"), then connect the
desktop app to it: Settings → Team config → paste this repo's URL → Connect.

## Layout

```
config.json            # { schemaVersion, marker: "alohomora-config" }
journeys/*.json        # event journeys
mocks/*.json           # mock rule sets
deeplinks/{module}/*.json   # typed deep-link definitions, grouped by module
deeplinks/README.md    # generated catalog (do not edit by hand)
```

Each artifact is one JSON file with a stable `id` and `name`; the filename is a readable slug of the
name. The app writes deterministic JSON, so re-saving an unchanged artifact produces no diff.

## Protect `main` (required for reviewed config)

Alohomora always works on a branch and links out for review; it never merges. Enforce that server-side
by protecting `main`:

- **GitHub:** Settings → Branches → protect `main`, require a pull request + review.
- **GitLab:** Settings → Repository → Protected branches + approval rules.
- **Bitbucket / Gitea / Azure DevOps / self-hosted:** follow that forge's branch-protection docs.

A forge with no branch-protection concept cannot offer this guarantee.

## How changes arrive

Contributors edit locally in the app and click **Share with team**. The app pushes a
`proposal/<name>-<user>-<date>` branch and opens (or links to) a review. Approve and merge in your
forge as usual.
