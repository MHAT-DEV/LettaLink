package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.domain.chronicle.ConflictType
import com.example.domain.chronicle.MarkdownDocumentEngine
import com.example.domain.chronicle.ParsedFactCandidate
import com.example.domain.chronicle.SourceType
import com.example.domain.chronicle.TemporalReasoningEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChronicleEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var temporalEngine: TemporalReasoningEngine
    private lateinit var markdownEngine: MarkdownDocumentEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        temporalEngine = TemporalReasoningEngine(db.chronicleDao())
        markdownEngine = MarkdownDocumentEngine(db.chronicleDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testTemporalFactIngestionAndEntityResolution() = runBlocking {
        val agentId = "agent-test-001"
        val candidate = ParsedFactCandidate(
            subject = "Alice",
            predicate = "WORKS_AT",
            objectValue = "AcmeCorp",
            validFrom = 1000000L,
            sourceRef = "Chat interaction with user",
            sourceType = SourceType.USER_MESSAGE
        )

        val fact = temporalEngine.ingestFact(agentId, candidate)

        assertEquals("Alice", fact.subject)
        assertEquals("WORKS_AT", fact.predicate)
        assertEquals("AcmeCorp", fact.objectValue)
        assertEquals(1000000L, fact.validFrom)
        assertNull(fact.validTo)

        // Verify entities created
        val entities = db.chronicleDao().getEntitiesForAgent(agentId).first()
        assertTrue(entities.any { it.canonicalName == "Alice" })
        assertTrue(entities.any { it.canonicalName == "AcmeCorp" })
    }

    @Test
    fun testNonDestructiveSuperseding() = runBlocking {
        val agentId = "agent-test-002"
        val fact1 = ParsedFactCandidate(
            subject = "Bob",
            predicate = "LIVES_IN",
            objectValue = "Tokyo",
            validFrom = 1000000L,
            sourceRef = "Initial declaration",
            sourceType = SourceType.USER_MESSAGE
        )
        temporalEngine.ingestFact(agentId, fact1)

        // New claim that Bob lives in Bangkok at a later time
        val fact2 = ParsedFactCandidate(
            subject = "Bob",
            predicate = "LIVES_IN",
            objectValue = "Bangkok",
            validFrom = 2000000L,
            sourceRef = "Relocation message",
            sourceType = SourceType.USER_MESSAGE
        )
        val newFact = temporalEngine.ingestFact(agentId, fact2)

        assertEquals("Bangkok", newFact.objectValue)
        assertNull(newFact.validTo)

        // Verify old fact is superseded, NOT deleted
        val allFacts = db.chronicleDao().getFactsForAgent(agentId).first()
        assertEquals(2, allFacts.size)

        val oldFact = allFacts.find { it.objectValue == "Tokyo" }
        assertNotNull(oldFact)
        assertEquals(2000000L, oldFact?.validTo) // Superseded at timestamp 2000000
    }

    @Test
    fun testMarkdownDocumentGenerationWithFrontmatter() = runBlocking {
        val agentId = "agent-test-003"
        val doc = markdownEngine.createGeneratedDocument(
            agentId = agentId,
            title = "Test System Document",
            fileName = "system.md",
            docType = "architecture-spec",
            body = "# Architecture\n\nVerified temporal knowledge layer."
        )

        assertTrue(doc.content.startsWith("---"))
        assertTrue(doc.content.contains("source_graph: lettachronicle"))
        assertTrue(doc.content.contains("document_type: architecture-spec"))
        assertTrue(doc.content.contains("Verified temporal knowledge layer."))
        assertEquals("system.md", doc.fileName)
        assertEquals(1, doc.knowledgeRevision)
    }
}
