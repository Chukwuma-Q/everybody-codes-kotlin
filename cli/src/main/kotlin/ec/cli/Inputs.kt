package ec.cli

import ec.Part
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.HexFormat

/**
 * Downloads and decrypts each requested part's input, skipping files already present unless [force].
 * A key saved with `make key` is preferred; otherwise it comes from the site.
 */
internal fun fetchInputs(
    command: Command.Fetch,
    workspace: Workspace,
    api: Api,
    force: Boolean = System.getenv("FORCE") == "1",
): Boolean {
    val quest = command.quest
    val wanted = command.parts.filter { part ->
        val file = workspace.input(quest, part)
        val needed = force || !file.hasContent()
        if (!needed) println("${file.path} already present. FORCE=1 to overwrite.")
        needed
    }
    if (wanted.isEmpty()) return true

    val encrypted = api.encryptedInputs(quest)
    val state by lazy { api.state(quest) }
    for (part in wanted) {
        val cipherText = encrypted[part] ?: fail("No ciphertext for part ${part.number} in the input file.")
        val saved = workspace.key(quest, part)
        val key = if (saved.hasContent()) {
            println("key   ${saved.path}")
            saved.readText().trim()
        } else {
            println("key   API")
            state.keys[part] ?: fail(
                if (part == Part.One) "No key1 from the site. Is EC_TOKEN current?"
                else "No key${part.number} yet. Solve and submit part ${part.number - 1} on the site first."
            )
        }
        val text = decrypt(cipherText, key)
        val file = workspace.input(quest, part)
        file.parentFile?.mkdirs()
        file.writeText(text)
        println("wrote ${file.path} (${lineCount(text)} lines)")
    }
    return true
}

/** AES-256-CBC as the site uses it: the 32-byte key as-is, and its first 16 bytes as the IV. */
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
