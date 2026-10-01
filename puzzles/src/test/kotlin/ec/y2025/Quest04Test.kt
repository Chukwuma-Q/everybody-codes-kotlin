package ec.y2025

import ec.Answer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Quest04Test {
    private val quest = Quest04()

    @Test fun `part 1 samples`() {
        assertEquals(Answer(32400), quest.part1(DOUBLING))
        assertEquals(Answer(15888), quest.part1(UNEVEN))
    }

    @Test fun `part 2 samples`() {
        assertEquals(Answer(625000000000L), quest.part2(DOUBLING))
        assertEquals(Answer(1274509803922L), quest.part2(UNEVEN))
    }

    @Test fun `part 3 samples`() {
        assertEquals(Answer(400), quest.part3(SAME_SPEED))
        assertEquals(Answer(6818), quest.part3(SHAFTS))
    }

    /** The reasoning, tested directly: changing or reordering the middle gears never changes an answer. */
    @Test fun `the middle gears cancel`() {
        for (middle in listOf(emptyList(), listOf("35", "50", "75"), listOf("7"), listOf("999", "1", "64"))) {
            val train = (listOf("102") + middle + listOf("13")).joinToString("\n")
            assertEquals(quest.part1(UNEVEN), quest.part1(train), "$middle")
            assertEquals(quest.part2(UNEVEN), quest.part2(train), "$middle")
        }
    }

    @Test fun `equal shafts change nothing`() {
        assertEquals(quest.part3(SHAFTS), quest.part3("5\n7|21\n18|36\n9|9\n10|50\n10|50\n11"))
    }

    @Test fun `shaft order changes nothing`() {
        assertEquals(quest.part3(SHAFTS), quest.part3("5\n10|50\n10|50\n27|27\n18|36\n7|21\n11"))
    }

    /** One model for all three parts: a single middle gear is exactly a shaft whose two gears are equal. */
    @Test fun `a single gear is an equal shaft`() {
        val asShafts = "102\n75|75\n50|50\n35|35\n13"
        assertEquals(quest.part1(UNEVEN), quest.part1(asShafts))
        assertEquals(quest.part2(UNEVEN), quest.part2(asShafts))
        assertEquals(quest.part3(UNEVEN), quest.part3(asShafts))
    }

    @Test fun `malformed trains are refused`() {
        for (input in listOf("7|21\n5\n11", "5\n7|21\n10|11", "5\n0|3\n11", "5\n7|\n11", "5\n7|x\n11", "5\n-3\n11", "5")) {
            assertFailsWith<IllegalArgumentException>(input) { quest.part3(input) }
        }
    }

    /** Seventy shafts that each double the speed: 2^70 turns, too large for a Long, must throw. */
    @Test fun `overflow is loud`() {
        val doubling = List(70) { "1|2" }.joinToString("\n")
        assertFailsWith<ArithmeticException> { quest.part3("2\n$doubling\n1") }
    }

    private companion object {
        const val DOUBLING = "128\n64\n32\n16\n8"
        const val UNEVEN = "102\n75\n50\n35\n13"
        const val SAME_SPEED = "5\n5|10\n10|20\n5"
        const val SHAFTS = "5\n7|21\n18|36\n27|27\n10|50\n10|50\n11"
    }
}
