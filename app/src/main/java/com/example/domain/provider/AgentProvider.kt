package com.example.domain.provider

import com.example.data.remote.ApiClient
import com.example.data.remote.LettaApiService
import com.example.data.remote.MessageInputDto
import com.example.data.remote.RawApiResponse
import com.example.data.remote.SendMessageRequest
import com.example.data.remote.StreamTokenEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ConnectionResult(
    val isConnected: Boolean,
    val latencyMs: Long,
    val serverVersion: String,
    val message: String
)

data class HealthReport(
    val status: String, // "HEALTHY", "DEGRADED", "OFFLINE"
    val latencyMs: Long,
    val serverVersion: String,
    val lastChecked: Long = System.currentTimeMillis(),
    val details: Map<String, String> = emptyMap()
)

data class AgentInfo(
    val id: String,
    val name: String,
    val description: String?,
    val model: String?,
    val systemPrompt: String?,
    val tags: List<String>,
    val createdAt: String?
)

data class ProviderCapabilities(
    val supportsStreaming: Boolean,
    val supportsCoreMemory: Boolean,
    val supportsArchivalMemory: Boolean,
    val supportsTools: Boolean,
    val supportsSources: Boolean,
    val supportsMultimodalImages: Boolean,
    val supportsRawApi: Boolean = true
)

data class AgentRequest(
    val agentId: String,
    val message: String,
    val streaming: Boolean = false
)

data class AgentResponse(
    val responseText: String,
    val reasoningText: String? = null,
    val toolCalls: List<String> = emptyList(),
    val tokensPrompt: Int? = null,
    val tokensCompletion: Int? = null
)

sealed class AgentEvent {
    data class Token(val text: String) : AgentEvent()
    data class Reasoning(val text: String) : AgentEvent()
    data class ToolCall(val name: String, val args: String) : AgentEvent()
    data class Completed(val fullText: String) : AgentEvent()
    data class Error(val error: String) : AgentEvent()
}

data class RawApiRequest(
    val method: String,
    val endpoint: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

interface AgentProvider {
    val providerId: String
    val displayName: String
    val baseUrl: String

    suspend fun connect(): ConnectionResult
    suspend fun healthCheck(): HealthReport
    suspend fun discoverAgents(): List<AgentInfo>
    suspend fun getCapabilities(): ProviderCapabilities
    suspend fun execute(request: AgentRequest): AgentResponse
    fun stream(request: AgentRequest): Flow<AgentEvent>
    suspend fun executeRaw(request: RawApiRequest): RawApiResponse
}

class LettaSelfHostedProvider(
    override val providerId: String,
    override val displayName: String,
    override val baseUrl: String,
    private val apiKey: String
) : AgentProvider {

    private val api: LettaApiService by lazy {
        ApiClient.createService(baseUrl, apiKey)
    }

    override suspend fun connect(): ConnectionResult {
        val start = System.currentTimeMillis()
        return try {
            val health = api.getHealth()
            val latency = System.currentTimeMillis() - start
            ConnectionResult(
                isConnected = true,
                latencyMs = latency,
                serverVersion = health.version ?: "v1.0",
                message = "Connected successfully to Letta Self-hosted at $baseUrl"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            ConnectionResult(
                isConnected = false,
                latencyMs = latency,
                serverVersion = "Unknown",
                message = e.localizedMessage ?: "Failed to connect to $baseUrl"
            )
        }
    }

    override suspend fun healthCheck(): HealthReport {
        val start = System.currentTimeMillis()
        return try {
            val health = api.getHealth()
            val latency = System.currentTimeMillis() - start
            val isHealthy = health.status.equals("ok", ignoreCase = true) || health.status.equals("healthy", ignoreCase = true)
            HealthReport(
                status = if (isHealthy) "HEALTHY" else "DEGRADED",
                latencyMs = latency,
                serverVersion = health.version ?: "v1.0",
                details = mapOf("status" to health.status, "url" to baseUrl)
            )
        } catch (e: Exception) {
            HealthReport(
                status = "OFFLINE",
                latencyMs = System.currentTimeMillis() - start,
                serverVersion = "None",
                details = mapOf("error" to (e.message ?: "Unreachable"))
            )
        }
    }

    override suspend fun discoverAgents(): List<AgentInfo> {
        val agents = api.listAgents(limit = 100)
        return agents.map { dto ->
            AgentInfo(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                model = dto.llm,
                systemPrompt = dto.system,
                tags = dto.tags ?: emptyList(),
                createdAt = dto.createdAt
            )
        }
    }

    override suspend fun getCapabilities(): ProviderCapabilities {
        return ProviderCapabilities(
            supportsStreaming = true,
            supportsCoreMemory = true,
            supportsArchivalMemory = true,
            supportsTools = true,
            supportsSources = true,
            supportsMultimodalImages = true,
            supportsRawApi = true
        )
    }

    override suspend fun execute(request: AgentRequest): AgentResponse {
        val resp = api.sendMessage(
            agentId = request.agentId,
            request = SendMessageRequest(
                messages = listOf(MessageInputDto(role = "user", content = request.message)),
                streaming = false
            )
        )
        val assistantText = StringBuilder()
        val reasoningText = StringBuilder()
        val tools = mutableListOf<String>()

        resp.messages?.forEach { msg ->
            if (msg.messageType == "assistant_message" || msg.role == "assistant") {
                msg.text?.let { assistantText.append(it).append("\n") }
            } else if (msg.messageType == "reasoning_message" || msg.messageType == "internal_monologue") {
                msg.text?.let { reasoningText.append(it).append("\n") }
            } else if (msg.messageType == "tool_call_message") {
                val callDesc = "${msg.toolCall?.name ?: "tool"}(${msg.toolCall?.arguments ?: ""})"
                tools.add(callDesc)
            }
        }

        return AgentResponse(
            responseText = assistantText.toString().trim(),
            reasoningText = if (reasoningText.isNotEmpty()) reasoningText.toString().trim() else null,
            toolCalls = tools,
            tokensPrompt = resp.usage?.promptTokens,
            tokensCompletion = resp.usage?.completionTokens
        )
    }

    override fun stream(request: AgentRequest): Flow<AgentEvent> {
        return ApiClient.streamMessage(
            baseUrl = baseUrl,
            apiKey = apiKey,
            agentId = request.agentId,
            messageText = request.message
        ).map { streamEvent ->
            when (streamEvent) {
                is StreamTokenEvent.Token -> AgentEvent.Token(streamEvent.text)
                is StreamTokenEvent.Reasoning -> AgentEvent.Reasoning(streamEvent.text)
                is StreamTokenEvent.ToolCall -> AgentEvent.ToolCall(streamEvent.name, streamEvent.args)
                is StreamTokenEvent.Done -> AgentEvent.Completed(streamEvent.totalText)
                is StreamTokenEvent.Error -> AgentEvent.Error(streamEvent.message)
                is StreamTokenEvent.Usage -> AgentEvent.Token("")
            }
        }
    }

    override suspend fun executeRaw(request: RawApiRequest): RawApiResponse {
        return ApiClient.executeRaw(
            baseUrl = baseUrl,
            apiKey = apiKey,
            method = request.method,
            endpoint = request.endpoint,
            headers = request.headers,
            requestBodyString = request.body
        )
    }
}
