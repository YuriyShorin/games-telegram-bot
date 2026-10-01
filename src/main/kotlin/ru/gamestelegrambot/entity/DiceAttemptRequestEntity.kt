package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import ru.gamestelegrambot.model.MatchSide
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "dice_attempt_request", schema = "games_bot")
class DiceAttemptRequestEntity(
    @Id
    var id: UUID,
    @Column(name = "duel_id", nullable = false, updatable = false)
    var duelId: UUID,
    @Column(name = "chat_id", nullable = false, updatable = false)
    var chatId: Long,
    @Column(name = "round_number", nullable = false, updatable = false)
    var roundNumber: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    var side: MatchSide,
    @Column(name = "prompted_at")
    var promptedAt: Instant? = null,
    var deadline: Instant? = null,
    @Column(name = "message_id")
    var messageId: Long? = null,
    var value: Int? = null,
    @Column(name = "sent_at")
    var sentAt: Instant? = null,
    @Version
    @Column(nullable = false)
    var version: Long? = null,
)
