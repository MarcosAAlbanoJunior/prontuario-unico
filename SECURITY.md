# Security policy

This project stores personal health data, so security reports are taken seriously.

## Reporting a vulnerability

Please **do not open a public issue**. Report privately through [GitHub private vulnerability reporting](https://github.com/MarcosAAlbanoJunior/prontuario-unico/security/advisories/new).

Include the affected version or commit, steps to reproduce and the impact you expect. You should get an answer within 7 days. Once a fix is released, the advisory is published with credit to the reporter, unless you prefer otherwise.

## Supported versions

Before v1.0, only the latest release receives security fixes.

## Scope

prontuario-unico is self-hosted: each person runs and operates their own instance.

In scope, for example:

- Authentication or session bypass in the web dashboard or the API.
- The Telegram bot serving a chat other than the configured one.
- Access to uploaded documents without authentication.
- Clinical content (message text, symptoms, lab values) leaking into logs, or outbound calls other than Telegram and the configured LLM.
- Insecure defaults in the code, the container image or the Docker Compose file.

Out of scope:

- Operational choices of an instance (TLS, firewall, backups, disk encryption), which the README documents as the operator's responsibility.
- What a cloud LLM provider does with the text you choose to send it.
- Vulnerabilities in dependencies that are not exploitable through this project. Please report those upstream.

## Defaults the project keeps

- The app refuses to start without its required secrets; there are no default credentials or demo users.
- The app port is published only on `127.0.0.1`, and the database publishes no port.
- `HttpOnly` and `SameSite=Strict` session cookies, CSRF protection, login rate limiting and a strict Content Security Policy.
- Uploads are checked by content, limited to 20 MB, stored by SHA-256 and downloaded only when authenticated.
- No telemetry.
