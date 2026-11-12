# LettaLink System Architecture

## Architectural Overview

LettaLink employs **Clean Architecture** paired with the **Repository Pattern** and **Unidirectional Data Flow (UDF)** in Jetpack Compose:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                         │
│  MainActivity ───► Screens (Home, Agents, Chat, Chronicle,      │
│                            Console, Settings)                   │
│                             ▲                                   │
│                             │ StateFlow / Events                │
│                      MainViewModel                              │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                        Domain Layer                             │
│   • AgentProvider Contract ───► LettaSelfHostedProvider         │
│   • LettaChronicle Engines:                                     │
│     - TemporalReasoningEngine (Duality, Non-destructive TKG)    │
│     - MarkdownDocumentEngine (YAML frontmatter, Revisions)      │
│     - LettaMemorySyncEngine (Core & Archival Sync)              │
│   • LettaTalk Universal TTS Manager:                            │
│     - AndroidNativeTtsEngine (Local offline)                    │
│     - CustomHttpTtsEngine (OpenAI-compatible)                   │
│     - TextNormalization & SentenceSegmenter                     │
│   • LanDiscoveryService (mDNS / NSD & Port 8283 Probe)          │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                         Data Layer                              │
│   Repositories: ServerRepo, AgentRepo, ChatRepo, ConsoleRepo    │
│                │                               │                │
│                ▼                               ▼                │
│         Local Persistence               Remote Network          │
│       (Room AppDatabase v3)          (OkHttp + Retrofit + SSE)  │
│       • servers                      • LettaApiService (/v1)    │
│       • command_history              • ApiClient Raw Engine     │
│       • api_audit_logs               • SSE Stream Reader        │
│       • tts_providers & settings     • Core Memory PATCH        │
│       • chronicle_entities           • Archival Memory POST     │
│       • chronicle_relationships                                 │
│       • chronicle_facts                                         │
│       • chronicle_conflicts                                     │
│       • chronicle_documents & versions                          │
│       • chronicle_sync_history                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Key Architectural Principles

1. **Production-First, Zero-Mock Runtime**
   Every action dispatched in the UI directly hits the active Letta server via `LettaApiService` or executes raw network calls through `ApiClient.executeRaw`.
2. **Temporal Knowledge Graph Duality (LettaChronicle)**
   Strictly separates Valid Time (when a fact is true in the world) from Transaction Time (when the system recorded it). Historical facts are non-destructively superseded with `validTo` timestamps.
3. **Strict Provenance & Verification**
   Every knowledge fact holds its originating source reference (`USER_MESSAGE`, `AGENT_MESSAGE`, `DOCUMENT`, `MANUAL`) and confidence score. Contradictions trigger operator review workflows.
4. **Bidirectional Markdown & Core Memory Consistency**
   Markdown memory files are tracked with standardized YAML frontmatter, monotonically increasing revisions, and live graph-to-document compilation. The sync engine translates active graph facts into concise memory blocks for Letta's `PATCH /v1/agents/{id}/core-memory/blocks/{block_label}`.
5. **Decoupled Universal TTS (LettaTalk)**
   Speech synthesis is decoupled from agent providers, supporting 100% offline local Android Native TTS alongside remote OpenAI-compatible speech services.
6. **Local Audit & Discovery Layer**
   Outbound HTTP communications are logged with millisecond duration and HTTP status codes into Room (`api_audit_logs`). Local network servers are discovered via Android `NsdManager`.
