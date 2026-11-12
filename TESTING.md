# Testing Strategy & Guidelines

## 1. Unit Testing
- Room DAO tests and Entity validation.
- Command parser parsing and CLI token tests.
- Provider Adapter contract verification.

## 2. Robolectric JVM Testing
- Critical User Journeys (CUJs) are tested locally via Robolectric in `ExampleRobolectricTest.kt`.
- Server registration and validation flow tests.

## 3. Real Server Integration Testing
- Connection ping verification (`/v1/health`).
- Agent listing (`/v1/agents`).
- Memory block mutation validation (`PATCH /v1/agents/{id}/core-memory/blocks/{label}`).
- SSE Stream consumer verification.
