package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY isPrimary DESC, isFavorite DESC, id ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): ServerEntity?

    @Query("SELECT * FROM servers WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryServerFlow(): Flow<ServerEntity?>

    @Query("SELECT * FROM servers WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryServer(): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Query("UPDATE servers SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    @Query("UPDATE servers SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimary(id: Long)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getCount(): Int
}

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY executedAt DESC LIMIT :limit")
    fun getRecentCommands(limit: Int = 100): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearAll()
}

@Dao
interface ApiAuditLogDao {
    @Query("SELECT * FROM api_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 100): Flow<List<ApiAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: ApiAuditLogEntity): Long

    @Query("DELETE FROM api_audit_logs")
    suspend fun clearAll()
}

@Dao
interface AgentCacheDao {
    @Query("SELECT * FROM cached_agents WHERE serverId = :serverId ORDER BY name ASC")
    fun getAgentsForServer(serverId: Long): Flow<List<CachedAgentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgents(agents: List<CachedAgentEntity>)

    @Query("DELETE FROM cached_agents WHERE serverId = :serverId")
    suspend fun clearForServer(serverId: Long)

    @Query("DELETE FROM cached_agents WHERE agentId = :agentId")
    suspend fun deleteAgent(agentId: String)
}

@Dao
interface TtsProviderDao {
    @Query("SELECT * FROM tts_providers ORDER BY isDefault DESC, id ASC")
    fun getAllProviders(): Flow<List<TtsProviderEntity>>

    @Query("SELECT * FROM tts_providers WHERE id = :id LIMIT 1")
    suspend fun getProviderById(id: Long): TtsProviderEntity?

    @Query("SELECT * FROM tts_providers WHERE isDefault = 1 LIMIT 1")
    fun getDefaultProviderFlow(): Flow<TtsProviderEntity?>

    @Query("SELECT * FROM tts_providers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProvider(): TtsProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: TtsProviderEntity): Long

    @Update
    suspend fun updateProvider(provider: TtsProviderEntity)

    @Query("UPDATE tts_providers SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE tts_providers SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)

    @Query("DELETE FROM tts_providers WHERE id = :id")
    suspend fun deleteProvider(id: Long)

    @Query("SELECT COUNT(*) FROM tts_providers")
    suspend fun getCount(): Int
}

@Dao
interface TtsSettingsDao {
    @Query("SELECT * FROM tts_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<TtsSettingsEntity?>

    @Query("SELECT * FROM tts_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): TtsSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: TtsSettingsEntity)
}

@Dao
interface ChronicleDao {
    // Entities
    @Query("SELECT * FROM chronicle_entities WHERE agentId = :agentId ORDER BY canonicalName ASC")
    fun getEntitiesForAgent(agentId: String): Flow<List<ChronicleEntityEntity>>

    @Query("SELECT * FROM chronicle_entities WHERE entityId = :entityId LIMIT 1")
    suspend fun getEntityById(entityId: String): ChronicleEntityEntity?

    @Query("SELECT * FROM chronicle_entities WHERE agentId = :agentId AND (canonicalName = :name OR aliasesJson LIKE '%' || :name || '%') LIMIT 1")
    suspend fun findEntityByNameOrAlias(agentId: String, name: String): ChronicleEntityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(entity: ChronicleEntityEntity)

    @Update
    suspend fun updateEntity(entity: ChronicleEntityEntity)

    @Query("DELETE FROM chronicle_entities WHERE entityId = :entityId")
    suspend fun deleteEntity(entityId: String)

    // Relationships
    @Query("SELECT * FROM chronicle_relationships WHERE agentId = :agentId ORDER BY recordedAt DESC")
    fun getRelationshipsForAgent(agentId: String): Flow<List<ChronicleRelationshipEntity>>

    @Query("SELECT * FROM chronicle_relationships WHERE agentId = :agentId AND status = 'ACTIVE' ORDER BY recordedAt DESC")
    fun getActiveRelationshipsForAgent(agentId: String): Flow<List<ChronicleRelationshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: ChronicleRelationshipEntity)

    @Update
    suspend fun updateRelationship(relationship: ChronicleRelationshipEntity)

    // Facts
    @Query("SELECT * FROM chronicle_facts WHERE agentId = :agentId ORDER BY recordedAt DESC")
    fun getFactsForAgent(agentId: String): Flow<List<ChronicleFactEntity>>

    @Query("SELECT * FROM chronicle_facts WHERE agentId = :agentId AND validTo IS NULL ORDER BY recordedAt DESC")
    fun getActiveFactsForAgent(agentId: String): Flow<List<ChronicleFactEntity>>

    @Query("SELECT * FROM chronicle_facts WHERE factId = :factId LIMIT 1")
    suspend fun getFactById(factId: String): ChronicleFactEntity?

    @Query("SELECT * FROM chronicle_facts WHERE agentId = :agentId AND subject = :subject AND predicate = :predicate AND validTo IS NULL")
    suspend fun findActiveFactBySubjectPredicate(agentId: String, subject: String, predicate: String): List<ChronicleFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: ChronicleFactEntity)

    @Update
    suspend fun updateFact(fact: ChronicleFactEntity)

    // Conflicts
    @Query("SELECT * FROM chronicle_conflicts WHERE agentId = :agentId AND status = 'OPEN' ORDER BY detectedAt DESC")
    fun getOpenConflictsForAgent(agentId: String): Flow<List<ChronicleConflictEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflict(conflict: ChronicleConflictEntity)

    @Update
    suspend fun updateConflict(conflict: ChronicleConflictEntity)

    // Documents
    @Query("SELECT * FROM chronicle_documents WHERE agentId = :agentId ORDER BY updatedAt DESC")
    fun getDocumentsForAgent(agentId: String): Flow<List<ChronicleDocumentEntity>>

    @Query("SELECT * FROM chronicle_documents WHERE documentId = :documentId LIMIT 1")
    suspend fun getDocumentById(documentId: String): ChronicleDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ChronicleDocumentEntity)

    @Update
    suspend fun updateDocument(document: ChronicleDocumentEntity)

    @Query("DELETE FROM chronicle_documents WHERE documentId = :documentId")
    suspend fun deleteDocument(documentId: String)

    // Document Versions
    @Query("SELECT * FROM chronicle_doc_versions WHERE documentId = :documentId ORDER BY revisionNumber DESC")
    fun getVersionsForDocument(documentId: String): Flow<List<ChronicleDocVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocVersion(version: ChronicleDocVersionEntity)

    // Sync History
    @Query("SELECT * FROM chronicle_sync_history WHERE agentId = :agentId ORDER BY timestamp DESC LIMIT 50")
    fun getSyncHistoryForAgent(agentId: String): Flow<List<ChronicleSyncHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncHistory(history: ChronicleSyncHistoryEntity)
}
