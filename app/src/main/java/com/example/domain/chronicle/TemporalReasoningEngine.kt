package com.example.domain.chronicle

import com.example.data.local.ChronicleConflictEntity
import com.example.data.local.ChronicleDao
import com.example.data.local.ChronicleEntityEntity
import com.example.data.local.ChronicleFactEntity
import com.example.data.local.ChronicleRelationshipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class TemporalReasoningEngine(
    private val chronicleDao: ChronicleDao
) {

    /**
     * Ingests a new temporal fact candidate with non-destructive versioning,
     * entity resolution, and conflict detection.
     */
    suspend fun ingestFact(
        agentId: String,
        candidate: ParsedFactCandidate
    ): ChronicleFactEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val subjectClean = candidate.subject.trim()
        val predicateClean = candidate.predicate.trim().uppercase().replace(" ", "_")
        val objectClean = candidate.objectValue.trim()

        // 1. Entity Resolution for Subject
        var subjectEntity = chronicleDao.findEntityByNameOrAlias(agentId, subjectClean)
        if (subjectEntity == null) {
            val newEnt = ChronicleEntityEntity(
                entityId = "ent-${UUID.randomUUID()}",
                agentId = agentId,
                entityType = inferEntityType(subjectClean),
                canonicalName = subjectClean,
                aliasesJson = "[]",
                description = "Entity discovered in conversation",
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )
            chronicleDao.insertEntity(newEnt)
            subjectEntity = newEnt
        }

        // 2. Entity Resolution for Object (if object looks like a named entity)
        var objectEntity: ChronicleEntityEntity? = null
        if (objectClean.length > 2 && objectClean.first().isUpperCase()) {
            objectEntity = chronicleDao.findEntityByNameOrAlias(agentId, objectClean)
            if (objectEntity == null) {
                val newEnt = ChronicleEntityEntity(
                    entityId = "ent-${UUID.randomUUID()}",
                    agentId = agentId,
                    entityType = inferEntityType(objectClean),
                    canonicalName = objectClean,
                    aliasesJson = "[]",
                    description = "Entity referenced in knowledge fact",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                )
                chronicleDao.insertEntity(newEnt)
                objectEntity = newEnt
            }
        }

        // 3. Temporal Reasoning & Conflict Detection against active facts
        val activeFacts = chronicleDao.findActiveFactBySubjectPredicate(agentId, subjectClean, predicateClean)
        var supersedesId: String? = null
        var contradictsId: String? = null

        for (oldFact in activeFacts) {
            if (oldFact.objectValue.equals(objectClean, ignoreCase = true)) {
                // Exact duplicate / reaffirmation
                return@withContext oldFact
            } else {
                // Different value for same subject + predicate
                val newValidFrom = candidate.validFrom ?: now

                // Check for temporal contradiction
                if (oldFact.validFrom != null && oldFact.validFrom <= newValidFrom && (oldFact.validTo == null || oldFact.validTo > newValidFrom)) {
                    // Overlapping claim -> potential conflict or update
                    val isExplicitUpdate = candidate.sourceType == SourceType.USER_MESSAGE || candidate.validFrom != null

                    if (isExplicitUpdate) {
                        // Mark old fact superseded as of new fact's validFrom
                        val updatedOldFact = oldFact.copy(validTo = newValidFrom)
                        chronicleDao.updateFact(updatedOldFact)
                        supersedesId = oldFact.factId
                    } else {
                        // Ambiguous collision -> flag conflict
                        contradictsId = oldFact.factId
                        val conflict = ChronicleConflictEntity(
                            conflictId = "conf-${UUID.randomUUID()}",
                            agentId = agentId,
                            conflictType = ConflictType.TEMPORAL_OVERLAP.name,
                            factIdA = oldFact.factId,
                            factIdB = "pending",
                            description = "Contradiction on $subjectClean $predicateClean: '${oldFact.objectValue}' vs '$objectClean'",
                            status = "OPEN",
                            detectedAt = now
                        )
                        chronicleDao.insertConflict(conflict)
                    }
                } else {
                    val updatedOldFact = oldFact.copy(validTo = newValidFrom)
                    chronicleDao.updateFact(updatedOldFact)
                    supersedesId = oldFact.factId
                }
            }
        }

        // 4. Create and Insert New Fact
        val newFactId = "fact-${UUID.randomUUID()}"
        val newFact = ChronicleFactEntity(
            factId = newFactId,
            agentId = agentId,
            subject = subjectClean,
            predicate = predicateClean,
            objectValue = objectClean,
            validFrom = candidate.validFrom ?: now,
            validTo = candidate.validTo,
            recordedAt = now,
            source = candidate.sourceRef,
            sourceType = candidate.sourceType.name,
            confidence = candidate.confidence,
            verificationStatus = if (contradictsId != null) "CONTRADICTED" else "VERIFIED",
            supersedesFactId = supersedesId,
            contradictsFactId = contradictsId
        )
        chronicleDao.insertFact(newFact)

        // 5. Create or Update Relationship if object was an entity
        if (objectEntity != null) {
            val rel = ChronicleRelationshipEntity(
                relationshipId = "rel-${UUID.randomUUID()}",
                agentId = agentId,
                sourceEntityId = subjectEntity.entityId,
                relationshipType = predicateClean,
                targetEntityId = objectEntity.entityId,
                validFrom = candidate.validFrom ?: now,
                validTo = candidate.validTo,
                recordedAt = now,
                confidence = candidate.confidence,
                sourceRef = candidate.sourceRef,
                status = "ACTIVE"
            )
            chronicleDao.insertRelationship(rel)
        }

        newFact
    }

    /**
     * Resolves an open conflict with user decision.
     */
    suspend fun resolveConflict(conflictId: String, resolutionChoice: String) = withContext(Dispatchers.IO) {
        val conflicts = chronicleDao.getOpenConflictsForAgent("")
        // DAO update or mark resolved
    }

    private fun inferEntityType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("corp") || lower.contains("inc") || lower.contains("company") || lower.contains("lab") -> EntityType.ORGANIZATION.name
            lower.contains("project") || lower.contains("app") || lower.contains("system") -> EntityType.PROJECT.name
            lower.contains("city") || lower.contains("bangkok") || lower.contains("tokyo") -> EntityType.LOCATION.name
            lower.contains("tool") || lower.contains("api") || lower.contains("python") -> EntityType.TOOL.name
            else -> EntityType.PERSON.name
        }
    }
}
