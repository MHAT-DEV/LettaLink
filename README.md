# LettaLink — Universal AI Agent Console

> **Tagline:** Self-hosted First. Cloud Connected. Agent Agnostic.

> [!WARNING]
> **Non-Production Demo:** This repository was generated with Gemini and is intended strictly for **educational purposes and further development**. It is **not a ready-to-use application** and should not be deployed in production environments.

**LettaLink** is a production-first Android console designed for managing, configuring, communicating with, and debugging stateful AI agents on **Letta (formerly MemGPT)** servers, now bundled with **LettaTalk Universal TTS & Read Aloud**.

Built with a **zero-mock runtime philosophy**, every metric, agent configuration, core memory block, message turn, speech synthesis, and raw HTTP query connects directly to real Letta v1 endpoints, native device engines, or persists within a local Room database.

---

## 🌟 Core Feature Modules

1. **Dashboard (Home)**
   - Live server health probe with latency measurement in milliseconds (`/v1/health`).
   - Server version indicator and active provider status (Self-hosted LAN, VPN, or Cloud).
   - Real-time API audit telemetry stream showing status codes, durations, and endpoints.
   - LettaTalk quick voice engine status and active agent carousel.

2. **Agent Studio**
   - Live query of Letta agents (`/v1/agents`).
   - Real-time agent creator with custom LLM selection, system prompt, and tags.
   - Dedicated **Agent Workspace**:
     - **Core Memory Manager:** In-place editor for `human`, `persona`, and custom memory blocks (`PATCH /v1/agents/{id}/core-memory/blocks/{label}`).
     - **Archival Memory:** Vector search and document passage ingestion.
     - **Tools & MCP Explorer:** View assigned schemas, parameters, and tool statuses.
     - **Data Sources:** Connected RAG sources and vector configurations.

3. **Multimodal AI Chat with LettaTalk**
   - Synchronous and SSE (Server-Sent Events) streaming modes (`/v1/agents/{id}/messages`).
   - Agent reasoning / internal monologue expandable inspection drawer.
   - Tool execution step cards detailing arguments and invocation status.
   - **Per-Message Manual Read Aloud:** Tap speaker button to listen to any response.
   - **Floating Mini Player:** Docked at the bottom with real-time text tracking, playback status, and stop button.
   - **Automatic Read Aloud Engine:** Speaks completed responses automatically (Off, Every Response, Selected Agent, Manual Only).

4. **LettaChronicle (Temporal Knowledge Graph & Markdown Documents)**
   - **Temporal Duality:** Strict separation of Valid Time (`validFrom`, `validTo`) vs Transaction Time (`recordedAt`).
   - **Strict Provenance:** Every fact records source reference and verification status (`USER_MESSAGE`, `AGENT_MESSAGE`, `DOCUMENT`, `MANUAL`).
   - **Non-Destructive Ingestion:** Historical knowledge is superseded with validity end timestamps, never destroyed or erased.
   - **Conflict Detection & Review:** Automatic detection of Direct Contradictions, Temporal Overlaps, Attribute Clashes, and Ambiguous Updates with operator resolution workflow.
   - **Interactive Knowledge Graph Canvas:** Pan, zoom, and inspect entity nodes and temporal relationships rendered directly on a high-performance Compose Canvas.
   - **Markdown Document Manager:** Structured `.md` memory files with standardized YAML frontmatter, version snapshots, and graph-to-document compilation.
   - **Bidirectional Letta Synchronization:** Real PATCH updates to Letta Core Memory blocks (`human`, `persona`, custom) and historical vector archival.

5. **LettaTalk Universal TTS Architecture**
   - **Decoupled Architecture:** Agent Provider and Speech Provider operate independently.
   - **Android Native Device TTS:** 100% offline, local-first, zero-cost, and private. Uses installed system voice engines.
   - **Custom & Self-Hosted Speech Services:** Connect custom OpenAI-compatible speech endpoints (e.g. Kokoro, Piper, ElevenLabs, FastAPI) with model and voice mapping.
   - **Text Normalization Layer:** Strips markdown syntax, filters code blocks, cleans URLs, and segments text into natural sentence boundaries.
   - **Automatic Multilingual Detection:** Detects Thai (`th-TH`), English (`en-US`), Chinese (`zh-CN`), and Japanese (`ja-JP`).
   - **Audio Caching:** Local disk cache with one-tap cache clearing.

6. **Console (CLI & Raw API Explorer)**
   - **CLI Terminal:** Interactive command interface supporting `provider list`, `provider health`, `agent list`, `agent memory list`, `agent tools list`, `agent message <id> <text>`, and `raw <METHOD> <endpoint>`.
   - Autocomplete suggestion chips, monospace viewport, and output copy.
   - **Raw API Explorer:** Direct HTTP method selector (GET, POST, PATCH, PUT, DELETE), JSON payload editor, live response latency tracking, formatted response body viewer, and one-tap **cURL exporter**.

7. **Settings, Discovery & Security**
   - Multi-server manager supporting Self-hosted instances, Letta Cloud, and OpenAI-compatible endpoints.
   - **Local-first Discovery:** Android Network Service Discovery (mDNS/NSD) for `_http._tcp.` alongside quick local subnet probing for port 8283.
   - Dedicated **LettaTalk Provider Manager** and Speech Settings (Rate, Pitch, Auto Detection, Filters).
   - Android Keystore credential safety and redaction in logs.

---

## 🛠️ Technology Stack

- **Platform:** Android (Min SDK 24, Target SDK 36)
- **Language:** Kotlin 2.2.10 (100% Kotlin Coroutines & Flow)
- **UI Toolkit:** Jetpack Compose + Material Design 3 (M3)
- **Speech Engine:** Android Native `TextToSpeech` + Android `MediaPlayer` for custom audio streams
- **Local Database:** Android Room 2.7.0 with Kotlin Symbol Processing (KSP)
- **Networking:** OkHttp 4.10, Retrofit 2.12, Moshi 1.15 with reflection codegen
- **Network Discovery:** Android NsdManager (DNS-SD / mDNS)
