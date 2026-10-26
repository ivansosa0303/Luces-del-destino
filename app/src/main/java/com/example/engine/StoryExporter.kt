package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.ChapterEntity
import com.example.data.model.ProjectEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exportador de novelas: PDF maquetado (portada, índice, capítulos y numeración)
 * y DOCX compatible con Microsoft Word.
 *
 * Se mantiene independiente de [PdfCompiler] (pensado para documentos sueltos)
 * porque un manuscrito necesita otra jerarquía tipográfica y saltos de página
 * por capítulo.
 */
class StoryExporter {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 56f
    private val contentWidth = pageWidth - margin * 2
    private val bottomLimit = pageHeight - 64f

    fun exportPdf(
        destFile: File,
        project: ProjectEntity,
        chapters: List<ChapterEntity>
    ): Boolean {
        val ordered = chapters.sortedBy { it.chapterNumber }
        if (ordered.isEmpty()) return false

        return try {
            val doc = PdfDocument()
            var pageNumber = 0
            var currentPage: PdfDocument.Page? = null
            var currentCanvas: Canvas? = null
            var y = margin

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(24, 24, 27)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            val chapterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(17, 24, 39)
                textSize = 16f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val bookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(9, 13, 16)
                textSize = 26f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            val tocPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 11f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            fun drawFooter(target: Canvas, number: Int) {
                target.drawLine(margin, pageHeight - 46f, pageWidth - margin, pageHeight - 46f, footerPaint)
                target.drawText(project.name.take(70), margin, pageHeight - 30f, footerPaint)
                val label = "$number"
                val w = footerPaint.measureText(label)
                target.drawText(label, pageWidth - margin - w, pageHeight - 30f, footerPaint)
            }

            fun endPage() {
                val page = currentPage ?: return
                val canvas = currentCanvas
                if (canvas != null && pageNumber > 1) drawFooter(canvas, pageNumber)
                doc.finishPage(page)
                currentPage = null
                currentCanvas = null
            }

            fun startPage(): Canvas {
                endPage()
                pageNumber++
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = doc.startPage(info)
                currentPage = page
                currentCanvas = page.canvas
                y = margin
                return page.canvas
            }

            fun ensureSpace(target: Canvas, needed: Float): Canvas {
                return if (y + needed > bottomLimit) startPage() else target
            }

            fun drawWrapped(
                target: Canvas,
                text: String,
                paint: Paint,
                lineHeight: Float,
                indent: Float = 0f,
                center: Boolean = false
            ): Canvas {
                var canvas = target
                if (text.isBlank()) return canvas
                val usable = contentWidth - indent
                for (line in wrap(text, paint, usable)) {
                    canvas = ensureSpace(canvas, lineHeight)
                    val x = if (center) {
                        ((pageWidth - paint.measureText(line)) / 2f).coerceAtLeast(margin)
                    } else {
                        margin + indent
                    }
                    canvas.drawText(line, x, y + paint.textSize, paint)
                    y += lineHeight
                }
                return canvas
            }

            // ---------- Portada ----------
            var canvas = startPage()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            var coverY = 220f

            for (line in wrap(project.name, bookPaint, contentWidth)) {
                val w = bookPaint.measureText(line)
                canvas.drawText(line, (pageWidth - w) / 2f, coverY, bookPaint)
                coverY += 34f
            }

            coverY += 12f
            val genreW = metaPaint.measureText(project.genre.uppercase(Locale.getDefault()))
            canvas.drawText(
                project.genre.uppercase(Locale.getDefault()),
                (pageWidth - genreW) / 2f,
                coverY,
                metaPaint
            )
            coverY += 26f
            canvas.drawLine(pageWidth / 2f - 46f, coverY, pageWidth / 2f + 46f, coverY, footerPaint)
            coverY += 38f

            if (project.synopsis.isNotBlank()) {
                for (line in wrap(project.synopsis, metaPaint, contentWidth - 90f).take(16)) {
                    val w = metaPaint.measureText(line)
                    canvas.drawText(line, (pageWidth - w) / 2f, coverY, metaPaint)
                    coverY += 15f
                }
            }

            val totalWords = ordered.sumOf { it.wordCount }
            val coverInfo = "$totalWords palabras · ${ordered.size} capítulos · $dateStr"
            val coverInfoW = footerPaint.measureText(coverInfo)
            canvas.drawText(coverInfo, (pageWidth - coverInfoW) / 2f, pageHeight - 96f, footerPaint)

            // ---------- Índice ----------
            canvas = startPage()
            canvas.drawText("Índice", margin, y + 22f, chapterPaint)
            y += 46f
            ordered.forEachIndexed { index, chapter ->
                canvas = ensureSpace(canvas, 18f)
                val label = "${index + 1}.  ${chapter.title}".take(80)
                canvas.drawText(label, margin, y + 10f, tocPaint)
                y += 18f
            }

            // ---------- Capítulos ----------
            ordered.forEachIndexed { index, chapter ->
                canvas = startPage()
                canvas.drawText("Capítulo ${index + 1}", margin, y + 24f, chapterPaint)
                y += 34f

                if (chapter.title.isNotBlank()) {
                    canvas = drawWrapped(canvas, chapter.title, tocPaint, 16f)
                }
                if (chapter.povCharacter.isNotBlank()) {
                    canvas = drawWrapped(canvas, "Punto de vista: ${chapter.povCharacter}", metaPaint, 14f)
                }
                y += 16f

                val paragraphs = chapter.content
                    .split(Regex("\n+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (paragraphs.isEmpty()) {
                    canvas = drawWrapped(canvas, "(Capítulo sin contenido)", metaPaint, 15f)
                } else {
                    paragraphs.forEach { paragraph ->
                        val isDialogue = paragraph.startsWith("—") || paragraph.startsWith("–") ||
                            paragraph.startsWith("«") || paragraph.startsWith("\"")
                        canvas = drawWrapped(
                            target = canvas,
                            text = paragraph,
                            paint = bodyPaint,
                            lineHeight = 16f,
                            indent = if (isDialogue) 0f else 14f
                        )
                        y += 7f
                    }
                }
            }

            endPage()
            FileOutputStream(destFile).use { out -> doc.writeTo(out) }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportDocx(
        destFile: File,
        project: ProjectEntity,
        chapters: List<ChapterEntity>
    ): Boolean {
        val ordered = chapters.sortedBy { it.chapterNumber }
        if (ordered.isEmpty()) return false
        return try {
            val sections = ordered.mapIndexed { index, chapter ->
                DocxProcessor.DocxSection(
                    title = chapter.title.ifBlank { "Capítulo ${index + 1}" },
                    subtitle = if (chapter.povCharacter.isNotBlank()) {
                        "Punto de vista: ${chapter.povCharacter}"
                    } else null,
                    content = chapter.content
                )
            }
            DocxProcessor().createDocxFile(destFile, project.name, sections)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                result.add(current.toString())
                current.setLength(0)
            }
        }

        for (word in text.split(Regex("\\s+"))) {
            if (word.isEmpty()) continue
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current.setLength(0)
                current.append(candidate)
            } else {
                flush()
                if (paint.measureText(word) <= maxWidth) {
                    current.append(word)
                } else {
                    var chunk = StringBuilder()
                    for (ch in word) {
                        if (paint.measureText(chunk.toString() + ch) <= maxWidth) {
                            chunk.append(ch)
                        } else {
                            result.add(chunk.toString())
                            chunk = StringBuilder().append(ch)
                        }
                    }
                    current.append(chunk)
                }
            }
        }
        flush()
        return result
    }
}
