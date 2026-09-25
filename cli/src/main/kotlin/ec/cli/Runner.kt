package ec.cli

import ec.Answer
import ec.Part
import ec.Quest
import ec.registry
import java.io.File
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/** Runs each requested part on its input, prints answers with timings, and records them. */
internal fun runQuest(command: Command.Run, workspace: Workspace): Boolean {
    val id = command.quest
    val quest = registry[id.year]?.get(id.number)?.invoke()
        ?: fail("No quest $id is registered. Create it with: make new Q=${id.padded}")
    val solved = command.parts.mapNotNull { part ->
        solve(quest, part, workspace.input(id, part))?.let { part to it.toString() }
    }.toMap()
    if (solved.isNotEmpty()) {
        val file = workspace.answers(id)
        Answers.record(file, solved)
        println("saved -> ${file.path}")
    }
    return solved.size == command.parts.size
}

private fun solve(quest: Quest, part: Part, input: File): Answer? {
    if (!input.hasContent()) {
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
        },
    )
}
