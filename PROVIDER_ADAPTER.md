# Provider Adapter Architecture

LettaLink defines an open contract `AgentProvider` to decouple the UI from individual server implementations.

```kotlin
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
```

## Implementations

1. **`LettaSelfHostedProvider`**:
   - Default primary provider.
   - Communicates with self-hosted Letta instances on LAN, localhost, or remote servers.
   - Full support for core memory blocks, archival vector memory, tool schema execution, and SSE streaming.

2. **`LettaCloudProvider`**:
   - Targeted at `https://api.letta.com`.
   - Dedicated Bearer authentication handling.

3. **`OpenAiCompatibleProvider`** (Extension Point):
   - Fallback adapter for standard OpenAI completions endpoints.
