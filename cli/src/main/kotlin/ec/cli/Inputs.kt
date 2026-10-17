package ec.cli

import ec.Part
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.HexFormat

/**
 * Downloads and decrypts each requested part's input, skipping files already present unless [force].
 * A key saved with `make key` is preferred; otherwise it comes from the site.
 *
 * The encrypted inputs and the site's key don't depend on each other, so both requests start
 * before either is awaited: the wait is the slower of the two, not their sum. [prefetched] lets
 * a caller that already started the input download pass it in instead of downloading twice.
 */
internal suspend fun fetchInputs(
    command: Command.Fetch,
    workspace: Workspace,
    api: Api,
    force: Boolean = System.getenv("FORCE") == "1",
    prefetched: Deferred<Map<Part, String>>? = null,
): Unit = coroutineScope {
    val quest = command.quest
    val wanted = partsToFetch(command, workspace, force)
    if (wanted.isEmpty()) return@coroutineScope

    // Start both downloads, then wait. Asking the site for a key is skipped when every
    // wanted part has a saved one.
    val encrypted = prefetched ?: async { api.encryptedInputs(quest) }
    val siteKeysNeeded = wanted.any { !workspace.key(quest, it).hasContent() }
    val state = if (siteKeysNeeded) async { api.state(quest) } else null

    val cipherTexts = encrypted.await()
    for (part in wanted) {
        val cipherText = cipherTexts[part] ?: fail("No ciphertext for part ${part.number} in the input file.")
        writeInput(workspace.input(quest, part), decrypt(cipherText, keyFor(quest, part, workspace, state)))
    }
}

/** The requested parts whose input is missing, or all of them when [force]d. Says which it skips. */
private fun partsToFetch(command: Command.Fetch, workspace: Workspace, force: Boolean): List<Part> =
    command.parts.filter { part ->
        val file = workspace.input(command.quest, part)
        val needed = force || !file.hasContent()
        if (!needed) println("${file.path} already present. FORCE=1 to overwrite.")
        needed
    }

/** The key saved with `make key` if there is one; otherwise the site's, from [state]. */
private suspend fun keyFor(quest: QuestId, part: Part, workspace: Workspace, state: Deferred<QuestState>?): String {
    val saved = workspace.key(quest, part)
    if (saved.hasContent()) {
        println("key   ${saved.path}")
        return saved.readText().trim()
    }
    println("key   API")
    // Not null: this part has no saved key, so fetchInputs asked the site.
    return checkNotNull(state).await().keys[part] ?: fail(missingKeyMessage(part))
}

private fun missingKeyMessage(part: Part): String =
    if (part == Part.One) "No key1 from the site. Is EC_TOKEN current?"
    else "No key${part.number} yet. Solve and submit part ${part.number - 1} on the site first."

private fun writeInput(file: File, text: String) {
    file.parentFile?.mkdirs()
    file.writeText(text)
    println("wrote ${file.path} (${lineCount(text)} lines)")
}

/** AES-256-CBC as the site uses it: the 32-byte key as-is, and its first 16 bytes as the IV. */
@Suppress("kotlin:S5542") // Mode is fixed by everybody.codes' input format, not our choice; GCM can't decrypt it.
internal fun decrypt(hexCipherText: String, key: String): String {
    val keyBytes = key.toByteArray(Charsets.UTF_8)
    if (keyBytes.size != KEY_BYTES) {
        fail("Key is ${keyBytes.size} bytes (${key.length} characters); expected $KEY_BYTES bytes.")
    }
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(keyBytes.copyOf(IV_BYTES)))
    val plain = try {
        cipher.doFinal(hexToBytes(hexCipherText))
    } catch (e: GeneralSecurityException) {
        fail("Decrypt failed: ${e.message ?: e::class.simpleName}")
    }
    return String(plain, Charsets.UTF_8)
}

private fun hexToBytes(hex: String): ByteArray =
    try {
        HexFormat.of().parseHex(hex.trim())
    } catch (e: IllegalArgumentException) {
        fail("The ciphertext isn't a hex string: ${e.message}")
    }

/** Lines as a person counts them: a final newline doesn't start another line. */
private fun lineCount(text: String): Int =
    if (text.isEmpty()) 0 else text.count { it == '\n' } + if (text.endsWith('\n')) 0 else 1

private const val KEY_BYTES = 32
private const val IV_BYTES = 16
