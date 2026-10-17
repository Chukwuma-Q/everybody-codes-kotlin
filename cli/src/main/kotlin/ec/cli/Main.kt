package ec.cli

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

/** A problem the user can fix, reported as one sentence instead of a stack trace. */
internal class Failure(message: String, val exitCode: Int = 1) : RuntimeException(message)

/** Stops the command with [message]. Returns [Nothing], so the compiler knows control ends here. */
internal fun fail(message: String, exitCode: Int = 1): Nothing = throw Failure(message, exitCode)

fun main(args: Array<String>) {
    try {
        val command = Command.parse(args.asList()) ?: fail(USAGE, exitCode = 2)
        val workspace = Workspace(File(""))
        val api by lazy { Api(Config.load(workspace.env)) }
        // runBlocking is the bridge from ordinary code into coroutines: it runs the suspending
        // commands on this thread and returns when they, and everything they started, are done.
        val succeeded = runBlocking {
            when (command) {
                is Command.Run    -> runQuest(command, workspace)
                is Command.Fetch  -> { fetchInputs(command, workspace, api); true }
                is Command.Submit -> submitAnswer(command, workspace, api)
                is Command.Check  -> checkAnswers(command, workspace, api)
                is Command.Key    -> { saveKey(command, workspace); true }
            }
        }
        if (!succeeded) exitProcess(1)
    } catch (failure: Failure) {
        System.err.println(failure.message)
        exitProcess(failure.exitCode)
    }
}
