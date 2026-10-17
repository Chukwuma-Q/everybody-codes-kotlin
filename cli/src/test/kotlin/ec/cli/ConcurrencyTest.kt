package ec.cli

import com.sun.net.httpserver.HttpServer
import ec.Part
import ec.cli.SiteTest.Companion.CIPHER_1
import ec.cli.SiteTest.Companion.CIPHER_2
import ec.cli.SiteTest.Companion.KEY_1
import ec.cli.SiteTest.Companion.KEY_2
import ec.cli.SiteTest.Companion.PLAIN_1
import ec.cli.SiteTest.Companion.PLAIN_2
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/**
 * Proves that independent requests overlap, against a fake site where every response is slow.
 *
 * The site counts how many requests it is serving at the same moment. A client that waits for
 * each response before sending the next request can never push that count above 1, so a count
 * of 2 is direct evidence of overlap. Timings are printed too, for the test report.
 */
class ConcurrencyTest {

    private val slow: Duration = 500.milliseconds
    private val routes = ConcurrentHashMap<String, String>()
    private val arrived = CopyOnWriteArrayList<String>()   // requests in the order they reached the site
    private val inFlight = AtomicInteger()
    private val mostAtOnce = AtomicInteger()

    // A thread per request. The server's default executor handles one exchange at a time,
    // which would serialise the requests on the server side and hide any overlap.
    private val pool = Executors.newCachedThreadPool()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        executor = pool
        createContext("/") { exchange ->
            val route = "${exchange.requestMethod} ${exchange.requestURI.path}"
            arrived += route
            val now = inFlight.incrementAndGet()
            mostAtOnce.accumulateAndGet(now) { a, b -> maxOf(a, b) }
            try {
                exchange.requestBody.readBytes()
                Thread.sleep(slow.inWholeMilliseconds)
                val reply = routes[route]
                if (reply == null) {
                    exchange.sendResponseHeaders(404, -1)
                } else {
                    val bytes = reply.encodeToByteArray()
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                }
            } catch (_: Exception) {
                // The client cancelled this request (a prefetch nobody needed) or the test ended.
            } finally {
                inFlight.decrementAndGet()
                exchange.close()
            }
        }
        start()
    }
    private val url = "http://127.0.0.1:${server.address.port}"
    private val root = createTempDirectory().toFile()
    private val workspace = Workspace(root)
    private val config = Config(mapOf("EC_TOKEN" to "test-token", "EC_SEED" to "79"))
    private val api = Api(config, site = url, api = url)
    private val quest = QuestId.of("2025", "01")!!

    @AfterTest
    fun stop() {
        server.stop(0)
        pool.shutdownNow()
        root.deleteRecursively()
    }

    @Test
    fun `fetch downloads the input and asks for the key at the same time`() {
        routes[ASSETS] = """{"1": "$CIPHER_1"}"""
        routes[STATE] = """{"penaltyLeftMs": 0, "key1": "$KEY_1"}"""

        val took = measureTime {
            runBlocking { fetchInputs(Command.Fetch(quest, listOf(Part.One)), workspace, api, force = false) }
        }

        assertEquals(PLAIN_1, workspace.input(quest, Part.One).readText())
        assertEquals(2, arrived.size)
        assertEquals(2, mostAtOnce.get(), "both requests should have been in flight together")
        report("make fetch", requests = 2, took)
        assertTrue(took < slow * 2, "overlapped requests should beat one-after-another (${slow * 2}); took $took")
    }

    @Test
    fun `submit downloads the next input while it checks the state, and reuses it`() {
        routes[STATE] = """{"penaltyLeftMs": 0, "key1": "$KEY_1", "key2": "$KEY_2"}"""
        routes[ANSWER] = """{"correct": true, "globalPlace": 7, "localTime": 1000}"""
        routes[ASSETS] = """{"1": "$CIPHER_1", "2": "$CIPHER_2"}"""
        Answers.record(workspace.answers(quest), mapOf(Part.One to "Fyrryn"))

        val took = measureTime {
            assertTrue(runBlocking { submitAnswer(Command.Submit(quest, Part.One), workspace, api) { true } })
        }

        assertEquals(PLAIN_2, workspace.input(quest, Part.Two).readText())
        assertEquals(2, mostAtOnce.get(), "the state and the next input should have been in flight together")
        assertEquals(1, arrived.count { it == ASSETS }, "the prefetched input is reused, not downloaded again")
        assertTrue(
            arrived.indexOf(ANSWER) < arrived.lastIndexOf(STATE),
            "the next key is asked for only after the answer is accepted: $arrived",
        )
        report("make submit", requests = arrived.size, took)
        assertTrue(took < slow * arrived.size, "overlap should beat one-after-another; took $took")
    }

    @Test
    fun `a prefetch that fails doesn't fail a submit that never needed it`() {
        // No ASSETS route: the prefetch gets a 404. The part is already solved, so it's never used.
        routes[STATE] = """{"key1": "$KEY_1", "answer1": "Fyrryn"}"""
        Answers.record(workspace.answers(quest), mapOf(Part.One to "Fyrryn"))

        assertTrue(runBlocking { submitAnswer(Command.Submit(quest, Part.One), workspace, api) { error("must not ask") } })
    }

    private fun report(command: String, requests: Int, took: Duration) = println(
        "$command: $requests requests to a site that takes ${slow.inWholeMilliseconds} ms each. " +
            "One after another: at least ${(slow * requests).inWholeMilliseconds} ms. " +
            "Measured: ${took.inWholeMilliseconds} ms, with up to ${mostAtOnce.get()} requests in flight at once.",
    )

    private companion object {
        const val ASSETS = "GET /assets/2025/1/input/79.json"
        const val STATE = "GET /event/2025/quest/1"
        const val ANSWER = "POST /event/2025/quest/1/part/1/answer"
    }
}
