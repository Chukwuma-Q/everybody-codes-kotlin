package ec.y2025

import ec.Answer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Quest06Test {

    private val quest = Quest06()

    @Test
    fun `part 1 sample`() {
        val sample = """
            ABabACacBCbca
        """.trimIndent()
        assertEquals(Answer(5), quest.part1(sample))

        // Only knights listed *earlier* count: a novice before every knight has no mentor.
        assertEquals(Answer(0), quest.part1("aA"))
    }

    @Test
    fun `part 2 sample`() {
        val sample = """
            ABabACacBCbca
        """.trimIndent()
        assertEquals(Answer(11), quest.part2(sample))
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
