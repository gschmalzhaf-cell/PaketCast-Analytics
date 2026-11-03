package com.paketcast.analytics.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.paketcast.analytics.data.model.Episode
import com.paketcast.analytics.data.model.PlaybackEvent
import com.paketcast.analytics.data.model.PocketCastsData
import com.paketcast.analytics.data.model.Podcast
import java.io.IOException

/**
 * Liest eine Kopie der Pocket-Casts-Datenbank (pocketcasts.db) read-only aus.
 *
 * Die Spaltennamen werden über PRAGMA table_info erkannt, damit verschiedene
 * Pocket-Casts-Versionen (unterschiedliche Schreibweisen der Spalten) funktionieren.
 */
object PocketCastsDbReader {

    fun read(dbPath: String): PocketCastsData {
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val podcastTable = listOf("podcasts").firstOrNull { columnsOf(db, it).isNotEmpty() }
            val episodeTable = listOf("episodes", "podcast_episodes").firstOrNull { columnsOf(db, it).isNotEmpty() }
            if (podcastTable == null || episodeTable == null) {
                throw IOException(
                    "Die Datei enthält nicht die erwarteten Tabellen ('podcasts' und 'episodes' bzw. 'podcast_episodes'). " +
                        "Ist es wirklich eine pocketcasts.db aus Pocket Casts?"
                )
            }
            val podcasts = readPodcasts(db, podcastTable, columnsOf(db, podcastTable))
            val episodes = readEpisodes(db, episodeTable, columnsOf(db, episodeTable))
            val playbackEvents = readPlaybackEvents(db)
            return PocketCastsData(podcasts, episodes, playbackEvents)
        } finally {
            db.close()
        }
    }

    private fun columnsOf(db: SQLiteDatabase, table: String): Set<String> {
        val result = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                result.add(cursor.getString(nameIndex))
            }
        }
        return result
    }

    private fun pick(columns: Set<String>, vararg candidates: String): String? =
        candidates.firstOrNull { it in columns }

    private fun selectExpr(actual: String?, alias: String): String =
        if (actual != null) "$actual AS $alias" else "NULL AS $alias"

    private fun readPodcasts(db: SQLiteDatabase, table: String, columns: Set<String>): List<Podcast> {
        val uuid = pick(columns, "uuid")
            ?: throw IOException("Spalte 'uuid' fehlt in Tabelle 'podcasts'")
        val title = pick(columns, "title")
            ?: throw IOException("Spalte 'title' fehlt in Tabelle 'podcasts'")
        val author = pick(columns, "author", "network")
        val feedUrl = pick(columns, "podcastUrl", "podcast_url", "feedUrl", "feed_url", "url")
        val added = pick(columns, "added_date", "addedDate", "added")
        val sql = "SELECT ${selectExpr(uuid, "uuid")}, ${selectExpr(title, "title")}, " +
            "${selectExpr(author, "author")}, ${selectExpr(feedUrl, "feedUrl")}, " +
            "${selectExpr(added, "added")} FROM $table"
        val result = mutableListOf<Podcast>()
        db.rawQuery(sql, null).use { c ->
            val iUuid = c.getColumnIndexOrThrow("uuid")
            val iTitle = c.getColumnIndexOrThrow("title")
            val iAuthor = c.getColumnIndexOrThrow("author")
            val iFeed = c.getColumnIndexOrThrow("feedUrl")
            val iAdded = c.getColumnIndexOrThrow("added")
            while (c.moveToNext()) {
                result.add(
                    Podcast(
                        uuid = c.getString(iUuid),
                        title = c.getString(iTitle) ?: "",
                        author = c.getString(iAuthor),
                        feedUrl = c.getString(iFeed),
                        addedEpochSec = c.getLongOrNull(iAdded)?.let(::normalizeEpoch),
                    )
                )
            }
        }
        return result
    }

    private fun readEpisodes(db: SQLiteDatabase, table: String, columns: Set<String>): List<Episode> {
        val uuid = pick(columns, "uuid")
            ?: throw IOException("Spalte 'uuid' fehlt in Tabelle 'episodes'")
        val podcastUuid = pick(columns, "podcast_uuid", "podcastUuid")
        val title = pick(columns, "title")
            ?: throw IOException("Spalte 'title' fehlt in Tabelle 'episodes'")
        val duration = pick(columns, "duration")
        val playedUpTo = pick(columns, "played_up_to", "playedUpTo")
        val playedStatus = pick(columns, "played_status", "playedStatus")
        val published = pick(columns, "published_date", "publishedDate", "published")
        val added = pick(columns, "added_date", "addedDate", "added")
        val archived = pick(columns, "archived")
        val sql = "SELECT ${selectExpr(uuid, "uuid")}, ${selectExpr(podcastUuid, "podcastUuid")}, " +
            "${selectExpr(title, "title")}, ${selectExpr(duration, "duration")}, " +
            "${selectExpr(playedUpTo, "playedUpTo")}, ${selectExpr(playedStatus, "playedStatus")}, " +
            "${selectExpr(published, "published")}, ${selectExpr(added, "added")}, " +
            "${selectExpr(archived, "archived")} FROM $table"
        val result = mutableListOf<Episode>()
        db.rawQuery(sql, null).use { c ->
            val iUuid = c.getColumnIndexOrThrow("uuid")
            val iPodcast = c.getColumnIndexOrThrow("podcastUuid")
            val iTitle = c.getColumnIndexOrThrow("title")
            val iDuration = c.getColumnIndexOrThrow("duration")
            val iPlayedUpTo = c.getColumnIndexOrThrow("playedUpTo")
            val iPlayedStatus = c.getColumnIndexOrThrow("playedStatus")
            val iPublished = c.getColumnIndexOrThrow("published")
            val iAdded = c.getColumnIndexOrThrow("added")
            val iArchived = c.getColumnIndexOrThrow("archived")
            while (c.moveToNext()) {
                result.add(
                    Episode(
                        uuid = c.getString(iUuid),
                        podcastUuid = c.getString(iPodcast),
                        title = c.getString(iTitle) ?: "",
                        durationSec = c.getLongOrZero(iDuration),
                        playedUpToSec = c.getLongOrZero(iPlayedUpTo),
                        playedStatus = c.getIntOrZero(iPlayedStatus),
                        publishedEpochSec = c.getLongOrNull(iPublished)?.let(::normalizeEpoch),
                        addedEpochSec = c.getLongOrNull(iAdded)?.let(::normalizeEpoch),
                        archived = c.getIntOrZero(iArchived) != 0,
                    )
                )
            }
        }
        return result
    }

    /**
     * Liest optional die Hör-Historie. Pocket-Casts-Versionen unterscheiden sich
     * (Tabellen-/Spaltennamen, Sekunden vs. Millisekunden) – daher defensive
     * Namensauflösung; fehlt alles, ist das Ergebnis einfach leer.
     */
    private fun readPlaybackEvents(db: SQLiteDatabase): List<PlaybackEvent> {
        val table = listOf("episode_interactions", "playback_events", "user_episode_interactions", "episode_history")
            .firstOrNull { columnsOf(db, it).isNotEmpty() } ?: return emptyList()
        val columns = columnsOf(db, table)
        val episode = pick(columns, "episode_uuid", "episodeUuid", "episode")
        val time = pick(columns, "time", "timestamp", "date", "created", "epoch", "added_date")
        val seconds = pick(columns, "duration", "seconds", "played_up_to", "playedUpTo", "length")
        val action = pick(columns, "action", "type", "event")
        if (time == null) return emptyList()
        val sql = buildString {
            append("SELECT ")
            append(if (episode != null) "$episode AS episodeUuid" else "NULL AS episodeUuid")
            append(", ")
            append("$time AS time")
            append(", ")
            append(if (seconds != null) "$seconds AS seconds" else "0 AS seconds")
            if (action != null) {
                append(", ")
                append("$action AS action")
            }
            append(" FROM $table")
        }
        val playActions = setOf("play", "played", "listen", "listened", "playback", "resume", "started")
        val result = mutableListOf<PlaybackEvent>()
        db.rawQuery(sql, null).use { c ->
            val iEpisode = c.getColumnIndexOrThrow("episodeUuid")
            val iTime = c.getColumnIndexOrThrow("time")
            val iSeconds = c.getColumnIndexOrThrow("seconds")
            val iAction = if (action != null) c.getColumnIndexOrThrow("action") else -1
            while (c.moveToNext()) {
                if (iAction >= 0) {
                    val a = c.getString(iAction)?.lowercase()
                    if (a != null && a !in playActions) continue
                }
                val rawTime = c.getLongOrZero(iTime)
                result.add(
                    PlaybackEvent(
                        episodeUuid = c.getString(iEpisode),
                        epochSec = normalizeEpoch(rawTime),
                        seconds = c.getLongOrZero(iSeconds).coerceAtLeast(0L),
                    )
                )
            }
        }
        return result
    }

    private fun normalizeEpoch(raw: Long): Long = when {
        raw > 10_000_000_000_000L -> raw / 1000L
        else -> raw
    }

    private fun Cursor.getLongOrNull(index: Int): Long? =
        if (isNull(index)) null else getNumber(index).toLong()

    private fun Cursor.getLongOrZero(index: Int): Long =
        if (isNull(index)) 0L else getNumber(index).toLong()

    private fun Cursor.getIntOrZero(index: Int): Int =
        if (isNull(index)) 0 else getNumber(index).toInt()

    private fun Cursor.getNumber(index: Int): Double = when (getType(index)) {
        Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
        Cursor.FIELD_TYPE_INTEGER -> getLong(index).toDouble()
        else -> getString(index)?.toDoubleOrNull() ?: 0.0
    }
}
