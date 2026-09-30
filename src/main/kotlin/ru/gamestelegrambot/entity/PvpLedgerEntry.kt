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
class PvpLedgerEntry(
    @Column(name = "chat_id", nullable = false, updatable = false)
    val chatId: Long,
    @Column(name = "player_id", nullable = false, updatable = false)
    val playerId: Long,
    @Column(nullable = false, length = 32, updatable = false)
    val reason: String,
    @Column(name = "available_delta", nullable = false, columnDefinition = "numeric", updatable = false)
    val availableDelta: BigDecimal,
    @Column(name = "committed_delta", nullable = false, columnDefinition = "numeric", updatable = false)
    val committedDelta: BigDecimal,
    @Column(name = "duel_id", updatable = false)
    val duelId: UUID? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
