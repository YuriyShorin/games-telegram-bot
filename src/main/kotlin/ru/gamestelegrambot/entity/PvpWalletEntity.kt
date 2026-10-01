package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "pvp_wallet", schema = "games_bot")
class PvpWalletEntity(
    @EmbeddedId
    var id: WalletId,
    @Column(nullable = false, columnDefinition = "numeric")
    var available: BigDecimal,
    @Column(nullable = false, columnDefinition = "numeric")
    var committed: BigDecimal = ZERO,
    @Column(name = "last_recovery_date")
    var lastRecoveryDate: LocalDate? = null,
) {
    companion object {
        private val ZERO = BigDecimal("0.00")
    }
}
