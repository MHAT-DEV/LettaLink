package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApiAuditLogEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.ServerEntity
import com.example.data.remote.ArchivalMemoryItem
import com.example.data.remote.BlockDto
import com.example.data.remote.CreateAgentRequest
import com.example.data.remote.CreateBlockDto
import com.example.data.remote.LettaAgentDto
import com.example.data.remote.RawApiResponse
import com.example.data.remote.SourceDto
import com.example.data.remote.StreamTokenEvent
import com.example.data.remote.ToolDto
import com.example.data.repository.AgentRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ConsoleRepository
import com.example.data.repository.ServerRepository
import com.example.domain.discovery.DiscoveredServer
import com.example.domain.discovery.LanDiscoveryService
import com.example.domain.provider.ConnectionResult
import com.example.domain.provider.HealthReport
import com.example.data.local.TtsProviderEntity
import com.example.data.local.TtsSettingsEntity
import com.example.domain.tts.LettaTalkManager
import com.example.data.local.ChronicleConflictEntity
import com.example.data.local.ChronicleDocVersionEntity
import com.example.data.local.ChronicleDocumentEntity
import com.example.data.local.ChronicleEntityEntity
import com.example.data.local.ChronicleFactEntity
import com.example.data.local.ChronicleRelationshipEntity
import com.example.data.local.ChronicleSyncHistoryEntity
import com.example.domain.tts.TalkPlaybackState
import com.example.domain.chronicle.EntityType
import com.example.domain.chronicle.LettaMemorySyncEngine
import com.example.domain.chronicle.MarkdownDocumentEngine
import com.example.domain.chronicle.ParsedFactCandidate
import com.example.domain.chronicle.SourceType
import com.example.domain.chronicle.TemporalReasoningEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system", "reasoning", "tool"
    val content: String,
    val toolCallName: String? = null,
    val toolCallArgs: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreamingChunk: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val serverRepo = ServerRepository(db.serverDao(), db.apiAuditLogDao())
    val agentRepo = AgentRepository(db.serverDao(), db.apiAuditLogDao())
    val chatRepo = ChatRepository(db.serverDao(), db.apiAuditLogDao())
    val consoleRepo = ConsoleRepository(db.serverDao(), db.commandHistoryDao(), db.apiAuditLogDao(), agentRepo)
    private val discoveryService = LanDiscoveryService(application)
    val talkManager = LettaTalkManager(application, db.ttsProviderDao(), db.ttsSettingsDao(), viewModelScope)

    val talkPlaybackState: StateFlow<TalkPlaybackState> = talkManager.playbackState
    val ttsSettings: StateFlow<TtsSettingsEntity> = talkManager.settings
    val ttsProviders: StateFlow<List<TtsProviderEntity>> = db.ttsProviderDao().getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val servers: StateFlow<List<ServerEntity>> = serverRepo.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val primaryServer: StateFlow<ServerEntity?> = serverRepo.primaryServerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<ApiAuditLogEntity>> = consoleRepo.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = consoleRepo.commandHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredServers: StateFlow<List<DiscoveredServer>> = discoveryService.discoveredServers
    val isScanningLan: StateFlow<Boolean> = discoveryService.isScanning

    // Server health state
    private val _healthReport = MutableStateFlow<HealthReport?>(null)
    val healthReport: StateFlow<HealthReport?> = _healthReport.asStateFlow()

    private val _isHealthChecking = MutableStateFlow(false)
    val isHealthChecking: StateFlow<Boolean> = _isHealthChecking.asStateFlow()

    // Agent list state
    private val _agents = MutableStateFlow<List<LettaAgentDto>>(emptyList())
    val agents: StateFlow<List<LettaAgentDto>> = _agents.asStateFlow()

    private val _isLoadingAgents = MutableStateFlow(false)
    val isLoadingAgents: StateFlow<Boolean> = _isLoadingAgents.asStateFlow()

    private val _agentErrorMessage = MutableStateFlow<String?>(null)
    val agentErrorMessage: StateFlow<String?> = _agentErrorMessage.asStateFlow()

    // Selected Agent Details & Workspace
    private val _selectedAgent = MutableStateFlow<LettaAgentDto?>(null)
    val selectedAgent: StateFlow<LettaAgentDto?> = _selectedAgent.asStateFlow()

    private val _coreMemoryBlocks = MutableStateFlow<List<BlockDto>>(emptyList())
    val coreMemoryBlocks: StateFlow<List<BlockDto>> = _coreMemoryBlocks.asStateFlow()

    private val _archivalItems = MutableStateFlow<List<ArchivalMemoryItem>>(emptyList())
    val archivalItems: StateFlow<List<ArchivalMemoryItem>> = _archivalItems.asStateFlow()

    private val _tools = MutableStateFlow<List<ToolDto>>(emptyList())
    val tools: StateFlow<List<ToolDto>> = _tools.asStateFlow()

    private val _sources = MutableStateFlow<List<SourceDto>>(emptyList())
    val sources: StateFlow<List<SourceDto>> = _sources.asStateFlow()

    // LettaChronicle: Temporal Knowledge Graph & Markdown Document Engines
    val chronicleDao = db.chronicleDao()
    val temporalEngine = TemporalReasoningEngine(chronicleDao)
    val markdownEngine = MarkdownDocumentEngine(chronicleDao)
    val memorySyncEngine = LettaMemorySyncEngine(chronicleDao, agentRepo)

    private var chronicleJob: Job? = null
    val chronicleEntities = MutableStateFlow<List<ChronicleEntityEntity>>(emptyList())
    val chronicleRelationships = MutableStateFlow<List<ChronicleRelationshipEntity>>(emptyList())
    val chronicleFacts = MutableStateFlow<List<ChronicleFactEntity>>(emptyList())
    val chronicleConflicts = MutableStateFlow<List<ChronicleConflictEntity>>(emptyList())
    val chronicleDocuments = MutableStateFlow<List<ChronicleDocumentEntity>>(emptyList())
    val chronicleSyncHistory = MutableStateFlow<List<ChronicleSyncHistoryEntity>>(emptyList())
    val selectedChronicleDoc = MutableStateFlow<ChronicleDocumentEntity?>(null)
    val docVersions = MutableStateFlow<List<ChronicleDocVersionEntity>>(emptyList())
    val isChronicleProcessing = MutableStateFlow(false)

    // Chat State
    private val _messages = MutableStateFlow<List<UiChatMessage>>(emptyList())
    val messages: StateFlow<List<UiChatMessage>> = _messages.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private var streamingJob: Job? = null

    // CLI Console state
    private val _cliLog = MutableStateFlow<List<String>>(
        listOf(
            "LettaLink Production Console initialized.",
            "Type 'help' for commands, or use the interactive buttons below."
        )
    )
    val cliLog: StateFlow<List<String>> = _cliLog.asStateFlow()

    // Raw API state
    private val _rawResult = MutableStateFlow<RawApiResponse?>(null)
    val rawResult: StateFlow<RawApiResponse?> = _rawResult.asStateFlow()

    private val _isRawLoading = MutableStateFlow(false)
    val isRawLoading: StateFlow<Boolean> = _isRawLoading.asStateFlow()

    // User Notification/Toast
    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    init {
        viewModelScope.launch {
            serverRepo.ensureDefaultServers()
            refreshHealth()
            loadAgents()
        }
    }

    fun dismissNotice() {
        _statusNotice.value = null
    }

    fun showNotice(msg: String) {
        _statusNotice.value = msg
    }

    fun refreshHealth() {
        viewModelScope.launch {
            _isHealthChecking.value = true
            val current = primaryServer.value
            if (current != null) {
                val res = serverRepo.testConnection(current)
                _healthReport.value = HealthReport(
                    status = if (res.isConnected) "HEALTHY" else "OFFLINE",
                    latencyMs = res.latencyMs,
                    serverVersion = res.serverVersion,
                    details = mapOf("message" to res.message)
                )
            } else {
                _healthReport.value = HealthReport(
                    status = "UNCONFIGURED",
                    latencyMs = 0,
                    serverVersion = "None"
                )
            }
            _isHealthChecking.value = false
        }
    }

    fun selectServer(id: Long) {
        viewModelScope.launch {
            serverRepo.setActiveServer(id)
            refreshHealth()
            loadAgents()
        }
    }

    fun addServer(name: String, providerType: String, baseUrl: String, apiKey: String, isPrimary: Boolean) {
        viewModelScope.launch {
            serverRepo.addServer(name, providerType, baseUrl, apiKey, isPrimary)
            showNotice("Server profile '$name' saved.")
            refreshHealth()
            loadAgents()
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            serverRepo.deleteServer(id)
            showNotice("Server removed.")
        }
    }

    fun testServer(server: ServerEntity, onResult: (ConnectionResult) -> Unit) {
        viewModelScope.launch {
            val result = serverRepo.testConnection(server)
            onResult(result)
        }
    }

    fun startLanScan() {
        discoveryService.startDiscovery()
    }

    fun stopLanScan() {
        discoveryService.stopDiscovery()
    }

    fun quickSubnetScan() {
        viewModelScope.launch {
            _statusNotice.value = "Scanning local host candidates..."
            val found = discoveryService.quickSubnetProbe()
            if (found.isNotEmpty()) {
                _statusNotice.value = "Discovered ${found.size} local service(s)!"
            } else {
                _statusNotice.value = "No responsive Letta server on standard local ports."
            }
        }
    }

    fun loadAgents() {
        viewModelScope.launch {
            _isLoadingAgents.value = true
            _agentErrorMessage.value = null
            try {
                val list = agentRepo.listAgents()
                _agents.value = list
                if (_selectedAgent.value == null && list.isNotEmpty()) {
                    selectAgent(list.first().id)
                }
            } catch (e: Exception) {
                _agentErrorMessage.value = e.localizedMessage ?: "Failed to list agents"
                _agents.value = emptyList()
            } finally {
                _isLoadingAgents.value = false
            }
        }
    }

    fun selectAgent(agentId: String) {
        viewModelScope.launch {
            val target = _agents.value.find { it.id == agentId }
            _selectedAgent.value = target
            if (target != null) {
                loadAgentDetails(target.id)
            }
        }
    }

    fun loadAgentDetails(agentId: String) {
        viewModelScope.launch {
            try {
                val fullAgent = agentRepo.getAgent(agentId)
                _selectedAgent.value = fullAgent
                _coreMemoryBlocks.value = agentRepo.getCoreMemoryBlocks(agentId)
                _archivalItems.value = agentRepo.listArchivalMemory(agentId)
                _tools.value = agentRepo.listTools()
                _sources.value = agentRepo.listSources()
                loadChronicleForAgent(agentId, fullAgent.name)
            } catch (e: Exception) {
                showNotice("Failed loading agent details: ${e.message}")
            }
        }
    }

    fun createAgent(name: String, description: String, systemPrompt: String, llm: String, tags: List<String>) {
        viewModelScope.launch {
            try {
                val defaultBlocks = listOf(
                    CreateBlockDto(name = "persona", value = "I am an intelligent assistant built with Letta."),
                    CreateBlockDto(name = "human", value = "The user interacting with me.")
                )
                val request = CreateAgentRequest(
                    name = name,
                    description = description.ifBlank { null },
                    system = systemPrompt.ifBlank { null },
                    llm = llm.ifBlank { null },
                    tags = tags,
                    memoryBlocks = defaultBlocks
                )
                val created = agentRepo.createAgent(request)
                showNotice("Agent '${created.name}' created!")
                loadAgents()
                selectAgent(created.id)
            } catch (e: Exception) {
                showNotice("Create agent failed: ${e.message}")
            }
        }
    }

    fun deleteAgent(agentId: String) {
        viewModelScope.launch {
            try {
                agentRepo.deleteAgent(agentId)
                showNotice("Agent deleted.")
                if (_selectedAgent.value?.id == agentId) {
                    _selectedAgent.value = null
                }
                loadAgents()
            } catch (e: Exception) {
                showNotice("Delete agent failed: ${e.message}")
            }
        }
    }

    fun updateCoreMemory(blockLabel: String, newValue: String) {
        val agent = _selectedAgent.value ?: return
        viewModelScope.launch {
            try {
                val updated = agentRepo.updateCoreMemoryBlock(agent.id, blockLabel, newValue)
                showNotice("Memory block '$blockLabel' updated.")
                _coreMemoryBlocks.value = _coreMemoryBlocks.value.map {
                    if (it.blockName == blockLabel) updated else it
                }
            } catch (e: Exception) {
                showNotice("Update memory failed: ${e.message}")
            }
        }
    }

    fun insertArchivalMemory(text: String) {
        val agent = _selectedAgent.value ?: return
        viewModelScope.launch {
            try {
                val item = agentRepo.insertArchivalMemory(agent.id, text)
                showNotice("Added passage to archival memory.")
                _archivalItems.value = listOf(item) + _archivalItems.value
            } catch (e: Exception) {
                showNotice("Failed inserting archival memory: ${e.message}")
            }
        }
    }

    fun sendMessage(text: String, useStreaming: Boolean = true) {
        val agent = _selectedAgent.value
        if (agent == null) {
            showNotice("Please select an agent first!")
            return
        }
        if (text.isBlank()) return

        val userMsg = UiChatMessage(role = "user", content = text)
        _messages.value = _messages.value + userMsg

        if (useStreaming) {
            sendStreaming(agent.id, text)
        } else {
            sendNonStreaming(agent.id, text)
        }
    }

    private fun sendNonStreaming(agentId: String, text: String) {
        viewModelScope.launch {
            _isSendingMessage.value = true
            try {
                val response = chatRepo.sendMessage(agentId, text)
                val newMsgs = mutableListOf<UiChatMessage>()
                response.messages?.forEach { m ->
                    when (m.messageType) {
                        "reasoning_message", "internal_monologue" -> {
                            m.text?.let {
                                newMsgs.add(UiChatMessage(role = "reasoning", content = it))
                            }
                        }
                        "tool_call_message" -> {
                            newMsgs.add(
                                UiChatMessage(
                                    role = "tool",
                                    content = "Calling tool: ${m.toolCall?.name ?: "tool"}",
                                    toolCallName = m.toolCall?.name,
                                    toolCallArgs = m.toolCall?.arguments
                                )
                            )
                        }
                        else -> {
                            val txt = m.text ?: ""
                            if (txt.isNotBlank()) {
                                newMsgs.add(UiChatMessage(role = "assistant", content = txt))
                            }
                        }
                    }
                }
                if (newMsgs.isEmpty()) {
                    newMsgs.add(UiChatMessage(role = "assistant", content = "[No response content returned from server]"))
                }
                _messages.value = _messages.value + newMsgs

                // LettaTalk Auto Read Aloud Hook
                val assistantResponseText = newMsgs.filter { it.role == "assistant" }.joinToString("\n") { it.content }
                val lastAssistantMsg = newMsgs.lastOrNull { it.role == "assistant" }
                if (assistantResponseText.isNotBlank() && lastAssistantMsg != null) {
                    talkManager.onAgentResponseCompleted(
                        agentId = agentId,
                        agentName = _selectedAgent.value?.name ?: "Agent",
                        messageId = lastAssistantMsg.id,
                        rawText = assistantResponseText
                    )
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + UiChatMessage(
                    role = "system",
                    content = "Error sending message: ${e.message}"
                )
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    private fun sendStreaming(agentId: String, text: String) {
        streamingJob?.cancel()
        _isSendingMessage.value = true

        val streamingMsgId = java.util.UUID.randomUUID().toString()
        val streamContent = StringBuilder()

        _messages.value = _messages.value + UiChatMessage(
            id = streamingMsgId,
            role = "assistant",
            content = "...",
            isStreamingChunk = true
        )

        streamingJob = viewModelScope.launch {
            try {
                chatRepo.streamMessage(agentId, text).collect { event ->
                    when (event) {
                        is StreamTokenEvent.Token -> {
                            streamContent.append(event.text)
                            updateStreamingMessage(streamingMsgId, streamContent.toString())
                        }
                        is StreamTokenEvent.Reasoning -> {
                            _messages.value = _messages.value + UiChatMessage(
                                role = "reasoning",
                                content = event.text
                            )
                        }
                        is StreamTokenEvent.ToolCall -> {
                            _messages.value = _messages.value + UiChatMessage(
                                role = "tool",
                                content = "Invoking ${event.name}",
                                toolCallName = event.name,
                                toolCallArgs = event.args
                            )
                        }
                        is StreamTokenEvent.Done -> {
                            updateStreamingMessage(streamingMsgId, streamContent.toString(), isDone = true)
                            // LettaTalk Auto Read Aloud Hook
                            if (streamContent.isNotBlank()) {
                                talkManager.onAgentResponseCompleted(
                                    agentId = agentId,
                                    agentName = _selectedAgent.value?.name ?: "Agent",
                                    messageId = streamingMsgId,
                                    rawText = streamContent.toString()
                                )
                            }
                        }
                        is StreamTokenEvent.Error -> {
                            _messages.value = _messages.value + UiChatMessage(
                                role = "system",
                                content = "Stream Error: ${event.message}"
                            )
                        }
                        is StreamTokenEvent.Usage -> {}
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + UiChatMessage(
                    role = "system",
                    content = "Stream interrupted: ${e.message}"
                )
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    private fun updateStreamingMessage(id: String, content: String, isDone: Boolean = false) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    content = if (content.isBlank()) "..." else content,
                    isStreamingChunk = !isDone
                )
            } else msg
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _isSendingMessage.value = false
        showNotice("Stream cancelled.")
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    fun runCliCommand(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            val history = _cliLog.value.toMutableList()
            history.add("> $input")
            val output = consoleRepo.runCliCommand(input)
            if (output == "CONSOLE_CLEARED") {
                _cliLog.value = listOf("LettaLink Console cleared.")
            } else {
                history.add(output)
                _cliLog.value = history
            }
        }
    }

    fun clearCli() {
        _cliLog.value = listOf("LettaLink Console cleared.")
    }

    fun executeRawApi(
        method: String,
        endpoint: String,
        headers: Map<String, String>,
        body: String?
    ) {
        viewModelScope.launch {
            _isRawLoading.value = true
            _rawResult.value = null
            try {
                val res = consoleRepo.executeRawApi(method, endpoint, headers, body)
                _rawResult.value = res
            } catch (e: Exception) {
                _rawResult.value = RawApiResponse(
                    statusCode = 0,
                    statusMessage = "Client Exception",
                    durationMs = 0,
                    headers = emptyMap(),
                    body = e.message ?: "Execution failed",
                    isSuccessful = false,
                    curlCommand = ""
                )
            } finally {
                _isRawLoading.value = false
            }
        }
    }

    // LettaTalk TTS Actions
    fun speakMessage(messageId: String, text: String, agentName: String = "Agent") {
        talkManager.speakMessage(messageId, text, agentName)
    }

    fun stopSpeaking() {
        talkManager.stop()
    }

    fun testVoice(sample: String) {
        talkManager.testSpeak(sample)
    }

    fun updateTtsSettings(settings: TtsSettingsEntity) {
        talkManager.updateSettings(settings)
        showNotice("TTS settings updated.")
    }

    fun addCustomTtsProvider(name: String, baseUrl: String, endpoint: String, apiKey: String, model: String, voice: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TtsProviderEntity(
                name = name,
                providerType = "OPENAI_COMPATIBLE",
                baseUrl = baseUrl,
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                defaultVoice = voice,
                isDefault = true
            )
            db.ttsProviderDao().clearDefaultFlags()
            db.ttsProviderDao().insertProvider(entity)
            talkManager.loadSettingsAndProvider()
            showNotice("Added custom TTS provider '$name'.")
        }
    }

    fun setDefaultTtsProvider(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id == -1L) {
                // Switch back to Android Native local TTS
                db.ttsProviderDao().clearDefaultFlags()
            } else {
                db.ttsProviderDao().clearDefaultFlags()
                db.ttsProviderDao().setDefault(id)
            }
            talkManager.loadSettingsAndProvider()
            showNotice("Switched active TTS provider.")
        }
    }

    fun deleteTtsProvider(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.ttsProviderDao().deleteProvider(id)
            talkManager.loadSettingsAndProvider()
            showNotice("Custom TTS provider removed.")
        }
    }

    fun clearAudioCache() {
        val freed = talkManager.clearAudioCache()
        showNotice("Audio cache cleared ($freed bytes).")
    }

    // ==========================================
    // LettaChronicle: Temporal KG & Docs Actions
    // ==========================================

    fun loadChronicleForAgent(agentId: String, agentName: String = "Agent") {
        chronicleJob?.cancel()
        chronicleJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                markdownEngine.ensureDefaultDocuments(agentId)
            } catch (_: Exception) {}

            launch {
                chronicleDao.getEntitiesForAgent(agentId).collect {
                    chronicleEntities.value = it
                }
            }
            launch {
                chronicleDao.getRelationshipsForAgent(agentId).collect {
                    chronicleRelationships.value = it
                }
            }
            launch {
                chronicleDao.getFactsForAgent(agentId).collect {
                    chronicleFacts.value = it
                }
            }
            launch {
                chronicleDao.getOpenConflictsForAgent(agentId).collect {
                    chronicleConflicts.value = it
                }
            }
            launch {
                chronicleDao.getDocumentsForAgent(agentId).collect { docs ->
                    chronicleDocuments.value = docs
                    if (selectedChronicleDoc.value == null && docs.isNotEmpty()) {
                        selectChronicleDocument(docs.first().documentId)
                    }
                }
            }
            launch {
                chronicleDao.getSyncHistoryForAgent(agentId).collect {
                    chronicleSyncHistory.value = it
                }
            }
        }
    }

    fun selectChronicleDocument(documentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = chronicleDao.getDocumentById(documentId)
            selectedChronicleDoc.value = doc
            if (doc != null) {
                chronicleDao.getVersionsForDocument(doc.documentId).collect {
                    docVersions.value = it
                }
            }
        }
    }

    fun ingestChronicleFact(
        agentId: String,
        subject: String,
        predicate: String,
        objectValue: String,
        statement: String = "$subject $predicate $objectValue",
        confidence: Float = 0.95f,
        sourceRef: String = "Manual operator entry",
        sourceType: String = SourceType.USER_MESSAGE.name,
        validFrom: Long = System.currentTimeMillis(),
        validTo: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isChronicleProcessing.value = true
            try {
                val candidate = ParsedFactCandidate(
                    subject = subject,
                    predicate = predicate,
                    objectValue = objectValue,
                    validFrom = validFrom,
                    validTo = validTo,
                    sourceRef = sourceRef,
                    sourceType = try { SourceType.valueOf(sourceType) } catch (_: Exception) { SourceType.USER_MESSAGE },
                    confidence = confidence
                )
                val fact = temporalEngine.ingestFact(agentId, candidate)
                showNotice("Fact ingested into Chronicle: ${fact.subject} ${fact.predicate} ${fact.objectValue}")
            } catch (e: Exception) {
                showNotice("Failed to ingest fact: ${e.message}")
            } finally {
                isChronicleProcessing.value = false
            }
        }
    }

    fun resolveChronicleConflict(conflictId: String, resolutionChoice: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                temporalEngine.resolveConflict(conflictId, resolutionChoice)
                showNotice("Conflict resolved as '$resolutionChoice'")
            } catch (e: Exception) {
                showNotice("Failed resolving conflict: ${e.message}")
            }
        }
    }

    fun createChronicleDocument(
        agentId: String,
        title: String,
        fileName: String,
        content: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = markdownEngine.createGeneratedDocument(
                    agentId = agentId,
                    title = title,
                    fileName = fileName,
                    docType = "knowledge-doc",
                    body = content
                )
                chronicleDao.insertDocument(doc)
                selectedChronicleDoc.value = doc
                showNotice("Document '${doc.title}' created.")
            } catch (e: Exception) {
                showNotice("Failed creating document: ${e.message}")
            }
        }
    }

    fun updateChronicleDocument(
        documentId: String,
        newContent: String,
        changeSummary: String = "Manual edit in Chronicle Console"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                markdownEngine.updateDocument(documentId, newContent, changeSummary)
                val updated = chronicleDao.getDocumentById(documentId)
                selectedChronicleDoc.value = updated
                showNotice("Document updated (Rev ${updated?.knowledgeRevision ?: 1}).")
            } catch (e: Exception) {
                showNotice("Failed updating document: ${e.message}")
            }
        }
    }

    fun deleteChronicleDocument(documentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chronicleDao.deleteDocument(documentId)
                if (selectedChronicleDoc.value?.documentId == documentId) {
                    selectedChronicleDoc.value = null
                }
                showNotice("Chronicle document deleted.")
            } catch (e: Exception) {
                showNotice("Delete document failed: ${e.message}")
            }
        }
    }

    fun compileGraphToDocument(
        agentId: String,
        title: String = "Compiled Knowledge",
        fileName: String = "compiled_knowledge.md"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isChronicleProcessing.value = true
            try {
                val doc = markdownEngine.compileGraphToMarkdown(agentId, title, fileName)
                chronicleDao.insertDocument(doc)
                selectedChronicleDoc.value = doc
                showNotice("Knowledge Graph compiled to '${doc.title}'")
            } catch (e: Exception) {
                showNotice("Failed compiling graph: ${e.message}")
            } finally {
                isChronicleProcessing.value = false
            }
        }
    }

    fun syncChronicleToCoreMemory(agentId: String, blockLabel: String = "human") {
        viewModelScope.launch(Dispatchers.IO) {
            isChronicleProcessing.value = true
            try {
                val record = memorySyncEngine.syncToCoreMemory(agentId, blockLabel)
                if (record.syncStatus == "SUCCESS") {
                    showNotice("Synced ${record.factsCount} facts to Core Memory '$blockLabel'.")
                    _coreMemoryBlocks.value = agentRepo.getCoreMemoryBlocks(agentId)
                } else {
                    showNotice("Sync failed: ${record.details}")
                }
            } catch (e: Exception) {
                showNotice("Sync error: ${e.message}")
            } finally {
                isChronicleProcessing.value = false
            }
        }
    }

    fun archiveChronicleFact(agentId: String, factId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val record = memorySyncEngine.archiveFact(agentId, factId)
                if (record.syncStatus == "SUCCESS") {
                    showNotice("Fact archived to Letta Vector Memory.")
                    _archivalItems.value = agentRepo.listArchivalMemory(agentId)
                } else {
                    showNotice("Archival failed: ${record.details}")
                }
            } catch (e: Exception) {
                showNotice("Failed archiving fact: ${e.message}")
            }
        }
    }
}
