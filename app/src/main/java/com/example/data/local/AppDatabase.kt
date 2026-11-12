package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ServerEntity::class,
        CommandHistoryEntity::class,
        ApiAuditLogEntity::class,
        CachedAgentEntity::class,
        TtsProviderEntity::class,
        TtsSettingsEntity::class,
        ChronicleEntityEntity::class,
        ChronicleRelationshipEntity::class,
        ChronicleFactEntity::class,
        ChronicleConflictEntity::class,
        ChronicleDocumentEntity::class,
        ChronicleDocVersionEntity::class,
        ChronicleSyncHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun apiAuditLogDao(): ApiAuditLogDao
    abstract fun agentCacheDao(): AgentCacheDao
    abstract fun ttsProviderDao(): TtsProviderDao
    abstract fun ttsSettingsDao(): TtsSettingsDao
    abstract fun chronicleDao(): ChronicleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lettalink_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
