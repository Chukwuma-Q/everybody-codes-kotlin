package ec.cli

import java.io.File
import java.time.Instant
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Settings from `.env`, with any `EC_*` environment variables taking precedence. */
internal class Config(private val values: Map<String, String>) {

    val token: String get() = required("EC_TOKEN", "the everybody-codes cookie value")
    val seed: String get() = required("EC_SEED", "the number in your input file's name")
    val cookie: String get() = values["EC_COOKIE"] ?: "everybody-codes"
    val userAgent: String get() = values["EC_USER_AGENT"] ?: "everybody-codes-kotlin"

    private fun required(name: String, what: String): String =
        values[name]?.takeIf { it.isNotBlank() } ?: fail("Set $name in .env ($what).")

    companion object {
        fun load(file: File, environment: Map<String, String> = System.getenv()): Config {
            val fromFile = if (file.isFile) file.readLines().mapNotNull(::entry).toMap() else emptyMap()
            return Config(fromFile + environment.filterKeys { it.startsWith("EC_") })
        }

        /** `KEY=value`, skipping blanks and comments, with one pair of surrounding quotes removed. */
        fun entry(line: String): Pair<String, String>? {
            val text = line.trim()
            if (text.isEmpty() || text.startsWith('#') || '=' !in text) return null
            val value = text.substringAfter('=').trim()
            val unquoted =
                if (value.length >= 2 && value.first() == value.last() && value.first() in "\"'") value.substring(1, value.length - 1)
                else value
            return text.substringBefore('=').trim() to unquoted
        }
    }
}

/** Time left before a JWT's `exp` claim, or null if [token] isn't a readable JWT. */
internal fun tokenLifetime(token: String, now: Instant = Instant.now()): Duration? = runCatching {
    val payload = String(Base64.getUrlDecoder().decode(token.split('.')[1]), Charsets.UTF_8)
    val expiry = Json.parseObject(payload).long("exp") ?: return null
    (expiry - now.epochSecond).seconds
}.getOrNull()
