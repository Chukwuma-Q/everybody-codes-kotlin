package ec.cli

import java.time.Instant
import java.util.Base64
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigTest {

    private val dir = createTempDirectory().toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `values lose exactly one pair of surrounding quotes`() {
        assertEquals(Config.entry("""EC_USER_AGENT="tool (+https://x)""""), "EC_USER_AGENT" to "tool (+https://x)")
        assertEquals(Config.entry("EC_SEED=79"), "EC_SEED" to "79")
        assertEquals(Config.entry("A=''it's''"), "A" to "'it's'")
        assertNull(Config.entry("# a comment"))
        assertNull(Config.entry("   "))
    }

    @Test
    fun `environment variables override the file`() {
        val file = dir.resolve(".env").also { it.writeText("EC_SEED=79\nEC_TOKEN=from-file\n") }
        val config = Config.load(file, mapOf("EC_TOKEN" to "from-env", "HOME" to "/ignored"))
        assertEquals("79", config.seed)
        assertEquals("from-env", config.token)
        assertEquals("everybody-codes", config.cookie)
    }

    @Test
    fun `a missing token is a sentence, not a crash`() {
        val failure = assertFailsWith<Failure> { Config(emptyMap()).token }
        assertTrue("EC_TOKEN" in failure.message.orEmpty())
    }

    @Test
    fun `token lifetime is read from the JWT payload`() {
        val now = Instant.ofEpochSecond(1_000_000)
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"id":1,"exp":1003600}""".toByteArray())
        assertEquals(3600L, tokenLifetime("header.$payload.signature", now)?.inWholeSeconds)
        assertNull(tokenLifetime("not-a-jwt", now))
    }
}
