package ec.cli

import ec.Part
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AnswersTest {

    @Test
    fun `recording one part keeps the others, in part order`() {
        val file = createTempDirectory().resolve("answers/2025/quest02.txt").toFile()
        Answers.record(file, mapOf(Part.Three to "52043"))
        Answers.record(file, mapOf(Part.One to "[119588,544027]", Part.Two to "539"))
        assertEquals("Part1: [119588,544027]\nPart2: 539\nPart3: 52043\n", file.readText())
        assertEquals(mapOf(Part.One to "[119588,544027]", Part.Two to "539", Part.Three to "52043"), Answers.read(file))
    }

    @Test
    fun `a missing file reads as no answers`() {
        assertEquals(emptyMap(), Answers.read(createTempDirectory().resolve("nope.txt").toFile()))
    }
}
