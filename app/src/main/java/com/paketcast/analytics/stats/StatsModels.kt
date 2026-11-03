package com.paketcast.analytics.stats

import com.paketcast.analytics.data.model.Episode
import com.paketcast.analytics.data.model.Podcast
import java.time.LocalDate

enum class EpisodeProgress {
    UNPLAYED,
    IN_PROGRESS,
    FINISHED,
}

data class EpisodeWithPodcast(
    val episode: Episode,
    val podcastTitle: String,
)

data class PodcastStats(
    val podcast: Podcast,
    val episodeCount: Int,
    val finishedCount: Int,
    val inProgressCount: Int,
    val unplayedCount: Int,
    val listenedSec: Long,
    val remainingSec: Long,
) {
    val progressRatio: Double
        get() = if (episodeCount == 0) 0.0 else finishedCount.toDouble() / episodeCount
}

data class NetworkStats(
    val network: String,
    val podcastCount: Int,
    val listenedSec: Long,
    val finishedEpisodes: Int,
)

data class OverviewStats(
    val podcastCount: Int,
    val episodeCount: Int,
    val totalListenedSec: Long,
    val finishedCount: Int,
    val inProgressCount: Int,
    val unplayedCount: Int,
    val remainingSec: Long,
    val averageListenedPerEpisodeSec: Long,
    val topPodcast: PodcastStats?,
    val longestEpisode: EpisodeWithPodcast?,
)

data class Top10(
    val byListened: List<PodcastStats>,
    val byFinished: List<PodcastStats>,
)

data class EpisodeExtremes(
    val longest: List<EpisodeWithPodcast>,
    val shortest: List<EpisodeWithPodcast>,
)

data class RemainingStats(
    val totalRemainingSec: Long,
    val openEpisodeCount: Int,
    val byPodcast: List<PodcastStats>,
    val episodes: List<EpisodeWithPodcast>,
)

data class DayBucket(
    val date: LocalDate,
    val seconds: Long,
)

data class WeekBucket(
    val weekStart: LocalDate,
    val seconds: Long,
)

data class HistoryStats(
    val available: Boolean,
    val daily: List<DayBucket>,
    val weekly: List<WeekBucket>,
    val totalSeconds: Long,
    val eventCount: Int,
)

data class AnalyticsSnapshot(
    val sourceName: String,
    val importedAtEpochSec: Long,
    val overview: OverviewStats,
    val podcasts: List<PodcastStats>,
    val top10: Top10,
    val networks: List<NetworkStats>,
    val extremes: EpisodeExtremes,
    val remaining: RemainingStats,
    val history: HistoryStats,
)
