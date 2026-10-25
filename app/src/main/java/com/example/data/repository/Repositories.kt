package com.example.data.repository

import com.example.data.local.CompilationRecordDao
import com.example.data.local.DocumentItemDao
import com.example.data.local.ProjectDao
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.example.engine.FileStorageManager
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val documentItemDao: DocumentItemDao,
    private val storageManager: FileStorageManager
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun createProject(name: String, description: String = "", colorHex: String = "#3B82F6"): Long {
        val project = ProjectEntity(
            name = name,
            description = description,
            colorHex = colorHex
        )
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: Long) {
        // Find documents in project and delete their physical files
        // Note: For safe cascade, we also clear records
        documentItemDao.deleteDocumentsByProject(id)
        projectDao.deleteProjectById(id)
    }
}

class DocumentRepository(
    private val documentItemDao: DocumentItemDao,
    private val storageManager: FileStorageManager
) {
    val allDocuments: Flow<List<DocumentItemEntity>> = documentItemDao.getAllDocuments()

    fun getDocumentsByProject(projectId: Long): Flow<List<DocumentItemEntity>> =
        documentItemDao.getDocumentsByProject(projectId)

    suspend fun getDocumentById(id: Long): DocumentItemEntity? =
        documentItemDao.getDocumentById(id)

    suspend fun getDocumentsByIds(ids: List<Long>): List<DocumentItemEntity> =
        documentItemDao.getDocumentsByIds(ids)

    suspend fun importDocumentFromStream(
        inputStream: InputStream,
        originalFileName: String,
        projectId: Long = 0,
        title: String = originalFileName.substringBeforeLast(".")
    ): Long {
        val (file, sha256) = storageManager.saveStreamToDocument(inputStream, originalFileName)
        val ext = originalFileName.substringAfterLast(".", "").lowercase()
        val relativePath = "documents/${file.name}"

        // Generate quick preview if text-based
        val preview = when (ext) {
            "txt", "md", "json" -> storageManager.readTextSafely(file).take(500)
            else -> ""
        }

        val doc = DocumentItemEntity(
            projectId = projectId,
            title = title,
            fileName = originalFileName,
            extension = ext,
            mimeType = getMimeType(ext),
            relativeFilePath = relativePath,
            fileSizeBytes = file.length(),
            sha256Checksum = sha256,
            textSummaryOrPreview = preview
        )
        return documentItemDao.insertDocument(doc)
    }

    suspend fun createTextDocument(
        title: String,
        content: String,
        extension: String,
        projectId: Long = 0
    ): Long {
        val fileName = "${FileStorageManager.sanitizeFileName(title)}.$extension"
        val (file, sha256) = storageManager.saveTextToDocument(content, fileName)
        val relativePath = "documents/${file.name}"

        val doc = DocumentItemEntity(
            projectId = projectId,
            title = title,
            fileName = fileName,
            extension = extension,
            mimeType = getMimeType(extension),
            relativeFilePath = relativePath,
            fileSizeBytes = file.length(),
            sha256Checksum = sha256,
            textSummaryOrPreview = content.take(500)
        )
        return documentItemDao.insertDocument(doc)
    }

    suspend fun updateDocumentText(item: DocumentItemEntity, newContent: String) {
        val file = storageManager.getFileFromRelativePath(item.relativeFilePath)
        file.writeText(newContent, Charsets.UTF_8)
        val newSha256 = storageManager.calculateSha256(file)

        documentItemDao.updateDocument(
            item.copy(
                fileSizeBytes = file.length(),
                sha256Checksum = newSha256,
                textSummaryOrPreview = newContent.take(500),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteDocument(id: Long) {
        val doc = documentItemDao.getDocumentById(id)
        if (doc != null) {
            storageManager.deleteFileSafely(doc.relativeFilePath)
            documentItemDao.deleteDocumentById(id)
        }
    }

    suspend fun updateDocumentOrder(id: Long, newIndex: Int) {
        val doc = documentItemDao.getDocumentById(id)
        if (doc != null) {
            documentItemDao.updateDocument(doc.copy(orderIndex = newIndex))
        }
    }

    private fun getMimeType(ext: String): String {
        return when (ext) {
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}

class CompilationRepository(
    private val compilationDao: CompilationRecordDao,
    private val storageManager: FileStorageManager
) {
    val allCompilations: Flow<List<CompilationRecordEntity>> = compilationDao.getAllCompilations()

    suspend fun saveCompilationRecord(
        projectId: Long?,
        projectName: String,
        title: String,
        outputFileName: String,
        outputFormat: String,
        relativeFilePath: String,
        fileSizeBytes: Long,
        sha256Checksum: String,
        sourceItemCount: Int
    ): Long {
        val record = CompilationRecordEntity(
            projectId = projectId,
            projectName = projectName,
            title = title,
            outputFileName = outputFileName,
            outputFormat = outputFormat,
            relativeFilePath = relativeFilePath,
            fileSizeBytes = fileSizeBytes,
            sha256Checksum = sha256Checksum,
            sourceItemCount = sourceItemCount
        )
        return compilationDao.insertCompilation(record)
    }

    suspend fun deleteCompilation(id: Long) {
        val record = compilationDao.getCompilationById(id)
        if (record != null) {
            storageManager.deleteFileSafely(record.relativeFilePath)
            compilationDao.deleteCompilationById(id)
        }
    }
}
