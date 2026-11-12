package com.example.data.repository

import android.content.Context
import com.example.data.local.ApiAuditLogDao
import com.example.data.local.ApiAuditLogEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CommandHistoryDao
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.ServerDao
import com.example.data.local.ServerEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.ArchivalMemoryItem
import com.example.data.remote.BlockDto
import com.example.data.remote.CreateAgentRequest
import com.example.data.remote.InsertArchivalRequest
import com.example.data.remote.LettaAgentDto
import com.example.data.remote.LettaApiService
import com.example.data.remote.MessageInputDto
import com.example.data.remote.RawApiResponse
import com.example.data.remote.SendMessageRequest
import com.example.data.remote.SendMessageResponse
import com.example.data.remote.SourceDto
import com.example.data.remote.StreamTokenEvent
import com.example.data.remote.ToolDto
import com.example.data.remote.UpdateBlockRequest
import com.example.domain.provider.ConnectionResult
import com.example.domain.provider.HealthReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerRepository(
    private val serverDao: ServerDao,
    private val auditLogDao: ApiAuditLogDao
) {
    val serversFlow: Flow<List<ServerEntity>> = serverDao.getAllServers()
    val primaryServerFlow: Flow<ServerEntity?> = serverDao.getPrimaryServerFlow()

    suspend fun ensureDefaultServers() = withContext(Dispatchers.IO) {
        if (serverDao.getCount() == 0) {
            val defaultLocal = ServerEntity(
                name = "Letta Localhost (Default)",
                providerType = "SELF_HOSTED",
                baseUrl = "http://10.0.2.2:8283",
                apiKey = "",
                isPrimary = true,
                isFavorite = true,
                lastHealthStatus = "UNCHECKED"
            )
            val defaultCloud = ServerEntity(
                name = "Letta Cloud Sandbox",
                providerType = "LETTA_CLOUD",
                baseUrl = "https://api.letta.com",
                apiKey = "",
                isPrimary = false,
                isFavorite = false,
                lastHealthStatus = "UNCHECKED"
            )
            serverDao.insertServer(defaultLocal)
            serverDao.insertServer(defaultCloud)
        }
    }

    suspend fun addServer(
        name: String,
        providerType: String,
        baseUrl: String,
        apiKey: String,
        setAsPrimary: Boolean
    ): Long = withContext(Dispatchers.IO) {
        if (setAsPrimary) {
            serverDao.clearPrimaryFlags()
        }
        val server = ServerEntity(
            name = name,
            providerType = providerType,
            baseUrl = baseUrl,
            apiKey = apiKey,
            isPrimary = setAsPrimary
        )
        serverDao.insertServer(server)
    }

    suspend fun updateServer(server: ServerEntity) = withContext(Dispatchers.IO) {
        serverDao.updateServer(server)
    }

    suspend fun deleteServer(id: Long) = withContext(Dispatchers.IO) {
        serverDao.deleteServerById(id)
    }

    suspend fun setActiveServer(id: Long) = withContext(Dispatchers.IO) {
        serverDao.clearPrimaryFlags()
        serverDao.setPrimary(id)
    }

    suspend fun testConnection(server: ServerEntity): ConnectionResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val api = ApiClient.createService(server.baseUrl, server.apiKey)
            val health = api.getHealth()
            val latency = System.currentTimeMillis() - start

            val updated = server.copy(
                lastHealthStatus = if (health.status.equals("ok", ignoreCase = true) || health.status.equals("healthy", ignoreCase = true)) "HEALTHY" else "DEGRADED",
                lastVersion = health.version ?: "v1.0",
                lastLatencyMs = latency,
                lastCheckedAt = System.currentTimeMillis()
            )
            serverDao.updateServer(updated)

            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "GET",
                    endpoint = "/v1/health",
                    statusCode = 200,
                    durationMs = latency,
                    responseSnippet = "status: ${health.status}, version: ${health.version}",
                    isSuccess = true
                )
            )

            ConnectionResult(
                isConnected = true,
                latencyMs = latency,
                serverVersion = health.version ?: "v1.0",
                message = "Connected! Status: ${health.status}, Latency: ${latency}ms"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            val updated = server.copy(
                lastHealthStatus = "OFFLINE",
                lastLatencyMs = latency,
                lastCheckedAt = System.currentTimeMillis()
            )
            serverDao.updateServer(updated)

            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "GET",
                    endpoint = "/v1/health",
                    statusCode = 0,
                    durationMs = latency,
                    responseSnippet = e.localizedMessage ?: "Connection error",
                    isSuccess = false
                )
            )

            ConnectionResult(
                isConnected = false,
                latencyMs = latency,
                serverVersion = "Unknown",
                message = e.localizedMessage ?: "Failed to connect to ${server.baseUrl}"
            )
        }
    }
}

