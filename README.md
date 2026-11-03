# PaketCast Analytics

Eine Android-App (Kotlin, Jetpack Compose, Material 3), die die Statistik der
Podcast-App **Pocket Casts** erweitert und lokal auswertet. Alle Daten bleiben
auf dem Gerät – es gibt keine Netzwerkzugriffe.

## Funktionen

- **Übersicht:** Gesamte Hörzeit, abgeschlossene / angefangene / ungespielte
  Folgen, noch zu hörende Zeit, Anzahl Podcasts, Ø Hörzeit pro Folge,
  meistgehörter Podcast, längste Folge.
- **Podcasts:** jeder Podcast mit Hörzeit, abgeschlossenen Folgen
  (wie oft gehört), offenen Folgen, Restzeit und Fortschrittsbalken –
  sortiert nach Hörzeit.
- **Top 10:** persönliche Top 10 nach Hörzeit und nach Anzahl
  abgeschlossener Folgen.
- **Netzwerke:** Auswertung nach Podcast-Netzwerk (Autor-Feld, Fallback:
  Feed-Domain) mit Anzahl Podcasts, Hörzeit und abgeschlossenen Folgen.
- **Episoden:** längste und kürzeste Folgen (Top 10 jeweils).
- **Offen:** was ist noch zu hören – Restzeit gesamt, offen pro Podcast und
  Liste der offenen Folgen (neueste zuerst).

## Datenquelle

Pocket Casts bietet keinen offiziellen Export der Hörstatistik. Die App liest
daher eine **Kopie der Pocket-Casts-Datenbank** `pocketcasts.db`:

- Paketname: `au.com.shiftyjelly.pocketcasts`
- Pfad: `/data/data/au.com.shiftyjelly.pocketcasts/databases/pocketcasts.db`

Ablauf (einmalig pro Datenstand):

1. Mit gerootetem Gerät oder per `adb backup` die Datenbank kopieren, z. B.:
   - `adb shell "su -c 'cp /data/data/au.com.shiftyjelly.pocketcasts/databases/pocketcasts.db /sdcard/'"` und anschließend `adb pull /sdcard/pocketcasts.db`
   - oder `adb backup -noapk au.com.shiftyjelly.pocketcasts` (Backup enthält die Datenbank)
2. In der App oben rechts auf das Import-Symbol tippen und die Datei
   `pocketcasts.db` auswählen.

Alternativ kann der **OPML-Export** aus Pocket Casts importiert werden
(Einstellungen → Import & Export). Damit sieht man die Liste der Abos; für die
Statistiken wird aber die Datenbank benötigt.

Hinweis: Falls die Datenbank im WAL-Modus vorliegt, kann die reine
`pocketcasts.db` die neuesten Daten vermissen. Am besten die App sauber
beenden oder die gesamte `databases`-Schreibweise inklusive `-wal`-Datei
kopieren.

## Annahmen zur Auswertung

- Hörzeit einer Folge = `played_up_to` (Sekunden), begrenzt auf die Dauer.
- Eine Folge gilt als **abgeschlossen**, wenn höchstens 10 s fehlen.
- **Noch zu hören** = nicht abgeschlossen und nicht archiviert; Restzeit =
  Dauer − bereits gehört.
- **Netzwerk** = `author` des Podcasts, sonst Domain der Feed-URL, sonst
  „Unbekannt“.

## Projektstruktur

- `data/model` – Datenklassen (Podcast, Episode, OPML-Abo)
- `data/db/PocketCastsDbReader` – liest `pocketcasts.db` read-only; Spalten
  werden per `PRAGMA table_info` erkannt (versionsrobust)
- `data/opml/OpmlParser` – parst den OPML-Export
- `data/repository` – Import-Flow (Datei kopieren, parsen, Snapshot bauen)
- `stats/StatisticsEngine` – reine Statistik-Berechnung (unit-getestet)
- `ui/` – Compose-Screens, Navigation, ViewModel
- `tools/verify_stats.py` – verifiziert SQL + Logik gegen eine synthetische
  Datenbank (`python3 tools/verify_stats.py`)

## Build

Voraussetzungen: JDK 17, Android SDK (compileSdk 35).

