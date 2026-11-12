# Local Database Schema (Room)

LettaLink stores local server configurations, CLI history, API audit logs, speech providers, and the complete LettaChronicle Temporal Knowledge Graph and Markdown memory files within SQLite database `lettalink_database` (Schema Version 3).

---

## 1. Core Server & Diagnostics Tables

### Table: `servers`
Stores configured Letta and provider server profiles.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | PRIMARY KEY AUTOINCREMENT | Unique server identifier |
| `name` | `TEXT` | NOT NULL | Friendly label (e.g. Local Letta) |
| `providerType` | `TEXT` | NOT NULL | `SELF_HOSTED`, `LETTA_CLOUD`, `OPENAI_COMPATIBLE` |
| `baseUrl` | `TEXT` | NOT NULL | Base URL (e.g. `http://10.0.2.2:8283`) |
| `apiKey` | `TEXT` | NOT NULL DEFAULT '' | Bearer authorization token |
| `isPrimary` | `INTEGER` | NOT NULL DEFAULT 0 | 1 if active default server, 0 otherwise |
| `isFavorite` | `INTEGER` | NOT NULL DEFAULT 0 | Starred server flag |
| `lastHealthStatus`| `TEXT` | NOT NULL | `HEALTHY`, `DEGRADED`, `OFFLINE`, `UNCHECKED` |
| `lastVersion` | `TEXT` | NOT NULL DEFAULT '' | Server version from `/v1/health` |
| `lastLatencyMs` | `INTEGER` | NOT NULL DEFAULT 0 | Roundtrip latency in ms |
| `lastCheckedAt` | `INTEGER` | NOT NULL DEFAULT 0 | Epoch timestamp of last ping |
| `createdAt` | `INTEGER` | NOT NULL | Epoch creation timestamp |

### Table: `command_history`
Stores interactive CLI commands and execution snippets.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | PRIMARY KEY AUTOINCREMENT | Unique command entry ID |
| `serverId` | `INTEGER` | NOT NULL | Server against which command was dispatched |
| `command` | `TEXT` | NOT NULL | Exact command string |
| `isSuccess` | `INTEGER` | NOT NULL | Execution status |
| `output` | `TEXT` | NOT NULL | Snippet of output response |
| `executedAt` | `INTEGER` | NOT NULL | Timestamp |

### Table: `api_audit_logs`
Audit record of every outbound network request.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | PRIMARY KEY AUTOINCREMENT | Unique audit entry ID |
| `serverId` | `INTEGER` | NOT NULL | Associated server ID |
| `method` | `TEXT` | NOT NULL | HTTP Method (`GET`, `POST`, etc.) |
| `endpoint` | `TEXT` | NOT NULL | Relative endpoint (`/v1/...`) |
| `statusCode` | `INTEGER` | NOT NULL | HTTP response code (e.g. 200, 404, 0) |
| `durationMs` | `INTEGER` | NOT NULL | Execution time in milliseconds |
| `requestSnippet`| `TEXT` | NOT NULL DEFAULT '' | Body/parameter preview |
| `responseSnippet`| `TEXT` | NOT NULL DEFAULT '' | Response preview |
| `isSuccess` | `INTEGER` | NOT NULL | Boolean success flag |
| `timestamp` | `INTEGER` | NOT NULL | Timestamp |

### Table: `cached_agents`
Stores cached agent metadata for fast offline viewing.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `agentId` | `TEXT` | PRIMARY KEY | Letta agent UUID |
| `serverId` | `INTEGER` | NOT NULL | Associated server profile ID |
| `name` | `TEXT` | NOT NULL | Agent label |
| `description` | `TEXT` | NOT NULL DEFAULT '' | Description text |
| `model` | `TEXT` | NOT NULL DEFAULT '' | LLM model identifier |
| `systemPrompt` | `TEXT` | NOT NULL DEFAULT '' | System instructions |
| `tagsJson` | `TEXT` | NOT NULL DEFAULT '[]' | JSON array of tags |
| `updatedAt` | `INTEGER` | NOT NULL | Timestamp |

---

## 2. LettaTalk Universal TTS Tables

### Table: `tts_providers`
Stores custom speech synthesis server profiles.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | PRIMARY KEY AUTOINCREMENT | Provider ID |
| `name` | `TEXT` | NOT NULL | Display name |
| `providerType` | `TEXT` | NOT NULL | `ANDROID_NATIVE`, `OPENAI_COMPATIBLE` |
| `baseUrl` | `TEXT` | NOT NULL DEFAULT '' | Endpoint URL |
| `endpoint` | `TEXT` | NOT NULL DEFAULT '/v1/audio/speech' | Speech generation path |
| `apiKey` | `TEXT` | NOT NULL DEFAULT '' | Authorization token |
| `model` | `TEXT` | NOT NULL DEFAULT 'tts-1' | Model identifier |
| `defaultVoice` | `TEXT` | NOT NULL DEFAULT 'alloy' | Voice name |
| `isDefault` | `INTEGER` | NOT NULL DEFAULT 0 | Default speech provider flag |

