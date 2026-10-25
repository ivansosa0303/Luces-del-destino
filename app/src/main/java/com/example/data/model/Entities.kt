package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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
