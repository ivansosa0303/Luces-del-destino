package com.example.service

import java.io.File
import java.security.MessageDigest

data class ContentMetrics(
    val characterCount: Int,
    val wordCount: Int,
    val lineCount: Int,
    val byteSize: Long,
    val sha256Checksum: String
) {
    companion object {
        fun fromText(text: String): ContentMetrics {
            val trimmed = text.trim()
            val words = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
            val lines = if (text.isEmpty()) 0 else text.lines().size
            val bytes = text.toByteArray(Charsets.UTF_8)
            val sha256 = calculateSha256(bytes)

            return ContentMetrics(
                characterCount = text.length,
                wordCount = words,
                lineCount = lines,
                byteSize = bytes.size.toLong(),
                sha256Checksum = sha256
            )
        }

        fun fromFile(file: File, fallbackText: String = ""): ContentMetrics {
            val bytes = if (file.exists()) file.readBytes() else fallbackText.toByteArray(Charsets.UTF_8)
            val sha256 = calculateSha256(bytes)
            val text = try {
                file.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                fallbackText
            }
            val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
            val lines = if (text.isEmpty()) 0 else text.lines().size

            return ContentMetrics(
                characterCount = text.length,
                wordCount = words,
                lineCount = lines,
                byteSize = bytes.size.toLong(),
                sha256Checksum = sha256
            )
        }

        fun calculateSha256(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(bytes).joinToString("") { "%02x".format(it) }
        }
    }
}
