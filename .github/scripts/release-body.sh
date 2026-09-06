#!/usr/bin/env bash
# Writes the GitHub release notes for one version: a header with the logo and the store
# buttons, then three sections: a downloads table, the changelog since the previous release
# folded under a click, and the main dependencies folded the same way.
#
#   release-body.sh <dir with the release files> <dependencies.md>
#
# Reads VERSION and GITHUB_REPOSITORY from the environment, CHANGELOG.md and the git tags from
# the working tree. Writes release-body.md (the GitHub release) and release-notes.md (this
# version's changelog section alone, which the AltStore feed carries).
set -euo pipefail

FILES=$1
DEPS=$2
: "${VERSION:?VERSION is not set}" "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is not set}"
BASE="https://github.com/${GITHUB_REPOSITORY}/releases/download/v${VERSION}"

# The previous release is the newest v* tag that is not this version, so a re-run of the same
# version still measures the changelog from the release before it.
PREV=$(git tag --list 'v*' --sort=-v:refname | grep -vx "v${VERSION}" | head -1 || true)
PREV=${PREV#v}

# This version's own section, for the AltStore feed and as a check that the section exists.
awk -v ver="## ${VERSION}" '
  $0 == ver { found = 1; next }
  found && /^## / { exit }
  found { print }
' CHANGELOG.md > release-notes.md
if [ ! -s release-notes.md ]; then
  echo "::error::CHANGELOG.md has no section for ${VERSION}" >&2
  exit 1
fi

# CHANGELOG.md is newest first, so everything above the previous release's heading is new.
# When that heading is absent (the previous release predates the file) the whole file is.
# Headings step down one level so the release page keeps its own three at the top.
awk -v stop="## ${PREV}" '
  !started && /^## / { started = 1 }
  !started { next }
  $0 == stop { exit }
  { print }
' CHANGELOG.md | sed -e 's/^### /#### /' -e 's/^## /### /' > release-changelog.md

size_mb() {
  local bytes
  bytes=$(stat -c%s "$1" 2>/dev/null || stat -f%z "$1")
  echo "$(( (bytes + 524288) / 1048576 )) MB"
}

# One table row per file matching a pattern; nothing when none does. Matched by pattern rather
# than by a spelled-out name, so the table follows whatever the build names its outputs.
row() {
  local pattern=$1 platform=$2 what=$3 f file
  for f in "$FILES"/$pattern; do
    [ -f "$f" ] || continue
    file=$(basename "$f")
    printf '| [`%s`](%s/%s) | %s | %s | %s |\n' "$file" "$BASE" "$file" "$platform" "$what" "$(size_mb "$f")"
  done
}

# Pin images to this checkout's commit. A version tag may predate new artwork on a rerun.
# Old release pages still keep immutable image URLs if the assets later move.
ASSET_COMMIT=$(git rev-parse --verify HEAD)
RAW="https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/${ASSET_COMMIT}"
ALTSTORE="https://celloserenity.github.io/altdirect/?url=https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/refs/heads/master/altstore_yuroyami.json"

{
  echo '<p align="center">'
  echo "  <img src=\"${RAW}/androidApp/src/main/res/mipmap-xxxhdpi/ic_launcher.png\" width=\"96\" alt=\"Synkplay\">"
  echo '</p>'
  echo '<p align="center">'
  echo "  <a href=\"https://play.google.com/store/apps/details?id=com.yuroyami.syncplay\"><img src=\"${RAW}/art/badges/google-play.png\" width=\"150\" alt=\"Get it on Google Play\"></a>"
  echo "  &nbsp;&nbsp;"
  echo "  <a href=\"https://apps.apple.com/us/app/synkplay/id6760187432\"><img src=\"${RAW}/art/badges/app-store.png\" width=\"150\" alt=\"Download on the App Store\"></a>"
  echo "  &nbsp;&nbsp;"
  echo "  <a href=\"https://apt.izzysoft.de/fdroid/index/apk/com.reddnek.syncplay\"><img src=\"${RAW}/art/badges/IzzyOnDroid.png\" width=\"150\" alt=\"Get it on IzzyOnDroid\"></a>"
  echo "  &nbsp;&nbsp;"
  echo "  <a href=\"${ALTSTORE}\"><img src=\"${RAW}/art/badges/AltSource_Blue.png\" width=\"150\" alt=\"Add the AltStore source\"></a>"
  echo '</p>'
  echo
  echo "## Downloads"
  echo
  echo "| File | Platform | What it is | Size |"
  echo "|---|---|---|---|"
  row "*-full-universal.apk" "Android 8.0 and up" \
    "The full app: ExoPlayer, mpv and KitePlayer, every CPU type in one file. Pick this one."
  row "*-exo-only.apk" "Android 8.0 and up" \
    "ExoPlayer only, and much smaller. This is the build IzzyOnDroid carries. It installs beside the full app as a separate app."
  row "*-ios.ipa" "iOS 14.1 and up" \
    "Sideload with AltStore or install it directly. The [install guide](https://github.com/${GITHUB_REPOSITORY}/wiki/How-to-install-the-app-on-iOS) covers both."
  row "*.dmg" "macOS" "The desktop app."
  row "*.msi" "Windows" "The desktop app."
  row "*.deb" "Linux (Debian and Ubuntu)" "The desktop app."
  echo
  echo "## Changelog"
  echo
  if [ -n "$PREV" ]; then
    echo "<details><summary><b>Everything since v${PREV}</b>. Click to unfold.</summary>"
  else
    echo "<details><summary><b>Everything in this release</b>. Click to unfold.</summary>"
  fi
  echo
  cat release-changelog.md
  echo
  echo "</details>"
  echo
  echo "## Dependencies"
  echo
  echo "<details><summary><b>The main ones</b>: toolchain, network stack, video engines. Click to unfold.</summary>"
  echo
  cat "$DEPS"
  echo
  echo "</details>"
} > release-body.md

echo "release-body.md written: $(wc -l < release-body.md) lines, changelog since ${PREV:-the beginning}"
