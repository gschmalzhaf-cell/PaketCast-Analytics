package com.paketcast.analytics.data.model

data class Podcast(
    val uuid: String,
    val title: String,
    val author: String?,
    val feedUrl: String?,
    val addedEpochSec: Long?,
)

data class Episode(
    val uuid: String,
    val podcastUuid: String?,
    val title: String,
    val durationSec: Long,
    val playedUpToSec: Long,
    val playedStatus: Int,
    val publishedEpochSec: Long?,
    val addedEpochSec: Long?,
    val archived: Boolean,
)

data class OpmlSubscription(
    val title: String,
    val feedUrl: String?,
)

/**
 * Einzelnes Hör-Event aus der Pocket-Casts-Historie (z. B. Tabelle
 * `episode_interactions`). `seconds` = in diesem Event gehörte Sekunden
 * (0, wenn die DB keine Dauer pro Event speichert).
 */
data class PlaybackEvent(
    val episodeUuid: String?,
    val epochSec: Long,
    val seconds: Long,
)

data class PocketCastsData(
    val podcasts: List<Podcast>,
    val episodes: List<Episode>,
    val playbackEvents: List<PlaybackEvent> = emptyList(),
)
