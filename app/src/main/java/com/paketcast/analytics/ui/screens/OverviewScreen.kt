package com.paketcast.analytics.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.ui.components.EmptyState
import com.paketcast.analytics.ui.components.EpisodeRow
import com.paketcast.analytics.ui.components.PodcastStatRow
import com.paketcast.analytics.ui.components.SectionHeader
import com.paketcast.analytics.ui.components.StatCard
import com.paketcast.analytics.ui.components.StateScaffold
import com.paketcast.analytics.ui.format.Format

@Composable
fun OverviewScreen(
    state: AnalyticsState,
    onImportDatabase: () -> Unit,
    onImportOpml: () -> Unit,
) {
    StateScaffold(
        state = state,
        emptyContent = { EmptyState(onImportDatabase, onImportOpml) },
    ) { snapshot ->
        val overview = snapshot.overview
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    "Quelle: ${snapshot.sourceName} • importiert am ${Format.date(snapshot.importedAtEpochSec)}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    StatCard(
                        title = "Gesamte Hörzeit",
                        value = Format.duration(overview.totalListenedSec),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatCard(
                        title = "Abgeschlossene Folgen",
                        value = "${overview.finishedCount}",
                        subtitle = "${overview.episodeCount} Folgen gesamt",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    StatCard(
                        title = "In Arbeit",
                        value = "${overview.inProgressCount}",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatCard(
                        title = "Ungespielt",
                        value = "${overview.unplayedCount}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    StatCard(
                        title = "Noch zu hören",
                        value = Format.duration(overview.remainingSec),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatCard(
                        title = "Podcasts",
                        value = "${overview.podcastCount}",
                        subtitle = "Ø ${Format.duration(overview.averageListenedPerEpisodeSec)} pro Folge",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (overview.topPodcast != null) {
                item {
                    SectionHeader("Meistgehörter Podcast")
                    PodcastStatRow(overview.topPodcast)
                }
            }
            if (overview.longestEpisode != null) {
                item {
                    SectionHeader("Längste Folge")
                    EpisodeRow(overview.longestEpisode)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
