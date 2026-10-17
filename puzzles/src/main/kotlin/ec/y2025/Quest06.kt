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

    override fun part3(input: String): Answer = TODO("quest 06 part 3")

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
