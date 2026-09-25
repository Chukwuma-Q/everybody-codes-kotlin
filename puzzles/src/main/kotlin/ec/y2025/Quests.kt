package ec.y2025

import ec.Quest

/** Quests for 2025. `make new` appends here; the compiler checks every entry exists. */
val quests: Map<Int, () -> Quest> = mapOf(
    1 to ::Quest01,
    2 to ::Quest02,
    // make new: quests
)
