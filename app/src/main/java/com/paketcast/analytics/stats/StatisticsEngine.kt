package com.paketcast.analytics.stats

import com.paketcast.analytics.data.model.Episode
import com.paketcast.analytics.data.model.Podcast
import java.net.URI

/**
 * Reine Berechnungslogik für alle Statistiken – ohne Android-Abhängigkeiten,
 * damit sie per Unit-Test geprüft werden kann.
 *
 * Hörzeit einer Folge ist `played_up_to` (Sekunden), begrenzt auf die Dauer.
 * Eine Folge gilt als abgeschlossen, wenn höchstens FINISH_TOLERANCE_SEC Sekunden fehlen.
 */
object StatisticsEngine {

    const val FINISH_TOLERANCE_SEC: Long = 10L
    const val TOP_LIMIT: Int = 10
    const val EXTREME_LIMIT: Int = 10

    fun listenedSeconds(episode: Episode): Long {
        val played = episode.playedUpToSec.coerceAtLeast(0L)
        return if (episode.durationSec > 0) played.coerceAtMost(episode.durationSec) else played
    }

    fun isFinished(episode: Episode): Boolean =
        episode.durationSec > 0 &&
            episode.playedUpToSec >= episode.durationSec - FINISH_TOLERANCE_SEC

    fun progressOf(episode: Episode): EpisodeProgress = when {
        isFinished(episode) -> EpisodeProgress.FINISHED
        listenedSeconds(episode) > 0 -> EpisodeProgress.IN_PROGRESS
        else -> EpisodeProgress.UNPLAYED
    }

    fun remainingSeconds(episode: Episode): Long =
        if (episode.durationSec > 0 && !isFinished(episode)) {
            (episode.durationSec - episode.playedUpToSec.coerceAtLeast(0L)).coerceAtLeast(0L)
        } else {
            0L
        }

    fun networkOf(podcast: Podcast): String {
        val author = podcast.author?.trim().orEmpty()
        if (author.isNotEmpty()) return author
        val feed = podcast.feedUrl?.trim().orEmpty()
        if (feed.isNotEmpty()) {
            val host = runCatching { URI(feed).host?.lowercase()?.removePrefix("www.") }.getOrNull()
            if (!host.isNullOrEmpty()) return host
        }
        return "Unbekannt"
    }

    fun podcastStats(podcast: Podcast, episodes: List<Episode>): PodcastStats {
        var finished = 0
        var inProgress = 0
        var unplayed = 0
        var listened = 0L
        var remaining = 0L
        for (episode in episodes) {
            when (progressOf(episode)) {
                EpisodeProgress.FINISHED -> finished++
                EpisodeProgress.IN_PROGRESS -> inProgress++
                EpisodeProgress.UNPLAYED -> unplayed++
            }
            listened += listenedSeconds(episode)
            if (!episode.archived) {
                remaining += remainingSeconds(episode)
            }
        }
        return PodcastStats(
            podcast = podcast,
            episodeCount = episodes.size,
            finishedCount = finished,
            inProgressCount = inProgress,
            unplayedCount = unplayed,
            listenedSec = listened,
            remainingSec = remaining,
        )
    }

    fun buildSnapshot(
        sourceName: String,
        podcasts: List<Podcast>,
        episodes: List<Episode>,
        importedAtEpochSec: Long = System.currentTimeMillis() / 1000L,
    ): AnalyticsSnapshot {
        val podcastByUuid = podcasts.associateBy { it.uuid }
        val episodesByPodcast = episodes.groupBy { it.podcastUuid }

        val podcastStatsList = podcasts
            .map { podcast -> podcastStats(podcast, episodesByPodcast[podcast.uuid].orEmpty()) }
            .sortedWith(
                compareByDescending<PodcastStats> { it.listenedSec }
                    .thenByDescending { it.finishedCount }
                    .thenBy { it.podcast.title.lowercase() }
            )

        fun withPodcast(episode: Episode) = EpisodeWithPodcast(
            episode = episode,
            podcastTitle = podcastByUuid[episode.podcastUuid]?.title ?: "Unbekannter Podcast",
        )

        val allWithPodcast = episodes.map(::withPodcast)

        val finishedCount = episodes.count { isFinished(it) }
        val inProgressCount = episodes.count { progressOf(it) == EpisodeProgress.IN_PROGRESS }
        val unplayedCount = episodes.size - finishedCount - inProgressCount
        val totalListened = episodes.sumOf { listenedSeconds(it) }
        val openEpisodes = episodes.filter { !it.archived && !isFinished(it) }
        val totalRemaining = openEpisodes.sumOf { remainingSeconds(it) }

        val overview = OverviewStats(
            podcastCount = podcasts.size,
            episodeCount = episodes.size,
            totalListenedSec = totalListened,
            finishedCount = finishedCount,
            inProgressCount = inProgressCount,
            unplayedCount = unplayedCount,
            remainingSec = totalRemaining,
            averageListenedPerEpisodeSec = if (episodes.isEmpty()) 0L else totalListened / episodes.size,
            topPodcast = podcastStatsList.firstOrNull(),
            longestEpisode = allWithPodcast
                .filter { it.episode.durationSec > 0 }
                .maxByOrNull { it.episode.durationSec },
        )

        val top10 = Top10(
            byListened = podcastStatsList.take(TOP_LIMIT),
            byFinished = podcastStatsList
                .sortedWith(
                    compareByDescending<PodcastStats> { it.finishedCount }
                        .thenByDescending { it.listenedSec }
                        .thenBy { it.podcast.title.lowercase() }
                )
                .take(TOP_LIMIT),
        )

        val networks = podcasts
            .groupBy { networkOf(it) }
            .map { (network, group) ->
                val groupEpisodes = group.flatMap { episodesByPodcast[it.uuid].orEmpty() }
                NetworkStats(
                    network = network,
                    podcastCount = group.size,
                    listenedSec = groupEpisodes.sumOf { listenedSeconds(it) },
                    finishedEpisodes = groupEpisodes.count { isFinished(it) },
                )
            }
            .sortedWith(
                compareByDescending<NetworkStats> { it.listenedSec }
                    .thenByDescending { it.podcastCount }
                    .thenBy { it.network.lowercase() }
            )

        val withDuration = allWithPodcast.filter { it.episode.durationSec > 0 }
        val extremes = EpisodeExtremes(
            longest = withDuration.sortedByDescending { it.episode.durationSec }.take(EXTREME_LIMIT),
            shortest = withDuration.sortedBy { it.episode.durationSec }.take(EXTREME_LIMIT),
        )

        val remaining = RemainingStats(
            totalRemainingSec = totalRemaining,
            openEpisodeCount = openEpisodes.size,
            byPodcast = podcastStatsList.filter { it.remainingSec > 0 }.sortedByDescending { it.remainingSec },
            episodes = openEpisodes
                .map(::withPodcast)
                .sortedWith(
                    compareByDescending<EpisodeWithPodcast> { it.episode.addedEpochSec ?: 0L }
                        .thenByDescending { remainingSeconds(it.episode) }
                ),
        )

        return AnalyticsSnapshot(
            sourceName = sourceName,
            importedAtEpochSec = importedAtEpochSec,
            overview = overview,
            podcasts = podcastStatsList,
            top10 = top10,
            networks = networks,
            extremes = extremes,
            remaining = remaining,
        )
    }
}
