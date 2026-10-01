--liquibase formatted sql

--changeset games-telegram-bot:004-dice-duels
CREATE TABLE games_bot.dice_duel (
    id UUID PRIMARY KEY REFERENCES games_bot.duel_invitation (id),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    rounds_played BIGINT NOT NULL CHECK (rounds_played >= 0),
    first_score BIGINT NOT NULL CHECK (first_score >= 0),
    second_score BIGINT NOT NULL CHECK (second_score >= 0),
    winner VARCHAR(16) CHECK (winner IN ('FIRST', 'SECOND')),
    is_tiebreak BOOLEAN NOT NULL,
    CHECK (first_score <= rounds_played AND second_score <= rounds_played - first_score),
    FOREIGN KEY (id) REFERENCES games_bot.duel_bank (id)
);

-- The previous version accepted stakes but could not record attempts or start matches.
-- Restore initial state only for outstanding commitments; settled banks have no known match result.
INSERT INTO games_bot.dice_duel (id, rounds_played, first_score, second_score, winner, is_tiebreak)
SELECT i.id, 0, 0, 0, NULL, FALSE
FROM games_bot.duel_invitation i
JOIN games_bot.duel_bank b ON b.id = i.id
WHERE i.status = 'ACCEPTED' AND b.status = 'LOCKED';
