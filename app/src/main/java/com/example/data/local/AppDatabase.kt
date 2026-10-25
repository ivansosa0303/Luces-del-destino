package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity

@Database(
    entities = [
        ProjectEntity::class,
        DocumentItemEntity::class,
        CompilationRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
