package com.example.domain.chronicle

enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    PROJECT,
    CONCEPT,
    TOOL,
    EVENT,
    SYSTEM,
    DOCUMENT
}

enum class RelationshipType {
    WORKS_AT,
    OWNS,
    LOCATED_IN,
    CREATED_BY,
    MEMBER_OF,
    DEPENDS_ON,
    USES,
    RELATED_TO,
    REPLACED_BY,
    PART_OF,
    KNOWS,
    PARTICIPATES_IN,
    CUSTOM
}

enum class SourceType {
    USER_MESSAGE,
    AGENT_MESSAGE,
    DOCUMENT,
    MANUAL,
    IMPORTED
}

enum class ConflictType {
    DIRECT_CONTRADICTION,
    TEMPORAL_OVERLAP,
    ATTRIBUTE_CONFLICT,
    RELATIONSHIP_CONFLICT,
    AMBIGUOUS_UPDATE
}

data class ParsedFactCandidate(
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val validFrom: Long? = null,
    val validTo: Long? = null,
    val sourceRef: String,
    val sourceType: SourceType = SourceType.USER_MESSAGE,
    val confidence: Float = 0.9f
)

data class GraphNode(
    val id: String,
    val label: String,
    val type: EntityType,
    val description: String,
    val x: Float = 0f,
    val y: Float = 0f
)

data class GraphEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val label: String,
    val validFrom: Long?,
    val validTo: Long?,
    val isSuperseded: Boolean = false
)
