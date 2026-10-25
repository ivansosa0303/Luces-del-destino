package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface DocumentItemDao {
    @Query("SELECT * FROM document_items ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllDocuments(): Flow<List<DocumentItemEntity>>

    @Query("SELECT * FROM document_items WHERE projectId = :projectId ORDER BY orderIndex ASC, createdAt ASC")
    fun getDocumentsByProject(projectId: Long): Flow<List<DocumentItemEntity>>

    @Query("SELECT * FROM document_items WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentItemEntity?

    @Query("SELECT * FROM document_items WHERE id IN (:ids)")
    suspend fun getDocumentsByIds(ids: List<Long>): List<DocumentItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentItemEntity>): List<Long>

    @Update
    suspend fun updateDocument(document: DocumentItemEntity)

    @Query("DELETE FROM document_items WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM document_items WHERE projectId = :projectId")
    suspend fun deleteDocumentsByProject(projectId: Long)
}

@Dao
interface CompilationRecordDao {
    @Query("SELECT * FROM compilation_records ORDER BY createdAt DESC")
    fun getAllCompilations(): Flow<List<CompilationRecordEntity>>

    @Query("SELECT * FROM compilation_records WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getCompilationsByProject(projectId: Long): Flow<List<CompilationRecordEntity>>

    @Query("SELECT * FROM compilation_records WHERE id = :id")
    suspend fun getCompilationById(id: Long): CompilationRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompilation(record: CompilationRecordEntity): Long

    @Query("DELETE FROM compilation_records WHERE id = :id")
    suspend fun deleteCompilationById(id: Long)
}
