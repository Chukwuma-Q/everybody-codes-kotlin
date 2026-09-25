package ec.cli

import ec.Part
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Posts the saved answer for one part, after checking it isn't already solved, still locked,
 * or in a wrong-answer lockout, and after you confirm. On success, fetches the next part's input.
 */
internal fun submitAnswer(
    command: Command.Submit,
    workspace: Workspace,
    api: Api,
    confirm: (String) -> Boolean = ::askYesNo,
): Boolean {
    val quest = command.quest
    val part = command.part
    val answer = Answers.read(workspace.answers(quest))[part]
        ?: fail("No saved answer for quest ${quest.padded} part ${part.number}. Run: make run Q=${quest.padded} P=${part.number}")

    val state = api.state(quest)
    state.answers[part]?.let { accepted ->
        val verdict = if (accepted == answer) "matches your saved answer" else "your saved answer is '$answer'"
        println("Part ${part.number} is already solved with '$accepted' ($verdict). Nothing sent.")
        return true
    }
    if (part !in state.keys) fail("Part ${part.number} is still locked. Solve part ${part.number - 1} first.")
    if (state.lockout.isPositive()) fail("Wrong-answer lockout: wait ${clock(state.lockout)} before submitting again.")

    if (System.getenv("YES") != "1" && !confirm("Submit '$answer' for quest ${quest.padded} part ${part.number}? [y/N] ")) {
        println("Not submitted.")
        return false
    }
    val verdict = api.submit(quest, part, answer)
    if (!verdict.correct) {
        println("Incorrect. The site replied: ${verdict.raw}")
        return false
    }
    println("Correct! Global place ${verdict.globalPlace ?: "?"}, local time ${verdict.localTime?.let(::clock) ?: "?"}.")
    Part.entries.getOrNull(part.ordinal + 1)?.let { next ->
        fetchInputs(Command.Fetch(quest, listOf(next)), workspace, api)
    }
    return true
}

/** Compares your saved answers with the ones the site accepted. False on any mismatch. */
internal fun checkAnswers(command: Command.Check, workspace: Workspace, api: Api): Boolean {
    val accepted = api.state(command.quest).answers
    val saved = Answers.read(workspace.answers(command.quest))
    var consistent = true
    for (part in Part.entries) {
        val site = accepted[part]
        val mine = saved[part]
        val status = when {
            site == null -> "not solved on the site yet"
            mine == null -> "accepted '$site', nothing saved locally"
            mine == site -> "ok  '$mine'"
            else         -> {
                consistent = false
                "MISMATCH  saved '$mine', accepted '$site'"
            }
        }
        println("Part ${part.number}: $status")
    }
    return consistent
}

/** Saves a key copied from the browser, readable by you alone. The fallback if the API fails. */
internal fun saveKey(
    command: Command.Key,
    workspace: Workspace,
    readSecret: (String) -> String? = ::readHidden
): Boolean {
    val key = readSecret("paste key${command.part.number} for quest ${command.quest.padded} (hidden): ")
        ?: fail("No key entered.")
    if (key.length != 32) fail("expected 32 characters, got ${key.length}")
    val path = workspace.key(command.quest, command.part).toPath()
    Files.createDirectories(path.parent)
    Files.deleteIfExists(path)
    Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(path, key)
    println("saved - now run: make fetch Q=${command.quest.padded} P=${command.part.number}")
    return true
}

private fun askYesNo(prompt: String): Boolean {
    print(prompt)
    System.out.flush()
    return readlnOrNull()?.trim()?.lowercase() == "y"
}

private fun readHidden(prompt: String): String? {
    val console = System.console()
    if (console != null) return console.readPassword(prompt)?.let { String(it) }
    System.err.println("warning: no terminal, so the key will be visible as you type")
    print(prompt)
    System.out.flush()
    return readlnOrNull()
}

private fun clock(duration: Duration): String = duration.inWholeSeconds.seconds.toString()
