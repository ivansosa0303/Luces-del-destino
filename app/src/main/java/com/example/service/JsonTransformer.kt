package com.example.service

import org.json.JSONArray
import org.json.JSONObject

class JsonTransformer {

    fun jsonToMarkdown(jsonString: String, docTitle: String): String {
        val trimmed = jsonString.trim()
        val sb = StringBuilder()
        sb.append("# ").append(docTitle).append("\n\n")

        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                sb.append("> **Tipo:** Matriz de datos JSON (${array.length()} elementos)\n\n")

                if (array.length() > 0 && array.get(0) is JSONObject) {
                    // Build Markdown Table from keys
                    val firstObj = array.getJSONObject(0)
                    val keys = firstObj.keys().asSequence().toList()

                    if (keys.isNotEmpty()) {
                        sb.append("## Tabla de Datos Estructurada\n\n")
                        // Header row
                        sb.append("| ").append(keys.joinToString(" | ")).append(" |\n")
                        // Separator row
                        sb.append("| ").append(keys.joinToString(" | ") { "---" }).append(" |\n")

                        for (i in 0 until array.length().coerceAtMost(50)) {
                            val obj = array.optJSONObject(i)
                            if (obj != null) {
                                val values = keys.map { k ->
                                    obj.opt(k)?.toString()?.replace("|", "/")?.replace("\n", " ") ?: ""
                                }
                                sb.append("| ").append(values.joinToString(" | ")).append(" |\n")
                            }
                        }
                        sb.append("\n")
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                sb.append("## Propiedades Principales\n\n")
                val keys = obj.keys().asSequence().toList()
                for (k in keys) {
                    val value = obj.get(k)
                    if (value !is JSONObject && value !is JSONArray) {
                        sb.append("- **").append(k).append("**: `").append(value).append("`\n")
                    } else if (value is JSONArray) {
                        sb.append("- **").append(k).append("**: *Array de ${value.length()} elementos*\n")
                    } else if (value is JSONObject) {
                        sb.append("- **").append(k).append("**: *Objeto con ${value.length()} atributos*\n")
                    }
                }
                sb.append("\n")
            }

            // Always embed formatted JSON code fence to preserve 100% data integrity
            sb.append("## Código Fuente JSON Original\n\n")
            sb.append("```json\n")
            val formatted = try {
                if (trimmed.startsWith("{")) JSONObject(trimmed).toString(2)
                else if (trimmed.startsWith("[")) JSONArray(trimmed).toString(2)
                else trimmed
            } catch (_: Exception) {
                trimmed
            }
            sb.append(formatted)
            sb.append("\n```\n")
        } catch (_: Exception) {
            sb.append("```json\n").append(trimmed).append("\n```\n")
        }

        return sb.toString()
    }

    fun jsonToPlainText(jsonString: String): String {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).toString(2)
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString(2)
            } else {
                trimmed
            }
        } catch (_: Exception) {
            jsonString
        }
    }

    fun plainTextToJson(text: String, docTitle: String): JSONObject {
        val root = JSONObject()
        root.put("title", docTitle)
        root.put("format", "plain_text")

        val lines = text.lines()
        val linesArray = JSONArray()
        val propertiesObj = JSONObject()

        for (line in lines) {
            linesArray.put(line)

            // Auto-detect key-value pairs e.g. "clave: valor" or "key = value"
            val colonSplit = line.split(":", limit = 2)
            val equalSplit = line.split("=", limit = 2)

            if (colonSplit.size == 2 && colonSplit[0].isNotBlank() && !colonSplit[0].contains(" ")) {
                val key = colonSplit[0].trim()
                val value = colonSplit[1].trim()
                propertiesObj.put(key, value)
            } else if (equalSplit.size == 2 && equalSplit[0].isNotBlank() && !equalSplit[0].contains(" ")) {
                val key = equalSplit[0].trim()
                val value = equalSplit[1].trim()
                propertiesObj.put(key, value)
            }
        }

        root.put("lineCount", lines.size)
        root.put("characterCount", text.length)
        if (propertiesObj.length() > 0) {
            root.put("detectedProperties", propertiesObj)
        }
        root.put("content", text)
        root.put("lines", linesArray)

        return root
    }
}
