package com.example.engine

import android.content.Context
import com.example.data.model.SupportedFormat
import com.example.service.ConversionReport
import com.example.service.DefaultFileConverterService
import com.example.service.FileConverterService
import java.io.File

class DocumentConverter(
    context: Context,
    storageManager: FileStorageManager
) {
    private val converterService: FileConverterService = DefaultFileConverterService(context, storageManager)

    data class ConversionResult(
        val success: Boolean,
        val outputFile: File?,
        val targetFormat: SupportedFormat,
        val message: String,
        val sha256: String = "",
        val report: ConversionReport? = null
    )

    fun getAvailableConversions(sourceFormat: SupportedFormat): List<SupportedFormat> {
        return converterService.getAvailableConversions(sourceFormat)
    }

    fun convertFile(
        sourceFile: File,
        sourceFileName: String,
        sourceFormat: SupportedFormat,
        targetFormat: SupportedFormat
    ): ConversionResult {
        val baseName = sourceFileName.substringBeforeLast(".")
        val report = converterService.convert(sourceFile, sourceFormat, targetFormat, baseName)

        return ConversionResult(
            success = report.success,
            outputFile = report.outputFile,
            targetFormat = report.targetFormat,
            message = report.message,
            sha256 = report.targetMetrics?.sha256Checksum ?: "",
            report = report
        )
    }
}
