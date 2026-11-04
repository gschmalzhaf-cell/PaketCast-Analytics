package com.paketcast.analytics.data

import com.paketcast.analytics.data.api.PocketCastsApiClient
import com.paketcast.analytics.data.json.MiniJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniJsonTest {

    @Test
    fun `parses nested objects arrays and scalars`() {
        val v = MiniJson.parse("""{"a": 1, "b": [true, null, "x\ny"], "c": {"d": -2.5}}""")
        val map = v as Map<*, *>
        assertEquals(1.0, map["a"])
        assertEquals(listOf(true, null, "x\ny"), map["b"])
        assertEquals(-2.5, (map["c"] as Map<*, *>)["d"])
    }

    @Test
    fun `parses unicode escapes`() {
        assertEquals("äöü", (MiniJson.parse("\"\\u00e4\\u00f6\\u00fc\"") as String))
    }

    @Test
    fun `rejects trailing garbage`() {
        try {
            MiniJson.parse("{} x")
            error("sollte werfen")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Position"))
        }
    }

    @Test
    fun `history mapping reads episodes with ms timestamps and duration`() {
        val json = """
            {"episodes": [
              {"episode_uuid": "e1", "time": 1700001000000, "duration": 600},
              {"uuid": "e2", "played_at": 1700002000, "duration": 300},
              {"episode_uuid": "e3"}
            ]}
        """.trimIndent()
        val events = PocketCastsApiClient.historyEventsFromJson(json)
        assertEquals(2, events.size)
        assertEquals("e1", events[0].episodeUuid)
        assertEquals(1700001000L, events[0].epochSec)
        assertEquals(600L, events[0].seconds)
        assertEquals(1700002000L, events[1].epochSec)
    }

    @Test
    fun `subscriptions and starred mapping`() {
        val subs = PocketCastsApiClient.subscriptionsFromJson(
            """{"podcasts": [{"title": "Alpha", "podcast_url": "https://a.de/f"}, {"title": "Beta"}]}"""
        )
        assertEquals(2, subs.size)
        assertEquals("https://a.de/f", subs[0].feedUrl)
        assertNull(subs[1].feedUrl)

        val starred = PocketCastsApiClient.starredFromJson("""{"episodes": [{"title": "Fav 1"}, {"title": "Fav 2"}]}""")
        assertEquals(listOf("Fav 1", "Fav 2"), starred)
    }
}
