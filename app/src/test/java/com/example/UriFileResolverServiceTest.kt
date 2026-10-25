package com.example

import android.content.Intent
import android.net.Uri
import com.example.data.model.SupportedFormat
import com.example.engine.FileStorageManager
import com.example.service.DefaultFileConverterService
import com.example.service.DefaultUriFileResolverService
import com.example.service.UriFileResolverService
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
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UriFileResolverServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context by lazy { RuntimeEnvironment.getApplication() }
    private val storageManager by lazy { FileStorageManager(context) }
    private val resolverService: UriFileResolverService by lazy {
        DefaultUriFileResolverService(context, storageManager)
    }
    private val converterService by lazy {
        DefaultFileConverterService(context, storageManager)
    }

    @Test
    fun testResolveFileUriAndVerifyIntegrity() {
        val sampleContent = "{\"key\": \"docmerge\", \"active\": true, \"items\": [1, 2, 3]}"
        val sourceFile = tempFolder.newFile("sample_config.json")
        sourceFile.writeText(sampleContent)

        val uri = Uri.fromFile(sourceFile)
        val result = resolverService.resolveUri(uri)

        assertTrue(result.isValid)
        assertEquals("sample_config.json", result.fileName)
        assertEquals("json", result.extension)
        assertEquals(SupportedFormat.JSON, result.format)
        assertEquals(sampleContent.toByteArray().size.toLong(), result.sizeBytes)
        assertNotNull(result.stagedFile)
        assertTrue(result.stagedFile!!.exists())

        // Verify SHA-256 exact match
        val expectedDigest = MessageDigest.getInstance("SHA-256")
            .digest(sampleContent.toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expectedDigest, result.sha256Checksum)

        // Verify that the staged file is ready for subsequent conversion
        val conversionReport = converterService.convert(
            sourceFile = result.stagedFile!!,
            sourceFormat = result.format,
            targetFormat = SupportedFormat.MD,
            customTitle = "Configuracion Convertida"
        )
        assertTrue(conversionReport.success)
        assertTrue(conversionReport.integrityVerified)
        assertNotNull(conversionReport.outputFile)
        assertTrue(conversionReport.outputFile!!.exists())
    }

    @Test
    fun testResolveActionSendIntent() {
        val noteContent = "# Reunion de Proyecto\n\n- Punto 1: Revision de conversion\n- Punto 2: ContentResolver"
        val noteFile = tempFolder.newFile("notas.md")
        noteFile.writeText(noteContent)

        val uri = Uri.fromFile(noteFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/markdown"
        }

        val results = resolverService.resolveIntent(intent)
        assertEquals(1, results.size)

        val resolved = results[0]
        assertTrue(resolved.isValid)
        assertEquals("notas.md", resolved.fileName)
        assertEquals("md", resolved.extension)
        assertEquals(SupportedFormat.MD, resolved.format)
        assertTrue(resolved.textPreview.contains("Reunion de Proyecto"))
    }
}
