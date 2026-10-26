package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfCompiler(private val context: Context) {

    data class PdfItem(
        val title: String,
        val formatName: String,
        val formatColorHex: Int,
        val subtitle: String,
        val textContent: String? = null,
        val isCode: Boolean = false,
        val imageFile: File? = null
    )

    fun compileToPdf(
        destFile: File,
        documentTitle: String,
        projectDescription: String,
        items: List<PdfItem>
    ): Boolean {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard width (pt)
            val pageHeight = 842 // A4 standard height (pt)
            val margin = 40f
            val contentWidth = pageWidth - (margin * 2)
            val bottomLimit = pageHeight - 50f

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = margin

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(37, 99, 235)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = Color.rgb(226, 232, 240)
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                val fPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(148, 163, 184)
                    textSize = 8f
                }
                c.drawLine(margin, pageHeight - 35f, pageWidth - margin, pageHeight - 35f, borderPaint)
                c.drawText("Luces del Destino • Compilación de Archivos Offline", margin, pageHeight - 22f, fPaint)
                val pageText = "Página $pNum"
                val pWidth = fPaint.measureText(pageText)
                c.drawText(pageText, pageWidth - margin - pWidth, pageHeight - 22f, fPaint)
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

            // --- Cover Header ---
            // Title
            canvas.drawText(documentTitle, margin, currentY + 20f, titlePaint)
            currentY += 28f

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val metaStr = "$dateStr • ${items.size} documentos unificados"
            canvas.drawText(metaStr, margin, currentY + 12f, subtitlePaint)
            currentY += 22f

            if (projectDescription.isNotBlank()) {
                val descLines = wrapText(projectDescription, subtitlePaint, contentWidth)
                descLines.forEach { line ->
                    canvas.drawText(line, margin, currentY + 12f, subtitlePaint)
                    currentY += 15f
                }
            }

            // Divider bar
            currentY += 10f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)
            currentY += 20f

            // --- Process Each Document Item ---
            items.forEachIndexed { index, item ->
                // Ensure room for section header (minimum 70pt)
                if (currentY + 70f > bottomLimit) {
                    canvas = advancePage()
                }

                // Format badge
                val badgeText = " ${item.formatName.uppercase(Locale.ROOT)} "
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val badgeWidth = badgePaint.measureText(badgeText) + 8f
                val badgeHeight = 16f

                bgPaint.color = item.formatColorHex
                val badgeRect = RectF(margin, currentY, margin + badgeWidth, currentY + badgeHeight)
                canvas.drawRoundRect(badgeRect, 4f, 4f, bgPaint)
                canvas.drawText(badgeText, margin + 4f, currentY + 12f, badgePaint)

                // Item Title
                val headerTitle = "${index + 1}. ${item.title}"
                canvas.drawText(headerTitle, margin + badgeWidth + 8f, currentY + 13f, headerPaint)
                currentY += 22f

                // Subtitle (File name, size, sha256)
                if (item.subtitle.isNotBlank()) {
                    canvas.drawText(item.subtitle, margin, currentY + 10f, subtitlePaint)
                    currentY += 18f
                }

                // If Image item
                if (item.imageFile != null && item.imageFile.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(item.imageFile.absolutePath)
                        if (bitmap != null) {
                            val imgWidth = bitmap.width.toFloat()
                            val imgHeight = bitmap.height.toFloat()
                            val scale = (contentWidth / imgWidth).coerceAtMost(350f / imgHeight)
                            val destW = imgWidth * scale
                            val destH = imgHeight * scale

                            if (currentY + destH + 20f > bottomLimit) {
                                canvas = advancePage()
                            }

                            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                            val destRect = RectF(margin, currentY, margin + destW, currentY + destH)
                            canvas.drawBitmap(bitmap, srcRect, destRect, null)
                            currentY += destH + 25f
                            bitmap.recycle()
                        }
                    } catch (_: Exception) {}
                }

                // If Text / Code item
                val text = item.textContent
                if (!text.isNullOrBlank()) {
                    val p = if (item.isCode) codePaint else textPaint
                    val lineHeight = if (item.isCode) 13f else 15f
                    val lines = text.split("\n")

                    // If code, draw a background box for the block
                    if (item.isCode) {
                        currentY += 5f
                    }

                    for (rawLine in lines) {
                        val wrapped = wrapText(rawLine, p, contentWidth - if (item.isCode) 16f else 0f)
                        val safeWrapped = if (wrapped.isEmpty()) listOf("") else wrapped

                        for (wLine in safeWrapped) {
                            if (currentY + lineHeight > bottomLimit) {
                                canvas = advancePage()
                            }

                            if (item.isCode) {
                                bgPaint.color = Color.rgb(241, 245, 249) // Slate 100
                                canvas.drawRect(margin, currentY - 2f, pageWidth - margin, currentY + lineHeight - 2f, bgPaint)
                                canvas.drawText(wLine, margin + 8f, currentY + 8f, p)
                            } else {
                                canvas.drawText(wLine, margin, currentY + 10f, p)
                            }
                            currentY += lineHeight
                        }
                    }
                    currentY += 15f
                }

                // Subtle item separator
                if (index < items.size - 1) {
                    if (currentY + 20f <= bottomLimit) {
                        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)
                        currentY += 20f
                    }
                }
            }

            // Draw final footer
            drawFooter(canvas, pageNumber)
            pdfDoc.finishPage(page)

            // Save to destFile
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
                // Check if single word exceeds max width
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
                    if (sub.isNotEmpty()) {
                        currentLine.append(sub)
                    }
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
