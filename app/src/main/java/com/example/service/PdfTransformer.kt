package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfTransformer(private val context: Context) {

    private val pageWidth = 595 // A4 standard width in points
    private val pageHeight = 842 // A4 standard height in points
    private val margin = 40f
    private val contentWidth = pageWidth - (margin * 2)
    private val bottomLimit = pageHeight - 50f

    fun renderTextToPdf(destFile: File, title: String, content: String, isCode: Boolean = false): Boolean {
        return try {
            val pdfDoc = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = margin

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42) // Slate 900
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139) // Slate 500
                textSize = 9f
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isCode) Color.rgb(51, 65, 85) else Color.rgb(30, 41, 59)
                textSize = if (isCode) 9f else 10f
                typeface = if (isCode) Typeface.MONOSPACE else Typeface.DEFAULT
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                c.drawLine(margin, pageHeight - 35f, pageWidth - margin, pageHeight - 35f, borderPaint)
                c.drawText("Luces del Destino • Transformación de Documentos", margin, pageHeight - 20f, metaPaint)
                val pageText = "Página $pNum"
                val pWidth = metaPaint.measureText(pageText)
                c.drawText(pageText, pageWidth - margin - pWidth, pageHeight - 20f, metaPaint)
            }

            fun advancePage(): Canvas {
                drawFooter(canvas, pageNumber)
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                currentY = margin
                return page.canvas
            }

            // Header
            canvas.drawText(title, margin, currentY + 18f, titlePaint)
            currentY += 24f

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Transformación de Archivo • $dateStr", margin, currentY + 10f, metaPaint)
            currentY += 18f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)
            currentY += 16f

            val lineHeight = if (isCode) 13f else 15f
            val lines = content.lines()

            for (rawLine in lines) {
                val wrapped = wrapText(rawLine, textPaint, contentWidth)
                val safeWrapped = if (wrapped.isEmpty()) listOf("") else wrapped

                for (wLine in safeWrapped) {
                    if (currentY + lineHeight > bottomLimit) {
                        canvas = advancePage()
                    }

                    if (isCode) {
                        bgPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(margin, currentY - 2f, pageWidth - margin, currentY + lineHeight - 2f, bgPaint)
                        canvas.drawText(wLine, margin + 4f, currentY + 8f, textPaint)
                    } else {
                        canvas.drawText(wLine, margin, currentY + 10f, textPaint)
                    }
                    currentY += lineHeight
                }
            }

            drawFooter(canvas, pageNumber)
            pdfDoc.finishPage(page)

            FileOutputStream(destFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun renderMarkdownToPdf(destFile: File, title: String, markdown: String): Boolean {
        return try {
            val pdfDoc = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = margin

            val h1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val h2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(37, 99, 235)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
                typeface = Typeface.MONOSPACE
            }

            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9f
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                c.drawLine(margin, pageHeight - 35f, pageWidth - margin, pageHeight - 35f, borderPaint)
                c.drawText("Luces del Destino • Transformación Markdown a PDF", margin, pageHeight - 20f, metaPaint)
                val pageText = "Página $pNum"
                val pWidth = metaPaint.measureText(pageText)
                c.drawText(pageText, pageWidth - margin - pWidth, pageHeight - 20f, metaPaint)
            }

            fun advancePage(): Canvas {
                drawFooter(canvas, pageNumber)
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                currentY = margin
                return page.canvas
            }

            // Document Header
            canvas.drawText(title, margin, currentY + 18f, h1Paint)
            currentY += 26f
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generado desde Markdown • $dateStr", margin, currentY + 10f, metaPaint)
            currentY += 18f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)
            currentY += 16f

            var inCodeBlock = false
            val lines = markdown.lines()

            for (line in lines) {
                val trimmed = line.trim()

                if (trimmed.startsWith("```")) {
                    inCodeBlock = !inCodeBlock
                    currentY += 4f
                    continue
                }

                if (inCodeBlock) {
                    if (currentY + 14f > bottomLimit) canvas = advancePage()
                    bgPaint.color = Color.rgb(241, 245, 249)
                    canvas.drawRect(margin, currentY - 2f, pageWidth - margin, currentY + 12f, bgPaint)
                    canvas.drawText(line, margin + 6f, currentY + 8f, codePaint)
                    currentY += 14f
                    continue
                }

                if (trimmed.startsWith("# ")) {
                    if (currentY + 28f > bottomLimit) canvas = advancePage()
                    currentY += 8f
                    canvas.drawText(trimmed.removePrefix("# ").trim(), margin, currentY + 16f, h1Paint)
                    currentY += 24f
                    continue
                }

                if (trimmed.startsWith("## ")) {
                    if (currentY + 22f > bottomLimit) canvas = advancePage()
                    currentY += 6f
                    canvas.drawText(trimmed.removePrefix("## ").trim(), margin, currentY + 13f, h2Paint)
                    currentY += 20f
                    continue
                }

                if (trimmed.startsWith("### ")) {
                    if (currentY + 18f > bottomLimit) canvas = advancePage()
                    currentY += 4f
                    canvas.drawText(trimmed.removePrefix("### ").trim(), margin, currentY + 11f, h2Paint)
                    currentY += 18f
                    continue
                }

                // Normal paragraph or bullet list
                val isBullet = trimmed.startsWith("- ") || trimmed.startsWith("* ")
                val cleanLine = if (isBullet) "•  ${trimmed.substring(2)}" else line
                val wrapped = wrapText(cleanLine, bodyPaint, contentWidth)
                val safeWrapped = if (wrapped.isEmpty()) listOf("") else wrapped

                for (wLine in safeWrapped) {
                    if (currentY + 15f > bottomLimit) canvas = advancePage()
                    canvas.drawText(wLine, margin, currentY + 10f, bodyPaint)
                    currentY += 15f
                }
            }

            drawFooter(canvas, pageNumber)
            pdfDoc.finishPage(page)

            FileOutputStream(destFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun extractTextFromPdf(pdfFile: File): String {
        return try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            renderer.close()
            pfd.close()

            // Extract plain text metadata and byte content
            val bytes = pdfFile.readBytes()
            val textStreamBuilder = StringBuilder()

            // Parse text objects in PDF streams: BT (Begin Text) to ET (End Text)
            val rawPdfString = String(bytes, Charsets.ISO_8859_1)
            val textRegex = Regex("\\((.*?)\\)\\s*Tj", RegexOption.DOT_MATCHES_ALL)
            val matches = textRegex.findAll(rawPdfString)

            for (m in matches) {
                val extracted = m.groupValues[1]
                if (extracted.isNotBlank()) {
                    textStreamBuilder.append(extracted).append(" ")
                }
            }

            val extractedStream = textStreamBuilder.toString().trim()

            if (extractedStream.isNotBlank()) {
                "DOCUMENTO PDF: ${pdfFile.name} ($pageCount páginas)\n\n$extractedStream"
            } else {
                "DOCUMENTO PDF: ${pdfFile.name}\nTotal de Páginas: $pageCount\nTamaño: ${pdfFile.length()} bytes\n(Documento gráfico vectorial indexado)"
            }
        } catch (e: Exception) {
            "Documento PDF: ${pdfFile.name} (Error al analizar: ${e.message})"
        }
    }

    fun extractPdfToJson(pdfFile: File): JSONObject {
        val root = JSONObject()
        root.put("format", "pdf")
        root.put("fileName", pdfFile.name)
        root.put("fileSizeBytes", pdfFile.length())

        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            root.put("pageCount", count)
            val pagesArray = JSONArray()

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val pageObj = JSONObject().apply {
                    put("pageNumber", i + 1)
                    put("widthPt", page.width)
                    put("heightPt", page.height)
                }
                page.close()
                pagesArray.put(pageObj)
            }

            renderer.close()
            pfd.close()
            root.put("pages", pagesArray)
        } catch (e: Exception) {
            root.put("error", e.message)
        }

        return root
    }

    fun extractPdfToMarkdown(pdfFile: File): String {
        val sb = StringBuilder()
        sb.append("# Documento PDF: ").append(pdfFile.name).append("\n\n")

        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            sb.append("> **Total de páginas:** ").append(count).append("  \n")
            sb.append("> **Tamaño:** ").append(pdfFile.length()).append(" bytes\n\n")

            sb.append("## Índice de Páginas\n\n")
            for (i in 0 until count) {
                val page = renderer.openPage(i)
                sb.append("- **Página ").append(i + 1).append("**: ")
                    .append(page.width).append("x").append(page.height).append(" pt\n")
                page.close()
            }
            sb.append("\n---\n\n")

            renderer.close()
            pfd.close()

            val textExtract = extractTextFromPdf(pdfFile)
            sb.append("## Contenido Extraído\n\n")
            sb.append(textExtract).append("\n")
        } catch (e: Exception) {
            sb.append("Error al procesar páginas PDF: ").append(e.message).append("\n")
        }

        return sb.toString()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                if (paint.measureText(word) > maxWidth) {
                    var sub = ""
                    for (ch in word) {
                        if (paint.measureText(sub + ch) <= maxWidth) {
                            sub += ch
                        } else {
                            result.add(sub)
                            sub = ch.toString()
                        }
                    }
                    if (sub.isNotEmpty()) currentLine.append(sub)
                } else {
                    currentLine.append(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }
}