### Table: `tts_settings`
Global speech configuration.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | PRIMARY KEY | Settings singleton ID |
| `autoReadMode` | `TEXT` | NOT NULL | `OFF`, `EVERY_RESPONSE`, `SELECTED_AGENT_ONLY`, `MANUAL_ONLY` |
| `selectedAgentId`| `TEXT` | NULLABLE | Agent bound to auto-read |
| `speechRate` | `REAL` | NOT NULL DEFAULT 1.0 | Playback rate (0.5 to 2.0) |
| `pitch` | `REAL` | NOT NULL DEFAULT 1.0 | Voice pitch (0.5 to 2.0) |
| `languageAutoDetect` | `INTEGER` | NOT NULL DEFAULT 1 | Multilingual auto-detection |
| `filterCodeBlocks` | `INTEGER` | NOT NULL DEFAULT 1 | Strip code markdown |
| `filterMarkdown` | `INTEGER` | NOT NULL DEFAULT 1 | Strip headers and bold text |
| `filterUrls` | `INTEGER` | NOT NULL DEFAULT 1 | Strip web URLs |

---

## 3. LettaChronicle: Temporal Knowledge Graph & Docs Tables

### Table: `chronicle_entities`
Named entities tracked within an agent's knowledge graph.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `entityId` | `TEXT` | PRIMARY KEY | Entity identifier (`ent-...`) |
| `agentId` | `TEXT` | NOT NULL | Letta agent UUID |
| `entityType` | `TEXT` | NOT NULL | `PERSON`, `ORGANIZATION`, `LOCATION`, `PROJECT`, `CONCEPT`, `TOOL`, `EVENT`, `SYSTEM`, `DOCUMENT` |
| `canonicalName` | `TEXT` | NOT NULL | Canonical resolved name |
| `aliasesJson` | `TEXT` | NOT NULL DEFAULT '[]' | JSON array of aliases |
| `description` | `TEXT` | NOT NULL DEFAULT '' | Entity description summary |
| `attributesJson`| `TEXT` | NOT NULL DEFAULT '{}' | Structured key-value attributes |
| `status` | `TEXT` | NOT NULL DEFAULT 'ACTIVE' | `ACTIVE`, `DEPRECATED`, `MERGED` |
| `createdAt` | `INTEGER` | NOT NULL | Creation timestamp |
| `updatedAt` | `INTEGER` | NOT NULL | Last update timestamp |

### Table: `chronicle_relationships`
Directed edges between entity nodes.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `relationshipId`| `TEXT` | PRIMARY KEY | Relationship UUID (`rel-...`) |
| `agentId` | `TEXT` | NOT NULL | Letta agent UUID |
| `sourceEntityId`| `TEXT` | NOT NULL | Origin entity ID |
| `relationshipType`| `TEXT` | NOT NULL | `WORKS_AT`, `OWNS`, `LOCATED_IN`, `CREATED_BY`, `MEMBER_OF`, `DEPENDS_ON`, `USES`, `RELATED_TO`, `REPLACED_BY`, `PART_OF`, `KNOWS`, `PARTICIPATES_IN`, `CUSTOM` |
| `targetEntityId`| `TEXT` | NOT NULL | Target entity ID |
| `validFrom` | `INTEGER` | NULLABLE | Valid Time start timestamp |
| `validTo` | `INTEGER` | NULLABLE | Valid Time end timestamp (NULL if active) |
| `recordedAt` | `INTEGER` | NOT NULL | Transaction Time |
| `confidence` | `REAL` | NOT NULL DEFAULT 1.0 | Confidence score (0.0 to 1.0) |
| `sourceRef` | `TEXT` | NOT NULL DEFAULT '' | Provenance citation |
| `status` | `TEXT` | NOT NULL DEFAULT 'ACTIVE' | `ACTIVE`, `SUPERSEDED`, `CONTRADICTED` |

