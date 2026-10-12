# 0004. Receive Telegram updates by long polling

Date: 2026-10-12 · Status: accepted

## Context

The bot is the main way to record data, so it must be reliable and quick. Telegram delivers updates either by webhook (Telegram calls a public HTTPS endpoint) or by long polling (the app calls `getUpdates` and waits). The app is self-hosted, often on a VPS or a home server that should expose as little as possible, and runs for a single user.

## Decision

- Use long polling with `getUpdates` (`timeout=50`) on one virtual thread, through a plain `RestClient`; no bot SDK (ADR 0001).
- Handle one update at a time. The work for an update and the new offset are stored in the same database transaction; replies are sent only after the commit.
- Persist the last handled `update_id` in `telegram_offset`, so a restart continues where it stopped.
- Serve only `TELEGRAM_ALLOWED_CHAT_ID`. Other chats are ignored, except that `/start` from any chat is answered with that chat's own id, so the owner can find theirs. Chat ids are personal data and are never logged.
- On network errors wait with exponential backoff (1 s to 60 s), and honour `retry_after` on HTTP 429.
- An update whose handling fails is skipped (offset advanced) and the user is asked to try again, so one bad message cannot stop the bot.

## Consequences

- No inbound port, TLS certificate or reverse proxy is needed for the bot; only outbound HTTPS to `api.telegram.org`.
- Replies arrive within about a second of the message, which is enough for one user.
- A crash after the commit and before the reply loses that reply, never the data. A crash before the commit re-handles the update; a unique index on `source.telegram_message_id` and the event `dedupe_key` keep that from recording twice.
- Only one instance may poll a bot token at a time; Telegram rejects concurrent `getUpdates` calls (HTTP 409), which the backoff absorbs.
- The bot token is part of every request URL, so HTTP errors are logged without their cause to keep the token out of logs.