class AgentRepository(
    private val serverDao: ServerDao,
    private val auditLogDao: ApiAuditLogDao
) {
    private suspend fun getActiveApi(): Pair<LettaApiService, ServerEntity> {
        val server = serverDao.getPrimaryServer()
            ?: serverDao.getAllServers().firstOrNull()?.firstOrNull()
            ?: throw IllegalStateException("No Letta server configured. Please add or select a server in Settings.")
        val api = ApiClient.createService(server.baseUrl, server.apiKey)
        return Pair(api, server)
    }

    suspend fun listAgents(): List<LettaAgentDto> = withContext(Dispatchers.IO) {
        val (api, server) = getActiveApi()
        val start = System.currentTimeMillis()
        try {
            val agents = api.listAgents(limit = 100)
            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "GET",
                    endpoint = "/v1/agents",
                    statusCode = 200,
                    durationMs = System.currentTimeMillis() - start,
                    responseSnippet = "Fetched ${agents.size} agents",
                    isSuccess = true
                )
            )
            agents
        } catch (e: Exception) {
            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "GET",
                    endpoint = "/v1/agents",
                    statusCode = 0,
                    durationMs = System.currentTimeMillis() - start,
                    responseSnippet = e.message ?: "Failed",
                    isSuccess = false
                )
            )
            throw e
        }
    }

    suspend fun getAgent(agentId: String): LettaAgentDto = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.getAgent(agentId)
    }

    suspend fun createAgent(request: CreateAgentRequest): LettaAgentDto = withContext(Dispatchers.IO) {
        val (api, server) = getActiveApi()
        val start = System.currentTimeMillis()
        try {
            val created = api.createAgent(request)
            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "POST",
                    endpoint = "/v1/agents",
                    statusCode = 201,
                    durationMs = System.currentTimeMillis() - start,
                    requestSnippet = "Name: ${request.name}",
                    responseSnippet = "Created ID: ${created.id}",
                    isSuccess = true
                )
            )
            created
        } catch (e: Exception) {
            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "POST",
                    endpoint = "/v1/agents",
                    statusCode = 0,
                    durationMs = System.currentTimeMillis() - start,
                    requestSnippet = "Name: ${request.name}",
                    responseSnippet = e.message ?: "Failed",
                    isSuccess = false
                )
            )
            throw e
        }
    }

    suspend fun deleteAgent(agentId: String): Boolean = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        val resp = api.deleteAgent(agentId)
        resp.isSuccessful
    }

    suspend fun getCoreMemoryBlocks(agentId: String): List<BlockDto> = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.getCoreMemoryBlocks(agentId)
    }

    suspend fun updateCoreMemoryBlock(agentId: String, blockLabel: String, value: String): BlockDto = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.updateCoreMemoryBlock(agentId, blockLabel, UpdateBlockRequest(value))
    }

    suspend fun listArchivalMemory(agentId: String, query: String? = null): List<ArchivalMemoryItem> = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.listArchivalMemory(agentId, query = query)
    }

    suspend fun insertArchivalMemory(agentId: String, text: String): ArchivalMemoryItem = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.insertArchivalMemory(agentId, InsertArchivalRequest(text))
    }

    suspend fun listTools(): List<ToolDto> = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.listTools()
    }

    suspend fun listSources(): List<SourceDto> = withContext(Dispatchers.IO) {
        val (api, _) = getActiveApi()
        api.listSources()
    }
}

