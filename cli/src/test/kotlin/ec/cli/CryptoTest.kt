package ec.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Vectors made with `openssl enc -aes-256-cbc`, the tool the old fetch.sh used, key as-is, IV = its first 16 bytes. */
class CryptoTest {

    @Test
    fun `decrypts what openssl encrypted`() {
        val cipherText =
            "1ab77db8be44df9652ea27e8fae4ff1d2247a2a75cebaef4d510130701246ea09bf07fc697ab17576752114776ad59e1"
        assertEquals("Vyrdax,Drakzyph,Fyrryn,Elarzris\n\nR3,L2,R3,L1", decrypt(cipherText, KEY))
    }

    @Test
    fun `the wrong key fails with a message`() {
        assertFailsWith<Failure> { decrypt("a0d4d267b9e2db34061d4c19f1339703", KEY) }
    }

    @Test
    fun `a key of the wrong length is refused before decrypting`() {
        assertFailsWith<Failure> { decrypt("a0d4d267b9e2db34061d4c19f1339703", "short") }
    }

    @Test
    fun `ciphertext with a non-hex character is refused`() {
        assertFailsWith<Failure> { decrypt("a0d4d267b9e2db34061d4c19f13397zz", KEY) }
    }

    @Test
    fun `ciphertext with an odd number of digits is refused`() {
        assertFailsWith<Failure> { decrypt("a0d4d267b9e2db34061d4c19f133970", KEY) }
    }

    private companion object {
        const val KEY = "abcdefghijklmnopqrstuvwxyz012345"
    }
}
