package ec.y2025

import ec.Answer
import ec.Quest

/**
 * Quest 4: Teeth of the Wind.
 *
 * Meshed gears turn in inverse proportion to their teeth, so each mesh multiplies the turns by
 * driver / driven. A single middle gear is a shaft whose two gears are equal, so one [Train]
 * model serves all three parts: in parts 1 and 2, every middle gear simply cancels.
 */
class Quest04 : Quest {
    override fun part1(input: String): Answer = Answer(lastGearTurns(input, firstTurns = 2025))

    /** Inverted: the first gear's turns so the last makes at least [TARGET], rounded up. */
    override fun part2(input: String): Answer {
        val ratio = Train.parse(input).ratio()
        return Answer(Math.ceilDiv(Math.multiplyExact(TARGET, ratio.den), ratio.num))
    }

    override fun part3(input: String): Answer = Answer(lastGearTurns(input, firstTurns = 100))

    /** Full turns of the last gear. Multiplying before dividing keeps it exact until one final rounding down. */
    private fun lastGearTurns(input: String, firstTurns: Long): Long {
        val ratio = Train.parse(input).ratio()
        return Math.multiplyExact(firstTurns, ratio.num) / ratio.den
    }

    /**
     * Turns of one gear per turn of another: a positive fraction, always in lowest terms. The
     * constructor is private and this isn't a data class, so there's no copy() to bypass it:
     * every value was built by [mesh] and is reduced.
     */
    class Ratio private constructor(val num: Long, val den: Long) {
        /**
         * This ratio times driver / driven. Every common factor is cancelled before multiplying, so
         * each product is only as large as the reduced result; [Math.multiplyExact] throws on a
         * result too large for a Long.
         */
        fun mesh(driver: Long, driven: Long): Ratio {
            val g = gcd(driver, driven)
            val dr = driver / g
            val dn = driven / g
            val a = gcd(num, dn)
            val b = gcd(dr, den)
            return Ratio(Math.multiplyExact(num / a, dr / b), Math.multiplyExact(den / b, dn / a))
        }

        companion object {
            val ONE = Ratio(1, 1)
        }
    }

    /**
     * A gear train as the notes describe it. The constructor is private, so every train came through
     * [parse]: a single gear at each end, since [teeth] refuses a shaft there, and at least one
     * tooth on every gear.
     */
    class Train private constructor(val first: Long, val shafts: List<Shaft>, val last: Long) {
        /** Two gears on one axle: [driven] meshes with the gear before it, [driver] with the gear after. */
        data class Shaft(val driven: Long, val driver: Long)

        /** Turns of the last gear per turn of the first. Each shaft's driver carries the turns on. */
        fun ratio(): Ratio {
            val (driver, ratio) = shafts.fold(first to Ratio.ONE) { (driver, ratio), shaft ->
                shaft.driver to ratio.mesh(driver, shaft.driven)
            }
            return ratio.mesh(driver, last)
        }

        companion object {
            fun parse(input: String): Train {
                val lines = input.trim().lines().map(String::trim)
                require(lines.size >= 2) { "a gear train needs at least two gears" }
                return Train(teeth(lines.first()), lines.subList(1, lines.size - 1).map(::shaft), teeth(lines.last()))
            }

            private fun teeth(text: String): Long {
                val digits = text.trim()
                val n = if (digits.isNotEmpty() && digits.all { it in '0'..'9' }) digits.toLongOrNull() else null
                require(n != null && n > 0) { "not a gear: '$digits'" }
                return n
            }

            private fun shaft(line: String): Shaft {
                val pipe = line.indexOf('|')
                if (pipe < 0) return teeth(line).let { Shaft(driven = it, driver = it) }
                return Shaft(driven = teeth(line.substring(0, pipe)), driver = teeth(line.substring(pipe + 1)))
            }
        }
    }

    companion object {
        const val TARGET = 10_000_000_000_000L
    }
}

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