class ChatRepository(
    private val serverDao: ServerDao,
    private val auditLogDao: ApiAuditLogDao
) {
    suspend fun sendMessage(agentId: String, text: String): SendMessageResponse = withContext(Dispatchers.IO) {
        val server = serverDao.getPrimaryServer() ?: throw IllegalStateException("No active server selected")
        val api = ApiClient.createService(server.baseUrl, server.apiKey)
        val start = System.currentTimeMillis()

        try {
            val response = api.sendMessage(
                agentId = agentId,
                request = SendMessageRequest(
                    messages = listOf(MessageInputDto(role = "user", content = text)),
                    streaming = false
                )
            )
            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "POST",
                    endpoint = "/v1/agents/$agentId/messages",
                    statusCode = 200,
                    durationMs = System.currentTimeMillis() - start,
                    requestSnippet = text.take(60),
                    responseSnippet = "Tokens: ${response.usage?.totalTokens ?: 0}",
                    isSuccess = true
                )
            )
            response
        } catch (e: Exception) {
            auditLogDao.insertAuditLog(
                ApiAuditLogEntity(
                    serverId = server.id,
                    method = "POST",
                    endpoint = "/v1/agents/$agentId/messages",
                    statusCode = 0,
                    durationMs = System.currentTimeMillis() - start,
                    requestSnippet = text.take(60),
                    responseSnippet = e.message ?: "Chat failed",
                    isSuccess = false
                )
            )
            throw e
        }
    }

    suspend fun streamMessage(agentId: String, text: String): Flow<StreamTokenEvent> {
        val server = serverDao.getPrimaryServer() ?: throw IllegalStateException("No active server selected")
        return ApiClient.streamMessage(
            baseUrl = server.baseUrl,
            apiKey = server.apiKey,
            agentId = agentId,
            messageText = text
        )
    }

    suspend fun loadHistory(agentId: String) = withContext(Dispatchers.IO) {
        val server = serverDao.getPrimaryServer() ?: throw IllegalStateException("No active server selected")
        val api = ApiClient.createService(server.baseUrl, server.apiKey)
        api.listMessages(agentId, limit = 50)
    }
}

