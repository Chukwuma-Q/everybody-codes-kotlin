package ec.cli

import com.sun.net.httpserver.HttpServer
import ec.Part
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** fetch, check, submit and key against a fake site, served by the JDK's built-in HTTP server. */
class SiteTest {

    private data class Seen(val route: String, val cookie: String?, val userAgent: String?, val body: String)

    private val routes = mutableMapOf<String, String>()
    private val seen = mutableListOf<Seen>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            val route = "${exchange.requestMethod} ${exchange.requestURI.path}"
            seen += Seen(
                route,
                exchange.requestHeaders.getFirst("Cookie"),
                exchange.requestHeaders.getFirst("User-Agent"),
                exchange.requestBody.readBytes().decodeToString(),
            )
            val reply = routes[route]
            if (reply == null) {
                exchange.sendResponseHeaders(404, -1)
            } else {
                val bytes = reply.encodeToByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        start()
    }
    private val url = "http://127.0.0.1:${server.address.port}"
    private val root = createTempDirectory().toFile()
    private val workspace = Workspace(root)
    private val config = Config(mapOf("EC_TOKEN" to "test-token", "EC_SEED" to "79", "EC_USER_AGENT" to "test-agent"))
    private val api = Api(config, site = url, api = url)
    private val quest = QuestId.of("2025", "01")!!

    @AfterTest
    fun stop() = server.stop(0)

    @Test
    fun `fetch downloads, decrypts and writes the input, without sending the cookie to the assets host`() {
        routes["GET /assets/2025/1/input/79.json"] = """{"1": "$CIPHER_1", "2": "$CIPHER_2", "3": "00"}"""
        routes["GET /event/2025/quest/1"] = """{"penaltyLeftMs": 0, "key1": "$KEY_1"}"""

        assertTrue(fetchInputs(Command.Fetch(quest, listOf(Part.One)), workspace, api, force = false))

        assertEquals(PLAIN_1, workspace.input(quest, Part.One).readText())
        assertEquals(null, seen.first { it.route.startsWith("GET /assets") }.cookie)
        assertEquals("everybody-codes=test-token", seen.first { it.route.startsWith("GET /event") }.cookie)
        assertEquals("test-agent", seen.first().userAgent)
    }

    @Test
    fun `fetch leaves an existing input alone unless forced`() {
        workspace.input(quest, Part.One).apply { parentFile.mkdirs(); writeText("mine") }
        assertTrue(fetchInputs(Command.Fetch(quest, listOf(Part.One)), workspace, api, force = false))
        assertEquals("mine", workspace.input(quest, Part.One).readText())
        assertTrue(seen.isEmpty(), "nothing should be requested")
    }

    @Test
    fun `a locked part says which part to solve first`() {
        routes["GET /assets/2025/1/input/79.json"] = """{"1": "$CIPHER_1", "2": "$CIPHER_2"}"""
        routes["GET /event/2025/quest/1"] = """{"penaltyLeftMs": 0, "key1": "$KEY_1"}"""
        val failure = assertFailsWith<Failure> {
            fetchInputs(
                Command.Fetch(quest, listOf(Part.Two)),
                workspace,
                api,
                force = false
            )
        }
        assertTrue("part 1" in failure.message.orEmpty())
    }

    @Test
    fun `a saved key is used instead of asking the site`() {
        routes["GET /assets/2025/1/input/79.json"] = """{"1": "$CIPHER_1"}"""
        saveKey(Command.Key(quest, Part.One), workspace) { KEY_1 }
        assertTrue(fetchInputs(Command.Fetch(quest, listOf(Part.One)), workspace, api, force = false))
        assertEquals(PLAIN_1, workspace.input(quest, Part.One).readText())
        assertFalse(seen.any { it.route.startsWith("GET /event") })
    }

    @Test
    fun `a saved key is readable by its owner only`() {
        saveKey(Command.Key(quest, Part.One), workspace) { KEY_1 }
        val permissions = Files.getPosixFilePermissions(workspace.key(quest, Part.One).toPath())
        assertEquals(PosixFilePermissions.fromString("rw-------"), permissions)
    }

