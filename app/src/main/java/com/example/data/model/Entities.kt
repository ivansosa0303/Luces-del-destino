package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#10B981",
    val isStoryProject: Boolean = false,
    val genre: String = "Novela",
    val synopsis: String = "",
    val targetWordCount: Int = 50000,
    val currentWordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val chapterNumber: Int,
    val title: String,
    val content: String = "",
    val summary: String = "",
    val wordCount: Int = 0,
    val status: String = "BORRADOR", // "BORRADOR", "EN_REVISION", "PULIDO"
    val povCharacter: String = "",
    val orderIndex: Int = 0,
    val targetWords: Int = 2000,
    val notes: String = "", // Objetivo de escena, ganchos o notas
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "story_bible")
data class StoryBibleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val category: String = "PERSONAJE", // "PERSONAJE", "ESCENARIO", "TRAMA_SECUNDARIA", "OBJETO"
    val name: String,
    val roleOrLocation: String = "",
    val description: String = "", // Motivación (personaje), Atmósfera sensorial (escenario), Conflicto (trama)
    val appearanceOrDetails: String = "", // Rasgos físicos (personaje), Reglas de lore (escenario), Personajes implicados (trama)
    val notes: String = "", // Defecto/Arco (personaje), Secretos (escenario), Resolución/Notas (trama)
    val status: String = "EN_DESARROLLO", // Para tramas: PLANTEADA, EN_DESARROLLO, CLIMAX, RESUELTA; Para personajes: ACTIVO, SECUNDARIO
    val colorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_writing_goals")
data class DailyWritingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0,
    val dateKey: String, // e.g. "2026-10-25"
    val wordsWritten: Int = 0,
    val targetWords: Int = 1000
)

@Entity(tableName = "document_items")
data class DocumentItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0, // 0 for standalone / uncategorized
    val title: String,
    val fileName: String,
    val extension: String,
    val mimeType: String,
    val relativeFilePath: String, // relative to app files directory
    val fileSizeBytes: Long,
    val sha256Checksum: String = "",
    val textSummaryOrPreview: String = "",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "compilation_records")
data class CompilationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val projectName: String = "",
    val title: String,
    val outputFileName: String,
    val outputFormat: String, // pdf, md, json, docx, txt, zip, png
    val relativeFilePath: String,
    val fileSizeBytes: Long,
    val sha256Checksum: String = "",
    val sourceItemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Proyección (no es una tabla) para mostrar el número real de capítulos de
 * cada novela en la lista de proyectos. Antes la UI pasaba un `0` fijo.
 */
data class ProjectChapterCount(
    val projectId: Long,
    val chapterCount: Int
)
