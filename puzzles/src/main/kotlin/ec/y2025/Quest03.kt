package ec.y2025

import ec.Answer
import ec.Quest
import ec.longs

class Quest03 : Quest {
    /**
     * Crates of different sizes always nest, so the heaviest set uses every distinct size once:
     * largest first, each fits inside the one before it.
     */
    override fun part1(input: String): Answer = Answer(input.longs().toSet().sum())
    override fun part2(input: String): Answer = TODO("quest 03 part 2")
    override fun part3(input: String): Answer = TODO("quest 03 part 3")
}
