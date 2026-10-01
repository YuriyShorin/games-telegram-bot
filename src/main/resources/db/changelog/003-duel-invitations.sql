--liquibase formatted sql

--changeset games-telegram-bot:003-duel-invitations
CREATE TABLE games_bot.duel_invitation (
    id UUID PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    challenger_id BIGINT NOT NULL CHECK (challenger_id > 0),
    opponent_id BIGINT NOT NULL CHECK (opponent_id > 0),
    stake NUMERIC NOT NULL CHECK (stake >= 10 AND scale(stake) <= 2),
    format VARCHAR(16) NOT NULL CHECK (format IN ('FIVE_ROUNDS', 'SEVEN_ROUNDS', 'UNTIL_VICTORY')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    CHECK (challenger_id <> opponent_id)
);
