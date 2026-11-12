package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String = "unknown",
    @Json(name = "version") val version: String? = null
)

@JsonClass(generateAdapter = true)
data class LettaAgentDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "system") val system: String? = null,
    @Json(name = "llm") val llm: String? = null,
    @Json(name = "embedding") val embedding: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList(),
    @Json(name = "memory") val memory: MemoryDto? = null,
    @Json(name = "tools") val tools: List<ToolDto>? = emptyList(),
    @Json(name = "sources") val sources: List<SourceDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class MemoryDto(
    @Json(name = "blocks") val blocks: List<BlockDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class BlockDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "value") val value: String = "",
    @Json(name = "limit") val limit: Int? = null,
    @Json(name = "is_read_only") val isReadOnly: Boolean? = false,
    @Json(name = "description") val description: String? = null
) {
    val blockName: String
        get() = name ?: label ?: "block"
}

@JsonClass(generateAdapter = true)
data class CreateAgentRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "system") val system: String? = null,
    @Json(name = "llm") val llm: String? = null,
    @Json(name = "embedding") val embedding: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList(),
    @Json(name = "memory_blocks") val memoryBlocks: List<CreateBlockDto>? = null
)

@JsonClass(generateAdapter = true)
data class CreateBlockDto(
    @Json(name = "name") val name: String,
    @Json(name = "value") val value: String,
    @Json(name = "limit") val limit: Int? = null
)

@JsonClass(generateAdapter = true)
data class UpdateBlockRequest(
    @Json(name = "value") val value: String
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "messages") val messages: List<MessageInputDto>,
    @Json(name = "streaming") val streaming: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MessageInputDto(
    @Json(name = "role") val role: String = "user",
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class SendMessageResponse(
    @Json(name = "messages") val messages: List<AgentMessageDto>? = emptyList(),
    @Json(name = "usage") val usage: UsageDto? = null
)

@JsonClass(generateAdapter = true)
data class AgentMessageDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "message_type") val messageType: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "tool_call") val toolCall: ToolCallDto? = null,
    @Json(name = "tool_return") val toolReturn: String? = null
)

@JsonClass(generateAdapter = true)
data class ToolCallDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "arguments") val arguments: String? = null
)

@JsonClass(generateAdapter = true)
data class ToolDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "source_type") val sourceType: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchivalMemoryItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "text") val text: String = "",
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class InsertArchivalRequest(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class UsageDto(
    @Json(name = "completion_tokens") val completionTokens: Int? = null,
    @Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @Json(name = "total_tokens") val totalTokens: Int? = null,
    @Json(name = "step_count") val stepCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class ModelDto(
    @Json(name = "name") val name: String,
    @Json(name = "model_endpoint_type") val modelEndpointType: String? = null
)
