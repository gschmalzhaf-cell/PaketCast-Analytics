package com.paketcast.analytics.data.api

import com.paketcast.analytics.data.json.MiniJson
import com.paketcast.analytics.data.model.PlaybackEvent
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Inoffizielle Pocket-Casts-API (Web-Player-Backend, api.pocketcasts.com).
 * Offiziell existiert keine API (siehe Pocket-Casts-Support); Endpunkte können
 * ohne Vorwarnung ändern. Login: POST /user/login (form-encoded) -> Bearer-Token.
 * Danach POST /user/history, /user/starred, /user/subscriptions mit dem Token.
 */
object PocketCastsApiClient {

    private const val BASE = "https://api.pocketcasts.com"

    data class ApiAccount(
        val subscriptions: List<ApiSubscription>,
        val history: List<PlaybackEvent>,
        val starredTitles: List<String>,
    )

    data class ApiSubscription(
        val title: String,
        val feedUrl: String?,
    )

    @Throws(IOException::class)
    fun login(email: String, password: String): String {
        val body = "email=${enc(email)}&password=${enc(password)}"
        val response = post("$BASE/user/login", body, contentType = "application/x-www-form-urlencoded", token = null)
        val token = MiniJson.parseObject(response)["token"] as? String
            ?: throw IOException("Login fehlgeschlagen: kein Token in der Antwort (E-Mail/Passwort prüfen?)")
        return token
    }

    @Throws(IOException::class)
    fun fetchAccount(token: String): ApiAccount {
        val historyJson = post("$BASE/user/history", "{}", token)
        val history = historyEventsFromJson(historyJson)
        val starredTitles = starredFromJson(post("$BASE/user/starred", "{}", token))
        val subscriptions = runCatching { subscriptionsFromJson(post("$BASE/user/subscriptions", "{}", token)) }
            .getOrDefault(emptyList())
        return ApiAccount(subscriptions, history, starredTitles)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun post(url: String, body: String, token: String?, contentType: String = "application/json"): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
        conn.disconnect()
        if (code !in 200..299) {
            throw IOException("Pocket-Casts-API: HTTP $code – ${text.take(200)}")
        }
        return text
    }

    private fun asList(value: Any?): List<Any?> =
        (value as? List<*>)?.filterNotNull().orEmpty()

    private fun asMap(value: Any?): Map<String, Any?> =
        (value as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v } ?: emptyMap()

    private fun pick(map: Map<String, Any?>, vararg keys: String): Any? =
        keys.firstNotNullOfOrNull { map[it] }

    private fun epochOf(raw: Any?): Long = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull() ?: 0L
        else -> 0L
    }.let { if (it > 10_000_000_000_000L) it / 1000L else it }

    /** Mappt die /user/history-Antwort auf PlaybackEvents (defensiv gegen Feldnamen-Varianten). */
    fun historyEventsFromJson(json: String): List<PlaybackEvent> {
        val root = MiniJson.parseObject(json)
        val rows = asList(pick(root, "episodes", "history", "items")) +
            asList(root["episodes"] ?: root["history"])
        return rows.mapNotNull { row ->
            val m = asMap(row)
            val time = pick(m, "time", "played_at", "playedAt", "date", "timestamp") ?: return@mapNotNull null
            val seconds = (pick(m, "duration", "seconds", "played_up_to") as? Number)?.toLong() ?: 0L
            PlaybackEvent(
                episodeUuid = pick(m, "episode_uuid", "episodeUuid", "uuid") as? String,
                epochSec = epochOf(time),
                seconds = seconds.coerceAtLeast(0L),
            )
        }.filter { it.epochSec > 0 }
    }

    fun starredFromJson(json: String): List<String> {
        val root = MiniJson.parseObject(json)
        val rows = asList(pick(root, "episodes", "items", "starred"))
        return rows.mapNotNull { pick(asMap(it), "title") as? String }
    }

    fun subscriptionsFromJson(json: String): List<ApiSubscription> {
        val root = MiniJson.parseObject(json)
        val rows = asList(pick(root, "podcasts", "subscriptions", "items"))
        return rows.mapNotNull { row ->
            val m = asMap(row)
            val title = pick(m, "title") as? String ?: return@mapNotNull null
            ApiSubscription(
                title = title,
                feedUrl = pick(m, "podcast_url", "podcastUrl", "url", "feed_url") as? String,
            )
        }
    }
}
