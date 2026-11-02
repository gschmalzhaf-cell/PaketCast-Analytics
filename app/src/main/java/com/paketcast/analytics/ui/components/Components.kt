package com.paketcast.analytics.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.model.OpmlSubscription
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.stats.AnalyticsSnapshot
import com.paketcast.analytics.stats.EpisodeProgress
import com.paketcast.analytics.stats.EpisodeWithPodcast
import com.paketcast.analytics.stats.PodcastStats
import com.paketcast.analytics.stats.StatisticsEngine
import com.paketcast.analytics.ui.format.Format

@Composable
fun StateScaffold(
    state: AnalyticsState,
    emptyContent: @Composable () -> Unit = { NoDataHint() },
    content: @Composable (AnalyticsSnapshot) -> Unit,
) {
    when (state) {
        is AnalyticsState.Loading -> LoadingState()
        is AnalyticsState.Error -> ErrorState(state.message)
        is AnalyticsState.Empty -> emptyContent()
        is AnalyticsState.OpmlOnly -> OpmlOnlyState(state.subscriptions)
        is AnalyticsState.Loaded -> content(state.snapshot)
    }
}

@Composable
fun NoDataHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Noch keine Daten geladen", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tippe oben rechts auf das Import-Symbol und wähle eine pocketcasts.db oder einen OPML-Export.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun EmptyState(onImportDatabase: () -> Unit, onImportOpml: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Noch keine Daten geladen", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Importiere eine Kopie der Pocket-Casts-Datenbank (pocketcasts.db) oder einen OPML-Export, " +
                "um deine Hör-Statistiken zu sehen.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onImportDatabase, modifier = Modifier.fillMaxWidth()) {
            Text("pocketcasts.db importieren")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onImportOpml, modifier = Modifier.fillMaxWidth()) {
            Text("OPML-Export importieren")
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Fehler beim Import",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun OpmlOnlyState(subscriptions: List<OpmlSubscription>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionHeader(
                "${subscriptions.size} abonnierte Podcasts (OPML). " +
                    "Für Statistiken wird zusätzlich eine pocketcasts.db benötigt."
            )
        }
        items(subscriptions, key = { it.feedUrl ?: it.title }) { sub ->
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(sub.title, style = MaterialTheme.typography.titleSmall)
                    if (sub.feedUrl != null) {
                        Text(
                            sub.feedUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
fun PodcastStatRow(stats: PodcastStats) {
    val podcast = stats.podcast
    val network = StatisticsEngine.networkOf(podcast)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(podcast.title, style = MaterialTheme.typography.titleSmall)
            val subtitle = when {
                network != "Unbekannt" -> network
                !podcast.feedUrl.isNullOrBlank() -> podcast.feedUrl
                else -> null
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Gehört: ${Format.duration(stats.listenedSec)} • " +
                    "Abgeschlossen: ${stats.finishedCount}/${stats.episodeCount} • " +
                    "Offen: ${stats.unplayedCount + stats.inProgressCount} • " +
                    "Noch ${Format.duration(stats.remainingSec)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (stats.episodeCount > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { stats.progressRatio.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    Format.percent(stats.progressRatio) + " abgeschlossen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
fun RankedPodcastRow(rank: Int, stats: PodcastStats) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            "#$rank  ${stats.podcast.title}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            "Gehört: ${Format.duration(stats.listenedSec)} • Abgeschlossen: ${stats.finishedCount} Folgen",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun EpisodeRow(item: EpisodeWithPodcast) {
    val episode = item.episode
    val statusText = when (StatisticsEngine.progressOf(episode)) {
        EpisodeProgress.FINISHED -> "abgeschlossen"
        EpisodeProgress.IN_PROGRESS ->
            "angehört ${Format.duration(StatisticsEngine.listenedSeconds(episode))}, " +
                "übrig ${Format.duration(StatisticsEngine.remainingSeconds(episode))}"
        EpisodeProgress.UNPLAYED -> "ungespielt"
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(episode.title, style = MaterialTheme.typography.titleSmall)
            Text(
                item.podcastTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Dauer: ${Format.duration(episode.durationSec)} • $statusText • ${Format.date(episode.publishedEpochSec)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
