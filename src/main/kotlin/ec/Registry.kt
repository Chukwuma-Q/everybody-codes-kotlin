package ec

/** Every year with quests. `make new` adds a year the first time it sees one. */
val registry: Map<Int, Map<Int, () -> Quest>> = mapOf(
    2025 to ec.y2025.quests,
    // make new: years
)
