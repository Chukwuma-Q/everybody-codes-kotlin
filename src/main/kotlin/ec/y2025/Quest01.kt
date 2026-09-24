package ec.y2025

import ec.Answer
import ec.Quest
import ec.blocks

/** Quest 1: Whispers in the Shell. */
class Quest01 : Quest {

    /** Walk the list, clamping at both ends. The name you land on is the answer. */
    override fun part1(input: String): Answer {
        val notes = Notes.parse(input)
        val landing = notes.moves.fold(0) { at, move ->
            (at + move.delta).coerceIn(notes.names.indices)
        }
        return Answer(notes.names[landing])
    }

    /** On a ring, moves compose by addition modulo its size, so the walk is one sum. */
    override fun part2(input: String): Answer {
        val notes = Notes.parse(input)
        val landing = notes.moves.sumOf { it.delta }.mod(notes.names.size)
        return Answer(notes.names[landing])
    }

    /** Each instruction swaps the top of the ring with the name it points at. */
    override fun part3(input: String): Answer {
        val notes = Notes.parse(input)
        val ring = notes.moves.fold(notes.names) { names, move ->
            names.swap(0, move.delta.mod(names.size))
        }
        return Answer(ring.first())
    }

    private fun <T> List<T>.swap(i: Int, j: Int): List<T> =
        toMutableList().also { it[i] = this[j]; it[j] = this[i] }
    
    /** A signed step along the list: L3 is -3, R3 is +3. */
    @JvmInline
    private value class Move(val delta: Int) {
        companion object {
            fun parse(token: String): Move {
                val sign = when (token.firstOrNull()) {
                    'L' -> -1; 'R' -> 1; else -> null
                }
                val steps = token.drop(1).toIntOrNull()
                require(sign != null && steps != null && steps >= 0) { "bad instruction: '$token'" }
                return Move(sign * steps)
            }
        }
    }

    /**
     * One eggshell section: names, a blank line, then instructions.
     * Only [parse] can build one, and it rejects blank names, so [names] is never empty.
     */
    private class Notes private constructor(val names: List<String>, val moves: List<Move>) {
        companion object {
            fun parse(input: String): Notes {
                val sections = input.blocks()
                require(sections.size == 2) { "expected names and instructions separated by a blank line" }
                val names = sections[0].split(',').map(String::trim)
                require(names.none(String::isEmpty)) { "blank name in the list" }
                val moves = sections[1].split(',').map { Move.parse(it.trim()) }
                return Notes(names, moves)
            }
        }
    }
}
