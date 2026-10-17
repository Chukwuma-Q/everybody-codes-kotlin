package ec.cli

import ec.Part
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.future.await

/** What the site knows about one quest: the keys you've unlocked, accepted answers, any lockout. */
internal class QuestState(val keys: Map<Part, String>, val answers: Map<Part, String>, val lockout: Duration)

/** The site's reply to a submitted answer. */
internal class Verdict(val correct: Boolean, val globalPlace: Long?, val localTime: Duration?, val raw: String)

/**
 * The two hosts the tool talks to, verified against the live site:
 *
 *     GET  {site}/assets/<year>/<quest>/input/<seed>.json        {"1": hex, "2": hex, "3": hex}
 *     GET  {api}/event/<year>/quest/<quest>                      key1…, answer1…, penaltyLeftMs
 *     POST {api}/event/<year>/quest/<quest>/part/<p>/answer      {"answer": "…"} → {"correct": …}
 *
 * The api calls authenticate with the session cookie; the encrypted inputs need none.
 *
 * Every call suspends instead of blocking: [HttpClient.sendAsync] does the waiting on the client's
 * own threads, and [await] resumes the caller when the response arrives. While one call waits,
 * the caller's thread is free to start another, which is what lets two downloads overlap.
 */
internal class Api(
    private val config: Config,
    private val site: String = "https://everybody.codes",
    private val api: String = "https://api.everybody.codes",
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT.toJavaDuration()).build(),
) {
    private var tokenChecked = false

    suspend fun encryptedInputs(quest: QuestId): Map<Part, String> {
        val blob = send(
            get("$site/assets/${quest.year}/${quest.number}/input/${config.seed}.json"),
            authenticated = false
        ).first
        return Part.entries.mapNotNull { part -> blob.text("${part.number}")?.let { part to it } }.toMap()
    }

    suspend fun state(quest: QuestId): QuestState {
        val obj = send(get("$api/event/${quest.year}/quest/${quest.number}"), authenticated = true).first
        return QuestState(
            keys = Part.entries.mapNotNull { part -> obj.text("key${part.number}")?.let { part to it } }.toMap(),
            answers = Part.entries.mapNotNull { part -> obj.text("answer${part.number}")?.let { part to it } }.toMap(),
            lockout = (obj.long("penaltyLeftMs") ?: 0).milliseconds,
        )
    }

    suspend fun submit(quest: QuestId, part: Part, answer: String): Verdict {
        val request =
            HttpRequest.newBuilder(URI("$api/event/${quest.year}/quest/${quest.number}/part/${part.number}/answer"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"answer":${Json.quote(answer)}}"""))
        val (obj, raw) = send(request, authenticated = true)
        return Verdict(obj.bool("correct") == true, obj.long("globalPlace"), obj.long("localTime")?.milliseconds, raw)
    }

    private fun get(url: String): HttpRequest.Builder = HttpRequest.newBuilder(URI(url)).GET()

    private suspend fun send(builder: HttpRequest.Builder, authenticated: Boolean): Pair<Json.Obj, String> {
        builder.timeout(TIMEOUT.toJavaDuration())
            .header("User-Agent", config.userAgent)
            .header("Accept", "application/json")
        if (authenticated) {
            checkToken()
            builder.header("Cookie", "${config.cookie}=${config.token}")
        }
        val request = builder.build()
        val where = "${request.method()} ${request.uri().path}"
        val response = try {
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        } catch (e: IOException) {
            fail("$where: ${e.message ?: e::class.simpleName}")
        }
        if (response.statusCode() != 200) fail("$where: HTTP ${response.statusCode()}")
        val body = response.body()
        val obj = runCatching { Json.parseObject(body) }.getOrElse {
            val start = body.trim().take(1).ifEmpty { "(empty)" }
            fail("$where: response is not JSON (starts with '$start'). Is EC_TOKEN current?")
        }
        return obj to body
    }

    /** Refuses an expired token up front, and warns in its last day, instead of failing mysteriously. */
    private fun checkToken() {
        if (tokenChecked) return
        tokenChecked = true
        val left = tokenLifetime(config.token) ?: return
        if (!left.isPositive()) fail("EC_TOKEN has expired. Copy a fresh everybody-codes cookie value into .env.")
        if (left < 1.days) System.err.println("note  EC_TOKEN expires in ${left.inWholeHours}h, refresh it soon")
    }

    private companion object {
        val TIMEOUT: Duration = 20_000.milliseconds
    }
}
