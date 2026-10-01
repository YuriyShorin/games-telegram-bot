package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.gamestelegrambot.model.BankStatus
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "duel_bank", schema = "games_bot")
class DuelBankEntity(
    @Id
    var id: UUID,
    @Column(name = "chat_id", nullable = false)
    var chatId: Long,
    @Column(name = "first_player_id", nullable = false)
    var firstPlayerId: Long,
    @Column(name = "second_player_id", nullable = false)
    var secondPlayerId: Long,
    @Column(nullable = false, columnDefinition = "numeric")
    var stake: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: BankStatus = BankStatus.LOCKED,
    @Column(name = "winner_id")
    var winnerId: Long? = null,
)
