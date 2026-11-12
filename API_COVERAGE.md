# Letta API Coverage Matrix

| Endpoint | HTTP Method | Request Payload | Response Model | Status | UI Mapping |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/v1/health` | `GET` | None | `HealthResponse` | Supported | Home ping card, Settings ping, CLI `provider health` |
| `/v1/agents` | `GET` | Query `limit`, `name` | `List<LettaAgentDto>` | Supported | Agents Screen list, Home recent agents carousel |
| `/v1/agents` | `POST` | `CreateAgentRequest` | `LettaAgentDto` | Supported | Create Agent Dialog |
| `/v1/agents/{id}` | `GET` | Path `agent_id` | `LettaAgentDto` | Supported | Agent Workspace details, CLI `agent get` |
| `/v1/agents/{id}` | `DELETE` | Path `agent_id` | `Unit` (200/204) | Supported | Delete Agent action |
| `/v1/agents/{id}/messages` | `POST` | `SendMessageRequest` | `SendMessageResponse` | Supported | Chat Screen (sync mode), CLI `agent message` |
| `/v1/agents/{id}/messages` | `POST` (SSE) | `streaming: true` | `text/event-stream` chunks | Supported | Chat Screen (streaming mode with tokens) |
| `/v1/agents/{id}/messages` | `GET` | Query `limit` | `List<AgentMessageDto>` | Supported | Chat Screen message history |
| `/v1/agents/{id}/core-memory/blocks` | `GET` | Path `agent_id` | `List<BlockDto>` | Supported | Agent Workspace (Core Memory tab), CLI `agent memory list` |
| `/v1/agents/{id}/core-memory/blocks/{label}` | `PATCH` | `UpdateBlockRequest` | `BlockDto` | Supported | Live inline memory editor in Agent Workspace |
| `/v1/agents/{id}/archival-memory` | `GET` | Query `query`, `limit` | `List<ArchivalMemoryItem>` | Supported | Agent Workspace (Archival Memory tab) |
| `/v1/agents/{id}/archival-memory` | `POST` | `InsertArchivalRequest` | `ArchivalMemoryItem` | Supported | Archival Memory insert form |
| `/v1/tools` | `GET` | None | `List<ToolDto>` | Supported | Agent Workspace (Tools tab), CLI `agent tools list` |
| `/v1/sources` | `GET` | None | `List<SourceDto>` | Supported | Agent Workspace (Sources tab), CLI `agent sources list` |
| `/v1/models` | `GET` | None | `List<ModelDto>` | Supported | Raw API Explorer / Model picker |
| Any custom endpoint | ANY | Raw string / JSON | `RawApiResponse` | Supported | Raw API Explorer with cURL generator |
