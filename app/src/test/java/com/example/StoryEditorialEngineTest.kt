package com.example

import com.example.engine.StoryEditorialEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryEditorialEngineTest {

    @Test
    fun testDialogueDashFix() {
        val input = """
            - Hola, ¿cómo estás? -preguntó Elena.
            - Muy bien, gracias -respondió Marcus.
        """.trimIndent()

        val fixed = StoryEditorialEngine.fixDialogueDashes(input)
        assertTrue("Lines should start with em-dash —", fixed.startsWith("—"))
        assertTrue("Dialogue incisos should use em-dash —", fixed.contains("—respondió Marcus"))
    }

    @Test
    fun testCleanFormattingWhitespace() {
        val input = "Había    una vez   en un reino lejano.\n\n\n\nVivía un dragón."
        val cleaned = StoryEditorialEngine.cleanFormattingWhitespace(input)
        assertFalse("Should not contain multiple spaces", cleaned.contains("    "))
        assertFalse("Should not contain four newlines", cleaned.contains("\n\n\n\n"))
        assertTrue("Should preserve double newline for paragraphs", cleaned.contains("\n\n"))
    }

    @Test
    fun testOpeningPunctuationFix() {
        val input = "— Que hora es?"
        val fixed = StoryEditorialEngine.fixOpeningPunctuation(input)
        assertTrue("Should add inverted question mark", fixed.contains("¿Que hora es?"))
    }

    @Test
    fun testEditorialAnalysisMetrics() {
        val text = """
            — Hola, bienvenido al castillo —dijo el caballero con cortesía.
            — Gracias por recibirme —respondió el viajero mirando a su alrededor.
            El viento soplaba fuerte en las almenas de la fortaleza mientras la noche caía lentamente.
        """.trimIndent()

        val result = StoryEditorialEngine.analyze(text)
        assertTrue("Word count should be positive", result.wordCount > 15)
        assertTrue("Dialogue percentage should be calculated (> 0)", result.dialoguePercentage > 0)
        assertTrue("Readability score should be within 10..100", result.readabilityScore in 10..100)
    }

    @Test
    fun testWordEchoDetection() {
        val textWithEchoes = """
            El dragón volaba sobre las colinas. El dragón buscaba a su presa.
            Nadie podía detener a aquel dragón furioso que rugía con furia.
        """.trimIndent()

        val result = StoryEditorialEngine.analyze(textWithEchoes)
        val dragonEcho = result.echoes.find { it.word == "dragón" }
        assertTrue("Should detect repeated word 'dragón'", dragonEcho != null)
        assertTrue("Echo count should be at least 3", (dragonEcho?.countInWindow ?: 0) >= 3)
    }
}
