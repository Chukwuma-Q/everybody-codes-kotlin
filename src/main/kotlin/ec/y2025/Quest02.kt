package ec.y2025

import ec.Answer
import ec.Quest
import ec.longs

/** Quest 2: From Complex to Clarity. */
class Quest02 : Quest {

    /** Three cycles of R = R * R / [10,10] + A, starting from [0,0]. */
    override fun part1(input: String): Answer {
        val a = Complex.parse(input)
        val result = (1..3).fold(Complex.ZERO) { r, _ -> r * r / Complex(10, 10) + a }
        return Answer(result.toString())
    }

    override fun part2(input: String): Answer = TODO("quest 02 part 2")
    override fun part3(input: String): Answer = TODO("quest 02 part 3")

    /**
     * The puzzle's "complex" number. Division is not true complex division: each
     * component is divided on its own, truncating toward zero, which is exactly
     * what Kotlin's `/` does on integers.
     *
     * Addition and multiplication are overflow-checked, so a value too large for
     * Long fails loudly instead of wrapping into a plausible-looking wrong answer.
     */
    private data class Complex(val x: Long, val y: Long) {

        operator fun plus(o: Complex) = Complex(Math.addExact(x, o.x), Math.addExact(y, o.y))

        operator fun times(o: Complex) = Complex(
            Math.subtractExact(Math.multiplyExact(x, o.x), Math.multiplyExact(y, o.y)),
            Math.addExact(Math.multiplyExact(x, o.y), Math.multiplyExact(y, o.x)),
        )

        operator fun div(o: Complex) = Complex(x / o.x, y / o.y)

        /** The puzzle's own notation, which is also the required answer format. */
        override fun toString() = "[$x,$y]"

        companion object {
            val ZERO = Complex(0, 0)

            fun parse(input: String): Complex {
                val parts = input.longs()
                require(parts.size == 2) { "expected A=[X,Y], got '${input.trim()}'" }
                return Complex(parts[0], parts[1])
            }
        }
    }
}
