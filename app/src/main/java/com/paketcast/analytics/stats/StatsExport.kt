package com.paketcast.analytics.stats

import com.paketcast.analytics.data.model.Episode
import java.time.format.DateTimeFormatter

/**
 * Serialisiert einen AnalyticsSnapshot als CSV (Podcasts, Semikolon-getrennt
 * für deutsche Excel-Versionen) und als JSON (komplette Statistik inkl. Verlauf).
 * Reine Kotlin-Logik ohne Android-/org.json-Abhängigkeit, damit unit-testbar.
 */
object StatsExport {

    private val DateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun snapshotToCsv(s: AnalyticsSnapshot): String = buildString {
        appendLine("# PaketCast Analytics – Export vom ${epochToIso(s.importedAtEpochSec)} (Quelle: ${s.sourceName})")
        appendLine("podcast;netzwerk;episoden;abgeschlossen;in_arbeit;ungespielt;gehoert_sek;noch_offen_sek")
        for (p in s.podcasts) {
            appendLine(
                listOf(
                    p.podcast.title,
                    networkOf(p),
                    p.episodeCount.toString(),
                    p.finishedCount.toString(),
                    p.inProgressCount.toString(),
                    p.unplayedCount.toString(),
                    p.listenedSec.toString(),
                    p.remainingSec.toString(),
                ).joinToString(";", transform = ::csvEsc)
            )
        }
        appendLine()
        appendLine("# Übersicht: ${s.overview.episodeCount} Folgen, gehört ${s.overview.totalListenedSec} s, noch offen ${s.overview.remainingSec} s")
    }

    private fun networkOf(p: PodcastStats): String =
        StatisticsEngine.networkOf(p.podcast)

    fun csvEsc(value: String): String =
        if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    fun snapshotToJson(s: AnalyticsSnapshot): String = buildString {
        appendLine("{")
        appendLine("  \"app\": \"PaketCast Analytics\",")
        appendLine("  \"source\": ${jsonEsc(s.sourceName)},")
        appendLine("  \"importedAt\": ${jsonEsc(epochToIso(s.importedAtEpochSec))},")
        appendLine("  \"overview\": " + overviewJson(s.overview) + ",")
        appendLine("  \"podcasts\": [")
        s.podcasts.forEachIndexed { i, p ->
            append("    " + podcastJson(p))
            appendLine(if (i < s.podcasts.lastIndex) "," else "")
        }
        appendLine("  ],")
        appendLine("  \"top10\": {")
        appendLine("    \"byListened\": [" + s.top10.byListened.joinToString(", ") { podcastJson(it, indent = "      ") } + "],")
        appendLine("    \"byFinished\": [" + s.top10.byFinished.joinToString(", ") { podcastJson(it, indent = "      ") } + "]")
        appendLine("  },")
        appendLine("  \"networks\": [" + s.networks.joinToString(", ") { networkJson(it) } + "],")
        appendLine("  \"extremes\": {")
        appendLine("    \"longest\": [" + s.extremes.longest.joinToString(", ") { episodeJson(it) } + "],")
        appendLine("    \"shortest\": [" + s.extremes.shortest.joinToString(", ") { episodeJson(it) } + "]")
        appendLine("  },")
        appendLine("  \"remaining\": {")
        appendLine("    \"totalRemainingSec\": ${s.remaining.totalRemainingSec},")
        appendLine("    \"openEpisodeCount\": ${s.remaining.openEpisodeCount},")
        appendLine("    \"episodes\": [" + s.remaining.episodes.joinToString(", ") { episodeJson(it) } + "]")
        appendLine("  },")
        appendLine("  \"history\": " + historyJson(s.history))
        appendLine("}")
    }

    private fun overviewJson(o: OverviewStats): String = buildString {
        append("{")
        append("\"podcastCount\": ${o.podcastCount}, ")
        append("\"episodeCount\": ${o.episodeCount}, ")
        append("\"totalListenedSec\": ${o.totalListenedSec}, ")
        append("\"finishedCount\": ${o.finishedCount}, ")
        append("\"inProgressCount\": ${o.inProgressCount}, ")
        append("\"unplayedCount\": ${o.unplayedCount}, ")
        append("\"remainingSec\": ${o.remainingSec}, ")
        append("\"averageListenedPerEpisodeSec\": ${o.averageListenedPerEpisodeSec}")
        append("}")
    }

    private fun podcastJson(p: PodcastStats, indent: String = "    "): String = buildString {
        append("{")
        append("\"title\": ${jsonEsc(p.podcast.title)}, ")
        append("\"network\": ${jsonEsc(networkOf(p))}, ")
        append("\"feedUrl\": ${jsonEsc(p.podcast.feedUrl)}, ")
        append("\"episodeCount\": ${p.episodeCount}, ")
        append("\"finishedCount\": ${p.finishedCount}, ")
        append("\"inProgressCount\": ${p.inProgressCount}, ")
        append("\"unplayedCount\": ${p.unplayedCount}, ")
        append("\"listenedSec\": ${p.listenedSec}, ")
        append("\"remainingSec\": ${p.remainingSec}")
        append("}")
    }

    private fun networkJson(n: NetworkStats): String = buildString {
        append("{")
        append("\"network\": ${jsonEsc(n.network)}, ")
        append("\"podcastCount\": ${n.podcastCount}, ")
        append("\"listenedSec\": ${n.listenedSec}, ")
        append("\"finishedEpisodes\": ${n.finishedEpisodes}")
        append("}")
    }

    private fun episodeJson(e: EpisodeWithPodcast): String {
        val ep: Episode = e.episode
        return buildString {
            append("{")
            append("\"title\": ${jsonEsc(ep.title)}, ")
            append("\"podcast\": ${jsonEsc(e.podcastTitle)}, ")
            append("\"durationSec\": ${ep.durationSec}, ")
            append("\"playedUpToSec\": ${ep.playedUpToSec}, ")
            append("\"remainingSec\": ${StatisticsEngine.remainingSeconds(ep)}, ")
            append("\"progress\": ${jsonEsc(StatisticsEngine.progressOf(ep).name)}, ")
            append("\"published\": ${jsonEsc(ep.publishedEpochSec?.let(::epochToIso))}")
            append("}")
        }
    }

    private fun historyJson(h: HistoryStats): String = buildString {
        append("{")
        append("\"available\": ${h.available}, ")
        append("\"eventCount\": ${h.eventCount}, ")
        append("\"totalSeconds\": ${h.totalSeconds}, ")
        append("\"daily\": [")
        append(h.daily.joinToString(", ") { "{\"date\": \"${it.date.format(DateFmt)}\", \"seconds\": ${it.seconds}}" })
        append("], ")
        append("\"weekly\": [")
        append(h.weekly.joinToString(", ") { "{\"weekStart\": \"${it.weekStart.format(DateFmt)}\", \"seconds\": ${it.seconds}}" })
        append("]")
        append("}")
    }

    fun jsonEsc(value: String?): String {
        if (value == null) return "null"
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.append("\"").toString()
    }

    private fun epochToIso(epochSec: Long): String =
        java.time.Instant.ofEpochSecond(epochSec).toString()
}
