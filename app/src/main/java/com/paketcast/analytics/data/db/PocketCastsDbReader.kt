package com.paketcast.analytics.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.paketcast.analytics.data.model.Episode
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
            val podcastColumns = columnsOf(db, "podcasts")
            val episodeColumns = columnsOf(db, "episodes")
            if (podcastColumns.isEmpty() || episodeColumns.isEmpty()) {
                throw IOException(
                    "Die Datei enthält nicht die erwarteten Tabellen 'podcasts' und 'episodes'. " +
                        "Ist es wirklich eine pocketcasts.db aus Pocket Casts?"
                )
            }
            val podcasts = readPodcasts(db, podcastColumns)
            val episodes = readEpisodes(db, episodeColumns)
            return PocketCastsData(podcasts, episodes)
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

    private fun readPodcasts(db: SQLiteDatabase, columns: Set<String>): List<Podcast> {
        val uuid = pick(columns, "uuid")
            ?: throw IOException("Spalte 'uuid' fehlt in Tabelle 'podcasts'")
        val title = pick(columns, "title")
            ?: throw IOException("Spalte 'title' fehlt in Tabelle 'podcasts'")
        val author = pick(columns, "author", "network")
        val feedUrl = pick(columns, "podcastUrl", "podcast_url", "feedUrl", "feed_url", "url")
        val added = pick(columns, "added_date", "addedDate", "added")
        val sql = "SELECT ${selectExpr(uuid, "uuid")}, ${selectExpr(title, "title")}, " +
            "${selectExpr(author, "author")}, ${selectExpr(feedUrl, "feedUrl")}, " +
            "${selectExpr(added, "added")} FROM podcasts"
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
                        addedEpochSec = c.getLongOrNull(iAdded),
                    )
                )
            }
        }
        return result
    }

    private fun readEpisodes(db: SQLiteDatabase, columns: Set<String>): List<Episode> {
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
            "${selectExpr(archived, "archived")} FROM episodes"
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
                        publishedEpochSec = c.getLongOrNull(iPublished),
                        addedEpochSec = c.getLongOrNull(iAdded),
                        archived = c.getIntOrZero(iArchived) != 0,
                    )
                )
            }
        }
        return result
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
