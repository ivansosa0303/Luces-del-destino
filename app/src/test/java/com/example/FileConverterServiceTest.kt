package com.example

import com.example.data.model.SupportedFormat
import com.example.engine.FileStorageManager
import com.example.service.ContentMetrics
import com.example.service.DefaultFileConverterService
import com.example.service.JsonTransformer
import com.example.service.MarkdownTransformer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileConverterServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context by lazy { RuntimeEnvironment.getApplication() }
    private val storageManager by lazy { FileStorageManager(context) }
    private val converterService by lazy { DefaultFileConverterService(context, storageManager) }

    private val markdownTransformer = MarkdownTransformer()
    private val jsonTransformer = JsonTransformer()

    @Test
    fun testContentMetricsIntegrity() {
        val sampleText = "Hello world, this is a test line.\nSecond line with data."
        val metrics = ContentMetrics.fromText(sampleText)

        assertEquals(2, metrics.lineCount)
        assertEquals(11, metrics.wordCount)
        assertTrue(metrics.characterCount > 0)
        assertEquals(64, metrics.sha256Checksum.length)
    }

    @Test
    fun testTxtToMdConversion() {
        val txtFile = tempFolder.newFile("sample.txt")
        val content = "Primer párrafo de texto.\nSegundo párrafo con notas:\n- Item A\n- Item B"
        txtFile.writeText(content)

        val report = converterService.convert(
            sourceFile = txtFile,
            sourceFormat = SupportedFormat.TXT,
            targetFormat = SupportedFormat.MD,
            customTitle = "Documento Demo"
        )

        assertTrue(report.success)
        assertTrue(report.integrityVerified)
        assertNotNull(report.outputFile)
        assertTrue(report.outputFile!!.exists())

        val mdContent = report.outputFile!!.readText()
        assertTrue(mdContent.contains("# Documento Demo"))
        assertTrue(mdContent.contains("Primer párrafo de texto."))
        assertTrue(mdContent.contains("- Item A"))
    }

    @Test
    fun testTxtToJsonConversion() {
        val txtFile = tempFolder.newFile("config.txt")
        val content = "autor: Alice\nversion: 2.0\nestado: activo"
        txtFile.writeText(content)

        val report = converterService.convert(
            sourceFile = txtFile,
            sourceFormat = SupportedFormat.TXT,
            targetFormat = SupportedFormat.JSON,
            customTitle = "Configuracion"
        )

        assertTrue(report.success)
        assertNotNull(report.outputFile)

        val jsonString = report.outputFile!!.readText()
        assertTrue(jsonString.contains("\"format\": \"plain_text\""))
        assertTrue(jsonString.contains("\"lineCount\": 3"))
        assertTrue(jsonString.contains("\"detectedProperties\""))
    }

    @Test
    fun testMdToTxtStripper() {
        val md = "# Título Principal\n\nEste es un texto con **negrita** y *cursiva*.\n\n```json\n{\"id\": 1}\n```"
        val plain = markdownTransformer.stripMarkdownToPlainText(md)

        assertTrue(plain.contains("TÍTULO PRINCIPAL"))
        assertTrue(plain.contains("Este es un texto con negrita y cursiva."))
        assertTrue(plain.contains("{\"id\": 1}"))
    }

    @Test
    fun testMdToJsonAST() {
        val md = "# Sección Uno\nTexto de sección uno.\n\n## Subsección Dos\nTexto de subsección dos."
        val json = markdownTransformer.parseMarkdownToJson(md, "Manual")

        assertEquals("Manual", json.getString("title"))
        val sections = json.getJSONArray("sections")
        assertTrue(sections.length() >= 2)
    }

    @Test
    fun testJsonToMdTable() {
        val jsonArray = """[
            {"nombre": "Doc A", "tipo": "pdf", "tamano": 1024},
            {"nombre": "Doc B", "tipo": "md", "tamano": 2048}
        ]"""

        val md = jsonTransformer.jsonToMarkdown(jsonArray, "Inventario")
        assertTrue(md.contains("# Inventario"))
        assertTrue(md.contains("| nombre | tipo | tamano |"))
        assertTrue(md.contains("| Doc A | pdf | 1024 |"))
        assertTrue(md.contains("```json"))
    }
}
