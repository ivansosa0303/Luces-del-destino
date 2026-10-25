package com.example.service

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.data.model.SupportedFormat
import com.example.engine.FileStorageManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class IncomingFileResult(
    val uri: Uri,
    val fileName: String,
    val extension: String,
    val mimeType: String,
    val format: SupportedFormat,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val stagedFile: File?,
    val textPreview: String = "",
    val isValid: Boolean,
    val errorMessage: String? = null
)

interface UriFileResolverService {
    fun resolveUri(uri: Uri): IncomingFileResult
    fun resolveUris(uris: List<Uri>): List<IncomingFileResult>
    fun resolveIntent(intent: Intent): List<IncomingFileResult>
}

class DefaultUriFileResolverService(
    private val context: Context,
    private val storageManager: FileStorageManager
) : UriFileResolverService {

    private val contentResolver: ContentResolver = context.contentResolver

    private val incomingStagingDir: File
        get() = File(context.filesDir, "staging").apply { if (!exists()) mkdirs() }

    override fun resolveUris(uris: List<Uri>): List<IncomingFileResult> {
        return uris.map { resolveUri(it) }
    }

    override fun resolveUri(uri: Uri): IncomingFileResult {
        try {
            // 1. Try to take persistable URI permission if available
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // Not all content providers support persistable permissions; staging handles this safely.
            }

            // 2. Query Metadata using ContentResolver
            var displayName: String? = null
            var querySize: Long = -1L

            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            querySize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("UriFileResolver", "Error al consultar metadatos con ContentResolver: ${e.message}")
            }

            // Fallback for file name
            val finalFileName = displayName?.ifBlank { null }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                ?: "archivo_importado_${System.currentTimeMillis()}"

            // 3. Resolve MIME Type & Format
            var mimeType = contentResolver.getType(uri) ?: ""
            var extension = finalFileName.substringAfterLast('.', "").lowercase()

            if (extension.isEmpty() && mimeType.isNotEmpty()) {
                val mappedExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (!mappedExt.isNullOrEmpty()) {
                    extension = mappedExt.lowercase()
                }
            }

            if (mimeType.isEmpty() && extension.isNotEmpty()) {
                val mappedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (!mappedMime.isNullOrEmpty()) {
                    mimeType = mappedMime
                }
            }

            val format = SupportedFormat.fromExtension(extension)

            // 4. Stream and Stage File locally with SHA-256 calculation
            val sanitizedName = FileStorageManager.sanitizeFileName(finalFileName)
            val uniqueStagedName = "${UUID.randomUUID().toString().take(8)}_$sanitizedName"
            val stagedFile = File(incomingStagingDir, uniqueStagedName)

            var actualBytesRead: Long = 0L
            val digest = MessageDigest.getInstance("SHA-256")

            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return IncomingFileResult(
                    uri = uri,
                    fileName = finalFileName,
                    extension = extension,
                    mimeType = mimeType,
                    format = format,
                    sizeBytes = 0L,
                    sha256Checksum = "",
                    stagedFile = null,
                    isValid = false,
                    errorMessage = "No se pudo abrir el flujo de entrada (InputStream) para el URI proporcionado."
                )

            inputStream.use { inStream ->
                FileOutputStream(stagedFile).use { outStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        actualBytesRead += read
                    }
                    outStream.flush()
                }
            }

            val calculatedSha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // 5. Generate Text Preview if text-based format
            val textPreview = if (format.isTextBased || format == SupportedFormat.DOCX) {
                try {
                    if (format == SupportedFormat.DOCX) {
                        com.example.engine.DocxProcessor().extractTextFromDocx(stagedFile).take(1000)
                    } else {
                        stagedFile.readText(Charsets.UTF_8).take(1000)
                    }
                } catch (_: Exception) {
                    ""
                }
            } else {
                ""
            }

            return IncomingFileResult(
                uri = uri,
                fileName = finalFileName,
                extension = extension,
                mimeType = mimeType.ifBlank { format.mimeType },
                format = format,
                sizeBytes = actualBytesRead,
                sha256Checksum = calculatedSha256,
                stagedFile = stagedFile,
                textPreview = textPreview,
                isValid = true
            )
        } catch (e: Exception) {
            Log.e("UriFileResolver", "Fallo al resolver URI de almacenamiento: ${e.message}", e)
            return IncomingFileResult(
                uri = uri,
                fileName = uri.lastPathSegment ?: "desconocido",
                extension = "",
                mimeType = "",
                format = SupportedFormat.TXT,
                sizeBytes = 0L,
                sha256Checksum = "",
                stagedFile = null,
                isValid = false,
                errorMessage = "Error al resolver archivo: ${e.message}"
            )
        }
    }

    override fun resolveIntent(intent: Intent): List<IncomingFileResult> {
        val action = intent.action
        val results = mutableListOf<IncomingFileResult>()

        if (Intent.ACTION_SEND == action) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: intent.data
            if (uri != null) {
                results.add(resolveUri(uri))
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                for (u in uris) {
                    results.add(resolveUri(u))
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val uri = intent.data
            if (uri != null) {
                results.add(resolveUri(uri))
            }
        }

        // Also check clipData if no results found
        if (results.isEmpty() && intent.clipData != null) {
            val clipData = intent.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        results.add(resolveUri(uri))
                    }
                }
            }
        }

        return results
    }
}