### Table: `chronicle_facts`
Atomic temporal facts with duality of Valid Time vs Transaction Time.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `factId` | `TEXT` | PRIMARY KEY | Fact UUID (`fact-...`) |
| `agentId` | `TEXT` | NOT NULL | Letta agent UUID |
| `subject` | `TEXT` | NOT NULL | Subject phrase |
| `predicate` | `TEXT` | NOT NULL | Predicate relationship |
| `objectValue` | `TEXT` | NOT NULL | Object phrase or literal |
| `validFrom` | `INTEGER` | NULLABLE | When fact became true in reality |
| `validTo` | `INTEGER` | NULLABLE | When fact ceased to be true (NULL if currently valid) |
| `recordedAt` | `INTEGER` | NOT NULL | Transaction time when system learned the fact |
| `source` | `TEXT` | NOT NULL DEFAULT '' | Source utterance or document |
| `sourceType` | `TEXT` | NOT NULL | `USER_MESSAGE`, `AGENT_MESSAGE`, `DOCUMENT`, `MANUAL`, `IMPORTED` |
| `confidence` | `REAL` | NOT NULL DEFAULT 1.0 | Probability score |
| `verificationStatus` | `TEXT` | NOT NULL | `VERIFIED`, `UNVERIFIED`, `CONTRADICTED` |
| `supersedesFactId` | `TEXT` | NULLABLE | ID of older fact superseded by this entry |
| `contradictsFactId` | `TEXT` | NULLABLE | ID of conflicting fact |
| `metadataJson` | `TEXT` | NOT NULL DEFAULT '{}' | Metadata JSON |

### Table: `chronicle_conflicts`
Detected knowledge contradictions and temporal overlaps.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `conflictId` | `TEXT` | PRIMARY KEY | Conflict ID (`conf-...`) |
| `agentId` | `TEXT` | NOT NULL | Letta agent UUID |
| `conflictType` | `TEXT` | NOT NULL | `DIRECT_CONTRADICTION`, `TEMPORAL_OVERLAP`, `ATTRIBUTE_CONFLICT`, `RELATIONSHIP_CONFLICT`, `AMBIGUOUS_UPDATE` |
| `factIdA` | `TEXT` | NOT NULL | First conflicting fact |
| `factIdB` | `TEXT` | NOT NULL | Second conflicting fact |
| `description` | `TEXT` | NOT NULL | Human-readable explanation of contradiction |
| `status` | `TEXT` | NOT NULL DEFAULT 'OPEN' | `OPEN`, `RESOLVED_A`, `RESOLVED_B`, `RESOLVED_BOTH_VALID`, `DISMISSED` |
| `detectedAt` | `INTEGER` | NOT NULL | Detection timestamp |

### Table: `chronicle_documents`
Markdown memory files linked to graph revisions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `documentId` | `TEXT` | PRIMARY KEY | Document UUID (`doc-...`) |
| `agentId` | `TEXT` | NOT NULL | Letta agent UUID |
| `title` | `TEXT` | NOT NULL | Document title |
| `fileName` | `TEXT` | NOT NULL | e.g. `profile.md`, `projects.md` |
| `content` | `TEXT` | NOT NULL | Full markdown with YAML frontmatter |
| `knowledgeRevision` | `INTEGER` | NOT NULL DEFAULT 1 | Monotonically increasing revision |
| `syncStatus` | `TEXT` | NOT NULL DEFAULT 'SYNCHRONIZED' | `SYNCHRONIZED`, `OUTDATED`, `NEEDS_REVIEW`, `CONFLICT` |
| `createdAt` | `INTEGER` | NOT NULL | Document creation timestamp |
| `updatedAt` | `INTEGER` | NOT NULL | Last update timestamp |

### Table: `chronicle_doc_versions`
Audit log of document versions for non-destructive diff and rollbacks.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `versionId` | `TEXT` | PRIMARY KEY | Version UUID (`ver-...`) |
| `documentId` | `TEXT` | NOT NULL | Parent document ID |
| `revisionNumber` | `INTEGER` | NOT NULL | Snapshot revision number |
| `content` | `TEXT` | NOT NULL | Complete markdown snapshot |
| `changeSummary` | `TEXT` | NOT NULL DEFAULT '' | Reason or description for update |
| `committedAt` | `INTEGER` | NOT NULL | Snapshot timestamp |

### Table: `chronicle_sync_history`
Audit records of synchronization events to Letta Core and Archival memory.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `syncId` | `TEXT` | PRIMARY KEY | Sync transaction UUID (`sync-...`) |
| `agentId` | `TEXT` | NOT NULL | Letta agent UUID |
| `targetType` | `TEXT` | NOT NULL | `CORE_MEMORY`, `ARCHIVAL_MEMORY` |
| `blockLabel` | `TEXT` | NOT NULL | Memory block name (e.g. `human`, `persona`) |
| `factsCount` | `INTEGER` | NOT NULL | Count of active facts synthesized |
| `syncStatus` | `TEXT` | NOT NULL | `SUCCESS`, `FAILED`, `REVIEW_REQUIRED` |
| `details` | `TEXT` | NOT NULL DEFAULT '' | Response status or error detail |
| `timestamp` | `INTEGER` | NOT NULL | Sync execution timestamp |
