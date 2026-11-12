package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val providerType: String, // "SELF_HOSTED", "LETTA_CLOUD", "OPENAI_COMPATIBLE"
    val baseUrl: String,
    val apiKey: String = "",
    val isPrimary: Boolean = false,
    val isFavorite: Boolean = false,
    val lastHealthStatus: String = "UNCHECKED", // "HEALTHY", "DEGRADED", "OFFLINE", "UNCHECKED"
    val lastVersion: String = "",
    val lastLatencyMs: Long = 0L,
    val lastCheckedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long = 0,
    val command: String,
    val isSuccess: Boolean = true,
    val output: String,
    val executedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_audit_logs")
data class ApiAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long = 0,
    val method: String,
    val endpoint: String,
    val statusCode: Int,
    val durationMs: Long,
    val requestSnippet: String = "",
    val responseSnippet: String = "",
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_agents")
data class CachedAgentEntity(
    @PrimaryKey
    val agentId: String,
    val serverId: Long,
    val name: String,
    val description: String = "",
    val model: String = "",
    val systemPrompt: String = "",
    val tagsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tts_providers")
data class TtsProviderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val providerType: String, // "ANDROID_NATIVE", "OPENAI_COMPATIBLE", "CUSTOM_REST"
    val baseUrl: String = "",
    val endpoint: String = "v1/audio/speech",
    val apiKey: String = "",
    val model: String = "tts-1",
    val defaultVoice: String = "alloy",
    val audioFormat: String = "mp3",
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tts_settings")
data class TtsSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val autoReadAloudMode: String = "OFF", // "OFF", "EVERY_RESPONSE", "SELECTED_AGENT_ONLY", "MANUAL_ONLY"
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val autoLanguageDetection: Boolean = true,
    val defaultLanguage: String = "auto",
    val skipCodeBlocks: Boolean = true,
    val skipUrls: Boolean = true,
    val skipJsonMetadata: Boolean = true,
    val cacheEnabled: Boolean = true
)

// ==========================================
// LettaChronicle: Temporal Knowledge Graph & Markdown Documents
// ==========================================

@Entity(tableName = "chronicle_entities")
data class ChronicleEntityEntity(
    @PrimaryKey
    val entityId: String,
    val agentId: String,
    val entityType: String, // "PERSON", "ORGANIZATION", "LOCATION", "PROJECT", "CONCEPT", "TOOL", "EVENT", "SYSTEM", "DOCUMENT"
    val canonicalName: String,
    val aliasesJson: String = "[]",
    val description: String = "",
    val attributesJson: String = "{}",
    val status: String = "ACTIVE", // "ACTIVE", "DEPRECATED", "MERGED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chronicle_relationships")
data class ChronicleRelationshipEntity(
    @PrimaryKey
    val relationshipId: String,
    val agentId: String,
    val sourceEntityId: String,
    val relationshipType: String, // "WORKS_AT", "OWNS", "LOCATED_IN", "CREATED_BY", "MEMBER_OF", "DEPENDS_ON", "USES", "RELATED_TO", "REPLACED_BY", "PART_OF", "KNOWS", "PARTICIPATES_IN", "CUSTOM"
    val targetEntityId: String,
    val validFrom: Long? = null,
    val validTo: Long? = null,
    val recordedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val sourceRef: String = "",
    val status: String = "ACTIVE" // "ACTIVE", "SUPERSEDED", "CONTRADICTED"
)

@Entity(tableName = "chronicle_facts")
data class ChronicleFactEntity(
    @PrimaryKey
    val factId: String,
    val agentId: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val validFrom: Long? = null,
    val validTo: Long? = null,
    val recordedAt: Long = System.currentTimeMillis(),
    val source: String = "",
    val sourceType: String = "USER_MESSAGE", // "USER_MESSAGE", "AGENT_MESSAGE", "DOCUMENT", "MANUAL", "IMPORTED"
    val confidence: Float = 1.0f,
    val verificationStatus: String = "VERIFIED", // "VERIFIED", "UNVERIFIED", "CONTRADICTED"
    val supersedesFactId: String? = null,
    val contradictsFactId: String? = null,
    val metadataJson: String = "{}"
)

@Entity(tableName = "chronicle_conflicts")
data class ChronicleConflictEntity(
    @PrimaryKey
    val conflictId: String,
    val agentId: String,
    val conflictType: String, // "DIRECT_CONTRADICTION", "TEMPORAL_OVERLAP", "ATTRIBUTE_CONFLICT", "RELATIONSHIP_CONFLICT", "AMBIGUOUS_UPDATE"
    val factIdA: String,
    val factIdB: String,
    val description: String,
    val status: String = "OPEN", // "OPEN", "RESOLVED_A", "RESOLVED_B", "RESOLVED_BOTH_VALID", "DISMISSED"
    val detectedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chronicle_documents")
data class ChronicleDocumentEntity(
    @PrimaryKey
    val documentId: String,
    val agentId: String,
    val title: String,
    val fileName: String, // e.g. "profile.md", "projects.md"
    val content: String,
    val knowledgeRevision: Int = 1,
    val syncStatus: String = "SYNCHRONIZED", // "SYNCHRONIZED", "OUTDATED", "NEEDS_REVIEW", "CONFLICT"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chronicle_doc_versions")
data class ChronicleDocVersionEntity(
    @PrimaryKey
    val versionId: String,
    val documentId: String,
    val revisionNumber: Int,
    val content: String,
    val changeSummary: String = "",
    val committedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chronicle_sync_history")
data class ChronicleSyncHistoryEntity(
    @PrimaryKey
    val syncId: String,
    val agentId: String,
    val targetType: String, // "CORE_MEMORY", "ARCHIVAL_MEMORY"
    val blockLabel: String,
    val factsCount: Int,
    val syncStatus: String, // "SUCCESS", "FAILED", "REVIEW_REQUIRED"
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
