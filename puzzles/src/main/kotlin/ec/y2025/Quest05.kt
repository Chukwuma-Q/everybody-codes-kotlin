package ec.y2025

import ec.Answer
import ec.Quest
import ec.trimmedLines

class Quest05 : Quest {

    override fun part1(input: String): Answer = Answer(
        Sword.parse(
            input.trimmedLines().single()
        ).fishbone.quality
    )

    /** The spread of the squires' swords: best quality minus weakest, one sword per line. */
    override fun part2(input: String): Answer {
        val qualities = input.trimmedLines().map { Sword.parse(it).fishbone.quality }
        return Answer(qualities.max() - qualities.min())
    }

    /** Swords sorted best first, then each identifier weighted by its position, counting from 1. */
    override fun part3(input: String): Answer {
        val sorted = input.trimmedLines()
            .map(Sword::parse)
            .sortedWith(Sword.BEST_FIRST)
        return Answer(sorted.withIndex().sumOf { (i, sword) -> (i + 1) * sword.id })
    }

    /** One line of the armourer's notes: the sword's identifier, then the numbers on its hilt. */
    private class Sword private constructor(val id: Long, numbers: List<Long>) {

        /** Built once: sorting compares each sword many times. */
        val fishbone: Fishbone = Fishbone.build(numbers)

        companion object {
            /**
             * The armourer's ranking: higher quality, then the first level that differs, top down,
             * then the higher identifier.
             */
            private val WORST_FIRST: Comparator<Sword> =
                compareBy<Sword> { it.fishbone.quality }
                    .then { a, b -> Fishbone.compareLevels(a.fishbone, b.fishbone) }
                    .thenBy { it.id }

            val BEST_FIRST: Comparator<Sword> = WORST_FIRST.reversed()

            fun parse(line: String): Sword {
                val id = line.substringBefore(':', missingDelimiterValue = "").trim().toLongOrNull()
                require(id != null) { "expected 'id:numbers', got '$line'" }
                val numbers = line.substringAfter(':').split(',').map { token ->
                    token.trim().toLongOrNull()?.takeIf { it >= 0 }
                        ?: throw IllegalArgumentException("bad number: '$token'")
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

            /** The level's digits read left to right, as one number. Throws if too large for a Long. */
            val level: Long get() = listOfNotNull(left, spine, right).joinToString("").toLong()
        }

        /** The spine's numbers written one after another. Throws if the result is too large for a Long. */
        val quality: Long get() = segments.joinToString("") { "${it.spine}" }.toLong()

        /** Each segment's [Segment.level], top down. */
        val levels: List<Long> = segments.map(Segment::level)

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

            /** The first level that differs decides; if none does, the fishbones tie. */
            fun compareLevels(a: Fishbone, b: Fishbone): Int =
                a.levels.zip(b.levels).firstNotNullOfOrNull { (x, y) -> x.compareTo(y).takeIf { it != 0 } } ?: 0
        }
    }
}