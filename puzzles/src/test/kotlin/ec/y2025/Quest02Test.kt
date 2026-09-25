package ec.y2025

import ec.Answer
import kotlin.test.Test
import kotlin.test.assertEquals

class Quest02Test {

    private val quest = Quest02()

    @Test
    fun `part 1 sample`() {
        val sample = """
            A=[25,9]
        """.trimIndent()
        assertEquals(Answer("[357,862]"), quest.part1(sample))
    }

    @Test
    fun `part 2 sample`() {
        val sample = """
        A=[35300,-64910]
    """.trimIndent()
        assertEquals(Answer(4076), quest.part2(sample))
    }

    @Test
    fun `part 3 sample`() {
        val sample = """
        A=[35300,-64910]
    """.trimIndent()
        assertEquals(Answer(406954), quest.part3(sample))
    }
}
