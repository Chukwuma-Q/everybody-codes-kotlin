package ec.cli

import ec.Part
import java.io.File

/**
 * The answers file: one `PartN: answer` line per solved part.
 * `run` writes it and `submit` and `check` read it, so the format is defined only here.
 */
internal object Answers {

    fun read(file: File): Map<Part, String> =
        if (!file.isFile) emptyMap() else file.readLines().mapNotNull(::entry).toMap()

    /** Merges [fresh] into [file], so running one part never erases another part's answer. */
    fun record(file: File, fresh: Map<Part, String>) {
        val merged = (read(file) + fresh).toSortedMap()
        file.parentFile?.mkdirs()
        file.writeText(
            merged.entries.joinToString(
                "\n",
                postfix = "\n"
            ) { (part, answer) -> "Part${part.number}: $answer" })
    }

    private fun entry(line: String): Pair<Part, String>? {
        val label = line.substringBefore(": ", missingDelimiterValue = "")
        val part = Part.entries.firstOrNull { "Part${it.number}" == label } ?: return null
        return part to line.substringAfter(": ")
    }
}
