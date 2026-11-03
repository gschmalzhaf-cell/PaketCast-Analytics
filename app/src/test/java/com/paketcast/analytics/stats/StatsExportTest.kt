package com.paketcast.analytics.stats

import com.paketcast.analytics.data.model.Episode
import com.paketcast.analytics.data.model.PlaybackEvent
import com.paketcast.analytics.data.model.Podcast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsExportTest {

    private fun sampleSnapshot(): AnalyticsSnapshot {
        val p = Podcast(uuid = "p1", title = "Alpha; \"Spezial\"", author = "Netz A", feedUrl = "https://a.de/f", addedEpochSec = null)
        val episodes = listOf(
            Episode("e1", "p1", "Folge 1", 600, 600, 2, 1700001000, 1700001000, false),
            Episode("e2", "p1", "Folge 2", 600, 300, 1, 1700002000, 1700002000, false),
        )
        val events = listOf(
            PlaybackEvent("e1", 1700001000, 600),
            PlaybackEvent("e2", 1700002000, 300),
        )
        return StatisticsEngine.buildSnapshot("pocketcasts.db", listOf(p), episodes, playbackEvents = events)
    }

    @Test
    fun `csv escapes semicolons and quotes`() {
        assertEquals("\"Alpha; \"\"Spezial\"\"\"", StatsExport.csvEsc("Alpha; \"Spezial\""))
        assertEquals("plain", StatsExport.csvEsc("plain"))
    }

    @Test
    fun `csv contains header and one escaped row per podcast`() {
        val csv = StatsExport.snapshotToCsv(sampleSnapshot())
        val lines = csv.lines()
        assertTrue(lines.any { it.startsWith("podcast;netzwerk;episoden;") })
        assertTrue(csv.contains("\"Alpha; \"\"Spezial\"\"\";Netz A;2;1;1;0;900;300"))
        assertTrue(csv.contains("# Übersicht: 2 Folgen, gehört 900 s"))
    }

    @Test
    fun `json escapes special chars and contains key sections`() {
        val json = StatsExport.snapshotToJson(sampleSnapshot())
        assertTrue(json.contains("\\\"title\\\": \\\"Alpha; \\\"Spezial\\\"\\\"".replace("\\\"title", "\"title").let { "\"title\": \"Alpha; \\\"Spezial\\\"\"" }))
        assertTrue(json.contains("\"totalListenedSec\": 900"))
        assertTrue(json.contains("\"podcasts\": ["))
        assertTrue(json.contains("\"networks\": ["))
        assertTrue(json.contains("\"history\": {"))
        assertTrue(json.contains("\"available\": true"))
        assertTrue(json.contains("\"daily\": ["))
        assertTrue(json.contains("\"seconds\": 900"))
        assertTrue(json.trim().startsWith("{") && json.trim().endsWith("}"))
    }

    @Test
    fun `json escaping handles control chars and null`() {
        assertEquals("\"a\\nb\"", StatsExport.jsonEsc("a\nb"))
        assertEquals("null", StatsExport.jsonEsc(null))
        assertEquals("\"\\\\\"", StatsExport.jsonEsc("\\"))
    }
}
