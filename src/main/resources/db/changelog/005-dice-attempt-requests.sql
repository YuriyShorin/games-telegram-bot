--liquibase formatted sql

--changeset games-telegram-bot:005-dice-attempt-requests
CREATE TABLE games_bot.dice_attempt_request (
    id UUID PRIMARY KEY,
    duel_id UUID NOT NULL REFERENCES games_bot.dice_duel (id),
    chat_id BIGINT NOT NULL,
    round_number BIGINT NOT NULL CHECK (round_number > 0),
    side VARCHAR(16) NOT NULL CHECK (side IN ('FIRST', 'SECOND')),
    prompted_at TIMESTAMP WITH TIME ZONE,
    deadline TIMESTAMP WITH TIME ZONE,
    message_id BIGINT CHECK (message_id > 0),
    value INTEGER CHECK (value BETWEEN 1 AND 6),
    sent_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (duel_id, round_number, side),
    UNIQUE (chat_id, message_id),
    CHECK ((prompted_at IS NULL AND deadline IS NULL)
        OR (prompted_at IS NOT NULL AND deadline IS NOT NULL AND deadline > prompted_at)),
    CHECK ((message_id IS NULL AND value IS NULL AND sent_at IS NULL)
        OR (message_id IS NOT NULL AND value IS NOT NULL AND sent_at IS NOT NULL
            AND prompted_at IS NOT NULL AND deadline IS NOT NULL
            AND sent_at >= prompted_at AND sent_at <= deadline))
);
