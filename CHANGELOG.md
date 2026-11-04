# Changelog

Alle nennenswerten Änderungen an PaketCast Analytics. Format: [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

**Regel:** Jeder PR pflegt den Abschnitt `[Unreleased]`. Beim Release wird dieser
Abschnitt per `./tools/release.sh <version>` in `## [vX.Y.Z]` umbenannt und landet
automatisch als Beschreibung (Release Notes) im GitHub-Release des Tags.

## [Unreleased]

### Hinzugefügt
- Pocket-Casts-Konto verbinden (inoffizielle API): Import-Dialog → „Konto verbinden…“, Login mit E-Mail/Passwort gegen `api.pocketcasts.com/user/login` (Passwort wird nicht gespeichert), lädt Abos, Verlauf (`/user/history`) und Sternchen (`/user/starred`); Verlauf ist im Tab „Verlauf“ sichtbar. INTERNET-Berechtigung wird nur für api.pocketcasts.com benötigt

## [v1.2.1]

### Behoben
- Bottom-Navigation: Tippen auf einen Tab wechselt jetzt zuverlässig den Screen (bisher blieb durch `saveState`/`restoreState` teils der alte Screen stehen) – vereinfacht auf `navigate` mit `popUpTo(start)` ohne State-Save

## [v1.2.0]

### Hinzugefügt
- Export der Statistiken: Menü oben rechts → „Podcasts als CSV teilen“ (Semikolon-CSV für Excel) oder „Alle Statistiken als JSON teilen“ (Übersicht, Podcasts, Top 10, Netzwerke, Extrema, Offen, Verlauf) – per Share-Sheet, lokal, ohne Datei-Provider

### Geplant
- Login über die inoffizielle Pocket-Casts-API (`api.pocketcasts.com/user/*`)
- Filter nach Zeitraum, Vergleich mehrerer Importe

## [v1.1.1] – 2026-11-03

### Hinzugefügt
- Doku: Wie komme ich an die `pocketcasts.db` (App-interner DB-Export ohne Root, Root+adb inkl. WAL-Hinweis, `adb backup`, OPML), Open-Source-Referenz (Automattic/pocket-casts-android, MPL-2.0) und API-Recherche (offiziell keine API; inoffiziell `api.pocketcasts.com/user/login` + `/user/history`, ~100 Einträge)

### Geändert
- DB-Reader unterstützt jetzt das aktuelle Room-Schema: Episoden-Tabelle `podcast_episodes` (alternativ alt: `episodes`); `added`/`published` werden als Millisekunden-Epochs normalisiert
- Release-Notes kommen ab jetzt aus diesem Changelog (Abschnitt des jeweiligen Tags)

## [v1.1.0] – 2026-11-03

### Hinzugefügt
- Neuer Screen **„Verlauf“**: Hörzeit pro Tag/Woche als Balkendiagramm (letzte 30 Tage / 12 Wochen), Tag/Woche-Umschalter, Gesamtsumme und Ø pro aktivem Tag
- DB-Reader liest optional die Verlauf-Tabelle (`episode_interactions` u. a., defensive Spaltenerkennung, Action-Filter, ms/s-Normalisierung)

## [v1.0.0] – 2026-11-03

### Hinzugefügt
- Erste Version: Import von `pocketcasts.db` (read-only, SAF) und OPML-Export; alles lokal, keine Berechtigungen, kein Netzwerk
- Screens: Start (Übersicht), Podcasts, Top 10 (nach Hörzeit und abgeschlossenen Folgen), Netzwerke, Episoden (längste/kürzeste), Offen (noch zu hören)
- Signierte Releases via GitHub Actions (Tag `v*.*.*` → APK+AAB als GitHub Release, Obtainium-kompatibel: `PaketCast-Analytics-<Version>.apk`)
