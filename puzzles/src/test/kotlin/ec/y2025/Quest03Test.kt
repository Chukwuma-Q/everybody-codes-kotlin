package ec.y2025

import ec.Answer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Quest03Test {

    private val quest = Quest03()

    @Test
    fun `part 1 sample`() {
        val sample = """
            10,5,1,10,3,8,5,2,2
        """.trimIndent()
        assertEquals(Answer(29), quest.part1(sample))
    }

    @Test
    fun `part 2 sample`() {
        val sample = """
        4,51,13,64,57,51,82,57,16,88,89,48,32,49,49,2,84,65,49,43,9,13,2,3,75,72,63,48,61,14,40,77
    """.trimIndent()
        assertEquals(Answer(781), quest.part2(sample))
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