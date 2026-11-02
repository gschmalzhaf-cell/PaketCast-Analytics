package com.paketcast.analytics.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.ui.components.PodcastStatRow
import com.paketcast.analytics.ui.components.SectionHeader
import com.paketcast.analytics.ui.components.StateScaffold

@Composable
fun PodcastsScreen(state: AnalyticsState) {
    StateScaffold(state) { snapshot ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SectionHeader("${snapshot.podcasts.size} Podcasts – sortiert nach Hörzeit")
            }
            items(snapshot.podcasts, key = { it.podcast.uuid }) { stats ->
                PodcastStatRow(stats)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
