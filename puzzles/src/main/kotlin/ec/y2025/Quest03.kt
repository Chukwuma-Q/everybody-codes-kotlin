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
    /** Any 20 distinct sizes nest, so the smallest such set is the 20 cheapest sizes. */
    override fun part2(input: String): Answer {
        val sizes = input.longs().distinct().sorted()
        require(sizes.size >= SET_SIZE) { "only ${sizes.size} distinct sizes, need $SET_SIZE" }
        return Answer(sizes.take(SET_SIZE).sum())
    }    override fun part3(input: String): Answer = TODO("quest 03 part 3")

    private companion object {
        /** The mushroom needs exactly this many crates around it. */
        const val SET_SIZE = 20
    }
}
