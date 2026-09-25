package ec.cli

import ec.Part

internal const val USAGE = """usage: ec run    <year> <quest> [1|2|3 ...]
       ec fetch  <year> <quest> [1|2|3 ...]
       ec submit <year> <quest> <1|2|3>
       ec check  <year> <quest>
       ec key    <year> <quest> <1|2|3>"""

/**
 * A quest in an Everybody Codes event: a year from 2024 on, and a quest from 1 to 20.
 * Deliberately not a data class: its generated `copy()` would bypass [of]'s checks.
 */
class QuestId private constructor(val year: Int, val number: Int) {

    /** Two digits, as used in file and class names: 3 is "03". */
    val padded: String get() = number.toString().padStart(2, '0')

    override fun equals(other: Any?) = other is QuestId && other.year == year && other.number == number
    override fun hashCode() = 31 * year + number
    override fun toString() = "$year/$padded"

    companion object {
        fun of(year: String?, number: String?): QuestId? {
            val y = year?.toIntOrNull()?.takeIf { it >= 2024 } ?: return null
            val n = number?.toIntOrNull()?.takeIf { it in 1..20 } ?: return null
            return QuestId(y, n)
        }
    }
}

/** Everything the tool can be asked to do. [parse] is the only way in from the command line. */
sealed interface Command {
    val quest: QuestId

    data class Run(override val quest: QuestId, val parts: List<Part>) : Command
    data class Fetch(override val quest: QuestId, val parts: List<Part>) : Command
    data class Submit(override val quest: QuestId, val part: Part) : Command
    data class Check(override val quest: QuestId) : Command
    data class Key(override val quest: QuestId, val part: Part) : Command

    companion object {
        /** Null for anything malformed: an unknown command, a bad quest, a part outside 1–3. */
        fun parse(args: List<String>): Command? {
            val quest = QuestId.of(args.getOrNull(1), args.getOrNull(2)) ?: return null
            val parts = args.drop(3).map { Part.of(it) ?: return null }.distinct().sorted()
            return when (args.first()) {
                "run"    -> Run(quest, parts.ifEmpty { Part.entries.toList() })
                "fetch"  -> Fetch(quest, parts.ifEmpty { Part.entries.toList() })
                "submit" -> parts.singleOrNull()?.let { Submit(quest, it) }
                "key"    -> parts.singleOrNull()?.let { Key(quest, it) }
                "check"  -> if (parts.isEmpty()) Check(quest) else null
                else     -> null
            }
        }
    }
}
