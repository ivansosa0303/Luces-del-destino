package com.example.engine

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.UUID

class FileStorageManager(private val context: Context) {

    private val documentsDir: File
        get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    private val compilationsDir: File
        get() = File(context.filesDir, "compilations").apply { if (!exists()) mkdirs() }

    fun saveStreamToDocument(inputStream: InputStream, originalFileName: String): Pair<File, String> {
        val sanitized = sanitizeFileName(originalFileName)
        val uniqueName = "${UUID.randomUUID().toString().take(8)}_$sanitized"
        val destFile = File(documentsDir, uniqueName)

        val digest = MessageDigest.getInstance("SHA-256")
        FileOutputStream(destFile).use { out ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
            }
            out.flush()
        }
        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        return Pair(destFile, sha256)
    }

    fun saveTextToDocument(text: String, originalFileName: String): Pair<File, String> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val sanitized = sanitizeFileName(originalFileName)
        val uniqueName = "${UUID.randomUUID().toString().take(8)}_$sanitized"
        val destFile = File(documentsDir, uniqueName)

        destFile.writeBytes(bytes)
        val sha256 = calculateSha256(bytes)
        return Pair(destFile, sha256)
    }

    fun saveBytesToDocument(bytes: ByteArray, originalFileName: String): Pair<File, String> {
        val sanitized = sanitizeFileName(originalFileName)
        val uniqueName = "${UUID.randomUUID().toString().take(8)}_$sanitized"
        val destFile = File(documentsDir, uniqueName)

        destFile.writeBytes(bytes)
        val sha256 = calculateSha256(bytes)
        return Pair(destFile, sha256)
    }

    fun saveStagedFileToDocument(stagedFile: File, originalFileName: String, precalculatedSha256: String? = null): Pair<File, String> {
        val sanitized = sanitizeFileName(originalFileName)
        val uniqueName = "${UUID.randomUUID().toString().take(8)}_$sanitized"
        val destFile = File(documentsDir, uniqueName)
        stagedFile.copyTo(destFile, overwrite = true)
        val sha256 = precalculatedSha256?.ifBlank { null } ?: calculateSha256(destFile)
        return Pair(destFile, sha256)
    }

    fun createCompilationFile(fileName: String): File {
        val sanitized = sanitizeFileName(fileName)
        return File(compilationsDir, sanitized)
    }

    fun getFileFromRelativePath(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    fun readTextSafely(file: File): String {
        return try {
            if (!file.exists()) return ""
            file.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            try {
                file.readText(Charsets.ISO_8859_1)
            } catch (_: Exception) {
                ""
            }
        }
    }

    fun readBytesSafely(file: File): ByteArray {
        return try {
            if (!file.exists()) ByteArray(0) else file.readBytes()
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    fun deleteFileSafely(relativePath: String): Boolean {
        return try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }

    fun calculateSha256(file: File): String {
        return try {
            if (!file.exists()) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    private fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val df = DecimalFormat("#,##0.#")
            val size = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return "${df.format(size)} ${units[digitGroups.coerceIn(0, units.size - 1)]}"
        }

        fun sanitizeFileName(fileName: String): String {
            return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        }
    }
}
