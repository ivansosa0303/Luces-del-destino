package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChapterEntity
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DailyWritingGoalEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectChapterCount
import com.example.data.model.StoryBibleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isStoryProject = 1 ORDER BY updatedAt DESC")
    fun getStoryProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isStoryProject = 1 ORDER BY updatedAt DESC")
    suspend fun getStoryProjectsList(): List<ProjectEntity>

    @Query("SELECT projectId, COUNT(*) AS chapterCount FROM chapters GROUP BY projectId")
    fun getChapterCounts(): Flow<List<ProjectChapterCount>>

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
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY orderIndex ASC, chapterNumber ASC")
    fun getChaptersByProject(projectId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY orderIndex ASC, chapterNumber ASC")
    suspend fun getChaptersListByProject(projectId: Long): List<ChapterEntity>

    @Query("""
        SELECT * FROM chapters 
        WHERE projectId = :projectId 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR povCharacter LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY orderIndex ASC, chapterNumber ASC
    """)
    fun searchChapters(projectId: Long, query: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters WHERE projectId = :projectId")
    suspend fun countChaptersByProject(projectId: Long): Int

    @Query("SELECT COALESCE(SUM(wordCount), 0) FROM chapters WHERE projectId = :projectId")
    fun getTotalWordCountByProject(projectId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    @Query("DELETE FROM chapters WHERE projectId = :projectId")
    suspend fun deleteChaptersByProject(projectId: Long)
}

@Dao
interface StoryBibleDao {
    @Query("SELECT * FROM story_bible WHERE projectId = :projectId ORDER BY category ASC, name ASC")
    fun getBibleEntriesByProject(projectId: Long): Flow<List<StoryBibleEntity>>

    @Query("SELECT * FROM story_bible WHERE projectId = :projectId ORDER BY category ASC, name ASC")
    suspend fun getBibleEntriesList(projectId: Long): List<StoryBibleEntity>

    @Query("SELECT * FROM story_bible WHERE projectId = :projectId AND category = :category ORDER BY name ASC")
    fun getBibleEntriesByCategory(projectId: Long, category: String): Flow<List<StoryBibleEntity>>

    @Query("SELECT * FROM story_bible WHERE projectId = :projectId AND category = 'PERSONAJE' ORDER BY name ASC")
    fun getCharactersByProject(projectId: Long): Flow<List<StoryBibleEntity>>

    @Query("SELECT * FROM story_bible WHERE projectId = :projectId AND category = 'ESCENARIO' ORDER BY name ASC")
    fun getSettingsByProject(projectId: Long): Flow<List<StoryBibleEntity>>

    @Query("SELECT * FROM story_bible WHERE projectId = :projectId AND category = 'TRAMA_SECUNDARIA' ORDER BY updatedAt DESC")
    fun getSubplotsByProject(projectId: Long): Flow<List<StoryBibleEntity>>

    @Query("""
        SELECT * FROM story_bible 
        WHERE projectId = :projectId 
        AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR roleOrLocation LIKE '%' || :query || '%' OR appearanceOrDetails LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchBibleEntries(projectId: Long, query: String): Flow<List<StoryBibleEntity>>

    @Query("SELECT * FROM story_bible WHERE id = :id")
    suspend fun getBibleEntryById(id: Long): StoryBibleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBibleEntry(entry: StoryBibleEntity): Long

    @Update
    suspend fun updateBibleEntry(entry: StoryBibleEntity)

    @Query("DELETE FROM story_bible WHERE id = :id")
    suspend fun deleteBibleEntryById(id: Long)

    @Query("DELETE FROM story_bible WHERE projectId = :projectId")
    suspend fun deleteBibleEntriesByProject(projectId: Long)
}

@Dao
interface DailyWritingGoalDao {
    @Query("SELECT * FROM daily_writing_goals WHERE dateKey = :dateKey AND projectId = :projectId LIMIT 1")
    suspend fun getGoalByDate(dateKey: String, projectId: Long): DailyWritingGoalEntity?

    @Query("SELECT * FROM daily_writing_goals WHERE projectId = :projectId ORDER BY dateKey DESC LIMIT 7")
    fun getRecentGoals(projectId: Long): Flow<List<DailyWritingGoalEntity>>

    @Query("SELECT * FROM daily_writing_goals WHERE projectId = :projectId ORDER BY dateKey DESC")
    suspend fun getGoalsByProject(projectId: Long): List<DailyWritingGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: DailyWritingGoalEntity): Long

    @Query("UPDATE daily_writing_goals SET wordsWritten = wordsWritten + :deltaWords WHERE dateKey = :dateKey AND projectId = :projectId")
    suspend fun addWordsToToday(dateKey: String, projectId: Long, deltaWords: Int)

    @Query("DELETE FROM daily_writing_goals WHERE projectId = :projectId")
    suspend fun deleteGoalsByProject(projectId: Long)
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