    @Test
    fun `check reports ok, unsolved and mismatch`() {
        routes["GET /event/2025/quest/1"] = """{"key1": "$KEY_1", "answer1": "Thymoryn", "answer2": "Selkrex"}"""
        Answers.record(workspace.answers(quest), mapOf(Part.One to "Thymoryn", Part.Two to "Wrong"))
        assertFalse(checkAnswers(Command.Check(quest), workspace, api))
        Answers.record(workspace.answers(quest), mapOf(Part.Two to "Selkrex"))
        assertTrue(checkAnswers(Command.Check(quest), workspace, api))
    }

    @Test
    fun `submit posts the saved answer as JSON, then fetches the next part`() {
        routes["GET /event/2025/quest/1"] = """{"penaltyLeftMs": 0, "key1": "$KEY_1", "key2": "$KEY_2"}"""
        routes["POST /event/2025/quest/1/part/1/answer"] =
            """{"correct": true, "globalPlace": 1505, "localTime": 1542335}"""
        routes["GET /assets/2025/1/input/79.json"] = """{"1": "$CIPHER_1", "2": "$CIPHER_2"}"""
        Answers.record(workspace.answers(quest), mapOf(Part.One to "Fyr\"ryn"))

        assertTrue(submitAnswer(Command.Submit(quest, Part.One), workspace, api) { true })

        assertEquals("""{"answer":"Fyr\"ryn"}""", seen.single { it.route.startsWith("POST") }.body)
        assertEquals(PLAIN_2, workspace.input(quest, Part.Two).readText())
    }

    @Test
    fun `submit sends nothing for a solved part, or without confirmation`() {
        Answers.record(workspace.answers(quest), mapOf(Part.One to "Thymoryn", Part.Two to "Selkrex"))
        routes["GET /event/2025/quest/1"] = """{"key1": "$KEY_1", "key2": "$KEY_2", "answer1": "Thymoryn"}"""
        assertTrue(submitAnswer(Command.Submit(quest, Part.One), workspace, api) { error("must not ask") })
        assertFalse(submitAnswer(Command.Submit(quest, Part.Two), workspace, api) { false })
        assertFalse(seen.any { it.route.startsWith("POST") })
    }

    @Test
    fun `submit refuses during a lockout and for a locked part`() {
        Answers.record(workspace.answers(quest), mapOf(Part.Two to "x", Part.Three to "y"))
        routes["GET /event/2025/quest/1"] = """{"key1": "$KEY_1", "key2": "$KEY_2", "penaltyLeftMs": 60000}"""
        assertTrue("wait 1m" in assertFailsWith<Failure> {
            submitAnswer(
                Command.Submit(quest, Part.Two),
                workspace,
                api
            ) { true }
        }.message.orEmpty())
        assertTrue("locked" in assertFailsWith<Failure> {
            submitAnswer(
                Command.Submit(quest, Part.Three),
                workspace,
                api
            ) { true }
        }.message.orEmpty())
    }

    @Test
    fun `an HTML page instead of JSON points at the token`() {
        routes["GET /event/2025/quest/1"] = "<html><title>Everybody Codes</title></html>"
        val failure = assertFailsWith<Failure> { checkAnswers(Command.Check(quest), workspace, api) }
        assertTrue("EC_TOKEN" in failure.message.orEmpty())
    }

    private companion object {
        const val KEY_1 = "abcdefghijklmnopqrstuvwxyz012345"
        const val KEY_2 = "ZYXWVUTSRQPONMLKJIHGFEDCBA987654"
        const val CIPHER_1 =
            "1ab77db8be44df9652ea27e8fae4ff1d2247a2a75cebaef4d510130701246ea09bf07fc697ab17576752114776ad59e1"
        const val CIPHER_2 = "a0d4d267b9e2db34061d4c19f1339703"
        const val PLAIN_1 = "Vyrdax,Drakzyph,Fyrryn,Elarzris\n\nR3,L2,R3,L1"
        const val PLAIN_2 = "A=[25,9]"
    }
}
