package com.example.engine

/**
 * Conteo de palabras único para toda la app.
 *
 * Antes existían tres implementaciones distintas (repositorio, ViewModel y
 * motor editorial) con criterios ligeramente diferentes, lo que hacía que el
 * contador del capítulo, la meta diaria y la barra de progreso no cuadraran.
 */
object TextMetrics {

    private val WHITESPACE = Regex("\\s+")

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(WHITESPACE).count { it.isNotBlank() }
    }

    fun readingMinutes(wordCount: Int, wordsPerMinute: Int = 200): Int {
        if (wordCount <= 0) return 0
        return ((wordCount.toDouble() / wordsPerMinute).coerceAtLeast(0.1)).toInt().coerceAtLeast(1)
    }
}
