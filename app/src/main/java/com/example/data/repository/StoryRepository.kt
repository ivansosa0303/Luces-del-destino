package com.example.data.repository

import com.example.data.local.ChapterDao
import com.example.data.local.DailyWritingGoalDao
import com.example.data.local.ProjectDao
import com.example.data.local.StoryBibleDao
import com.example.data.backup.BackupProject
import com.example.data.backup.ImportSummary
import com.example.data.backup.StoryBackup
import com.example.data.model.ChapterEntity
import com.example.data.model.DailyWritingGoalEntity
import com.example.data.model.ProjectChapterCount
import com.example.data.model.ProjectEntity
import com.example.data.model.StoryBibleEntity
import com.example.data.model.SupportedFormat
import com.example.engine.FileStorageManager
import com.example.engine.TextMetrics
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StoryRepository(
    private val projectDao: ProjectDao,
    private val chapterDao: ChapterDao,
    private val storyBibleDao: StoryBibleDao,
    private val dailyGoalDao: DailyWritingGoalDao,
    private val storageManager: FileStorageManager
) {

    fun getStoryProjects(): Flow<List<ProjectEntity>> = projectDao.getStoryProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun createStoryProject(
        title: String,
        genre: String = "Novela",
        synopsis: String = "",
        targetWordCount: Int = 50000,
        colorHex: String = "#10B981"
    ): Long {
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            name = title.trim(),
            description = synopsis.trim(),
            colorHex = colorHex,
            isStoryProject = true,
            genre = genre.trim().ifBlank { "Novela" },
            synopsis = synopsis.trim(),
            targetWordCount = targetWordCount,
            currentWordCount = 0,
            createdAt = now,
            updatedAt = now
        )
        return projectDao.insertProject(entity)
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteStoryProject(projectId: Long) {
        chapterDao.deleteChaptersByProject(projectId)
        storyBibleDao.deleteBibleEntriesByProject(projectId)
        // Sin esto quedaban metas diarias huérfanas que ya no pertenecían a
        // ninguna novela y engordaban la base de datos.
        dailyGoalDao.deleteGoalsByProject(projectId)
        projectDao.deleteProjectById(projectId)
    }

    // Chapters
    fun getChapters(projectId: Long): Flow<List<ChapterEntity>> =
        chapterDao.getChaptersByProject(projectId)

    fun searchChapters(projectId: Long, query: String): Flow<List<ChapterEntity>> =
        chapterDao.searchChapters(projectId, query.trim())

    suspend fun getChapterById(id: Long): ChapterEntity? = chapterDao.getChapterById(id)

    suspend fun createChapter(
        projectId: Long,
        title: String,
        content: String = "",
        summary: String = "",
        povCharacter: String = "",
        targetWords: Int = 2000,
        notes: String = ""
    ): Long {
        val currentCount = chapterDao.countChaptersByProject(projectId)
        val words = TextMetrics.countWords(content)
        val entity = ChapterEntity(
            projectId = projectId,
            chapterNumber = currentCount + 1,
            title = title.trim().ifBlank { "Capítulo ${currentCount + 1}" },
            content = content,
            summary = summary,
            wordCount = words,
            status = "BORRADOR",
            povCharacter = povCharacter,
            orderIndex = currentCount + 1,
            targetWords = targetWords,
            notes = notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = chapterDao.insertChapter(entity)
        refreshProjectWordCount(projectId)
        return id
    }

    /**
     * Cambia solo el estado editorial del capítulo.
     *
     * No reutiliza [updateChapter] a propósito: ese método escribe también el
     * contenido recibido, y la UI suele tener una copia antigua del texto. Un
     * toque en la etiqueta de estado podía sobrescribir lo recién escrito.
     */
    suspend fun updateChapterStatus(chapterId: Long, status: String) {
        val existing = chapterDao.getChapterById(chapterId) ?: return
        chapterDao.updateChapter(
            existing.copy(status = status, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateChapter(chapter: ChapterEntity) {
        val words = TextMetrics.countWords(chapter.content)
        chapterDao.updateChapter(
            chapter.copy(
                wordCount = words,
                updatedAt = System.currentTimeMillis()
            )
        )
        refreshProjectWordCount(chapter.projectId)
    }

    /**
     * Guardado real de un capítulo desde el editor.
     *
     * Relee la fila de la base de datos antes de escribir, de modo que no se
     * pisen cambios de metadatos (título, estado, POV) hechos mientras el
     * usuario escribía. Además calcula el delta de palabras y lo registra en la
     * meta diaria: antes ese delta siempre era 0 y la racha nunca subía.
     */
    suspend fun saveChapterContent(chapterId: Long, content: String): ChapterEntity? {
        val existing = chapterDao.getChapterById(chapterId) ?: return null
        val words = TextMetrics.countWords(content)
        val delta = (words - existing.wordCount).coerceAtLeast(0)
        val updated = existing.copy(
            content = content,
            wordCount = words,
            updatedAt = System.currentTimeMillis()
        )
        chapterDao.updateChapter(updated)
        refreshProjectWordCount(existing.projectId)
        if (delta > 0) {
            recordWritingProgress(existing.projectId, delta)
        }
        return updated
    }

    fun getChapterCounts(): Flow<List<ProjectChapterCount>> = projectDao.getChapterCounts()

    suspend fun updateChapterMetadata(
        chapterId: Long,
        title: String,
        summary: String,
        povCharacter: String,
        targetWords: Int,
        notes: String
    ) {
        val existing = chapterDao.getChapterById(chapterId) ?: return
        chapterDao.updateChapter(
            existing.copy(
                title = title.trim().ifBlank { existing.title },
                summary = summary.trim(),
                povCharacter = povCharacter.trim(),
                targetWords = targetWords,
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveChapterUp(projectId: Long, chapterId: Long): Boolean {
        val list = chapterDao.getChaptersListByProject(projectId).toMutableList()
        val index = list.indexOfFirst { it.id == chapterId }
        if (index <= 0) return false

        // Swap with previous
        val current = list[index]
        val prev = list[index - 1]
        list[index - 1] = current
        list[index] = prev

        // Reassign indices and chapter numbers sequentially
        val updated = list.mapIndexed { idx, chap ->
            chap.copy(
                orderIndex = idx + 1,
                chapterNumber = idx + 1,
                updatedAt = System.currentTimeMillis()
            )
        }
        chapterDao.updateChapters(updated)
        return true
    }

    suspend fun moveChapterDown(projectId: Long, chapterId: Long): Boolean {
        val list = chapterDao.getChaptersListByProject(projectId).toMutableList()
        val index = list.indexOfFirst { it.id == chapterId }
        if (index < 0 || index >= list.size - 1) return false

        // Swap with next
        val current = list[index]
        val next = list[index + 1]
        list[index + 1] = current
        list[index] = next

        // Reassign indices and chapter numbers sequentially
        val updated = list.mapIndexed { idx, chap ->
            chap.copy(
                orderIndex = idx + 1,
                chapterNumber = idx + 1,
                updatedAt = System.currentTimeMillis()
            )
        }
        chapterDao.updateChapters(updated)
        return true
    }

    suspend fun duplicateChapter(chapterId: Long): Long? {
        val origin = chapterDao.getChapterById(chapterId) ?: return null
        val count = chapterDao.countChaptersByProject(origin.projectId)
        val copyEntity = origin.copy(
            id = 0,
            chapterNumber = count + 1,
            title = "${origin.title} (Copia)",
            orderIndex = count + 1,
            status = "BORRADOR",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = chapterDao.insertChapter(copyEntity)
        refreshProjectWordCount(origin.projectId)
        return newId
    }

    suspend fun cycleChapterStatus(chapter: ChapterEntity): ChapterEntity {
        val nextStatus = when (chapter.status) {
            "BORRADOR" -> "EN_REVISION"
            "EN_REVISION" -> "PULIDO"
            else -> "BORRADOR"
        }
        val updated = chapter.copy(status = nextStatus, updatedAt = System.currentTimeMillis())
        chapterDao.updateChapter(updated)
        return updated
    }

    suspend fun deleteChapter(chapterId: Long, projectId: Long) {
        chapterDao.deleteChapterById(chapterId)
        // Renumber remaining chapters
        val remaining = chapterDao.getChaptersListByProject(projectId)
        val renumbered = remaining.mapIndexed { idx, chap ->
            chap.copy(orderIndex = idx + 1, chapterNumber = idx + 1)
        }
        chapterDao.updateChapters(renumbered)
        refreshProjectWordCount(projectId)
    }

    private suspend fun refreshProjectWordCount(projectId: Long) {
        val project = projectDao.getProjectById(projectId) ?: return
        val list = chapterDao.getChaptersListByProject(projectId)
        val totalWords = list.sumOf { it.wordCount }
        projectDao.updateProject(project.copy(currentWordCount = totalWords, updatedAt = System.currentTimeMillis()))
    }

    // Story Bible (Lore, Characters, Locations, Subplots)
    fun getBibleEntries(projectId: Long): Flow<List<StoryBibleEntity>> =
        storyBibleDao.getBibleEntriesByProject(projectId)

    fun getBibleEntriesByCategory(projectId: Long, category: String): Flow<List<StoryBibleEntity>> =
        storyBibleDao.getBibleEntriesByCategory(projectId, category)

    fun getCharacters(projectId: Long): Flow<List<StoryBibleEntity>> =
        storyBibleDao.getCharactersByProject(projectId)

    fun getSettings(projectId: Long): Flow<List<StoryBibleEntity>> =
        storyBibleDao.getSettingsByProject(projectId)

    fun getSubplots(projectId: Long): Flow<List<StoryBibleEntity>> =
        storyBibleDao.getSubplotsByProject(projectId)

    fun searchBibleEntries(projectId: Long, query: String): Flow<List<StoryBibleEntity>> =
        storyBibleDao.searchBibleEntries(projectId, query)

    suspend fun insertBibleEntry(entry: StoryBibleEntity): Long =
        storyBibleDao.insertBibleEntry(entry)

    suspend fun updateBibleEntry(entry: StoryBibleEntity) =
        storyBibleDao.updateBibleEntry(entry.copy(updatedAt = System.currentTimeMillis()))

    suspend fun cycleSubplotStatus(entry: StoryBibleEntity): StoryBibleEntity {
        val nextStatus = when (entry.status) {
            "PLANTEADA" -> "EN_DESARROLLO"
            "EN_DESARROLLO" -> "CLIMAX"
            "CLIMAX" -> "RESUELTA"
            else -> "PLANTEADA"
        }
        val updated = entry.copy(status = nextStatus, updatedAt = System.currentTimeMillis())
        storyBibleDao.updateBibleEntry(updated)
        return updated
    }

    suspend fun deleteBibleEntry(id: Long) =
        storyBibleDao.deleteBibleEntryById(id)

    // Daily Goals
    suspend fun recordWritingProgress(projectId: Long, deltaWords: Int) {
        if (deltaWords <= 0) return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existing = dailyGoalDao.getGoalByDate(todayKey, projectId)
        if (existing == null) {
            dailyGoalDao.insertOrUpdateGoal(
                DailyWritingGoalEntity(
                    projectId = projectId,
                    dateKey = todayKey,
                    wordsWritten = deltaWords,
                    targetWords = 1000
                )
            )
        } else {
            dailyGoalDao.addWordsToToday(todayKey, projectId, deltaWords)
        }
    }

    fun getRecentGoals(projectId: Long): Flow<List<DailyWritingGoalEntity>> =
        dailyGoalDao.getRecentGoals(projectId)

    // ---- Copia de seguridad completa (JSON) ----

    suspend fun exportBackupJson(): String {
        val projects = projectDao.getStoryProjectsList()
        val backups = projects.map { project ->
            BackupProject(
                name = project.name,
                genre = project.genre,
                synopsis = project.synopsis,
                targetWordCount = project.targetWordCount,
                colorHex = project.colorHex,
                chapters = chapterDao.getChaptersListByProject(project.id),
                bible = storyBibleDao.getBibleEntriesList(project.id),
                goals = dailyGoalDao.getGoalsByProject(project.id)
            )
        }
        return StoryBackup.encode(backups)
    }

    suspend fun importBackupJson(json: String): ImportSummary {
        val backups = StoryBackup.decode(json)
        var chapterTotal = 0
        var bibleTotal = 0
        val now = System.currentTimeMillis()

        backups.forEach { backup ->
            val projectId = projectDao.insertProject(
                ProjectEntity(
                    name = backup.name.ifBlank { "Novela restaurada" },
                    description = backup.synopsis,
                    colorHex = backup.colorHex,
                    isStoryProject = true,
                    genre = backup.genre.ifBlank { "Novela" },
                    synopsis = backup.synopsis,
                    targetWordCount = backup.targetWordCount,
                    currentWordCount = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )

            backup.chapters.forEach { chapter ->
                chapterDao.insertChapter(
                    chapter.copy(
                        id = 0,
                        projectId = projectId,
                        wordCount = if (chapter.wordCount > 0) {
                            chapter.wordCount
                        } else {
                            TextMetrics.countWords(chapter.content)
                        },
                        createdAt = now,
                        updatedAt = now
                    )
                )
                chapterTotal++
            }

            backup.bible.forEach { entry ->
                storyBibleDao.insertBibleEntry(
                    entry.copy(id = 0, projectId = projectId, createdAt = now, updatedAt = now)
                )
                bibleTotal++
            }

            backup.goals.forEach { goal ->
                dailyGoalDao.insertOrUpdateGoal(goal.copy(id = 0, projectId = projectId))
            }

            refreshProjectWordCount(projectId)
        }

        return ImportSummary(
            projects = backups.size,
            chapters = chapterTotal,
            bibleEntries = bibleTotal
        )
    }

    // Book Compilation helper
    fun buildBookManuscriptText(
        project: ProjectEntity,
        chapters: List<ChapterEntity>,
        format: SupportedFormat
    ): String {
        val sb = StringBuilder()
        when (format) {
            SupportedFormat.MD -> {
                sb.append("# ${project.name}\n\n")
                if (project.synopsis.isNotBlank()) {
                    sb.append("> ${project.synopsis}\n\n")
                }
                sb.append("**Género:** ${project.genre}\n\n---\n\n")
                chapters.sortedBy { it.chapterNumber }.forEach { chap ->
                    sb.append("## Capítulo ${chap.chapterNumber}: ${chap.title}\n\n")
                    if (chap.povCharacter.isNotBlank()) {
                        sb.append("*POV: ${chap.povCharacter}*\n\n")
                    }
                    sb.append(chap.content.trim())
                    sb.append("\n\n---\n\n")
                }
            }
            SupportedFormat.JSON -> {
                val jsonList = chapters.map { chap ->
                    """
                    {
                      "number": ${chap.chapterNumber},
                      "title": "${escapeJson(chap.title)}",
                      "pov": "${escapeJson(chap.povCharacter)}",
                      "status": "${chap.status}",
                      "words": ${chap.wordCount},
                      "content": "${escapeJson(chap.content)}"
                    }
                    """.trimIndent()
                }.joinToString(",\n")
                return """
                {
                  "title": "${escapeJson(project.name)}",
                  "genre": "${escapeJson(project.genre)}",
                  "synopsis": "${escapeJson(project.synopsis)}",
                  "targetWords": ${project.targetWordCount},
                  "chapters": [
                    $jsonList
                  ]
                }
                """.trimIndent()
            }
            else -> {
                // Standard TXT manuscript
                sb.append("=========================================\n")
                sb.append("${project.name.uppercase(Locale.ROOT)}\n")
                sb.append("Género: ${project.genre}\n")
                if (project.synopsis.isNotBlank()) {
                    sb.append("Sinopsis: ${project.synopsis}\n")
                }
                sb.append("=========================================\n\n\n")
                chapters.sortedBy { it.chapterNumber }.forEach { chap ->
                    sb.append("-----------------------------------------\n")
                    sb.append("CAPÍTULO ${chap.chapterNumber}: ${chap.title}\n")
                    if (chap.povCharacter.isNotBlank()) {
                        sb.append("[Punto de Vista: ${chap.povCharacter}]\n")
                    }
                    sb.append("-----------------------------------------\n\n")
                    sb.append(chap.content.trim())
                    sb.append("\n\n\n")
                }
                sb.append("### FIN ###\n")
            }
        }
        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
