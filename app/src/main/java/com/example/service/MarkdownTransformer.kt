package com.example.service

import org.json.JSONArray
import org.json.JSONObject

class MarkdownTransformer {

    fun stripMarkdownToPlainText(markdown: String): String {
        if (markdown.isBlank()) return ""

        val lines = markdown.lines()
        val result = StringBuilder()

        var inCodeBlock = false

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                continue
            }

            if (inCodeBlock) {
                result.append(line).append("\n")
                continue
            }

            // Headings
            if (trimmed.startsWith("# ")) {
                val title = trimmed.removePrefix("# ").trim()
                result.append("\n").append(title.uppercase()).append("\n")
                result.append("=".repeat(title.length.coerceIn(4, 60))).append("\n")
                continue
            } else if (trimmed.startsWith("## ")) {
                val title = trimmed.removePrefix("## ").trim()
                result.append("\n").append(title).append("\n")
                result.append("-".repeat(title.length.coerceIn(4, 60))).append("\n")
                continue
            } else if (trimmed.startsWith("### ")) {
                val title = trimmed.removePrefix("### ").trim()
                result.append("\n--- ").append(title).append(" ---\n")
                continue
            }

            // Clean inline formatting: bold, italic, links, inline code
            var cleaned = line
            cleaned = cleaned.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // **bold**
            cleaned = cleaned.replace(Regex("__(.*?)__"), "$1") // __bold__
            cleaned = cleaned.replace(Regex("\\*(.*?)\\*"), "$1") // *italic*
            cleaned = cleaned.replace(Regex("_(.*?)_"), "$1") // _italic_
            cleaned = cleaned.replace(Regex("`([^`]+)`"), "$1") // `code`
            cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)"), "$1 ($2)") // [text](url)
            cleaned = cleaned.replace(Regex("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)"), "[Imagen: $1]") // ![alt](url)

            result.append(cleaned).append("\n")
        }

        return result.toString().trim()
    }

    fun parseMarkdownToJson(markdown: String, docTitle: String): JSONObject {
        val root = JSONObject()
        root.put("format", "markdown")
        root.put("title", docTitle)

        val sections = JSONArray()
        var currentSection: JSONObject? = null
        var currentSectionContent = StringBuilder()
        val codeBlocks = JSONArray()

        var inCodeBlock = false
        var currentCode = StringBuilder()
        var currentCodeLang = ""

        val lines = markdown.lines()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // closing code block
                    val blockObj = JSONObject()
                    blockObj.put("language", currentCodeLang.ifBlank { "text" })
                    blockObj.put("code", currentCode.toString().trimEnd())
                    codeBlocks.put(blockObj)
                    currentSectionContent.append("\n[Bloque de código: ").append(currentCodeLang).append("]\n")
                    currentCode.clear()
                    inCodeBlock = false
                } else {
                    // opening code block
                    inCodeBlock = true
                    currentCodeLang = trimmed.removePrefix("```").trim()
                    currentCode.clear()
                }
                continue
            }

            if (inCodeBlock) {
                currentCode.append(line).append("\n")
                continue
            }

            if (trimmed.startsWith("#")) {
                // Flush previous section
                if (currentSection != null) {
                    currentSection.put("content", currentSectionContent.toString().trim())
                    sections.put(currentSection)
                    currentSectionContent.clear()
                }

                val level = trimmed.takeWhile { it == '#' }.length
                val heading = trimmed.removePrefix("#".repeat(level)).trim()

                currentSection = JSONObject().apply {
                    put("level", level)
                    put("heading", heading)
                }
            } else {
                currentSectionContent.append(line).append("\n")
            }
        }

        if (currentSection != null) {
            currentSection.put("content", currentSectionContent.toString().trim())
            sections.put(currentSection)
        } else if (markdown.isNotBlank()) {
            val fallbackSec = JSONObject().apply {
                put("level", 1)
                put("heading", docTitle)
                put("content", markdown.trim())
            }
            sections.put(fallbackSec)
        }

        root.put("sections", sections)
        root.put("codeBlocks", codeBlocks)
        root.put("rawMarkdown", markdown)

        return root
    }

    fun plainTextToMarkdown(text: String, docTitle: String): String {
        val sb = StringBuilder()
        sb.append("# ").append(docTitle).append("\n\n")

        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                sb.append("- ").append(trimmed.substring(2)).append("\n")
            } else if (trimmed.matches(Regex("^\\d+\\.\\s+.*"))) {
                sb.append(trimmed).append("\n")
            } else if (trimmed.startsWith(">")) {
                sb.append(line).append("\n")
            } else if (trimmed.isEmpty()) {
                sb.append("\n")
            } else {
                sb.append(line).append("\n")
            }
        }

        return sb.toString().trim()
    }
}
