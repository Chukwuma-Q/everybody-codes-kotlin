package ec.y2025

import ec.Answer
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

    @Test
    fun `part 3 sample`() {
        val sample = """
            AABCBABCABCabcabcABCCBAACBCa
        """.trimIndent()
        assertEquals(34L, quest.pairsNearby(sample, distance = 10, repeats = 1))
        // Not 2 × 34: novices near the join reach knights in the other copy.
        assertEquals(72L, quest.pairsNearby(sample, distance = 10, repeats = 2))
        assertEquals(Answer(3442321), quest.part3(sample))
    }
}
