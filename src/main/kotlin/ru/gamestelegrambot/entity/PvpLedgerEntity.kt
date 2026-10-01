package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "pvp_ledger", schema = "games_bot")
class PvpLedgerEntity(
    @Column(name = "chat_id", nullable = false, updatable = false)
    var chatId: Long,
    @Column(name = "player_id", nullable = false, updatable = false)
    var playerId: Long,
    @Column(nullable = false, length = 32, updatable = false)
    var reason: String,
    @Column(name = "available_delta", nullable = false, columnDefinition = "numeric", updatable = false)
    var availableDelta: BigDecimal,
    @Column(name = "committed_delta", nullable = false, columnDefinition = "numeric", updatable = false)
    var committedDelta: BigDecimal,
    @Column(name = "duel_id", updatable = false)
    var duelId: UUID? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
