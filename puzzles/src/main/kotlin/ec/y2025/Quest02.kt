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

    /** Count the engraved points of a 101 × 101 grid spanning A to A + [1000,1000]. */
    override fun part2(input: String): Answer =
        Answer(engravedPoints(Complex.parse(input), perSide = 101))

    /** The same engraving at ten times the resolution: 1001 × 1001 points. */
    override fun part3(input: String): Answer =
        Answer(engravedPoints(Complex.parse(input), perSide = 1001))

    /** Grid points, [perSide] to a side, from [corner] to [corner] + [1000,1000], that get engraved. */
    private fun engravedPoints(corner: Complex, perSide: Int): Int {
        require(1000 % (perSide - 1) == 0) { "a $perSide-point side can't space evenly across 1000" }
        val step = 1000L / (perSide - 1)
        return (0 until perSide).sumOf { row ->
            (0 until perSide).count { col -> engraved(corner + Complex(col * step, row * step)) }
        }
    }

    /** Engraved if 100 cycles of R = R * R / [DIVISOR] + P never leave [LIMIT]. */
    private fun engraved(point: Complex): Boolean =
        generateSequence(Complex.ZERO) { r -> r * r / DIVISOR + point }
            .drop(1)
            .take(100)
            .all { it.x in LIMIT && it.y in LIMIT }

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

    /** The engraving rule's two constants, kept side by side. */
    private companion object {
        val DIVISOR = Complex(100_000, 100_000)
        val LIMIT = -1_000_000L..1_000_000L
    }
}
