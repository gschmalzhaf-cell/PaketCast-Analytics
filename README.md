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
- **Verlauf (ab v1.1.0):** Hörzeit pro Tag/Woche als Balkendiagramm
  (aus der Verlauf-Tabelle `episode_interactions` der Datenbank; fehlt diese
  Tabelle, zeigt der Screen einen entsprechenden Hinweis) mit Gesamtsumme
  und Ø pro aktivem Tag.

## Wo finde ich die Datenbank auf dem Handy?

Die Datei liegt an folgendem Pfad (nur mit Root oder per adb erreichbar, da
`/data/data/...` geschützt ist):

```
/data/data/au.com.shiftyjelly.pocketcasts/databases/pocketcasts.db
```

Paketname: `au.com.shiftyjelly.pocketcasts`. Liegt die App im WAL-Modus, gehören
dazu noch `pocketcasts.db-wal` und `pocketcasts.db-shm` (damit fehlen keine
neuesten Einträge) – am einfachsten Pocket Casts vor dem Kopieren sauber beenden.

**Ohne Root (einfachster Weg für jeden Nutzer):** Neuere Pocket-Casts-Versionen
können die Datenbank selbst exportieren (Support-Artikel „Exporting the App
Database“): In Pocket Casts unter **Einstellungen → Erweitert/Support → Datenbank
exportieren** die Datei per Teilen-Dialog (z. B. an dich selbst mailen/Cloud)
übertragen und in dieser App über das Import-Symbol auswählen. Der gleiche
Hinweis steht direkt im Import-Dialog der App.

## Datenquelle: wie komme ich an die Pocket-Casts-Daten?

