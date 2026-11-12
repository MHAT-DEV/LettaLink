package com.example.domain.chronicle

import com.example.data.local.ChronicleDao
import com.example.data.local.ChronicleSyncHistoryEntity
import com.example.data.repository.AgentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LettaMemorySyncEngine(
    private val chronicleDao: ChronicleDao,
    private val agentRepo: AgentRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Synthesizes verified active facts into a concise Letta Core Memory block
     * and syncs it via real PATCH /v1/agents/{id}/core-memory/blocks/{block_label}.
     */
    suspend fun syncToCoreMemory(
        agentId: String,
        blockLabel: String = "human"
    ): ChronicleSyncHistoryEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val syncId = "sync-${UUID.randomUUID()}"

        val activeFacts = chronicleDao.getActiveFactsForAgent(agentId).firstOrNull() ?: emptyList()
        val entities = chronicleDao.getEntitiesForAgent(agentId).firstOrNull() ?: emptyList()

        if (activeFacts.isEmpty() && entities.isEmpty()) {
            val record = ChronicleSyncHistoryEntity(
                syncId = syncId,
                agentId = agentId,
                targetType = "CORE_MEMORY",
                blockLabel = blockLabel,
                factsCount = 0,
                syncStatus = "FAILED",
                details = "No active facts or entities to sync.",
                timestamp = now
            )
            chronicleDao.insertSyncHistory(record)
            return@withContext record
        }

        // Format compact, high-density facts for Core Memory
        val sb = StringBuilder()
        sb.appendLine("[Chronicle Temporal Knowledge - Updated ${dateFormat.format(Date(now))}]")
        activeFacts.take(15).forEach { f ->
            val fromStr = f.validFrom?.let { " (since ${dateFormat.format(Date(it))})" } ?: ""
            sb.appendLine("- ${f.subject} ${f.predicate.lowercase().replace('_', ' ')} ${f.objectValue}$fromStr")
        }

        try {
            val updatedBlock = agentRepo.updateCoreMemoryBlock(
                agentId = agentId,
                blockLabel = blockLabel,
                value = sb.toString().trim()
            )

            val record = ChronicleSyncHistoryEntity(
                syncId = syncId,
                agentId = agentId,
                targetType = "CORE_MEMORY",
                blockLabel = blockLabel,
                factsCount = activeFacts.size,
                syncStatus = "SUCCESS",
                details = "Successfully synced ${activeFacts.size} active facts to '$blockLabel' block (Block ID: ${updatedBlock.id ?: "ok"}).",
                timestamp = now
            )
            chronicleDao.insertSyncHistory(record)
            record
        } catch (e: Exception) {
            val record = ChronicleSyncHistoryEntity(
                syncId = syncId,
                agentId = agentId,
                targetType = "CORE_MEMORY",
                blockLabel = blockLabel,
                factsCount = activeFacts.size,
                syncStatus = "FAILED",
                details = "Server error: ${e.message}",
                timestamp = now
            )
            chronicleDao.insertSyncHistory(record)
            record
        }
    }

    /**
     * Pushes historical facts or knowledge snapshots into Letta Archival Memory
     * via real POST /v1/agents/{id}/archival-memory.
     */
    suspend fun archiveFact(
        agentId: String,
        factId: String
    ): ChronicleSyncHistoryEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val syncId = "sync-${UUID.randomUUID()}"
        val fact = chronicleDao.getFactById(factId)

        if (fact == null) {
            val record = ChronicleSyncHistoryEntity(
                syncId = syncId,
                agentId = agentId,
                targetType = "ARCHIVAL_MEMORY",
                blockLabel = "archival",
                factsCount = 0,
                syncStatus = "FAILED",
                details = "Fact not found: $factId",
                timestamp = now
            )
            chronicleDao.insertSyncHistory(record)
            return@withContext record
        }

        val fromStr = fact.validFrom?.let { dateFormat.format(Date(it)) } ?: "unknown"
        val toStr = fact.validTo?.let { dateFormat.format(Date(it)) } ?: "present"
        val passage = "Historical Fact: ${fact.subject} ${fact.predicate} ${fact.objectValue}. Valid from $fromStr to $toStr. Recorded at ${dateFormat.format(Date(fact.recordedAt))}. Source: ${fact.source}."

        try {
            agentRepo.insertArchivalMemory(agentId, passage)
            val record = ChronicleSyncHistoryEntity(
                syncId = syncId,
                agentId = agentId,
                targetType = "ARCHIVAL_MEMORY",
                blockLabel = "archival",
                factsCount = 1,
                syncStatus = "SUCCESS",
                details = "Archived fact to vector memory: '$passage'",
                timestamp = now
            )
            chronicleDao.insertSyncHistory(record)
            record
        } catch (e: Exception) {
            val record = ChronicleSyncHistoryEntity(
                syncId = syncId,
                agentId = agentId,
                targetType = "ARCHIVAL_MEMORY",
                blockLabel = "archival",
                factsCount = 1,
                syncStatus = "FAILED",
                details = "Failed archiving fact: ${e.message}",
                timestamp = now
            )
            chronicleDao.insertSyncHistory(record)
            record
        }
    }
}
