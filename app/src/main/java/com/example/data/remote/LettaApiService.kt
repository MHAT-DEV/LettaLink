package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LettaApiService {
    @GET("v1/health")
    suspend fun getHealth(): HealthResponse

    @GET("v1/agents")
    suspend fun listAgents(
        @Query("limit") limit: Int = 100,
        @Query("name") name: String? = null
    ): List<LettaAgentDto>

    @GET("v1/agents/{agent_id}")
    suspend fun getAgent(
        @Path("agent_id") agentId: String
    ): LettaAgentDto

    @POST("v1/agents")
    suspend fun createAgent(
        @Body request: CreateAgentRequest
    ): LettaAgentDto

    @DELETE("v1/agents/{agent_id}")
    suspend fun deleteAgent(
        @Path("agent_id") agentId: String
    ): Response<Unit>

    @POST("v1/agents/{agent_id}/messages")
    suspend fun sendMessage(
        @Path("agent_id") agentId: String,
        @Body request: SendMessageRequest
    ): SendMessageResponse

    @GET("v1/agents/{agent_id}/messages")
    suspend fun listMessages(
        @Path("agent_id") agentId: String,
        @Query("limit") limit: Int = 50
    ): List<AgentMessageDto>

    @GET("v1/agents/{agent_id}/core-memory/blocks")
    suspend fun getCoreMemoryBlocks(
        @Path("agent_id") agentId: String
    ): List<BlockDto>

    @PATCH("v1/agents/{agent_id}/core-memory/blocks/{block_label}")
    suspend fun updateCoreMemoryBlock(
        @Path("agent_id") agentId: String,
        @Path("block_label") blockLabel: String,
        @Body body: UpdateBlockRequest
    ): BlockDto

    @GET("v1/agents/{agent_id}/archival-memory")
    suspend fun listArchivalMemory(
        @Path("agent_id") agentId: String,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 50
    ): List<ArchivalMemoryItem>

    @POST("v1/agents/{agent_id}/archival-memory")
    suspend fun insertArchivalMemory(
        @Path("agent_id") agentId: String,
        @Body body: InsertArchivalRequest
    ): ArchivalMemoryItem

    @GET("v1/tools")
    suspend fun listTools(): List<ToolDto>

    @GET("v1/sources")
    suspend fun listSources(): List<SourceDto>

    @GET("v1/models")
    suspend fun listModels(): List<ModelDto>
}
