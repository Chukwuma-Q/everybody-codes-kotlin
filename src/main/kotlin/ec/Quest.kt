package ec


interface Quest {
    fun part1(input: String): Answer
    fun part2(input: String): Answer
    fun part3(input: String): Answer
}

/** The three parts of a quest. There is no fourth, and the type says so. */
enum class Part(val number: Int) {
    One(1),
    Two(2),
    Three(3);

    companion object {
        fun of(text: String): Part? = entries.firstOrNull { "${it.number}" == text }
    }
}

sealed interface Answer {
    data class Num(val value: Long) : Answer {
        override fun toString() = "$value"
    }

    data class Text(val value: String) : Answer {
        override fun toString() = value
    }
}

fun Answer(value: Int): Answer = Answer.Num(value.toLong())
fun Answer(value: Long): Answer = Answer.Num(value)
fun Answer(value: String): Answer = Answer.Text(value)


/**
 * Trimmed, then split into lines. Deliberately not named `lines()`: shadowing the
 * stdlib function with different behaviour hides the trim from every reader.
 */
fun String.trimmedLines(): List<String> = trim().lineSequence().toList()

/** Paragraphs separated by one or more blank lines. */
fun String.blocks(): List<String> = trim().split(Regex("""\R\s*\R"""))

/** Every integer in the text, sign-aware. */
fun String.ints(): List<Int> = NUMBER.findAll(this).map { it.value.toInt() }.toList()

/** Every sign-aware integer in the text as Long, for values that overflow Int. */
fun String.longs(): List<Long> = NUMBER.findAll(this).map { it.value.toLong() }.toList()

private val NUMBER = Regex("""-?\d+""")
