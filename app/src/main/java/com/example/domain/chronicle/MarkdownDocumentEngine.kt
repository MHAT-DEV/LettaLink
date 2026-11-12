package com.example.domain.chronicle

import com.example.data.local.ChronicleDao
import com.example.data.local.ChronicleDocVersionEntity
import com.example.data.local.ChronicleDocumentEntity
import com.example.data.local.ChronicleFactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MarkdownDocumentEngine(
    private val chronicleDao: ChronicleDao
) {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    private val readableFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Initializes default Markdown documents for an agent if empty.
     */
    suspend fun ensureDefaultDocuments(agentId: String) = withContext(Dispatchers.IO) {
        val existing = chronicleDao.getDocumentsForAgent(agentId).firstOrNull()
        if (existing.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val defaultProfile = createGeneratedDocument(
                agentId = agentId,
                title = "Agent Profile & User Context",
                fileName = "profile.md",
                docType = "profile-summary",
                body = """
                    # User & Agent Profile

                    ## Overview
                    This document tracks verified personal attributes, roles, and historical transitions.

                    ## Key Facts
                    *No facts recorded yet. Converse with the agent to build memory.*
                """.trimIndent()
            )

            val defaultProjects = createGeneratedDocument(
                agentId = agentId,
                title = "Projects & Initiatives",
                fileName = "projects.md",
                docType = "project-tracker",
                body = """
                    # Projects & Knowledge Initiatives

                    ## Active Projects
                    - LettaLink Android Agent Console

                    ## Dependencies & Stack
                    - Letta Self-hosted / MemGPT
                    - Temporal Knowledge Graph
                """.trimIndent()
            )

            chronicleDao.insertDocument(defaultProfile)
            chronicleDao.insertDocument(defaultProjects)
        }
    }

    /**
     * Creates a new Markdown document with standard YAML frontmatter.
     */
    fun createGeneratedDocument(
        agentId: String,
        title: String,
        fileName: String,
        docType: String,
        body: String,
        revision: Int = 1
    ): ChronicleDocumentEntity {
        val docId = "doc-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val isoDate = isoFormat.format(Date(now))

        val frontmatter = """
            ---
            document_id: $docId
            agent_id: $agentId
            generated_at: $isoDate
            knowledge_revision: $revision
            source_graph: lettachronicle
            document_type: $docType
            ---

        """.trimIndent()

        val fullContent = frontmatter + body

        return ChronicleDocumentEntity(
            documentId = docId,
            agentId = agentId,
            title = title,
            fileName = if (fileName.endsWith(".md")) fileName else "$fileName.md",
            content = fullContent,
            knowledgeRevision = revision,
            syncStatus = "SYNCHRONIZED",
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Updates an existing document, creating a version history snapshot.
     */
    suspend fun updateDocument(
        documentId: String,
        newContent: String,
        changeSummary: String = "Manual edit"
    ) = withContext(Dispatchers.IO) {
        val existing = chronicleDao.getDocumentById(documentId) ?: return@withContext
        val now = System.currentTimeMillis()
        val nextRevision = existing.knowledgeRevision + 1

        // 1. Snapshot previous version
        val version = ChronicleDocVersionEntity(
            versionId = "ver-${UUID.randomUUID()}",
            documentId = documentId,
            revisionNumber = existing.knowledgeRevision,
            content = existing.content,
            changeSummary = changeSummary,
            committedAt = now
        )
        chronicleDao.insertDocVersion(version)

        // 2. Update document
        val updated = existing.copy(
            content = newContent,
            knowledgeRevision = nextRevision,
            syncStatus = "SYNCHRONIZED",
            updatedAt = now
        )
        chronicleDao.updateDocument(updated)
    }

    /**
     * Regenerates document content from active Knowledge Graph facts.
     */
    suspend fun compileGraphToMarkdown(
        agentId: String,
        title: String,
        fileName: String
    ): ChronicleDocumentEntity = withContext(Dispatchers.IO) {
        val facts = chronicleDao.getFactsForAgent(agentId).firstOrNull() ?: emptyList()
        val entities = chronicleDao.getEntitiesForAgent(agentId).firstOrNull() ?: emptyList()

        val sb = StringBuilder()
        sb.appendLine("# Knowledge Graph Summary: $title")
        sb.appendLine()
        sb.appendLine("## Active Entities (${entities.size})")
        entities.forEach { e ->
            sb.appendLine("- **${e.canonicalName}** (`${e.entityType}`): ${e.description}")
        }

        sb.appendLine()
        sb.appendLine("## Temporal Facts (${facts.size})")
        facts.forEach { f ->
            val validFromStr = f.validFrom?.let { readableFormat.format(Date(it)) } ?: "unknown"
            val validToStr = f.validTo?.let { readableFormat.format(Date(it)) } ?: "present"
            val statusBadge = if (f.validTo == null) "[ACTIVE]" else "[HISTORICAL]"
            sb.appendLine("- $statusBadge **${f.subject}** *${f.predicate}* `${f.objectValue}` (Valid: $validFromStr to $validToStr, Source: ${f.source})")
        }

        createGeneratedDocument(
            agentId = agentId,
            title = title,
            fileName = fileName,
            docType = "graph-compiled-summary",
            body = sb.toString()
        )
    }
}
