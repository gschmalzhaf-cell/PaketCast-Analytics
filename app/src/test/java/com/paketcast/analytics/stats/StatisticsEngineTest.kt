package com.paketcast.analytics.stats

import com.paketcast.analytics.data.model.Episode
import com.paketcast.analytics.data.model.Podcast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsEngineTest {

    private fun podcast(
        uuid: String,
        title: String,
        author: String? = null,
        feedUrl: String? = null,
    ) = Podcast(uuid = uuid, title = title, author = author, feedUrl = feedUrl, addedEpochSec = null)

    private fun episode(
        uuid: String,
        podcastUuid: String,
        title: String = uuid,
        durationSec: Long,
        playedUpToSec: Long,
        archived: Boolean = false,
        playedStatus: Int = 0,
        addedEpochSec: Long? = null,
    ) = Episode(
        uuid = uuid,
        podcastUuid = podcastUuid,
        title = title,
        durationSec = durationSec,
        playedUpToSec = playedUpToSec,
        playedStatus = playedStatus,
        publishedEpochSec = null,
        addedEpochSec = addedEpochSec,
        archived = archived,
    )

    @Test
    fun `listened seconds are clamped to duration`() {
        val e = episode("e1", "p1", durationSec = 100, playedUpToSec = 250)
        assertEquals(100, StatisticsEngine.listenedSeconds(e))
        val negative = episode("e2", "p1", durationSec = 100, playedUpToSec = -5)
        assertEquals(0, StatisticsEngine.listenedSeconds(negative))
    }

    @Test
    fun `finished within tolerance`() {
        val almostDone = episode("e1", "p1", durationSec = 1000, playedUpToSec = 995)
        val notYet = episode("e2", "p1", durationSec = 1000, playedUpToSec = 980)
        val noDuration = episode("e3", "p1", durationSec = 0, playedUpToSec = 500)
        assertTrue(StatisticsEngine.isFinished(almostDone))
        assertFalse(StatisticsEngine.isFinished(notYet))
        assertFalse(StatisticsEngine.isFinished(noDuration))
    }

    @Test
    fun `snapshot aggregates per podcast and overview`() {
        val p1 = podcast("p1", "Alpha", author = "Netz A")
        val p2 = podcast("p2", "Beta", author = "Netz B")
        val episodes = listOf(
            episode("e1", "p1", durationSec = 600, playedUpToSec = 600),
            episode("e2", "p1", durationSec = 600, playedUpToSec = 300),
            episode("e3", "p2", durationSec = 300, playedUpToSec = 0),
            episode("e4", "p2", durationSec = 120, playedUpToSec = 120, archived = true),
        )
        val snapshot = StatisticsEngine.buildSnapshot("test", listOf(p1, p2), episodes)

        assertEquals(2, snapshot.overview.podcastCount)
        assertEquals(4, snapshot.overview.episodeCount)
        assertEquals(600 + 300 + 0 + 120, snapshot.overview.totalListenedSec)
        assertEquals(2, snapshot.overview.finishedCount)
        assertEquals(1, snapshot.overview.inProgressCount)
        assertEquals(1, snapshot.overview.unplayedCount)
        assertEquals(600, snapshot.overview.remainingSec)

        val alpha = snapshot.podcasts.first { it.podcast.uuid == "p1" }
        assertEquals(900, alpha.listenedSec)
        assertEquals(1, alpha.finishedCount)
        assertEquals(1, alpha.inProgressCount)
        assertEquals(300, alpha.remainingSec)
        assertEquals(0.5, alpha.progressRatio, 0.0001)
    }

    @Test
    fun `top10 is limited to ten and ordered by listened time`() {
        val podcasts = (1..12).map { podcast("p$it", "Podcast $it") }
        val episodes = podcasts.mapIndexed { index, p ->
            episode("e$index", p.uuid, durationSec = 100L * (index + 1), playedUpToSec = 100L * (index + 1))
        }
        val snapshot = StatisticsEngine.buildSnapshot("test", podcasts, episodes)
        assertEquals(10, snapshot.top10.byListened.size)
        assertEquals("p12", snapshot.top10.byListened.first().podcast.uuid)
        assertEquals("p3", snapshot.top10.byListened.last().podcast.uuid)
    }

    @Test
    fun `network falls back to feed host when author is missing`() {
        val withAuthor = podcast("p1", "A", author = "  Externer Autor ")
        val withFeed = podcast("p2", "B", feedUrl = "https://www.example.com/feed.xml")
        val none = podcast("p3", "C")
        assertEquals("Externer Autor", StatisticsEngine.networkOf(withAuthor))
        assertEquals("example.com", StatisticsEngine.networkOf(withFeed))
        assertEquals("Unbekannt", StatisticsEngine.networkOf(none))

        val snapshot = StatisticsEngine.buildSnapshot("test", listOf(withAuthor, withFeed, none), emptyList())
        assertEquals(3, snapshot.networks.size)
        assertEquals(
            setOf("Externer Autor", "example.com", "Unbekannt"),
            snapshot.networks.map { it.network }.toSet(),
        )
    }

    @Test
    fun `extremes ignore episodes without duration`() {
        val p = podcast("p1", "A")
        val episodes = listOf(
            episode("long", "p1", durationSec = 9000, playedUpToSec = 0),
            episode("mid", "p1", durationSec = 600, playedUpToSec = 0),
            episode("short", "p1", durationSec = 60, playedUpToSec = 0),
            episode("unknown", "p1", durationSec = 0, playedUpToSec = 5),
        )
        val snapshot = StatisticsEngine.buildSnapshot("test", listOf(p), episodes)
        assertEquals("long", snapshot.extremes.longest.first().episode.uuid)
        assertEquals("short", snapshot.extremes.shortest.first().episode.uuid)
        assertEquals(3, snapshot.extremes.longest.size)
        assertEquals(3, snapshot.extremes.shortest.size)
    }

    @Test
    fun `remaining excludes finished and archived episodes`() {
        val p = podcast("p1", "A")
        val episodes = listOf(
            episode("e1", "p1", durationSec = 1000, playedUpToSec = 1000),
            episode("e2", "p1", durationSec = 1000, playedUpToSec = 100),
            episode("e3", "p1", durationSec = 1000, playedUpToSec = 100, archived = true),
        )
        val snapshot = StatisticsEngine.buildSnapshot("test", listOf(p), episodes)
        assertEquals(900, snapshot.remaining.totalRemainingSec)
        assertEquals(1, snapshot.remaining.openEpisodeCount)
        assertEquals("e2", snapshot.remaining.episodes.single().episode.uuid)
    }

    @Test
    fun `remaining episodes are sorted newest first`() {
        val p = podcast("p1", "A")
        val episodes = listOf(
            episode("old", "p1", durationSec = 1000, playedUpToSec = 0, addedEpochSec = 100),
            episode("new", "p1", durationSec = 1000, playedUpToSec = 0, addedEpochSec = 200),
        )
        val snapshot = StatisticsEngine.buildSnapshot("test", listOf(p), episodes)
        assertEquals(listOf("new", "old"), snapshot.remaining.episodes.map { it.episode.uuid })
    }
}
