package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.gamestelegrambot.model.InvitationStatus
import ru.gamestelegrambot.model.SeriesFormat
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "duel_invitation", schema = "games_bot")
class DuelInvitationEntity(
    @Id
    var id: UUID,
    @Column(name = "chat_id", nullable = false)
    var chatId: Long,
    @Column(name = "challenger_id", nullable = false)
    var challengerId: Long,
    @Column(name = "opponent_id", nullable = false)
    var opponentId: Long,
    @Column(nullable = false, columnDefinition = "numeric")
    var stake: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var format: SeriesFormat,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: InvitationStatus = InvitationStatus.PENDING,
)