Pocket Casts ist **Open Source** (seit Okt. 2022, MPL 2.0):
[github.com/Automattic/pocket-casts-android](https://github.com/Automattic/pocket-casts-android)
(plus `pocket-casts-ios`). Das DB-Schema (Room-Entities, z. B. Tabelle
`podcast_episodes`) ist dort unter `modules/services/model` einsehbar –
diese App liest sowohl das alte Schema (`episodes`) als auch das Room-Schema
(`podcast_episodes`), inkl. Sekunden/Millisekunden-Zeitstempeln.

### Variante A – Datenbank-Export direkt in der Pocket-Casts-App (ohne Root, empfohlen)

Neuere Pocket-Casts-Versionen können die Datenbank selbst exportieren
(siehe Pocket-Casts-Support: „Exporting the App Database“):

1. In Pocket Casts: **Profil/Einstellungen → (Erweitert/Support) → Datenbank exportieren**
   (liefert eine ZIP/DB per Teilen-Dialog).
2. Datei auf den PC übertragen (USB, Cloud, Mail an sich selbst).
3. In PaketCast Analytics oben rechts auf das Import-Symbol tippen und die
   `pocketcasts.db` (ggf. aus der ZIP entpacken) auswählen.

### Variante B – gerootetes Gerät + adb

- Paketname: `au.com.shiftyjelly.pocketcasts`
- Pfad: `/data/data/au.com.shiftyjelly.pocketcasts/databases/pocketcasts.db`

```bash
adb shell "su -c 'cp /data/data/au.com.shiftyjelly.pocketcasts/databases/pocketcasts.db* /sdcard/'"
adb pull /sdcard/pocketcasts.db        # + pocketcasts.db-wal / -shm, falls vorhanden
```

Hinweis WAL-Modus: Liegt die DB im WAL-Modus, fehlen in der reinen
`pocketcasts.db` evtl. die neuesten Einträge – am besten Pocket Casts sauber
beenden oder `pocketcasts.db-wal`/`-shm` mitkopieren.

### Variante C – adb backup (eingeschränkt)

`adb backup -noapk au.com.shiftyjelly.pocketcasts` funktioniert nur, wenn die
App `allowBackup` erlaubt; seit Android 12 ist `adb backup` für die meisten
Apps deaktiviert. Das Backup (.ab) lässt sich z. B. mit
[android-backup-extractor](https://github.com/ultra-utility/android-backup-extractor)
entpacken.

### Alternative: OPML-Export

Einstellungen → Import & Export → OPML exportieren. Damit sieht man in der App
die Liste der Abos; für Statistiken wird aber die Datenbank benötigt.

### Gibt es eine API?

**Offiziell nein** – laut [Pocket-Casts-Support](https://support.pocketcasts.com/knowledge-base/pocket-casts-api/)
gibt es keine öffentliche API (Feature-Request offen). Es existiert jedoch eine
**inoffizielle interne API** (`https://api.pocketcasts.com`), die der Web-Player
nutzt: `POST /user/login` (E-Mail + Passwort) liefert ein Bearer-Token, damit z. B.
`POST /user/history` (laut Forum auf ca. 100 Einträge begrenzt). Bekannte Wrapper:
[npm `pocketcasts`](https://www.npmjs.com/package/pocketcasts),
[PyPI `pocketcasts-api`](https://pypi.org/project/pocketcasts-api/).
Risiken: Endpunkte können ohne Vorwarnung ändern/abgeschaltet werden; Login
erfordert das Account-Passwort. Daher nutzt diese App bewusst den lokalen
DB-Import (keine Cloud, keine Passwörter). Ein optionaler Login über die
inoffizielle API ist als Roadmap-Punkt vorgemerkt.

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

### Release auslösen (mit Changelog-Regel)

**Regel: Der Changelog landet immer im aktuellen Release-Tag.** Jeder PR pflegt
`CHANGELOG.md` im Abschnitt `[Unreleased]`. Beim Release wird dieser Abschnitt
automatisch zum Tag-Abschnitt und als **Release Notes (Beschreibung)** des
GitHub-Releases veröffentlicht – so sieht jeder Nutzer sofort, was neu ist.

```bash
# 1. CHANGELOG.md unter [Unreleased] gepflegt? Dann:
./tools/release.sh 1.2.0
# macht: [Unreleased] -> [v1.2.0] in CHANGELOG.md, versionName/versionCode in
# app/build.gradle.kts erhöhen, Commit, annotierter Tag v1.2.0, Push von Branch + Tag.
# CI liest den Changelog-Abschnitt [v1.2.0] und setzt ihn als Release-Beschreibung.
```

Manuell geht es auch, dann aber Changelog nicht vergessen:

```bash
git tag v1.2.0 && git push origin v1.2.0
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

Ohne Konto-Verbindung arbeitet die App komplett offline: Die gewählte Datei
wird in den privaten App-Speicher kopiert und nur lokal ausgelesen.

**Ausnahme „Konto verbinden“ (v1.3.0):** Dabei werden E-Mail + Passwort einmalig
an `https://api.pocketcasts.com/user/login` (inoffizielle API des Web-Players)
gesendet; die App erhält ein Bearer-Token und lädt Abos/Verlauf/Sternchen von
`api.pocketcasts.com`. Das Passwort wird nicht gespeichert, der Token nur im
Arbeitsspeicher gehalten. Es findet keine weitere Netzwerkkommunikation statt
(INTERNET-Berechtigung wird nur für diesen optionalen Schritt genutzt).

## Roadmap / Ideen

- [x] Hörverlauf pro Tag/Woche (v1.1.0, via `episode_interactions`)
- [x] Export der Statistiken als CSV/JSON (v1.2.0)
- [x] Login über die inoffizielle Pocket-Casts-API (`/user/login` + `/user/history`, `/user/starred`, `/user/subscriptions`) (v1.3.0)
- [ ] **Logische Ergänzungen (neu aufgenommen):**
  - [ ] Suche in Podcasts/Episoden (Textfilter in den Listen)
  - [ ] Sortierung der Podcasts-Liste wählbar (Hörzeit, Name, neueste Folge, Fortschritt)
  - [ ] Filter-Chips in den Listen (Alle / nur offen / nur abgeschlossen / in Arbeit)
  - [ ] Zeitraum-Filter für Verlauf und Statistiken (z. B. „dieses Jahr“, „letzte 30 Tage“)
  - [ ] Jahresrückblick („End of Year“-Style: Top-Podcasts des Jahres, meiste Hörzeit pro Monat)
  - [ ] Vergleich mehrerer Importe (Entwicklung über die Zeit)
  - [ ] API: mehr Endpunkte (Up-Next-Queue, Bookmarks, Hörbücher/User-Files) + Token sicher im Android Keystore statt nur im RAM
  - [ ] Dark Theme / dynamisches Theme
  - [ ] Home-Screen-Widget „zuletzt gehört / noch offen“
- [ ] weitere Vorschläge gerne als Issue ergänzen