```bash
# Gradle-Wrapper einmalig erzeugen (falls gradlew fehlt):
gradle wrapper --gradle-version 8.9

./gradlew assembleDebug          # APK bauen
./gradlew testDebugUnitTest      # Unit-Tests der Statistik-Engine
```

Oder das Projekt direkt in Android Studio öffnen.

## Release (erste Version v1.0.0)

Der Release läuft über GitHub Actions (`.github/workflows/release.yml`): Beim Push eines
Tags `v*.*.*` wird die App gebaut, getestet und als **GitHub Release** veröffentlicht.

### Einmalig: Release-Keystore anlegen und als GitHub-Secrets hinterlegen

Am einfachsten lokal auf dem eigenen Rechner (Key verlässt das Gerät nicht automatisch):

```bash
./tools/create-keystore.sh
```

Das Skript erzeugt `local-secrets/paketcast-release.keystore` plus `local-secrets/keystore.b64`
(per `.gitignore` ausgeschlossen) und zeigt an, welche Werte wohin gehören.

Manuell geht es so:

```bash
# Es wird kein vorhandener Keystore benötigt – keytool legt einen neuen an (PKCS12):
keytool -genkey -v -storetype PKCS12 -keystore release.keystore -alias paketcast \
  -keyalg RSA -keysize 2048 -validity 10000
# Keystore als Base64 in ein Secret packen (Linux):
base64 -w0 release.keystore
# macOS:
# base64 -i release.keystore
```

Hinweis: Bestehenden **JKS**-Keystore wiederverwenden? Dann in `app/build.gradle.kts`
im `signingConfigs.release` statt `storeType = "PKCS12"` den Wert `"JKS"` setzen.
Die Dateiendung (`.jks`/`.keystore`/`.p12`) ist egal – das Format zählt.

Im Repo unter **Settings → Secrets and variables → Actions** anlegen:

- `KEYSTORE_BASE64` – Base64-Inhalt des Keystores
- `KEYSTORE_PASSWORD` – Keystore-Passwort
- `KEY_ALIAS` – z. B. `paketcast`
- `KEY_PASSWORD` – Key-Passwort

**Ohne** diese Secrets baut der Workflow stattdessen eine debug-signierte APK
(nur zum Testen, nicht zur Verteilung geeignet).

### Release auslösen

```bash
git tag v1.0.0
git push origin v1.0.0
```

Der Workflow erstellt dann automatisch das GitHub Release. Die Assets heißen:

- `PaketCast-Analytics-<Version>.apk` – signierte APK (sobald der Keystore konfiguriert ist)
- `PaketCast-Analytics-<Version>-debug.apk` – debug-signierte Test-APK (ohne Keystore)
- `PaketCast-Analytics-<Version>.aab` – signiertes Bundle für den Play Store

Jeder Push auf `vibe/**` bzw. jeder PR wird außerdem von `.github/workflows/build.yml`
gebaut und getestet (`assembleDebug testDebugUnitTest`).

### Installation mit Obtainium

1. Obtainium installieren (z. B. über F-Droid).
2. In Obtainium „App hinzufügen“ → Quelle **GitHub** → Repository `gschmalzhaf-cell/PaketCast-Analytics`.
3. Release `v1.0.0` auswählen und als Asset das APK mit dem Muster `PaketCast-Analytics-.*\.apk` wählen
   (aktuell `PaketCast-Analytics-1.0.0-debug.apk`; nach dem Keystore-Setup `PaketCast-Analytics-1.0.0.apk`).
4. Bei jedem neuen Tag (z. B. `v1.0.1`) meldet Obtainium automatisch ein verfügbares Update.

Hinweis: Die debug-signierte und die später signierte Version haben unterschiedliche Signaturen –
beim Wechsel einmal deinstallieren (die importierte Datenbank liegt im App-Speicher).

## Datenschutz

Die App benötigt keine Berechtigungen und keine Internetverbindung. Die
 gewählte Datei wird in den privaten App-Speicher kopiert und nur lokal
 ausgelesen.

## Roadmap / Ideen

- [ ] Hörverlauf pro Tag/Woche (sobald Zeitstempel des Hörens verfügbar sind)
- [ ] Export der Statistiken als CSV/JSON
- [ ] Filter nach Zeitraum
- [ ] Vergleich mehrerer Importe (Entwicklung über die Zeit)
- [ ] weitere Vorschläge gerne als Issue ergänzen
