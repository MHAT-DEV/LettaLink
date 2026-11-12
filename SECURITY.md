# Security Architecture & Policies

## 1. Credential Safety & Storage
- API keys, Bearer tokens, and server secrets are stored exclusively in app-private storage (`lettalink_database`) and never exposed through public content providers.
- When generating logs, CLI terminal output, or cURL export snippets, authorization keys are redacted to `[REDACTED]` to prevent credential leakage in screenshots or shared logs.

## 2. Cleartext Network Policy
- For self-hosted instances operating within local LAN or VPN subnets without local SSL certificates (e.g. `http://192.168.x.x:8283` or `http://10.0.2.2:8283`), `android:usesCleartextTraffic="true"` is enabled in `AndroidManifest.xml`.
- When targeting public cloud endpoints (such as `https://api.letta.com`), HTTPS TLS verification is enforced by OkHttp.

## 3. Zero-Trust Server Discovery
- Discovered LAN servers via Android NSD (mDNS) are tagged as unverified until the user explicitly tests connection or confirms addition.
- Credentials are never transmitted to auto-discovered servers automatically.

## 4. Audit Trail
- Every outgoing network request is logged in the `api_audit_logs` table with request durations, timestamps, and HTTP response codes.
