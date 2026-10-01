package ec.y2025

import ec.Answer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Quest05Test {

    private val quest = Quest05()

    @Test
    fun `part 1 sample`() {
        val sample = """
            58:5,3,7,8,9,10,4,5,7,8,8
        """.trimIndent()
        assertEquals(Answer(581078), quest.part1(sample))
    }

    @Ignore("unlocks after part 1: paste its sample, then delete this line")
    @Test
    fun `part 2 sample`() {
        val sample = """
            REPLACE ME
        """.trimIndent()
        assertEquals(Answer("REPLACE ME"), quest.part2(sample))
    }

    @Ignore("unlocks after part 2: paste its sample, then delete this line")
    @Test
    fun `part 3 sample`() {
        val sample = """
            REPLACE ME
        """.trimIndent()
        assertEquals(Answer("REPLACE ME"), quest.part3(sample))
    }
}
