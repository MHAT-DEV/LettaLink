# Changelog

## [1.2.0] - LettaChronicle Temporal Knowledge Graph & Markdown Memory Release

### Added
- **LettaChronicle Module**: Temporal Knowledge Graph (TKG) and Markdown document management system for AI agents.
- **Temporal Duality Engine**: Strict separation of Valid Time (`validFrom`, `validTo`) vs Transaction Time (`recordedAt`).
- **Strict Provenance & Verification**: Every fact requires verifiable source citation, source category (`USER_MESSAGE`, `AGENT_MESSAGE`, `DOCUMENT`, `MANUAL`), and confidence score.
- **Non-Destructive Memory Updates**: Historical statements are superseded with validTo timestamps, never erased or overwritten.
- **Automatic Conflict Detection & Review**: Detects Direct Contradictions, Temporal Overlaps, Attribute Clashes, and Ambiguous Updates with interactive 3-way operator resolution.
- **Interactive Knowledge Graph Canvas**: High-performance Compose Canvas with orbit node layout, entity coloring by type (`PERSON`, `ORGANIZATION`, `CONCEPT`, `PROJECT`), directed relationship arrows, touch-to-inspect drawers, pan, and pinch-to-zoom.
- **Temporal Timeline View**: Chronological audit trail showing active vs superseded facts with Valid Time windows and recorded timestamps.
- **Markdown Document Manager with YAML Frontmatter**: Structured memory files (`profile.md`, `projects.md`) with standardized frontmatter, revision tracking, and revision snapshots for non-destructive diffs.
- **Graph-to-Markdown Compilation**: Automatic translation of graph entities and temporal facts into compiled Markdown documents.
- **Bidirectional Core Memory Sync Engine**: Synthesizes verified active facts into high-density Markdown blocks and sends real `PATCH /v1/agents/{id}/core-memory/blocks/{block_label}` requests to Letta.
- **Archival Memory Bridge**: Direct export of historical facts to Letta's vector memory via `POST /v1/agents/{id}/archival-memory`.
- **Database Schema v3**: Added 7 new Room entities and DAOs (`chronicle_entities`, `chronicle_relationships`, `chronicle_facts`, `chronicle_conflicts`, `chronicle_documents`, `chronicle_doc_versions`, `chronicle_sync_history`).
- **JVM Unit Test Suite**: `ChronicleEngineTest` covering entity resolution, temporal reasoning, non-destructive superseding, and markdown frontmatter formatting.

## [1.1.0] - LettaTalk Universal TTS & Read Aloud Release

### Added
- **LettaTalk Universal TTS Engine**: Decoupled, multilingual, lightweight speech synthesis module.
- **Android Native Device TTS**: Built-in, offline, local-first speech synthesis using Android `TextToSpeech` engine without external cloud dependencies.
- **Custom / Self-Hosted TTS Provider**: Support for OpenAI-compatible audio speech endpoints (`v1/audio/speech`, Kokoro, Piper, ElevenLabs, FastAPI) with local MP3 disk cache.
- **Manual Read Aloud**: Per-message Play/Stop button on assistant chat messages with animated speaking status.
- **Docked Mini Player**: Bottom player floating above the chat input field showing agent name, reading text snippet, and stop control.
- **Automatic Read Aloud**: Option to read incoming agent responses automatically (`OFF`, `EVERY_RESPONSE`, `SELECTED_AGENT_ONLY`, `MANUAL_ONLY`).
- **Text Normalization Pipeline**: Markdown syntax stripping, code block filtering, URL handling, sentence segmentation, and Thai/CJK/Latin locale auto-detection.
- **Speech Settings & Controls**: Dynamic speech rate slider (0.5x - 2.0x), pitch slider (0.5x - 1.5x), content filter toggles, live voice test button, and audio cache clearing.
- **Database Schema v2**: Added `tts_providers` and `tts_settings` Room entities and DAOs.

## [1.0.0] - Initial Production Release

### Added
- **Production-First Core**: Real Letta v1 REST API client and SSE streaming engine.
- **Provider Architecture**: Implemented `AgentProvider` contract and `LettaSelfHostedProvider`.
- **Home Dashboard**: Live health check, latency badge, server selector, metrics cards, recent agents, and real-time telemetry stream.
- **Agent Studio**: Agent listing, filtering by name/tag, agent creation, and deletion.
- **Agent Workspace**:
  - Live core memory blocks viewer and inline server-synchronized editor.
  - Vector archival memory passage search and insertion.
  - Installed tools and JSON schemas browser.
  - Attached document RAG sources viewer.
- **Multimodal AI Chat**:
  - SSE real-time token streaming with cancel control.
  - Expandable agent reasoning / internal monologue drawer.
  - Tool execution cards detailing function call parameters.
- **Console Suite**:
  - Interactive CLI Terminal with command parser, history, and autocomplete chips.
  - Raw API Explorer supporting all HTTP methods, JSON editor, latency tracking, and cURL export.
- **Settings & Discovery**:
  - Multi-server manager (Self-hosted, Letta Cloud, Custom).
  - Android NSD (mDNS) Wi-Fi service discovery and quick local subnet probe.
  - Security audit logs and Android Keystore storage.
- **Custom Adaptive Launcher Icon**: Cyan & amber glowing neural link glyph centered on `#0B132B`.
- **Full Documentation**: README, ARCHITECTURE, API_COVERAGE, CAPABILITY_MATRIX, DATABASE_SCHEMA, SECURITY, PROVIDER_ADAPTER, EXTENSION_SPEC, BUILD, TESTING, and CHANGELOG.
