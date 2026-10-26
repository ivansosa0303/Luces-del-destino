package com.example.engine

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DocxProcessor {

    /**
     * Extracts all paragraph text from a .docx file preserving paragraph breaks.
     */
    fun extractTextFromDocx(file: File): String {
        return try {
            FileInputStream(file).use { fis ->
                extractTextFromDocxStream(fis)
            }
        } catch (e: Exception) {
            "Error al leer documento DOCX: ${e.message}"
        }
    }

    fun extractTextFromDocxStream(inputStream: InputStream): String {
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry
        val textBuilder = StringBuilder()

        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xmlContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
                parseDocumentXml(xmlContent, textBuilder)
                break
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        return textBuilder.toString().trim()
    }

    private fun parseDocumentXml(xml: String, output: StringBuilder) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentParagraph = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    when (name) {
                        "p" -> {
                            currentParagraph = StringBuilder()
                        }
                        "t" -> {
                            val text = parser.nextText()
                            currentParagraph.append(text)
                        }
                        "tab" -> {
                            currentParagraph.append("\t")
                        }
                        "br", "cr" -> {
                            currentParagraph.append("\n")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p") {
                        val pText = currentParagraph.toString().trim()
                        if (pText.isNotEmpty()) {
                            output.append(pText).append("\n\n")
                        }
                        currentParagraph.clear()
                    }
                }
            }
            eventType = parser.next()
        }
    }

    data class DocxSection(
        val title: String,
        val subtitle: String? = null,
        val content: String,
        val isCodeBlock: Boolean = false
    )

    /**
     * Generates a fully valid ECMA-376 OpenXML .docx file with sections, styled headings, and paragraphs.
     */
    fun createDocxFile(destFile: File, documentTitle: String, sections: List<DocxSection>) {
        ZipOutputStream(FileOutputStream(destFile)).use { zipOut ->
            // 1. [Content_Types].xml
            zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
            zipOut.write(CONTENT_TYPES_XML.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 2. _rels/.rels
            zipOut.putNextEntry(ZipEntry("_rels/.rels"))
            zipOut.write(ROOT_RELS_XML.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 3. word/document.xml
            zipOut.putNextEntry(ZipEntry("word/document.xml"))
            val docXml = buildDocumentXml(documentTitle, sections)
            zipOut.write(docXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }
    }

    private fun buildDocumentXml(documentTitle: String, sections: List<DocxSection>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        // Main Title
        sb.append("<w:p>")
        sb.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
        sb.append("<w:r>")
        sb.append("<w:rPr><w:b/><w:sz w:val=\"48\"/><w:color w:val=\"1E293B\"/></w:rPr>")
        sb.append("<w:t>").append(escapeXml(documentTitle)).append("</w:t>")
        sb.append("</w:r>")
        sb.append("</w:p>")

        // Subtitle / Compiled Date
        sb.append("<w:p>")
        sb.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
        sb.append("<w:r>")
        sb.append("<w:rPr><w:i/><w:sz w:val=\"20\"/><w:color w:val=\"64748B\"/></w:rPr>")
        sb.append("<w:t>Luces del Destino · Manuscrito exportado sin conexión</w:t>")
        sb.append("</w:r>")
        sb.append("</w:p>")

        sb.append("<w:p><w:r><w:t></w:t></w:r></w:p>") // spacer

        // Sections
        sections.forEachIndexed { index, section ->
            // Section Divider / Heading 1
            sb.append("<w:p>")
            sb.append("<w:pPr><w:spacing w:before=\"240\" w:after=\"120\"/></w:pPr>")
            sb.append("<w:r>")
            sb.append("<w:rPr><w:b/><w:sz w:val=\"32\"/><w:color w:val=\"2563EB\"/></w:rPr>")
            sb.append("<w:t>${index + 1}. ").append(escapeXml(section.title)).append("</w:t>")
            sb.append("</w:r>")
            sb.append("</w:p>")

            if (!section.subtitle.isNullOrBlank()) {
                sb.append("<w:p>")
                sb.append("<w:pPr><w:spacing w:after=\"120\"/></w:pPr>")
                sb.append("<w:r>")
                sb.append("<w:rPr><w:i/><w:sz w:val=\"18\"/><w:color w:val=\"6B7280\"/></w:rPr>")
                sb.append("<w:t>").append(escapeXml(section.subtitle)).append("</w:t>")
                sb.append("</w:r>")
                sb.append("</w:p>")
            }

            // Paragraphs inside section
            val lines = section.content.split("\n")
            lines.forEach { line ->
                val trimmed = line.trimEnd()
                sb.append("<w:p>")
                sb.append("<w:pPr><w:spacing w:after=\"80\"/></w:pPr>")
                sb.append("<w:r>")
                if (section.isCodeBlock) {
                    sb.append("<w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/><w:sz w:val=\"18\"/><w:color w:val=\"334155\"/></w:rPr>")
                } else {
                    sb.append("<w:rPr><w:sz w:val=\"22\"/><w:color w:val=\"1E293B\"/></w:rPr>")
                }
                sb.append("<w:t xml:space=\"preserve\">").append(escapeXml(trimmed)).append("</w:t>")
                sb.append("</w:r>")
                sb.append("</w:p>")
            }

            // Section separator
            sb.append("<w:p><w:pPr><w:spacing w:after=\"180\"/></w:pPr><w:r><w:t></w:t></w:r></w:p>")
        }

        sb.append("</w:body>")
        sb.append("</w:document>")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    companion object {
        private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }
}
