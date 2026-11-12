# LettaLink Capability Matrix

| Capability | API Supported | Letta Self-Hosted | Letta Cloud | UI Handling |
| :--- | :--- | :--- | :--- | :--- |
| **Agent Listing & Inspection** | Yes | Yes (v1) | Yes (v1) | Full Card & Workspace View |
| **Agent Creation** | Yes | Yes (v1) | Yes (v1) | Modal dialog with system prompts & tags |
| **Agent Deletion** | Yes | Yes (v1) | Yes (v1) | Confirmative dialog with destructive styling |
| **Real-Time Message Exchange** | Yes | Yes | Yes | Multi-turn Chat Screen |
| **Server-Sent Events (SSE) Streaming** | Yes | Yes | Yes | Real-time token streaming with cancel control |
| **Internal Monologue / Reasoning** | Yes | Yes | Yes | Collapsible Amber card with raw thought inspection |
| **Tool Invocation Inspection** | Yes | Yes | Yes | Emerald neon card detailing function name & JSON args |
| **Core Memory Editing** | Yes | Yes | Yes | In-place editor with direct PATCH to server |
| **Archival Vector Memory** | Yes | Yes | Yes | Search query and text passage submission |
| **Tools Discovery** | Yes | Yes | Yes | Schema viewer |
| **Document Sources (RAG)** | Yes | Yes | Yes | Attached source viewer |
| **Temporal Knowledge Graph (TKG)** | Local Engine + Remote Sync | Yes | Yes | Interactive Canvas orbit view with node/edge inspection |
| **Non-Destructive Memory Superseding**| Local Engine | Yes | Yes | Valid-time timeline with history preservation |
| **Conflict Detection & Review** | Local Engine | Yes | Yes | Dedicated review tab with 3-way resolution actions |
| **Markdown Memory Documents** | Local Room + YAML Frontmatter | Yes | Yes | In-app editor with revisions and diff versions |
| **Graph-to-Core Memory Synthesis** | Yes (PATCH blocks) | Yes | Yes | Automatic formatting and live sync to Core Memory |
| **Universal Read Aloud (LettaTalk)**| Android Local + Cloud TTS | Yes | Yes | Floating mini-player & per-turn speech actions |
| **Offline Android Native TTS** | Android Native API | Yes | Yes | 100% offline device voice engine |
| **Custom Speech Services (HTTP)** | OpenAI-compatible API | Yes | Yes | Custom endpoints (Kokoro, Piper, ElevenLabs) |
| **Local Subnet Discovery** | Android NsdManager | Yes (mDNS & probe) | N/A | Wi-Fi NSD scan + port 8283 probe |
| **Raw HTTP & cURL Export** | Yes | Yes | Yes | Arbitrary method/endpoint console |
