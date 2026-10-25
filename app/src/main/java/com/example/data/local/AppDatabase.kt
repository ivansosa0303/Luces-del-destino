package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChapterEntity
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DailyWritingGoalEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.StoryBibleEntity

@Database(
    entities = [
        ProjectEntity::class,
        ChapterEntity::class,
        StoryBibleEntity::class,
        DailyWritingGoalEntity::class,
        DocumentItemEntity::class,
        CompilationRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun storyBibleDao(): StoryBibleDao
    abstract fun dailyWritingGoalDao(): DailyWritingGoalDao
    abstract fun documentItemDao(): DocumentItemDao
    abstract fun compilationRecordDao(): CompilationRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docmerge_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
