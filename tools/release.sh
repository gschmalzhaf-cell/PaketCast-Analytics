#!/usr/bin/env bash
# Release-Prozess mit Changelog-Regel:
#   ./tools/release.sh 1.2.0
# 1. CHANGELOG.md: Abschnitt [Unreleased] wird zu [v1.2.0], frischer [Unreleased] oben angelegt
# 2. versionName/versionCode in app/build.gradle.kts aktualisiert
# 3. Commit + annotierter Tag v1.2.0 + Push (Branch und Tag)
# 4. CI baut signiertes APK/AAB und erstellt das GitHub Release –
#    die Release-Beschreibung wird aus dem Changelog-Abschnitt [v1.2.0] gelesen.
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Aufruf: $0 <version>   z. B. $0 1.2.0"
  exit 1
fi

VERSION="$1"
TAG="v${VERSION}"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_DIR"

GRADLE="app/build.gradle.kts"
CHANGELOG="CHANGELOG.md"

[ -f "$GRADLE" ] || { echo "$GRADLE nicht gefunden"; exit 1; }
[ -f "$CHANGELOG" ] || { echo "$CHANGELOG nicht gefunden"; exit 1; }
grep -q '^## \[Unreleased\]' "$CHANGELOG" || { echo "Kein [Unreleased]-Abschnitt in $CHANGELOG – zuerst pflegen!"; exit 1; }
git diff --quiet || { echo "Es gibt uncommittete Änderungen – erst committen."; exit 1; }
git tag -l "$TAG" | grep -q . && { echo "Tag $TAG existiert bereits."; exit 1; }

echo "== 1/4 Changelog: [Unreleased] -> [$TAG] =="
sed -i "0,/^## \[Unreleased\]/s//## [$TAG]/" "$CHANGELOG"
python3 - "$CHANGELOG" <<'PY'
import sys
path = sys.argv[1]
lines = open(path).read().splitlines(keepends=True)
out = []
inserted = False
for line in lines:
    if not inserted and line.startswith("## ["):
        out.append("## [Unreleased]\n\n")
        inserted = True
    out.append(line)
open(path, "w").writelines(out)
PY

echo "== 2/4 Version in $GRADLE =="
OLD_CODE=$(grep -oP 'versionCode = \K\d+' "$GRADLE")
NEW_CODE=$((OLD_CODE + 1))
sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" "$GRADLE"
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" "$GRADLE"
echo "   versionCode $OLD_CODE -> $NEW_CODE, versionName -> $VERSION"

echo "== 3/4 Commit + Tag =="
git add "$CHANGELOG" "$GRADLE"
git commit -m "chore(release): $TAG – Changelog-Abschnitt [Unreleased] übernommen, versionCode $NEW_CODE"
git tag -a "$TAG" -m "PaketCast Analytics $TAG (siehe CHANGELOG.md)"

echo "== 4/4 Push =="
BRANCH=$(git rev-parse --abbrev-ref HEAD)
git push origin "$BRANCH"
git push origin "$TAG"

echo "Fertig: $TAG gepusht. CI erstellt das GitHub Release inkl. Changelog als Release Notes."
