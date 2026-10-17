package ec.cli

import ec.Part
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Posts the saved answer for one part, after checking it isn't already solved, still locked,
 * or in a wrong-answer lockout, and after you confirm. On success, fetches the next part's input.
 *
 * Most of this is a chain where each step needs the one before: the checks need the site's
 * state, the post needs your yes, and the next key exists only once the answer is accepted.
 * The next part's encrypted input is the exception. It depends on none of them, so it starts
 * downloading first and is usually finished by the time you've answered the prompt.
 */
internal suspend fun submitAnswer(
    command: Command.Submit,
    workspace: Workspace,
    api: Api,
    confirm: (String) -> Boolean = ::askYesNo,
): Boolean {
    val quest = command.quest
    val part = command.part
    val answer = Answers.read(workspace.answers(quest))[part]
        ?: fail("No saved answer for quest ${quest.padded} part ${part.number}. Run: make run Q=${quest.padded} P=${part.number}")
    val next = Part.entries.getOrNull(part.ordinal + 1)

    // supervisorScope, not coroutineScope: the download is a guess that we'll need it. In a
    // coroutineScope its failure would fail the whole command, even when the part turns out to be
    // solved already and the input is never used. Here a failure stays inside the Deferred and
    // surfaces only if something awaits it.
    return supervisorScope {
        val nextInput = next?.let { async { api.encryptedInputs(quest) } }
        try {
            checkAndPost(quest, part, answer, workspace, api, confirm, next, nextInput)
        } finally {
            // Stops the download on every path that didn't use it: already solved, a lockout,
            // "N" at the prompt, or a wrong answer. Cancelling a finished download does nothing.
            nextInput?.cancel()
        }
    }
}

private suspend fun checkAndPost(
    quest: QuestId,
    part: Part,
    answer: String,
    workspace: Workspace,
    api: Api,
    confirm: (String) -> Boolean,
    next: Part?,
    nextInput: Deferred<Map<Part, String>>?,
): Boolean {
    val state = api.state(quest)
    state.answers[part]?.let { accepted ->
        val verdict = if (accepted == answer) "matches your saved answer" else "your saved answer is '$answer'"
        println("Part ${part.number} is already solved with '$accepted' ($verdict). Nothing sent.")
        return true
    }
    if (part !in state.keys) fail("Part ${part.number} is still locked. Solve part ${part.number - 1} first.")
    if (state.lockout.isPositive()) fail("Wrong-answer lockout: wait ${clock(state.lockout)} before submitting again.")

    // confirm blocks this thread while you type. The download keeps going regardless: the bytes
    // arrive on HttpClient's threads, and the rest of it runs as soon as this thread is free again.
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
    // fetchInputs asks the site for the state again: the key for the next part didn't exist
    // until this answer was accepted, so the state from the start of the command is out of date.
    next?.let { fetchInputs(Command.Fetch(quest, listOf(it)), workspace, api, prefetched = nextInput) }
    return true
}

/** Compares your saved answers with the ones the site accepted. False on any mismatch. */
internal suspend fun checkAnswers(command: Command.Check, workspace: Workspace, api: Api): Boolean {
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
) {
    val key = readSecret("paste key${command.part.number} for quest ${command.quest.padded} (hidden): ")
        ?: fail("No key entered.")
    if (key.length != 32) fail("expected 32 characters, got ${key.length}")
    val path = workspace.key(command.quest, command.part).toPath()
    Files.createDirectories(path.parent)
    Files.deleteIfExists(path)
    Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(path, key)
    println("saved - now run: make fetch Q=${command.quest.padded} P=${command.part.number}")
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
