package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "pvp_wallet", schema = "games_bot")
class PvpWallet(
    @EmbeddedId
    val id: WalletId,
    @Column(nullable = false, columnDefinition = "numeric")
    var available: BigDecimal = BigDecimal("1000.00"),
    @Column(nullable = false, columnDefinition = "numeric")
    var committed: BigDecimal = BigDecimal("0.00"),
    @Column(name = "last_recovery_date")
    var lastRecoveryDate: LocalDate? = null,
)
