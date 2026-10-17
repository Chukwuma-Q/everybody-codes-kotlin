package ec.y2025

import ec.Answer
import ec.Quest
import ec.trimmedLines

class Quest06 : Quest {

    /** Novice-mentor pairs in sword fighting: each novice pairs with every swordsman listed before them. */
    override fun part1(input: String): Answer = Answer(Notes.parse(input).pairs(Profession.Sword))

    /** Novice-mentor pairs in every profession: part 1's count, once per profession, added up. */
    override fun part2(input: String): Answer {
        val notes = Notes.parse(input)
        return Answer(Profession.entries.sumOf { notes.pairs(it) })
    }

    /** Pairs by tent distance: the notes repeat 1000 times, and a mentor is at most 1000 tents away. */
    override fun part3(input: String): Answer = Answer(pairsNearby(input, distance = 1000, repeats = 1000))

    /** Part 3 with its two numbers as parameters, so the samples with smaller ones can be tested. */
    internal fun pairsNearby(input: String, distance: Int, repeats: Int): Long =
        Notes.parse(input).pairsNearby(distance, repeats)

    /** The three professions, each written as one letter: upper case for a knight, lower for a novice. */
    private enum class Profession(val letter: Char) {
        Sword('A'),
        Archery('B'),
        Magic('C')
    }

    /** Knights mentor and novices are mentored. Nobody is both, and the type says so. */
    private enum class Role { Knight, Novice }

    private class Person(val role: Role, val profession: Profession)

    /** The captain's note: everyone in the order they are listed, built only by [parse]. */
    private class Notes private constructor(val people: List<Person>) {

        /**
         * Pairs within [profession], counted in one pass: walking the list, each novice can be
         * mentored by every knight of the same profession already passed.
         */
        fun pairs(profession: Profession): Long {
            var knightsSoFar = 0L
            var pairs = 0L
            for (person in people) {
                if (person.profession != profession) continue
                when (person.role) {
                    Role.Knight -> knightsSoFar++
                    Role.Novice -> pairs += knightsSoFar
                }
            }
            return pairs
        }

        /**
         * Pairs when the notes repeat [repeats] times in a row of tents, and a knight can mentor any
         * novice of the same profession at most [distance] tents away, on either side.
         *
         * knightsBefore[p][i] is how many knights of profession p stand in the first i tents. The
         * knights between tents lo and hi are then one subtraction, so each novice costs O(1)
         * instead of checking up to 2 × distance neighbours.
         */
        fun pairsNearby(distance: Int, repeats: Int): Long {
            require(distance >= 0 && repeats >= 1) { "distance >= 0 and repeats >= 1, got $distance and $repeats" }
            val tents = people.size * repeats
            fun tent(i: Int): Person = people[i % people.size]   // the repeated row, never copied

            val knightsBefore = Array(Profession.entries.size) { IntArray(tents + 1) }
            for (i in 0 until tents) {
                for (counts in knightsBefore) counts[i + 1] = counts[i]
                val person = tent(i)
                if (person.role == Role.Knight) knightsBefore[person.profession.ordinal][i + 1]++
            }

            var pairs = 0L
            for (i in 0 until tents) {
                val person = tent(i)
                if (person.role != Role.Novice) continue
                val lo = maxOf(0, i - distance)
                val hi = minOf(tents - 1, i + distance)
                val counts = knightsBefore[person.profession.ordinal]
                pairs += counts[hi + 1] - counts[lo]
            }
            return pairs
        }

        companion object {
            fun parse(input: String): Notes {
                val line = input.trimmedLines().single()
                return Notes(line.map { c ->
                    val profession = Profession.entries.firstOrNull { it.letter == c.uppercaseChar() }
                        ?: throw IllegalArgumentException("unknown letter '$c' in the notes")
                    Person(if (c.isUpperCase()) Role.Knight else Role.Novice, profession)
                })
            }
        }
    }
}
