package com.paketcast.analytics.data.repository

import android.content.Context
import android.net.Uri
import com.paketcast.analytics.data.db.PocketCastsDbReader
import com.paketcast.analytics.data.model.OpmlSubscription
import com.paketcast.analytics.data.opml.OpmlParser
import com.paketcast.analytics.stats.AnalyticsSnapshot
import com.paketcast.analytics.stats.StatisticsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface AnalyticsState {
    data object Empty : AnalyticsState
    data object Loading : AnalyticsState
    data class Loaded(
        val snapshot: AnalyticsSnapshot,
        val opmlSubscriptions: List<OpmlSubscription>,
    ) : AnalyticsState
    data class OpmlOnly(val subscriptions: List<OpmlSubscription>) : AnalyticsState
    data class Error(val message: String) : AnalyticsState
}

/**
 * Lädt Pocket-Casts-Daten (pocketcasts.db und/oder OPML-Export) aus einer vom
 * Nutzer gewählten Datei. Die Datenbank wird vor dem Öffnen in den privaten
 * App-Speicher kopiert und dort ausschließlich lokal verarbeitet.
 */
class AnalyticsRepository(private val context: Context) {

    private val _state = MutableStateFlow<AnalyticsState>(AnalyticsState.Empty)
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private var opmlSubscriptions: List<OpmlSubscription> = emptyList()

    private val dbFile: File
        get() = File(context.filesDir, "pocketcasts.db")

    suspend fun importDatabase(uri: Uri) {
        _state.value = AnalyticsState.Loading
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val dest = dbFile
                context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) throw IllegalStateException("Datei konnte nicht geöffnet werden.")
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                val data = PocketCastsDbReader.read(dest.absolutePath)
                StatisticsEngine.buildSnapshot(
                    sourceName = "pocketcasts.db",
                    podcasts = data.podcasts,
                    episodes = data.episodes,
                    playbackEvents = data.playbackEvents,
                )
            }
        }
        _state.value = result.fold(
            onSuccess = { snapshot -> AnalyticsState.Loaded(snapshot, opmlSubscriptions) },
            onFailure = { AnalyticsState.Error(it.message ?: "Unbekannter Fehler beim Import") },
        )
    }

    suspend fun importOpml(uri: Uri) {
        _state.value = AnalyticsState.Loading
        val result = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) throw IllegalStateException("Datei konnte nicht geöffnet werden.")
                    OpmlParser.parse(input)
                }
            }
        }
        result.fold(
            onSuccess = { subscriptions ->
                opmlSubscriptions = subscriptions
                val current = _state.value
                _state.value = if (current is AnalyticsState.Loaded) {
                    current.copy(opmlSubscriptions = subscriptions)
                } else {
                    AnalyticsState.OpmlOnly(subscriptions)
                }
            },
            onFailure = { _state.value = AnalyticsState.Error(it.message ?: "Unbekannter Fehler beim Import") },
        )
    }
}
