package ec.cli

import ec.Part
import java.io.File

/**
 * Where the tool's files live, relative to [root]. Every path is defined here, once.
 * An empty root means the current directory and keeps printed paths short.
 */
internal class Workspace(private val root: File) {
    val env: File get() = root.resolve(".env")

    fun input(quest: QuestId, part: Part): File =
        root.resolve("inputs/${quest.year}/quest${quest.padded}_part${part.number}.txt")

    fun answers(quest: QuestId): File =
        root.resolve("answers/${quest.year}/quest${quest.padded}.txt")

    fun key(quest: QuestId, part: Part): File =
        root.resolve("keys/${quest.year}/quest${quest.padded}_part${part.number}.key")
}

internal fun File.hasContent(): Boolean = isFile && length() > 0
