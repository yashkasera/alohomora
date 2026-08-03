#!/bin/bash
#
# Writes Alohomora's build-metadata manifest into an iOS app bundle.
#
# This is the iOS counterpart to `GenerateAlohomoraConfigTask` in `alohomora-gradle-plugin`. An app
# built from an `.xcodeproj` has no Gradle build for the plugin to hook, so the same git metadata is
# collected here and dropped into the app bundle as JSON, which the library reads at init. The field
# names, the millisecond timestamps and the release-build webhook gate all match the Gradle task
# deliberately — the two producers feed one runtime contract (`BundledBuildInfo`).
#
# Add to the app target as a "Run Script" phase, placed AFTER "Copy Bundle Resources":
#
#     "$SRCROOT/../scripts/alohomora-build-info.sh"
#
# Leave the phase's output files EMPTY and tick "Based on dependency analysis" off: git state changes
# without any input file changing, so a cached phase would ship yesterday's commit sha.
#
# Standalone usage (for testing the output shape):
#
#     scripts/alohomora-build-info.sh --output /tmp/alohomora-build-info.json --configuration Debug
#
set -uo pipefail

readonly RESOURCE_NAME="alohomora-build-info.json"
readonly SCHEMA_VERSION=1

log()  { echo "[Alohomora] $*"; }
warn() { echo "warning: [Alohomora] $*"; }

output=""
repo="${SRCROOT:-$PWD}"
max_commits="${ALOHOMORA_MAX_COMMITS:-50}"
slack_webhook="${ALOHOMORA_SLACK_WEBHOOK_URL:-}"
configuration="${CONFIGURATION:-}"

while [ $# -gt 0 ]; do
  case "$1" in
    --output)         output="${2:-}"; shift 2 ;;
    --repo)           repo="${2:-}"; shift 2 ;;
    --max-commits)    max_commits="${2:-}"; shift 2 ;;
    --slack-webhook)  slack_webhook="${2:-}"; shift 2 ;;
    --configuration)  configuration="${2:-}"; shift 2 ;;
    -h|--help)        sed -n '2,30p' "$0"; exit 0 ;;
    *)                warn "ignoring unknown argument '$1'"; shift ;;
  esac
done

# Default to the app bundle Xcode is currently assembling. Writing into BUILT_PRODUCTS_DIR rather
# than a tracked source file keeps this a zero-touch install: no .xcodeproj edit to add the resource
# to "Copy Bundle Resources", and nothing generated ends up in git.
if [ -z "$output" ]; then
  if [ -n "${BUILT_PRODUCTS_DIR:-}" ] && [ -n "${UNLOCALIZED_RESOURCES_FOLDER_PATH:-}" ]; then
    output="${BUILT_PRODUCTS_DIR}/${UNLOCALIZED_RESOURCES_FOLDER_PATH}/${RESOURCE_NAME}"
  else
    echo "error: [Alohomora] not running under Xcode; pass --output <file>" >&2
    exit 1
  fi
fi

# `--` terminates option parsing so a commit subject beginning with a dash cannot be read as a flag.
git_in_repo() { git -C "$repo" "$@" 2>/dev/null; }

# Only escapes what a git subject can actually contain. `%s` is a single line by definition, so there
# are no raw newlines to worry about; backslash must be substituted first or it would double-escape
# the quotes added after it.
json_escape() {
  printf '%s' "$1" \
    | LC_ALL=C sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e $'s/\t/\\\\t/g' \
    | tr -d '\n\r'
}

if ! git_in_repo rev-parse --git-dir >/dev/null; then
  # Not a failure. CI archives from shallow or exported trees routinely have no `.git`, and an app
  # that builds without git history is far better than a build phase that fails the build.
  warn "no git repository at '$repo'; writing a manifest without git metadata"
fi

branch="$(git_in_repo rev-parse --abbrev-ref HEAD)"
commit_sha="$(git_in_repo rev-parse --short HEAD)"
[ -n "$(git_in_repo status --porcelain)" ] && is_dirty=true || is_dirty=false
build_timestamp_ms="$(( $(date -u +%s) * 1000 ))"

# Mirrors the Gradle task's `debuggable` gate. The URL lands in a plaintext file inside the .ipa —
# a secret recoverable with `unzip` — so a Release configuration never gets one.
if [ -n "$slack_webhook" ] && [ "$configuration" != "Debug" ]; then
  warn "slack webhook is set but configuration '$configuration' is not Debug; omitting it so the secret does not ship in the archive"
  slack_webhook=""
fi

# Xcode configuration names are capitalised ("Debug"); buildType is lowercased to match the value
# Android reports, so anything comparing across platforms sees one spelling. iOS has no flavour
# concept, so flavorName stays null rather than being faked from the scheme name.
build_type="$(printf '%s' "$configuration" | tr '[:upper:]' '[:lower:]')"

mkdir -p "$(dirname "$output")"

{
  printf '{\n'
  printf '  "schemaVersion": %d,\n' "$SCHEMA_VERSION"
  printf '  "branch": "%s",\n' "$(json_escape "${branch:-unknown}")"
  printf '  "commitSha": "%s",\n' "$(json_escape "${commit_sha:-unknown}")"
  printf '  "isDirty": %s,\n' "$is_dirty"
  printf '  "buildTimestampUtc": %s,\n' "$build_timestamp_ms"
  printf '  "variantName": "%s",\n' "$(json_escape "${configuration:-unknown}")"
  if [ -n "$build_type" ]; then
    printf '  "buildType": "%s",\n' "$(json_escape "$build_type")"
  else
    printf '  "buildType": null,\n'
  fi
  printf '  "flavorName": null,\n'
  if [ -n "$slack_webhook" ]; then
    printf '  "slackWebhookUrl": "%s",\n' "$(json_escape "$slack_webhook")"
  else
    printf '  "slackWebhookUrl": null,\n'
  fi
  printf '  "commits": [\n'

  commit_count=0
  # 0x1f (ASCII unit separator) as the delimiter, same as the Gradle task: commit subjects routinely
  # contain '|', which would shift every following field into the wrong slot.
  while IFS=$'\x1f' read -r sha author message committed_at; do
    [ -n "$sha" ] || continue
    [ "$commit_count" -gt 0 ] && printf ',\n'
    # %ct is epoch SECONDS. Converted here, once, because every other timestamp in the project is
    # milliseconds and the panels format it as such.
    printf '    { "sha": "%s", "author": "%s", "message": "%s", "timestamp": %s }' \
      "$(json_escape "$sha")" \
      "$(json_escape "$author")" \
      "$(json_escape "$message")" \
      "$(( ${committed_at:-0} * 1000 ))"
    commit_count=$(( commit_count + 1 ))
    # `tformat`, not `format`: the latter omits the trailing newline, so `read` returns non-zero on
    # the final entry and the loop silently drops the oldest commit.
  done < <(git_in_repo log "-${max_commits}" --pretty=tformat:'%h%x1f%an%x1f%s%x1f%ct')

  [ "$commit_count" -gt 0 ] && printf '\n'
  printf '  ]\n'
  printf '}\n'
} > "$output"

log "wrote ${output##*/}: ${commit_sha:-no sha} on ${branch:-no branch}, dirty=$is_dirty, $commit_count commits"
