#!/usr/bin/env bash
# Erzeugt lokal einen Release-Keystore für PaketCast Analytics und bereitet die
# GitHub-Secrets vor. Läuft auf DEINEM Rechner: Keystore und Passwörter verlassen
# das Gerät nur, wenn du sie selbst in GitHub einträgst. Nichts wird committed
# (local-secrets/ ist per .gitignore ausgeschlossen).
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$REPO_DIR/local-secrets"
KS="$OUT/paketcast-release.keystore"
B64="$OUT/keystore.b64"
ALIAS="paketcast"

mkdir -p "$OUT"

if [ -f "$KS" ]; then
  echo "Keystore existiert bereits: $KS"
  echo "Zum Neuerzeugen Datei löschen. Vorher sichern, falls bereits Releases damit signiert wurden!"
  exit 0
fi

echo "== 1/3 Keystore erstellen (Passwörter selbst vergeben und im Passwort-Manager speichern!) =="
keytool -genkey -v -keystore "$KS" -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000

echo "== 2/3 Base64-Datei schreiben =="
if base64 -w0 "$KS" > "$B64" 2>/dev/null; then
  :
else
  base64 -i "$KS" > "$B64"   # macOS
fi

echo "== 3/3 Fertig. Folgende 4 Secrets in GitHub eintragen =="
echo "   Repo -> Settings -> Secrets and variables -> Actions -> New repository secret"
echo ""
echo "   KEYSTORE_BASE64   = kompletter Inhalt von: $B64"
echo "                       (anzeigen z. B. mit: cat $B64)"
echo "   KEYSTORE_PASSWORD = Keystore-Passwort aus Schritt 1"
echo "   KEY_ALIAS         = $ALIAS"
echo "   KEY_PASSWORD      = Key-Passwort aus Schritt 1"
echo ""
echo "Keystore sichern:  cp $KS <sicherer Ort>"
echo "Danach Tag v1.0.0 erneut pushen -> CI baut signiertes APK + AAB."
