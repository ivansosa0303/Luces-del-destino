package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import com.example.data.model.DocumentItemEntity
import com.example.data.model.SupportedFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentMerger(
    private val context: Context,
    private val storageManager: FileStorageManager
) {
    private val docxProcessor = DocxProcessor()
    private val pdfCompiler = PdfCompiler(context)
    private val pdfExtractor = PdfExtractor()

    data class MergeOptions(
        val targetFormat: SupportedFormat,
        val outputTitle: String,
        val includeTableOfContents: Boolean = true,
        val includeSha256Checksums: Boolean = true,
        val includeSeparators: Boolean = true
    )

    data class ExtractedDoc(
        val item: DocumentItemEntity,
        val file: File,
        val text: String,
        val format: SupportedFormat,
        val isCode: Boolean
    )

    fun prepareDocumentData(item: DocumentItemEntity): ExtractedDoc {
        val file = storageManager.getFileFromRelativePath(item.relativeFilePath)
        val format = SupportedFormat.fromExtension(item.extension)

        val (extractedText, isCode) = when (format) {
            SupportedFormat.TXT -> Pair(storageManager.readTextSafely(file), false)
            SupportedFormat.MD -> Pair(storageManager.readTextSafely(file), false)
            SupportedFormat.JSON -> Pair(storageManager.readTextSafely(file), true)
            SupportedFormat.DOCX -> Pair(docxProcessor.extractTextFromDocx(file), false)
            SupportedFormat.PDF -> {
                // If PDF, provide page count and description or extract preview
                val pages = pdfExtractor.getPageCount(file)
                val summary = "Documento PDF (${pages} página${if (pages == 1) "" else "s"})\n" +
                        "Ubicación interna: ${file.name}\n" +
                        "Tamaño: ${FileStorageManager.formatFileSize(item.fileSizeBytes)}"
                Pair(summary, false)
            }
            SupportedFormat.PNG, SupportedFormat.JPG -> {
                val summary = "Archivo de Imagen (${format.displayName})\n" +
                        "Nombre: ${item.fileName}\n" +
                        "Tamaño: ${FileStorageManager.formatFileSize(item.fileSizeBytes)}"
                Pair(summary, false)
            }
            SupportedFormat.ZIP -> {
                Pair("Paquete ZIP: ${item.fileName} (${FileStorageManager.formatFileSize(item.fileSizeBytes)})", false)
            }
        }

        return ExtractedDoc(
            item = item,
            file = file,
            text = extractedText,
            format = format,
            isCode = isCode
        )
    }

    fun compile(items: List<DocumentItemEntity>, options: MergeOptions): Pair<File, String>? {
        if (items.isEmpty()) return null

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanTitle = FileStorageManager.sanitizeFileName(options.outputTitle.ifBlank { "Compilacion_$timestamp" })
        val targetExt = options.targetFormat.extension
        val outputFileName = "${cleanTitle}_$timestamp.$targetExt"
        val destFile = storageManager.createCompilationFile(outputFileName)

        val extractedDocs = items.map { prepareDocumentData(it) }

        val success = when (options.targetFormat) {
            SupportedFormat.PDF -> compilePdf(destFile, options, extractedDocs)
            SupportedFormat.MD -> compileMarkdown(destFile, options, extractedDocs)
            SupportedFormat.JSON -> compileJsonBundle(destFile, options, extractedDocs)
            SupportedFormat.DOCX -> compileDocx(destFile, options, extractedDocs)
            SupportedFormat.TXT -> compileTxt(destFile, options, extractedDocs)
            SupportedFormat.ZIP -> compileZip(destFile, options, extractedDocs)
            SupportedFormat.PNG, SupportedFormat.JPG -> compileImageSheet(destFile, options, extractedDocs)
        }

        return if (success) {
            val sha256 = storageManager.calculateSha256(destFile)
            Pair(destFile, sha256)
        } else {
            null
        }
    }

    private fun compilePdf(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        val pdfItems = docs.map { doc ->
            val isImg = doc.format == SupportedFormat.PNG || doc.format == SupportedFormat.JPG
            val formatColor = when (doc.format) {
                SupportedFormat.TXT -> android.graphics.Color.rgb(100, 116, 139)
                SupportedFormat.MD -> android.graphics.Color.rgb(2, 132, 199)
                SupportedFormat.JSON -> android.graphics.Color.rgb(217, 119, 6)
                SupportedFormat.PDF -> android.graphics.Color.rgb(220, 38, 38)
                SupportedFormat.DOCX -> android.graphics.Color.rgb(37, 99, 235)
                SupportedFormat.PNG, SupportedFormat.JPG -> android.graphics.Color.rgb(5, 150, 105)
                SupportedFormat.ZIP -> android.graphics.Color.rgb(124, 58, 237)
            }

            val subtitle = StringBuilder("${doc.item.fileName} • ${FileStorageManager.formatFileSize(doc.item.fileSizeBytes)}")
            if (options.includeSha256Checksums && doc.item.sha256Checksum.isNotBlank()) {
                subtitle.append(" • SHA-256: ${doc.item.sha256Checksum.take(12)}...")
            }

            PdfCompiler.PdfItem(
                title = doc.item.title,
                formatName = doc.format.extension,
                formatColorHex = formatColor,
                subtitle = subtitle.toString(),
                textContent = if (!isImg) doc.text else null,
                isCode = doc.isCode,
                imageFile = if (isImg) doc.file else null
            )
        }

        return pdfCompiler.compileToPdf(
            destFile = destFile,
            documentTitle = options.outputTitle,
            projectDescription = "Compilación multi-formato sin pérdida de datos. Creado sin conexión con DocMerge.",
            items = pdfItems
        )
    }

    private fun compileMarkdown(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        return try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sb = StringBuilder()
            sb.append("# ${options.outputTitle}\n\n")
            sb.append("> **Fecha de compilación:** $dateStr  \n")
            sb.append("> **Total de archivos unificados:** ${docs.size}  \n")
            sb.append("> **Generado con:** DocMerge (100% Offline)\n\n")

            if (options.includeTableOfContents) {
                sb.append("## Tabla de Contenidos\n\n")
                docs.forEachIndexed { idx, doc ->
                    sb.append("${idx + 1}. [${doc.item.title} (${doc.item.fileName})](#seccion-${idx + 1})\n")
                }
                sb.append("\n---\n\n")
            }

            docs.forEachIndexed { idx, doc ->
                sb.append("## <a name=\"seccion-${idx + 1}\"></a>${idx + 1}. ${doc.item.title}\n\n")
                sb.append("- **Archivo:** `${doc.item.fileName}`\n")
                sb.append("- **Formato:** `${doc.format.displayName}`\n")
                sb.append("- **Tamaño:** ${FileStorageManager.formatFileSize(doc.item.fileSizeBytes)}\n")
                if (options.includeSha256Checksums && doc.item.sha256Checksum.isNotBlank()) {
                    sb.append("- **Integridad SHA-256:** `${doc.item.sha256Checksum}`\n")
                }
                sb.append("\n")

                when (doc.format) {
                    SupportedFormat.JSON -> {
                        sb.append("```json\n")
                        sb.append(doc.text)
                        sb.append("\n```\n\n")
                    }
                    SupportedFormat.PNG, SupportedFormat.JPG -> {
                        // Embed image as base64 data URI so markdown is 100% standalone and portable!
                        try {
                            val bytes = doc.file.readBytes()
                            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val mime = doc.format.mimeType
                            sb.append("![${doc.item.title}](data:$mime;base64,$b64)\n\n")
                        } catch (_: Exception) {
                            sb.append("*(Imagen: ${doc.item.fileName})*\n\n")
                        }
                    }
                    else -> {
                        sb.append(doc.text)
                        sb.append("\n\n")
                    }
                }

                if (options.includeSeparators && idx < docs.size - 1) {
                    sb.append("\n---\n\n")
                }
            }

            destFile.writeText(sb.toString(), Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compileJsonBundle(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        return try {
            val root = JSONObject()
            root.put("bundleTitle", options.outputTitle)
            root.put("compiledAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            root.put("totalFiles", docs.size)
            root.put("generator", "DocMerge Android Offline Engine")

            val docArray = JSONArray()
            docs.forEachIndexed { idx, doc ->
                val obj = JSONObject()
                obj.put("index", idx + 1)
                obj.put("title", doc.item.title)
                obj.put("fileName", doc.item.fileName)
                obj.put("extension", doc.format.extension)
                obj.put("mimeType", doc.format.mimeType)
                obj.put("sizeBytes", doc.item.fileSizeBytes)
                obj.put("sha256Checksum", doc.item.sha256Checksum)

                if (doc.format.isTextBased) {
                    obj.put("contentType", "text")
                    obj.put("content", doc.text)
                } else {
                    obj.put("contentType", "binary")
                    obj.put("textSummary", doc.text)
                    try {
                        val bytes = doc.file.readBytes()
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        obj.put("dataBase64", b64)
                    } catch (_: Exception) {}
                }
                docArray.put(obj)
            }
            root.put("documents", docArray)

            destFile.writeText(root.toString(2), Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compileDocx(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        return try {
            val sections = docs.map { doc ->
                val subtitle = StringBuilder("Archivo: ${doc.item.fileName} | Tamaño: ${FileStorageManager.formatFileSize(doc.item.fileSizeBytes)}")
                if (options.includeSha256Checksums && doc.item.sha256Checksum.isNotBlank()) {
                    subtitle.append(" | SHA-256: ${doc.item.sha256Checksum.take(16)}...")
                }

                DocxProcessor.DocxSection(
                    title = doc.item.title,
                    subtitle = subtitle.toString(),
                    content = doc.text,
                    isCodeBlock = doc.isCode
                )
            }

            docxProcessor.createDocxFile(destFile, options.outputTitle, sections)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compileTxt(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        return try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sb = StringBuilder()
            val bannerLine = "=".repeat(72)
            val subLine = "-".repeat(72)

            sb.append(bannerLine).append("\n")
            sb.append("  ").append(options.outputTitle.uppercase(Locale.ROOT)).append("\n")
            sb.append("  Compilado: ").append(dateStr).append(" (DocMerge Offline)\n")
            sb.append("  Total de Documentos: ").append(docs.size).append("\n")
            sb.append(bannerLine).append("\n\n")

            if (options.includeTableOfContents) {
                sb.append("TABLA DE CONTENIDOS:\n")
                docs.forEachIndexed { idx, doc ->
                    sb.append("  [${idx + 1}] ${doc.item.title} (${doc.item.fileName})\n")
                }
                sb.append("\n").append(subLine).append("\n\n")
            }

            docs.forEachIndexed { idx, doc ->
                sb.append("[${idx + 1}] ").append(doc.item.title.uppercase(Locale.ROOT)).append("\n")
                sb.append("Archivo: ").append(doc.item.fileName).append(" | ")
                sb.append("Formato: ").append(doc.format.displayName).append(" | ")
                sb.append("Tamaño: ").append(FileStorageManager.formatFileSize(doc.item.fileSizeBytes)).append("\n")
                if (options.includeSha256Checksums && doc.item.sha256Checksum.isNotBlank()) {
                    sb.append("SHA-256: ").append(doc.item.sha256Checksum).append("\n")
                }
                sb.append(subLine).append("\n\n")
                sb.append(doc.text).append("\n\n")

                if (options.includeSeparators && idx < docs.size - 1) {
                    sb.append("\n").append(bannerLine).append("\n\n")
                }
            }

            destFile.writeText(sb.toString(), Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compileZip(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(destFile)).use { zipOut ->
                // 1. Add compilation summary markdown file
                val manifestName = "00_COMPILACION_RESUMEN.md"
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val manifestSb = StringBuilder()
                manifestSb.append("# Resumen de Compilación: ${options.outputTitle}\n\n")
                manifestSb.append("- Fecha: $dateStr\n")
                manifestSb.append("- Generador: DocMerge Offline Project Compiler\n\n")
                manifestSb.append("## Archivos incluidos en este paquete:\n\n")

                docs.forEachIndexed { idx, doc ->
                    manifestSb.append("### ${idx + 1}. ${doc.item.fileName}\n")
                    manifestSb.append("- Título: ${doc.item.title}\n")
                    manifestSb.append("- Formato: ${doc.format.displayName}\n")
                    manifestSb.append("- Tamaño: ${FileStorageManager.formatFileSize(doc.item.fileSizeBytes)}\n")
                    manifestSb.append("- SHA-256: `${doc.item.sha256Checksum}`\n\n")
                }

                zipOut.putNextEntry(ZipEntry(manifestName))
                zipOut.write(manifestSb.toString().toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 2. Add each file keeping original bytes intact!
                docs.forEachIndexed { idx, doc ->
                    val cleanIndex = String.format(Locale.ROOT, "%02d", idx + 1)
                    val entryName = "${cleanIndex}_${FileStorageManager.sanitizeFileName(doc.item.fileName)}"
                    zipOut.putNextEntry(ZipEntry(entryName))
                    if (doc.file.exists()) {
                        FileInputStream(doc.file).use { fis ->
                            fis.copyTo(zipOut)
                        }
                    }
                    zipOut.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compileImageSheet(destFile: File, options: MergeOptions, docs: List<ExtractedDoc>): Boolean {
        return try {
            val width = 1080
            val heightPerItem = 380
            val headerHeight = 220
            val totalHeight = (headerHeight + (docs.size * heightPerItem)).coerceAtMost(8000)

            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(248, 250, 252)) // Slate 50

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 22f
            }

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 20f
            }

            // Draw Header
            canvas.drawText(options.outputTitle, 50f, 80f, titlePaint)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("$dateStr • ${docs.size} documentos compilados • DocMerge", 50f, 130f, subPaint)
            canvas.drawLine(50f, 170f, width - 50f, 170f, borderPaint)

            var y = 200f
            docs.forEachIndexed { index, doc ->
                if (y + heightPerItem - 40 > totalHeight) return@forEachIndexed

                // Card background
                val cardRect = android.graphics.RectF(50f, y, width - 50f, y + heightPerItem - 30f)
                canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)
                canvas.drawRoundRect(cardRect, 16f, 16f, borderPaint)

                // Item Title
                val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(37, 99, 235)
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("${index + 1}. ${doc.item.title}", 80f, y + 50f, itemTitlePaint)
                canvas.drawText("${doc.item.fileName} • ${doc.format.displayName} • ${FileStorageManager.formatFileSize(doc.item.fileSizeBytes)}", 80f, y + 85f, subPaint)

                // Preview lines
                val sampleLines = doc.text.lines().take(6)
                var lineY = y + 130f
                for (line in sampleLines) {
                    val trimmed = if (line.length > 70) line.take(67) + "..." else line
                    canvas.drawText(trimmed, 80f, lineY, textPaint)
                    lineY += 28f
                }

                y += heightPerItem
            }

            FileOutputStream(destFile).use { fos ->
                val format = if (options.targetFormat == SupportedFormat.JPG) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                bitmap.compress(format, 95, fos)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
