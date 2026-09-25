package ec.y2025

import ec.Answer
import kotlin.test.Test
import kotlin.test.assertEquals

class Quest01Test {

    private val quest = Quest01()

    @Test
    fun `part 1 sample`() {
        val sample = """
            Vyrdax,Drakzyph,Fyrryn,Elarzris

            R3,L2,R3,L1
        """.trimIndent()
        assertEquals(Answer("Fyrryn"), quest.part1(sample))
    }

    @Test
    fun `part 2 sample`() {
        val sample = """
        Vyrdax,Drakzyph,Fyrryn,Elarzris

        R3,L2,R3,L1
    """.trimIndent()
        assertEquals(Answer("Elarzris"), quest.part2(sample))
    }

    @Test
    fun `part 3 sample`() {
        val sample = """
        Vyrdax,Drakzyph,Fyrryn,Elarzris

        R3,L2,R3,L3
    """.trimIndent()
        assertEquals(Answer("Drakzyph"), quest.part3(sample))
    }

}
