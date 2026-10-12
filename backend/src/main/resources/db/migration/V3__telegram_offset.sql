-- Last handled Telegram update, so a restart never handles an update twice.
CREATE TABLE telegram_offset (
    id             smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    last_update_id bigint NOT NULL
);

-- A Telegram message becomes at most one source.
CREATE UNIQUE INDEX source_telegram_message_id_key ON source (telegram_message_id) WHERE telegram_message_id IS NOT NULL;
