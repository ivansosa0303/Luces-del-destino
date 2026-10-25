package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class SupportedFormat(
    val extension: String,
    val displayName: String,
    val mimeType: String,
    val badgeColor: Color,
    val isTextBased: Boolean,
    val description: String
) {
    TXT(
        extension = "txt",
        displayName = "Texto Plano",
        mimeType = "text/plain",
        badgeColor = Color(0xFF64748B), // Slate
        isTextBased = true,
        description = "Texto simple sin formato con codificación UTF-8 pura"
    ),
    MD(
        extension = "md",
        displayName = "Markdown",
        mimeType = "text/markdown",
        badgeColor = Color(0xFF0284C7), // Sky Blue
        isTextBased = true,
        description = "Texto estructurado con encabezados, listas, tablas y código"
    ),
    JSON(
        extension = "json",
        displayName = "JSON",
        mimeType = "application/json",
        badgeColor = Color(0xFFD97706), // Amber
        isTextBased = true,
        description = "Datos estructurados en objetos, matrices y atributos"
    ),
    PDF(
        extension = "pdf",
        displayName = "PDF",
        mimeType = "application/pdf",
        badgeColor = Color(0xFFDC2626), // Red
        isTextBased = false,
        description = "Documento vectorial portable con diseño fijo y páginas"
    ),
    DOCX(
        extension = "docx",
        displayName = "Word DOCX",
        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        badgeColor = Color(0xFF2563EB), // Royal Blue
        isTextBased = false,
        description = "Documento de Microsoft Word basado en OpenXML con estilos"
    ),
    PNG(
        extension = "png",
        displayName = "Imagen PNG",
        mimeType = "image/png",
        badgeColor = Color(0xFF059669), // Emerald
        isTextBased = false,
        description = "Imagen rasterizada sin pérdida con soporte de transparencia"
    ),
    JPG(
        extension = "jpg",
        displayName = "Imagen JPEG",
        mimeType = "image/jpeg",
        badgeColor = Color(0xFF0D9488), // Teal
        isTextBased = false,
        description = "Fotografía o imagen comprimida estándar"
    ),
    ZIP(
        extension = "zip",
        displayName = "Archivo ZIP",
        mimeType = "application/zip",
        badgeColor = Color(0xFF7C3AED), // Purple
        isTextBased = false,
        description = "Paquete comprimido con integridad total de archivos"
    );

    companion object {
        fun fromExtension(ext: String?): SupportedFormat {
            val cleanExt = ext?.lowercase()?.trim()?.removePrefix(".") ?: ""
            return when (cleanExt) {
                "txt" -> TXT
                "md", "markdown" -> MD
                "json" -> JSON
                "pdf" -> PDF
                "docx" -> DOCX
                "png" -> PNG
                "jpg", "jpeg" -> JPG
                "zip" -> ZIP
                else -> TXT
            }
        }

        fun fromFileName(fileName: String): SupportedFormat {
            val ext = fileName.substringAfterLast(".", "")
            return fromExtension(ext)
        }
    }
}
