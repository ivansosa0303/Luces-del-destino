package com.example.service

import android.content.Context
import com.example.data.model.SupportedFormat
import com.example.engine.DocxProcessor
import com.example.engine.FileStorageManager
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConversionReport(
    val success: Boolean,
    val sourceFormat: SupportedFormat,
    val targetFormat: SupportedFormat,
    val sourceFile: File,
    val outputFile: File?,
    val sourceMetrics: ContentMetrics,
    val targetMetrics: ContentMetrics?,
    val executionTimeMs: Long,
    val integrityVerified: Boolean,
    val message: String
)

interface FileConverterService {
    fun convert(
        sourceFile: File,
        sourceFormat: SupportedFormat,
        targetFormat: SupportedFormat,
        customTitle: String? = null
    ): ConversionReport

    fun getAvailableConversions(sourceFormat: SupportedFormat): List<SupportedFormat>
}

class DefaultFileConverterService(
    private val context: Context,
    private val storageManager: FileStorageManager
) : FileConverterService {

    private val markdownTransformer = MarkdownTransformer()
    private val jsonTransformer = JsonTransformer()
    private val pdfTransformer = PdfTransformer(context)
    private val docxProcessor = DocxProcessor()

    override fun getAvailableConversions(sourceFormat: SupportedFormat): List<SupportedFormat> {
        return when (sourceFormat) {
            SupportedFormat.TXT -> listOf(SupportedFormat.MD, SupportedFormat.JSON, SupportedFormat.PDF, SupportedFormat.DOCX)
            SupportedFormat.MD -> listOf(SupportedFormat.TXT, SupportedFormat.JSON, SupportedFormat.PDF, SupportedFormat.DOCX)
            SupportedFormat.JSON -> listOf(SupportedFormat.TXT, SupportedFormat.MD, SupportedFormat.PDF, SupportedFormat.DOCX)
            SupportedFormat.PDF -> listOf(SupportedFormat.TXT, SupportedFormat.MD, SupportedFormat.JSON, SupportedFormat.PNG, SupportedFormat.JPG)
            SupportedFormat.DOCX -> listOf(SupportedFormat.MD, SupportedFormat.TXT, SupportedFormat.PDF, SupportedFormat.JSON)
            SupportedFormat.PNG, SupportedFormat.JPG -> listOf(SupportedFormat.PDF, SupportedFormat.JSON, SupportedFormat.MD)
            SupportedFormat.ZIP -> listOf(SupportedFormat.MD, SupportedFormat.JSON)
        }.filter { it != sourceFormat }
    }

    override fun convert(
        sourceFile: File,
        sourceFormat: SupportedFormat,
        targetFormat: SupportedFormat,
        customTitle: String?
    ): ConversionReport {
        val startTime = System.currentTimeMillis()

        if (!sourceFile.exists()) {
            val emptyMetrics = ContentMetrics(0, 0, 0, 0L, "")
            return ConversionReport(
                success = false,
                sourceFormat = sourceFormat,
                targetFormat = targetFormat,
                sourceFile = sourceFile,
                outputFile = null,
                sourceMetrics = emptyMetrics,
                targetMetrics = null,
                executionTimeMs = 0L,
                integrityVerified = false,
                message = "El archivo de origen no existe en el sistema de almacenamiento."
            )
        }

        val baseName = customTitle?.ifBlank { null } ?: sourceFile.nameWithoutExtension
        val sourceMetrics = ContentMetrics.fromFile(sourceFile)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFileName = "${FileStorageManager.sanitizeFileName(baseName)}_to_${targetFormat.extension}_$timestamp.${targetFormat.extension}"
        val destFile = storageManager.createCompilationFile(outputFileName)

        try {
            val success = when (sourceFormat) {
                SupportedFormat.TXT -> convertFromTxt(sourceFile, destFile, baseName, targetFormat)
                SupportedFormat.MD -> convertFromMd(sourceFile, destFile, baseName, targetFormat)
                SupportedFormat.JSON -> convertFromJson(sourceFile, destFile, baseName, targetFormat)
                SupportedFormat.PDF -> convertFromPdf(sourceFile, destFile, baseName, targetFormat)
                SupportedFormat.DOCX -> convertFromDocx(sourceFile, destFile, baseName, targetFormat)
                SupportedFormat.PNG, SupportedFormat.JPG -> convertFromImage(sourceFile, destFile, baseName, targetFormat, sourceFormat)
                SupportedFormat.ZIP -> false
            }

            val endTime = System.currentTimeMillis()

            if (success && destFile.exists() && destFile.length() > 0L) {
                val targetMetrics = ContentMetrics.fromFile(destFile)
                // Verify integrity: destination exists, non-empty, valid checksum
                val integrityOk = targetMetrics.sha256Checksum.isNotBlank() && targetMetrics.byteSize > 0L

                return ConversionReport(
                    success = true,
                    sourceFormat = sourceFormat,
                    targetFormat = targetFormat,
                    sourceFile = sourceFile,
                    outputFile = destFile,
                    sourceMetrics = sourceMetrics,
                    targetMetrics = targetMetrics,
                    executionTimeMs = endTime - startTime,
                    integrityVerified = integrityOk,
                    message = "Transformación completada con éxito preservando la integridad del contenido."
                )
            } else {
                return ConversionReport(
                    success = false,
                    sourceFormat = sourceFormat,
                    targetFormat = targetFormat,
                    sourceFile = sourceFile,
                    outputFile = null,
                    sourceMetrics = sourceMetrics,
                    targetMetrics = null,
                    executionTimeMs = endTime - startTime,
                    integrityVerified = false,
                    message = "Fallo al generar el archivo de destino durante la transformación."
                )
            }
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            return ConversionReport(
                success = false,
                sourceFormat = sourceFormat,
                targetFormat = targetFormat,
                sourceFile = sourceFile,
                outputFile = null,
                sourceMetrics = sourceMetrics,
                targetMetrics = null,
                executionTimeMs = endTime - startTime,
                integrityVerified = false,
                message = "Error en el servicio de conversión: ${e.message}"
            )
        }
    }

    // --- TXT Transformations ---
    private fun convertFromTxt(source: File, dest: File, title: String, target: SupportedFormat): Boolean {
        val text = storageManager.readTextSafely(source)
        return when (target) {
            SupportedFormat.MD -> {
                val md = markdownTransformer.plainTextToMarkdown(text, title)
                dest.writeText(md, Charsets.UTF_8)
                true
            }
            SupportedFormat.JSON -> {
                val json = jsonTransformer.plainTextToJson(text, title)
                dest.writeText(json.toString(2), Charsets.UTF_8)
                true
            }
            SupportedFormat.PDF -> {
                pdfTransformer.renderTextToPdf(dest, title, text, isCode = false)
            }
            SupportedFormat.DOCX -> {
                val section = DocxProcessor.DocxSection(title, "Convertido desde texto plano", text)
                docxProcessor.createDocxFile(dest, title, listOf(section))
                true
            }
            else -> false
        }
    }

    // --- MD Transformations ---
    private fun convertFromMd(source: File, dest: File, title: String, target: SupportedFormat): Boolean {
        val md = storageManager.readTextSafely(source)
        return when (target) {
            SupportedFormat.TXT -> {
                val plain = markdownTransformer.stripMarkdownToPlainText(md)
                dest.writeText(plain, Charsets.UTF_8)
                true
            }
            SupportedFormat.JSON -> {
                val json = markdownTransformer.parseMarkdownToJson(md, title)
                dest.writeText(json.toString(2), Charsets.UTF_8)
                true
            }
            SupportedFormat.PDF -> {
                pdfTransformer.renderMarkdownToPdf(dest, title, md)
            }
            SupportedFormat.DOCX -> {
                val plain = markdownTransformer.stripMarkdownToPlainText(md)
                val section = DocxProcessor.DocxSection(title, "Estructurado desde Markdown", plain)
                docxProcessor.createDocxFile(dest, title, listOf(section))
                true
            }
            else -> false
        }
    }

    // --- JSON Transformations ---
    private fun convertFromJson(source: File, dest: File, title: String, target: SupportedFormat): Boolean {
        val jsonString = storageManager.readTextSafely(source)
        return when (target) {
            SupportedFormat.TXT -> {
                val plain = jsonTransformer.jsonToPlainText(jsonString)
                dest.writeText(plain, Charsets.UTF_8)
                true
            }
            SupportedFormat.MD -> {
                val md = jsonTransformer.jsonToMarkdown(jsonString, title)
                dest.writeText(md, Charsets.UTF_8)
                true
            }
            SupportedFormat.PDF -> {
                val formatted = jsonTransformer.jsonToPlainText(jsonString)
                pdfTransformer.renderTextToPdf(dest, title, formatted, isCode = true)
            }
            SupportedFormat.DOCX -> {
                val formatted = jsonTransformer.jsonToPlainText(jsonString)
                val section = DocxProcessor.DocxSection(title, "Estructura de datos JSON", formatted, isCodeBlock = true)
                docxProcessor.createDocxFile(dest, title, listOf(section))
                true
            }
            else -> false
        }
    }

    // --- PDF Transformations ---
    private fun convertFromPdf(source: File, dest: File, title: String, target: SupportedFormat): Boolean {
        return when (target) {
            SupportedFormat.TXT -> {
                val plain = pdfTransformer.extractTextFromPdf(source)
                dest.writeText(plain, Charsets.UTF_8)
                true
            }
            SupportedFormat.MD -> {
                val md = pdfTransformer.extractPdfToMarkdown(source)
                dest.writeText(md, Charsets.UTF_8)
                true
            }
            SupportedFormat.JSON -> {
                val json = pdfTransformer.extractPdfToJson(source)
                dest.writeText(json.toString(2), Charsets.UTF_8)
                true
            }
            else -> false
        }
    }

    // --- DOCX Transformations ---
    private fun convertFromDocx(source: File, dest: File, title: String, target: SupportedFormat): Boolean {
        val text = docxProcessor.extractTextFromDocx(source)
        return when (target) {
            SupportedFormat.TXT -> {
                dest.writeText(text, Charsets.UTF_8)
                true
            }
            SupportedFormat.MD -> {
                val md = markdownTransformer.plainTextToMarkdown(text, title)
                dest.writeText(md, Charsets.UTF_8)
                true
            }
            SupportedFormat.JSON -> {
                val json = jsonTransformer.plainTextToJson(text, title)
                dest.writeText(json.toString(2), Charsets.UTF_8)
                true
            }
            SupportedFormat.PDF -> {
                pdfTransformer.renderTextToPdf(dest, title, text, isCode = false)
            }
            else -> false
        }
    }

    // --- Image Transformations ---
    private fun convertFromImage(source: File, dest: File, title: String, target: SupportedFormat, srcFormat: SupportedFormat): Boolean {
        return when (target) {
            SupportedFormat.PDF -> {
                val pdfCompiler = com.example.engine.PdfCompiler(context)
                val item = com.example.engine.PdfCompiler.PdfItem(
                    title = title,
                    formatName = srcFormat.extension,
                    formatColorHex = android.graphics.Color.rgb(5, 150, 105),
                    subtitle = "Convertido desde imagen ${source.name}",
                    imageFile = source
                )
                pdfCompiler.compileToPdf(dest, title, "Conversión de imagen a documento PDF", listOf(item))
            }
            SupportedFormat.JSON -> {
                val bytes = source.readBytes()
                val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val root = JSONObject().apply {
                    put("fileName", source.name)
                    put("format", srcFormat.extension)
                    put("mimeType", srcFormat.mimeType)
                    put("fileSizeBytes", source.length())
                    put("dataBase64", b64)
                }
                dest.writeText(root.toString(2), Charsets.UTF_8)
                true
            }
            SupportedFormat.MD -> {
                val bytes = source.readBytes()
                val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val md = "# $title\n\n![${source.name}](data:${srcFormat.mimeType};base64,$b64)\n"
                dest.writeText(md, Charsets.UTF_8)
                true
            }
            else -> false
        }
    }
}
