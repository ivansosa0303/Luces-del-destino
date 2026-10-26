package com.example.engine

import java.util.Locale
import kotlin.math.roundToInt

data class WordEcho(
    val word: String,
    val countInWindow: Int,
    val sampleSnippet: String
)

data class StyleAlert(
    val type: StyleAlertType,
    val title: String,
    val message: String,
    val snippet: String = ""
)

enum class StyleAlertType {
    MULETILLA,
    ADVERBIO_MENTE,
    ORACION_LARGA,
    RITMO
}

data class FormattingIssue(
    val title: String,
    val description: String,
    val fixable: Boolean = true
)

data class EditorialAnalysisResult(
    val wordCount: Int,
    val characterCount: Int,
    val paragraphCount: Int,
    val readingTimeMinutes: Double,
    val dialoguePercentage: Int,
    val readabilityScore: Int, // 0 - 100 Fernández-Huerta
    val readabilityLabel: String,
    val echoes: List<WordEcho>,
    val styleAlerts: List<StyleAlert>,
    val formattingIssues: List<FormattingIssue>
)

object StoryEditorialEngine {

    private val SPANISH_STOPWORDS = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "a", "al", "en",
        "con", "por", "para", "y", "e", "o", "u", "que", "se", "su", "sus", "le", "les",
        "me", "te", "nos", "os", "mi", "mis", "tu", "tus", "lo", "como", "pero", "si", "no",
        "es", "son", "fue", "era", "eran", "ha", "han", "habia", "habían", "muy", "tan", "este",
        "esta", "estos", "estas", "ese", "esa", "esos", "esas", "aquel", "aquella", "ya",
        "cuando", "donde", "quien", "quienes", "cual", "cuales", "mas", "más", "bien", "sin",
        "sobre", "tras", "entre", "hasta", "hacia", "desde", "durante", "mediante", "yo", "tu",
        "tú", "el", "él", "ella", "ellos", "ellas", "nosotros", "vosotros"
    )

    private val COMMON_FILLER_PHRASES = listOf(
        "de repente",
        "de pronto",
        "por supuesto",
        "sin duda alguna",
        "sin duda",
        "la verdad es que",
        "en realidad",
        "básicamente",
        "obviamente",
        "literalmente",
        "un poco",
        "por así decirlo",
        "en cierto modo",
        "claramente",
        "a decir verdad",
        "dar cuenta",
        "se dio cuenta"
    )

    fun analyze(text: String): EditorialAnalysisResult {
        if (text.isBlank()) {
            return EditorialAnalysisResult(
                wordCount = 0,
                characterCount = 0,
                paragraphCount = 0,
                readingTimeMinutes = 0.0,
                dialoguePercentage = 0,
                readabilityScore = 100,
                readabilityLabel = "Vacío",
                echoes = emptyList(),
                styleAlerts = emptyList(),
                formattingIssues = emptyList()
            )
        }

        val rawWords = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = TextMetrics.countWords(text)
        val characterCount = text.length
        val paragraphs = text.split(Regex("\n+")).filter { it.trim().isNotEmpty() }
        val paragraphCount = paragraphs.size
        val readingTime = (wordCount / 200.0)

        // 1. Dialogue vs Narrative percentage
        var dialogueWordCount = 0
        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            if (trimmed.startsWith("—") || trimmed.startsWith("-") || trimmed.startsWith("«") || trimmed.startsWith("\"")) {
                dialogueWordCount += trimmed.split(Regex("\\s+")).size
            }
        }
        val dialoguePercentage = if (wordCount > 0) ((dialogueWordCount.toDouble() / wordCount) * 100).roundToInt().coerceIn(0, 100) else 0

        // 2. Readability Score (Fernández-Huerta formula adapted for Spanish)
        // R = 206.84 - (0.60 * P) - (1.02 * F) where P = syllables per 100 words, F = words per sentence
        val sentences = text.split(Regex("[.!?¡¿]+\\s*")).filter { it.trim().isNotEmpty() }
        val avgWordsPerSentence = if (sentences.isNotEmpty()) wordCount.toDouble() / sentences.size else 15.0
        val estimatedSyllables = countEstimatedSpanishSyllables(text)
        val syllablesPer100Words = if (wordCount > 0) (estimatedSyllables * 100.0) / wordCount else 180.0
        val rawReadability = 206.84 - (0.60 * syllablesPer100Words) - (1.02 * avgWordsPerSentence)
        val readabilityScore = rawReadability.roundToInt().coerceIn(10, 100)
        val readabilityLabel = when {
            readabilityScore >= 80 -> "Muy ágil y fluido"
            readabilityScore >= 65 -> "Fluido (Novela estándar)"
            readabilityScore >= 50 -> "Estilo denso / pausado"
            else -> "Complejo / Frases muy largas"
        }

        // 3. Detect echoes / close repetition
        val echoes = detectEchoes(rawWords, windowSize = 45)

        // 4. Style alerts (muletillas, -mente, overlong sentences)
        val styleAlerts = mutableListOf<StyleAlert>()

        // Check filler phrases
        val lowerText = text.lowercase(Locale.ROOT)
        for (phrase in COMMON_FILLER_PHRASES) {
            val count = Regex(Regex.escape(phrase)).findAll(lowerText).count()
            if (count > 0) {
                styleAlerts.add(
                    StyleAlert(
                        type = StyleAlertType.MULETILLA,
                        title = "Muletilla frecuente: \"$phrase\"",
                        message = "Aparece $count veces. Considera podarla para ganar ritmo narrativo.",
                        snippet = phrase
                    )
                )
            }
        }

        // Check excessive -mente adverbs
        val menteWords = rawWords.filter { it.lowercase(Locale.ROOT).endsWith("mente") && it.length > 7 }
        if (menteWords.size >= 3) {
            styleAlerts.add(
                StyleAlert(
                    type = StyleAlertType.ADVERBIO_MENTE,
                    title = "Abundancia de adverbios en -mente (${menteWords.size})",
                    message = "Ejemplos: ${menteWords.take(3).joinToString(", ")}. Sustituir por acciones visuales mejora la prosa.",
                    snippet = menteWords.first()
                )
            )
        }

        // Check overlong sentences
        for (sentence in sentences) {
            val wordsInSentence = sentence.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (wordsInSentence.size > 38) {
                styleAlerts.add(
                    StyleAlert(
                        type = StyleAlertType.ORACION_LARGA,
                        title = "Oración excesivamente larga (${wordsInSentence.size} palabras)",
                        message = "Puede fatigar al lector. Considera dividirla con punto y seguido.",
                        snippet = sentence.take(65) + "..."
                    )
                )
                if (styleAlerts.count { it.type == StyleAlertType.ORACION_LARGA } >= 3) break
            }
        }

        // 5. Formatting issues
        val formattingIssues = mutableListOf<FormattingIssue>()

        // Hyphen instead of dialogue dash
        if (Regex("(^|\\n)\\s*[-–]\\s+").containsMatchIn(text)) {
            formattingIssues.add(
                FormattingIssue(
                    title = "Guion corto en diálogos (-)",
                    description = "Uso de guion estándar o semirraya en lugar de raya de diálogo española (—)."
                )
            )
        }

        // Multiple consecutive spaces
        if (Regex("[ \\t]{2,}").containsMatchIn(text)) {
            formattingIssues.add(
                FormattingIssue(
                    title = "Espacios dobles redundantes",
                    description = "Se detectaron espacios consecutivos que rompen la maquetación del libro."
                )
            )
        }

        // Question / Exclamation without opening mark (Spanish rule: ¿ and ¡)
        val missingOpeningQuestion = Regex("(^|[.!?]\\s+)[A-ZÁÉÍÓÚa-záéíóú\\s]{3,}[?]").containsMatchIn(text)
        val missingOpeningExclamation = Regex("(^|[.!?]\\s+)[A-ZÁÉÍÓÚa-záéíóú\\s]{3,}[!]").containsMatchIn(text)
        if (missingOpeningQuestion || missingOpeningExclamation) {
            formattingIssues.add(
                FormattingIssue(
                    title = "Signos de apertura ausentes (¿ / ¡)",
                    description = "En español los diálogos y exclamaciones requieren signo de apertura."
                )
            )
        }

        return EditorialAnalysisResult(
            wordCount = wordCount,
            characterCount = characterCount,
            paragraphCount = paragraphCount,
            readingTimeMinutes = readingTime,
            dialoguePercentage = dialoguePercentage,
            readabilityScore = readabilityScore,
            readabilityLabel = readabilityLabel,
            echoes = echoes,
            styleAlerts = styleAlerts,
            formattingIssues = formattingIssues
        )
    }

    private fun detectEchoes(rawWords: List<String>, windowSize: Int): List<WordEcho> {
        val cleanTokens = rawWords.map { word ->
            word.lowercase(Locale.ROOT).replace(Regex("[^a-záéíóúñü]"), "")
        }

        val detected = mutableListOf<WordEcho>()
        val seenWords = mutableSetOf<String>()

        for (i in cleanTokens.indices) {
            val token = cleanTokens[i]
            if (token.length <= 4 || SPANISH_STOPWORDS.contains(token) || seenWords.contains(token)) {
                continue
            }

            val windowEnd = (i + windowSize).coerceAtMost(cleanTokens.size)
            var count = 1
            for (j in (i + 1) until windowEnd) {
                if (cleanTokens[j] == token) {
                    count++
                }
            }

            if (count >= 3) {
                seenWords.add(token)
                val snippetStart = (i - 3).coerceAtLeast(0)
                val snippetEnd = (i + 6).coerceAtMost(rawWords.size)
                val snippet = rawWords.subList(snippetStart, snippetEnd).joinToString(" ")
                detected.add(
                    WordEcho(
                        word = token,
                        countInWindow = count,
                        sampleSnippet = "...$snippet..."
                    )
                )
            }
        }

        return detected.take(6)
    }

    private fun countEstimatedSpanishSyllables(text: String): Int {
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'á', 'é', 'í', 'ó', 'ú', 'ü')
        var count = 0
        var prevWasVowel = false
        for (char in text.lowercase(Locale.ROOT)) {
            val isVowel = vowels.contains(char)
            if (isVowel && !prevWasVowel) {
                count++
            }
            prevWasVowel = isVowel
        }
        return count.coerceAtLeast(1)
    }

    // Quick auto-fixers
    fun fixDialogueDashes(text: String): String {
        // Replace line starting with - or – or -- with em-dash —
        var fixed = text.replace(Regex("(?m)^\\s*[-–]{1,2}\\s*"), "— ")
        // Incisos inside dialogue: e.g. "gracias -respondió", "gracias - respondió"
        fixed = fixed.replace(Regex("(?<=[^\\s—-])\\s*[-–]{1,2}\\s*([a-záéíóúñ])"), " —$1")
        // Spaced dashes
        fixed = fixed.replace(Regex("\\s+[-–]{1,2}\\s+"), " — ")
        return fixed
    }

    fun cleanFormattingWhitespace(text: String): String {
        return text
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("(?m)[ \\t]+$"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    fun fixOpeningPunctuation(text: String): String {
        // Fix missing ¿ before a sentence ending in ?
        var result = text
        // If line or sentence starts with capital letter and ends with ? without ¿
        result = result.replace(Regex("(?m)(^|\\n|—\\s*)([A-ZÁÉÍÓÚ][^?.!\\n]+)(\\?)")) { match ->
            val prefix = match.groupValues[1]
            val content = match.groupValues[2]
            if (!content.contains("¿")) {
                "$prefix¿$content?"
            } else {
                match.value
            }
        }
        // If line starts with capital letter and ends with ! without ¡
        result = result.replace(Regex("(?m)(^|\\n|—\\s*)([A-ZÁÉÍÓÚ][^?.!\\n]+)(!)")) { match ->
            val prefix = match.groupValues[1]
            val content = match.groupValues[2]
            if (!content.contains("¡")) {
                "$prefix¡$content!"
            } else {
                match.value
            }
        }
        return result
    }
}
