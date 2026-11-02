#!/usr/bin/env python3
"""Verifiziert die SQL-Abfragen und die Statistik-Logik der App gegen eine
synthetische pocketcasts.db.

Die SELECT-Anweisungen und Grenzwerte (FINISH_TOLERANCE_SEC = 10) entsprechen
1:1 der Kotlin-Implementierung (PocketCastsDbReader / StatisticsEngine).

Aufruf: python3 tools/verify_stats.py
"""
import os
import sqlite3
import sys
import tempfile

FINISH_TOLERANCE_SEC = 10

PODCASTS_SQL = (
    "SELECT uuid AS uuid, title AS title, author AS author, "
    "podcastUrl AS feedUrl, added_date AS added FROM podcasts"
)
EPISODES_SQL = (
    "SELECT uuid AS uuid, podcast_uuid AS podcastUuid, title AS title, "
    "duration AS duration, played_up_to AS playedUpTo, played_status AS playedStatus, "
    "published_date AS published, added_date AS added, archived AS archived FROM episodes"
)


def listened_seconds(duration, played_up_to):
    played = max(0, played_up_to)
    return min(played, duration) if duration > 0 else played


def is_finished(duration, played_up_to):
    return duration > 0 and played_up_to >= duration - FINISH_TOLERANCE_SEC


def remaining_seconds(duration, played_up_to):
    if duration > 0 and not is_finished(duration, played_up_to):
        return max(0, duration - max(0, played_up_to))
    return 0


def build_db(path):
    db = sqlite3.connect(path)
    db.executescript(
        """
        CREATE TABLE podcasts (
            uuid TEXT PRIMARY KEY,
            title TEXT,
            author TEXT,
            podcastUrl TEXT,
            added_date INTEGER
        );
        CREATE TABLE episodes (
            uuid TEXT PRIMARY KEY,
            podcast_uuid TEXT,
            title TEXT,
            duration INTEGER,
            played_up_to INTEGER,
            played_status INTEGER,
            published_date INTEGER,
            added_date INTEGER,
            archived INTEGER,
            size INTEGER
        );
        """
    )
    db.executemany(
        "INSERT INTO podcasts VALUES (?,?,?,?,?)",
        [
            ("p1", "Alpha", "Netz A", "https://feeds.alpha.de/rss", 1700000000),
            ("p2", "Beta", None, "https://www.beta.fm/feed.xml", 1700000100),
            ("p3", "Gamma", "", None, 1700000200),
        ],
    )
    db.executemany(
        "INSERT INTO episodes VALUES (?,?,?,?,?,?,?,?,?,?)",
        [
            ("e1", "p1", "A1", 600, 600, 2, 1700001000, 1700001000, 0, 1000),
            ("e2", "p1", "A2", 600, 300, 1, 1700002000, 1700002000, 0, 1000),
            ("e3", "p2", "B1", 300, 0, 0, 1700003000, 1700003000, 0, 1000),
            ("e4", "p2", "B2", 120, 120, 3, 1700004000, 1700004000, 1, 1000),
            ("e5", "p3", "G1", 0, 45, 1, 1700005000, 1700005000, 0, 1000),
            ("e6", "p1", "A3", 3600, 100, 1, 1700006000, 1700006000, 0, 1000),
        ],
    )
    db.commit()
    db.close()


def main():
    with tempfile.TemporaryDirectory() as tmp:
        db_path = os.path.join(tmp, "pocketcasts.db")
        build_db(db_path)
        db = sqlite3.connect(db_path)
        db.row_factory = sqlite3.Row

        podcasts = [dict(r) for r in db.execute(PODCASTS_SQL)]
        episodes = [dict(r) for r in db.execute(EPISODES_SQL)]
        db.close()

        assert len(podcasts) == 3, podcasts
        assert len(episodes) == 6, episodes

        total_listened = sum(listened_seconds(e["duration"], e["playedUpTo"]) for e in episodes)
        finished = [e for e in episodes if is_finished(e["duration"], e["playedUpTo"])]
        in_progress = [
            e for e in episodes
            if not is_finished(e["duration"], e["playedUpTo"])
            and listened_seconds(e["duration"], e["playedUpTo"]) > 0
        ]
        unplayed = len(episodes) - len(finished) - len(in_progress)
        open_eps = [e for e in episodes if not e["archived"] and not is_finished(e["duration"], e["playedUpTo"])]
        total_remaining = sum(remaining_seconds(e["duration"], e["playedUpTo"]) for e in open_eps)

        assert total_listened == 600 + 300 + 0 + 120 + 45 + 100, total_listened
        assert len(finished) == 2 and {e["uuid"] for e in finished} == {"e1", "e4"}
        assert len(in_progress) == 3 and {e["uuid"] for e in in_progress} == {"e2", "e5", "e6"}
        assert unplayed == 1
        assert total_remaining == 300 + 300 + 0 + 3500, total_remaining

        per_podcast = {}
        for e in episodes:
            per_podcast.setdefault(e["podcastUuid"], []).append(e)
        p1_listened = sum(listened_seconds(e["duration"], e["playedUpTo"]) for e in per_podcast["p1"])
        assert p1_listened == 600 + 300 + 100, p1_listened

        def network_of(p):
            author = (p["author"] or "").strip()
            if author:
                return author
            feed = (p["feedUrl"] or "").strip()
            if feed:
                host = feed.split("://", 1)[-1].split("/", 1)[0].lower()
                if host.startswith("www."):
                    host = host[4:]
                if host:
                    return host
            return "Unbekannt"

        networks = {network_of(p) for p in podcasts}
        assert networks == {"Netz A", "beta.fm", "Unbekannt"}, networks

        with_duration = [e for e in episodes if e["duration"] > 0]
        longest = max(with_duration, key=lambda e: e["duration"])
        shortest = min(with_duration, key=lambda e: e["duration"])
        assert longest["uuid"] == "e6" and longest["duration"] == 3600
        assert shortest["uuid"] == "e4" and shortest["duration"] == 120

        print("OK: Alle Assertions bestanden.")
        print(f"  Gesamte Hörzeit: {total_listened} s")
        print(f"  Abgeschlossen: {len(finished)}, in Arbeit: {len(in_progress)}, ungespielt: {unplayed}")
        print(f"  Noch zu hören: {total_remaining} s über {len(open_eps)} offene Folgen")
        print(f"  Netzwerke: {sorted(networks)}")
        print(f"  Längste Folge: {longest['uuid']} ({longest['duration']} s), kürzeste: {shortest['uuid']} ({shortest['duration']} s)")


if __name__ == "__main__":
    sys.exit(main())
