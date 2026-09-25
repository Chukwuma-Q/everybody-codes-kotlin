package ec

import java.io.File
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/**
 * ec <year> <quest> [1|2|3 ...]
 *
 * Reads inputs/<year>/quest<NN>_part<P>.txt, prints each answer with its time,
 * and merges the results into answers/<year>/quest<NN>.txt.
 */
fun main(args: Array<String>) {
    val call = Invocation.parse(args) ?: fail("usage: ec <year> <quest 1-20> [1|2|3 ...]", code = 2)
    val quest = registry[call.year]?.get(call.quest)?.invoke()
        ?: fail("No quest ${call.year}/${call.quest} is registered. Create it with: make new Q=${call.quest}")

    val name = "quest%02d".format(call.quest)
    val solved = call.parts.mapNotNull { part ->
        solve(quest, part, File("inputs/${call.year}/${name}_part${part.number}.txt"))?.let { part to it }
    }.toMap()

    if (solved.isNotEmpty()) record(File("answers/${call.year}/$name.txt"), solved)
    if (solved.size < call.parts.size) exitProcess(1)
}

/** A parsed command line. Only [parse] can build one, so every instance is valid. */
private class Invocation private constructor(val year: Int, val quest: Int, val parts: List<Part>) {
    companion object {
        fun parse(args: Array<String>): Invocation? {
            val year = args.getOrNull(0)?.toIntOrNull() ?: return null
            val quest = args.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..20 } ?: return null
            val parts = args.drop(2).map { Part.of(it) ?: return null }
            return Invocation(year, quest, parts.ifEmpty { Part.entries })
        }
    }
}

private fun solve(quest: Quest, part: Part, input: File): Answer? {
    if (!input.isFile || input.length() == 0L) {
        println("Part ${part.number}: has no input — paste it into ${input.path}")
        return null
    }
    val text = input.readText()
    val (outcome, took) = measureTimedValue {
        runCatching {
            when (part) {
                Part.One   -> quest.part1(text)
                Part.Two   -> quest.part2(text)
                Part.Three -> quest.part3(text)
            }
        }
    }
    val time = took.toString(DurationUnit.MILLISECONDS, 3)
    return outcome.fold(
        onSuccess = { println("Part ${part.number}: $it  ($time)"); it },
        onFailure = { e ->
            val at = e.stackTrace.firstOrNull { it.className.startsWith("ec.y") }
                ?.let { " (${it.fileName}:${it.lineNumber})" } ?: ""
            println("Part ${part.number}: FAILED — ${e.message ?: e::class.simpleName}$at  ($time)")
            null
        }
    )
}

/** Merges into the existing file, so running one part never erases another's answer. */
private fun record(file: File, solved: Map<Part, Answer>) {
    val previous: Map<Part, String> =
        if (!file.isFile) emptyMap()
        else file.readLines().mapNotNull { line ->
            val label = line.substringBefore(": ", missingDelimiterValue = "")
            val part = Part.entries.firstOrNull { "Part${it.number}" == label } ?: return@mapNotNull null
            part to line.substringAfter(": ")
        }.toMap()

    val merged = (previous + solved.mapValues { it.value.toString() }).toSortedMap()
    file.parentFile.mkdirs()
    file.writeText(merged.entries.joinToString("\n", postfix = "\n") { "Part${it.key.number}: ${it.value}" })
    println("saved -> ${file.path}")
}

private fun fail(message: String, code: Int = 1): Nothing {
    System.err.println(message)
    exitProcess(code)
}