class ConsoleRepository(
    private val serverDao: ServerDao,
    private val commandHistoryDao: CommandHistoryDao,
    private val auditLogDao: ApiAuditLogDao,
    private val agentRepo: AgentRepository
) {
    val commandHistoryFlow: Flow<List<CommandHistoryEntity>> = commandHistoryDao.getRecentCommands(100)
    val auditLogsFlow: Flow<List<ApiAuditLogEntity>> = auditLogDao.getRecentAuditLogs(100)

    suspend fun executeRawApi(
        method: String,
        endpoint: String,
        headers: Map<String, String>,
        body: String?
    ): RawApiResponse = withContext(Dispatchers.IO) {
        val server = serverDao.getPrimaryServer()
            ?: serverDao.getAllServers().firstOrNull()?.firstOrNull()
            ?: throw IllegalStateException("No server configured.")

        val response = ApiClient.executeRaw(
            baseUrl = server.baseUrl,
            apiKey = server.apiKey,
            method = method,
            endpoint = endpoint,
            headers = headers,
            requestBodyString = body
        )

        auditLogDao.insertAuditLog(
            ApiAuditLogEntity(
                serverId = server.id,
                method = method.uppercase(),
                endpoint = endpoint,
                statusCode = response.statusCode,
                durationMs = response.durationMs,
                requestSnippet = body?.take(50) ?: "",
                responseSnippet = response.body.take(100),
                isSuccess = response.isSuccessful
            )
        )

        response
    }

    suspend fun runCliCommand(rawInput: String): String = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return@withContext ""

        val server = serverDao.getPrimaryServer()
        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0].lowercase()

        var isSuccess = true
        val output = try {
            when (command) {
                "help" -> """
                    LettaLink Universal AI Agent Console
                    Available Commands:
                      provider list               List configured servers & health
                      provider health             Run live health check on active server
                      agent list                  List agents on current server
                      agent get <id>              Retrieve agent details
                      agent memory list <id>      View core memory blocks
                      agent tools list            List registered tools
                      agent sources list          List connected sources
                      agent message <id> <text>   Send a direct message to agent
                      raw <GET|POST> <endpoint>   Execute raw HTTP request
                      clear                       Clear screen
                """.trimIndent()

                "provider" -> {
                    val sub = parts.getOrNull(1)?.lowercase()
                    when (sub) {
                        "list" -> {
                            val list = serverDao.getAllServers().firstOrNull() ?: emptyList()
                            if (list.isEmpty()) {
                                "No servers registered. Go to Settings to add one."
                            } else {
                                buildString {
                                    appendLine("Configured Letta Servers (${list.size}):")
                                    list.forEach { s ->
                                        val primaryMark = if (s.isPrimary) "[PRIMARY]" else "         "
                                        appendLine(" $primaryMark #${s.id} ${s.name} (${s.baseUrl}) - ${s.lastHealthStatus} (${s.lastLatencyMs}ms)")
                                    }
                                }
                            }
                        }
                        "health" -> {
                            if (server == null) {
                                isSuccess = false
                                "No active server selected."
                            } else {
                                val start = System.currentTimeMillis()
                                val api = ApiClient.createService(server.baseUrl, server.apiKey)
                                val h = api.getHealth()
                                val lat = System.currentTimeMillis() - start
                                "HEALTH CHECK OK: status=${h.status}, version=${h.version ?: "unknown"}, latency=${lat}ms at ${server.baseUrl}"
                            }
                        }
                        else -> "Unknown subcommand 'provider $sub'. Usage: provider list | provider health"
                    }
                }

                "agent" -> {
                    val sub = parts.getOrNull(1)?.lowercase()
                    when (sub) {
                        "list" -> {
                            val agents = agentRepo.listAgents()
                            if (agents.isEmpty()) {
                                "No agents found on active server (${server?.baseUrl})."
                            } else {
                                buildString {
                                    appendLine("Found ${agents.size} Agents:")
                                    agents.forEach { a ->
                                        appendLine(" • [${a.id}] ${a.name} | Model: ${a.llm ?: "default"} | Tags: ${a.tags?.joinToString() ?: "-"}")
                                    }
                                }
                            }
                        }
                        "get" -> {
                            val id = parts.getOrNull(2)
                            if (id.isNullOrBlank()) {
                                isSuccess = false
                                "Usage: agent get <agent_id>"
                            } else {
                                val a = agentRepo.getAgent(id)
                                "Agent: ${a.name}\nID: ${a.id}\nModel: ${a.llm}\nDescription: ${a.description}\nSystem: ${a.system}"
                            }
                        }
                        "memory" -> {
                            val id = parts.getOrNull(3) ?: parts.getOrNull(2)
                            if (id.isNullOrBlank()) {
                                isSuccess = false
                                "Usage: agent memory list <agent_id>"
                            } else {
                                val blocks = agentRepo.getCoreMemoryBlocks(id)
                                buildString {
                                    appendLine("Core Memory Blocks for $id (${blocks.size}):")
                                    blocks.forEach { b ->
                                        appendLine("=== Block: ${b.blockName} (Limit: ${b.limit ?: "none"}) ===")
                                        appendLine(b.value)
                                    }
                                }
                            }
                        }
                        "tools" -> {
                            val tools = agentRepo.listTools()
                            buildString {
                                appendLine("Tools (${tools.size}):")
                                tools.forEach { t ->
                                    appendLine(" • ${t.name}: ${t.description ?: "No description"}")
                                }
                            }
                        }
                        "sources" -> {
                            val sources = agentRepo.listSources()
                            buildString {
                                appendLine("Sources (${sources.size}):")
                                sources.forEach { s ->
                                    appendLine(" • ${s.name}: ${s.description ?: "No description"}")
                                }
                            }
                        }
                        "message" -> {
                            val id = parts.getOrNull(2)
                            val text = parts.drop(3).joinToString(" ")
                            if (id.isNullOrBlank() || text.isBlank()) {
                                isSuccess = false
                                "Usage: agent message <agent_id> <text...>"
                            } else {
                                val (api, _) = Pair(ApiClient.createService(server!!.baseUrl, server.apiKey), server)
                                val resp = api.sendMessage(id, SendMessageRequest(listOf(MessageInputDto(content = text))))
                                val reply = resp.messages?.lastOrNull { it.role == "assistant" || it.messageType == "assistant_message" }?.text ?: "No text returned"
                                "[$id reply]: $reply"
                            }
                        }
                        else -> "Unknown agent command '$sub'. Use 'help' for syntax."
                    }
                }

                "raw" -> {
                    val method = parts.getOrNull(1)?.uppercase() ?: "GET"
                    val endpoint = parts.getOrNull(2) ?: "v1/health"
                    val body = if (parts.size > 3) parts.drop(3).joinToString(" ") else null
                    val resp = executeRawApi(method, endpoint, emptyMap(), body)
                    "HTTP ${resp.statusCode} (${resp.durationMs}ms):\n${resp.body}"
                }

                "clear" -> "CONSOLE_CLEARED"

                else -> "Command not recognized: '$command'. Type 'help' to see valid commands."
            }
        } catch (e: Exception) {
            isSuccess = false
            "Error executing '$trimmed': ${e.message ?: e.localizedMessage}"
        }

        commandHistoryDao.insertCommand(
            CommandHistoryEntity(
                serverId = server?.id ?: 0,
                command = trimmed,
                isSuccess = isSuccess,
                output = output.take(500)
            )
        )

        output
    }
}
