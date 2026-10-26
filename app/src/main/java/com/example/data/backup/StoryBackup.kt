package com.example.data.backup

import com.example.data.model.ChapterEntity
import com.example.data.model.DailyWritingGoalEntity
import com.example.data.model.StoryBibleEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Copia de seguridad completa de una o varias novelas en un único JSON legible.
 *
 * Incluye capítulos, fichas narrativas y metas diarias. Los identificadores de
 * base de datos no se exportan: al importar se generan nuevos y se reasignan
 * las relaciones, de forma que el respaldo se puede restaurar en cualquier
 * instalación sin colisiones.
 */
data class BackupProject(
    val name: String,
    val genre: String,
    val synopsis: String,
    val targetWordCount: Int,
    val colorHex: String,
    val chapters: List<ChapterEntity>,
    val bible: List<StoryBibleEntity>,
    val goals: List<DailyWritingGoalEntity>
)

data class ImportSummary(
    val projects: Int,
    val chapters: Int,
    val bibleEntries: Int
)

object StoryBackup {

    const val SCHEMA = 1
    const val APP_TAG = "luces-del-destino"

    fun encode(
        projects: List<BackupProject>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("schema", SCHEMA)
        root.put("exportedAt", exportedAt)

        val projectsArray = JSONArray()
        projects.forEach { backup ->
            val projectJson = JSONObject()
            projectJson.put("name", backup.name)
            projectJson.put("genre", backup.genre)
            projectJson.put("synopsis", backup.synopsis)
            projectJson.put("targetWordCount", backup.targetWordCount)
            projectJson.put("colorHex", backup.colorHex)

            val chaptersArray = JSONArray()
            backup.chapters.sortedBy { it.chapterNumber }.forEach { chapter ->
                val c = JSONObject()
                c.put("chapterNumber", chapter.chapterNumber)
                c.put("orderIndex", chapter.orderIndex)
                c.put("title", chapter.title)
                c.put("content", chapter.content)
                c.put("summary", chapter.summary)
                c.put("wordCount", chapter.wordCount)
                c.put("status", chapter.status)
                c.put("povCharacter", chapter.povCharacter)
                c.put("targetWords", chapter.targetWords)
                c.put("notes", chapter.notes)
                chaptersArray.put(c)
            }
            projectJson.put("chapters", chaptersArray)

            val bibleArray = JSONArray()
            backup.bible.forEach { entry ->
                val b = JSONObject()
                b.put("category", entry.category)
                b.put("name", entry.name)
                b.put("roleOrLocation", entry.roleOrLocation)
                b.put("description", entry.description)
                b.put("appearanceOrDetails", entry.appearanceOrDetails)
                b.put("notes", entry.notes)
                b.put("status", entry.status)
                b.put("colorHex", entry.colorHex)
                bibleArray.put(b)
            }
            projectJson.put("bible", bibleArray)

            val goalsArray = JSONArray()
            backup.goals.forEach { goal ->
                val g = JSONObject()
                g.put("dateKey", goal.dateKey)
                g.put("wordsWritten", goal.wordsWritten)
                g.put("targetWords", goal.targetWords)
                goalsArray.put(g)
            }
            projectJson.put("goals", goalsArray)

            projectsArray.put(projectJson)
        }
        root.put("projects", projectsArray)
        return root.toString(2)
    }

    fun decode(json: String): List<BackupProject> {
        val root = JSONObject(json)
        val schema = root.optInt("schema", SCHEMA)
        require(schema <= SCHEMA) {
            "El respaldo fue creado por una versión más reciente (schema $schema)."
        }

        val projectsArray = root.optJSONArray("projects")
            ?: throw IllegalArgumentException("El archivo no contiene proyectos.")

        val result = mutableListOf<BackupProject>()
        for (i in 0 until projectsArray.length()) {
            val projectJson = projectsArray.optJSONObject(i) ?: continue

            val chapters = mutableListOf<ChapterEntity>()
            val chaptersArray = projectJson.optJSONArray("chapters")
            if (chaptersArray != null) {
                for (j in 0 until chaptersArray.length()) {
                    val c = chaptersArray.optJSONObject(j) ?: continue
                    val number = c.optInt("chapterNumber", j + 1)
                    chapters.add(
                        ChapterEntity(
                            id = 0,
                            projectId = 0,
                            chapterNumber = number,
                            title = c.optString("title", "Capítulo $number"),
                            content = c.optString("content", ""),
                            summary = c.optString("summary", ""),
                            wordCount = c.optInt("wordCount", 0),
                            status = c.optString("status", "BORRADOR"),
                            povCharacter = c.optString("povCharacter", ""),
                            orderIndex = c.optInt("orderIndex", number),
                            targetWords = c.optInt("targetWords", 2000),
                            notes = c.optString("notes", "")
                        )
                    )
                }
            }

            val bible = mutableListOf<StoryBibleEntity>()
            val bibleArray = projectJson.optJSONArray("bible")
            if (bibleArray != null) {
                for (j in 0 until bibleArray.length()) {
                    val b = bibleArray.optJSONObject(j) ?: continue
                    bible.add(
                        StoryBibleEntity(
                            id = 0,
                            projectId = 0,
                            category = b.optString("category", "PERSONAJE"),
                            name = b.optString("name", "Sin nombre"),
                            roleOrLocation = b.optString("roleOrLocation", ""),
                            description = b.optString("description", ""),
                            appearanceOrDetails = b.optString("appearanceOrDetails", ""),
                            notes = b.optString("notes", ""),
                            status = b.optString("status", "EN_DESARROLLO"),
                            colorHex = b.optString("colorHex", "#10B981")
                        )
                    )
                }
            }

            val goals = mutableListOf<DailyWritingGoalEntity>()
            val goalsArray = projectJson.optJSONArray("goals")
            if (goalsArray != null) {
                for (j in 0 until goalsArray.length()) {
                    val g = goalsArray.optJSONObject(j) ?: continue
                    val dateKey = g.optString("dateKey", "")
                    if (dateKey.isBlank()) continue
                    goals.add(
                        DailyWritingGoalEntity(
                            id = 0,
                            projectId = 0,
                            dateKey = dateKey,
                            wordsWritten = g.optInt("wordsWritten", 0),
                            targetWords = g.optInt("targetWords", 1000)
                        )
                    )
                }
            }

            result.add(
                BackupProject(
                    name = projectJson.optString("name", "Novela sin título"),
                    genre = projectJson.optString("genre", "Novela"),
                    synopsis = projectJson.optString("synopsis", ""),
                    targetWordCount = projectJson.optInt("targetWordCount", 50000),
                    colorHex = projectJson.optString("colorHex", "#10B981"),
                    chapters = chapters,
                    bible = bible,
                    goals = goals
                )
            )
        }

        if (result.isEmpty()) {
            throw IllegalArgumentException("El archivo no contiene novelas válidas.")
        }
        return result
    }
}
