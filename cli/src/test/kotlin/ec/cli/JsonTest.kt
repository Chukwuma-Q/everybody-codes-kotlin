package ec.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JsonTest {

    @Test
    fun `reads the shape of the keys response`() {
        val obj = Json.parseObject("""{"penaltyUntil": null, "penaltyLeftMs": 0, "key1": "K!Bu]\"x\\y", "answer1": "Thymoryn"}""")
        assertEquals("K!Bu]\"x\\y", obj.text("key1"))
        assertEquals(0L, obj.long("penaltyLeftMs"))
        assertNull(obj.text("penaltyUntil"))
        assertNull(obj.text("key2"))
    }

    @Test
    fun `numbers can be read as text, since answers can be either`() {
        assertEquals("539", Json.parseObject("""{"answer2": 539}""").text("answer2"))
    }

    @Test
    fun `nested values and unicode escapes parse`() {
        val obj = Json.parseObject("""{"a": [1, {"b": true}], "c": "\u00e9"}""")
        assertEquals("é", obj.text("c"))
    }

    @Test
    fun `malformed input is rejected, not guessed at`() {
        assertFailsWith<IllegalArgumentException> { Json.parse("<html>") }
        assertFailsWith<IllegalArgumentException> { Json.parse("""{"a": 1,}""") }
        assertFailsWith<IllegalArgumentException> { Json.parse("""{"a": 1} extra""") }
    }

    @Test
    fun `quoting survives a round trip through the parser`() {
        val tricky = "say \"hi\"\\ \n\t end"
        assertEquals(tricky, (Json.parse(Json.quote(tricky)) as Json.Str).value)
    }
}
