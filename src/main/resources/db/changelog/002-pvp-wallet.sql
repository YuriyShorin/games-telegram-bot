--liquibase formatted sql

--changeset games-telegram-bot:002-pvp-wallet
CREATE TABLE games_bot.pvp_wallet (
    chat_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL CHECK (player_id > 0),
    available NUMERIC NOT NULL CHECK (available >= 0 AND scale(available) <= 2),
    committed NUMERIC NOT NULL CHECK (committed >= 0 AND scale(committed) <= 2),
    last_recovery_date DATE,
    PRIMARY KEY (chat_id, player_id)
);

CREATE TABLE games_bot.duel_bank (
    id UUID PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    first_player_id BIGINT NOT NULL,
    second_player_id BIGINT NOT NULL,
    stake NUMERIC NOT NULL CHECK (stake >= 10 AND scale(stake) <= 2),
    status VARCHAR(16) NOT NULL CHECK (status IN ('LOCKED', 'WON', 'FORFEITED', 'INTERRUPTED')),
    winner_id BIGINT,
    CHECK (first_player_id <> second_player_id),
    CHECK (
        (status IN ('LOCKED', 'INTERRUPTED') AND winner_id IS NULL)
        OR (status IN ('WON', 'FORFEITED') AND winner_id IS NOT NULL
            AND winner_id IN (first_player_id, second_player_id))
    ),
    FOREIGN KEY (chat_id, first_player_id) REFERENCES games_bot.pvp_wallet (chat_id, player_id)
        DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (chat_id, second_player_id) REFERENCES games_bot.pvp_wallet (chat_id, player_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE games_bot.pvp_ledger (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    reason VARCHAR(32) NOT NULL CHECK (
        reason IN ('STARTING_GRANT', 'DUEL_COMMITMENT', 'WON', 'FORFEITED', 'INTERRUPTED', 'DAILY_RECOVERY')
    ),
    available_delta NUMERIC NOT NULL CHECK (scale(available_delta) <= 2),
    committed_delta NUMERIC NOT NULL CHECK (scale(committed_delta) <= 2),
    duel_id UUID REFERENCES games_bot.duel_bank (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id, player_id) REFERENCES games_bot.pvp_wallet (chat_id, player_id)
);

CREATE INDEX pvp_ledger_wallet_idx ON games_bot.pvp_ledger (chat_id, player_id, id);
CREATE INDEX pvp_ledger_duel_idx ON games_bot.pvp_ledger (duel_id);
