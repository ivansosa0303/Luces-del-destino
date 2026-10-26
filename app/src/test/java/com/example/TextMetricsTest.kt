package com.example

import com.example.engine.TextMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class TextMetricsTest {

    @Test
    fun `texto vacio no cuenta palabras`() {
        assertEquals(0, TextMetrics.countWords(""))
        assertEquals(0, TextMetrics.countWords("   \n\t "))
    }

    @Test
    fun `cuenta palabras separadas por cualquier espacio`() {
        assertEquals(4, TextMetrics.countWords("Luces del destino brillan"))
        assertEquals(4, TextMetrics.countWords("Luces\ndel\tdestino   brillan"))
    }

    @Test
    fun `no cuenta espacios sobrantes`() {
        assertEquals(3, TextMetrics.countWords("  uno   dos   tres  "))
    }

    @Test
    fun `mantiene signos pegados a la palabra`() {
        assertEquals(3, TextMetrics.countWords("—Vamos —dijo ella."))
    }

    @Test
    fun `minutos de lectura nunca son cero si hay texto`() {
        assertEquals(1, TextMetrics.readingMinutes(10))
        assertEquals(0, TextMetrics.readingMinutes(0))
        assertEquals(5, TextMetrics.readingMinutes(1000))
    }
}
