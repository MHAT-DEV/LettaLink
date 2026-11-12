# Extension Specification

LettaLink provides three extension integration points:

## 1. Provider Extensions
New server types can be added by implementing `AgentProvider`.
- Must provide capability reporting via `getCapabilities()`.
- Must support fallback execution when streaming is unsupported.

## 2. Tool Extensions (MCP & Sandboxed Tools)
- LettaLink reads tool JSON schemas from `/v1/tools`.
- Tools can be reviewed, edited, or invoked through dynamic form generation based on the schema properties.

## 3. Interface Extensions
- CLI Command parser in `ConsoleRepository` is extensible by matching new command tokens (e.g. `agent memory export`, `agent backup`).
