package ec.cli

import ec.Part
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CommandTest {

    private fun parse(line: String) = Command.parse(line.split(" "))

    @Test
    fun `run with no parts means all three`() {
        val run = assertIs<Command.Run>(parse("run 2025 02"))
        assertEquals(Part.entries.toList(), run.parts)
        assertEquals("2025/02", run.quest.toString())
    }

    @Test
    fun `a fourth part is rejected at the boundary`() {
        assertNull(parse("run 2025 02 4"))
        assertNull(parse("submit 2025 02 4"))
    }

    @Test
    fun `submit and key need exactly one part`() {
        assertNull(parse("submit 2025 02"))
        assertNull(parse("submit 2025 02 1 2"))
        assertEquals(Part.Three, assertIs<Command.Submit>(parse("submit 2025 02 3")).part)
        assertIs<Command.Key>(parse("key 2025 02 1"))
    }

    @Test
    fun `check takes no part`() {
        assertIs<Command.Check>(parse("check 2025 02"))
        assertNull(parse("check 2025 02 1"))
    }

    @Test
    fun `quests outside the event are rejected`() {
        assertNull(parse("run 2025 0"))
        assertNull(parse("run 2025 21"))
        assertNull(parse("run 2023 01"))
        assertNull(parse("run 2025"))
    }

    @Test
    fun `unknown commands are rejected`() {
        assertNull(parse("solve 2025 01"))
    }

    @Test
    fun `padded and plain quest numbers name the same quest`() {
        assertEquals(parse("check 2025 3")?.quest, parse("check 2025 03")?.quest)
    }
}
