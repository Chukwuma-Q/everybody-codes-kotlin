package ec.y2025

import ec.Answer
import ec.Quest

class Quest05 : Quest {

    override fun part1(input: String): Answer = Answer(Sword.parse(input.trim()).fishbone().quality)

    override fun part2(input: String): Answer = TODO("quest 05 part 2")
    override fun part3(input: String): Answer = TODO("quest 05 part 3")

    /** One line of the armourer's notes: the sword's identifier, then the numbers on its hilt. */
    private class Sword private constructor(val id: Long, val numbers: List<Long>) {

        fun fishbone(): Fishbone = Fishbone.build(numbers)

        companion object {
            fun parse(line: String): Sword {
                val id = line.substringBefore(':', missingDelimiterValue = "").trim().toLongOrNull()
                require(id != null) { "expected 'id:numbers', got '$line'" }
                val numbers = line.substringAfter(':').split(',').map { token ->
                    token.trim().toLongOrNull()?.takeIf { it >= 0 } ?: throw IllegalArgumentException("bad number: '$token'")
                }
                return Sword(id, numbers)
            }
        }
    }

    /**
     * A fishbone, built only by [build]: each number goes to the first segment, top down, with a
     * free side it belongs on (smaller to the left, larger to the right). Failing that, it starts
     * a new segment at the bottom. An equal number never fits a side, so it always moves down.
     */
    private class Fishbone private constructor(val segments: List<Segment>) {

        class Segment(val left: Long?, val spine: Long, val right: Long?) {
            fun accepts(n: Long): Boolean = (n < spine && left == null) || (n > spine && right == null)
            fun with(n: Long): Segment = if (n < spine) Segment(n, spine, right) else Segment(left, spine, n)
        }

        /** The spine's numbers written one after another. Throws if the result is too large for a Long. */
        val quality: Long get() = segments.joinToString("") { "${it.spine}" }.toLong()

        companion object {
            fun build(numbers: List<Long>): Fishbone {
                require(numbers.isNotEmpty()) { "a sword needs at least one number" }
                val segments = mutableListOf<Segment>()
                for (n in numbers) {
                    val at = segments.indexOfFirst { it.accepts(n) }
                    if (at < 0) segments += Segment(null, n, null) else segments[at] = segments[at].with(n)
                }
                return Fishbone(segments)
            }
        }
    }
}