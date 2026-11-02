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
