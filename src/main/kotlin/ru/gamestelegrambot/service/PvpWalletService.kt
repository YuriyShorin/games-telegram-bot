package ru.gamestelegrambot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gamestelegrambot.entity.DuelBank
import ru.gamestelegrambot.entity.PvpLedgerEntry
import ru.gamestelegrambot.entity.PvpWallet
import ru.gamestelegrambot.entity.WalletId
import ru.gamestelegrambot.model.BankStatus
import ru.gamestelegrambot.model.DuelFunds
import ru.gamestelegrambot.model.WalletBalance
import ru.gamestelegrambot.repository.DuelBankRepository
import ru.gamestelegrambot.repository.PvpLedgerRepository
import ru.gamestelegrambot.repository.PvpWalletRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional
class PvpWalletService(
    private val walletRepository: PvpWalletRepository,
    private val bankRepository: DuelBankRepository,
    private val ledgerRepository: PvpLedgerRepository,
    private val clock: Clock,
) {
    fun register(
        chatId: Long,
        playerId: Long,
    ): WalletBalance {
        require(playerId > 0) { "Player ID must be positive" }
        val inserted = walletRepository.insertIfAbsent(chatId, playerId)
        val wallet = lockWallet(chatId, playerId)
        if (inserted == 1) {
            record(wallet, "STARTING_GRANT", BigDecimal("1000.00"), ZERO)
        }
        return wallet.snapshot()
    }

    @Transactional(readOnly = true)
    fun balance(
        chatId: Long,
        playerId: Long,
    ): WalletBalance =
        checkNotNull(walletRepository.findById(WalletId(chatId, playerId)).orElse(null)) {
            "Player is not registered in this group"
        }.snapshot()

    fun acceptDuel(
        duelId: UUID,
        chatId: Long,
        firstPlayerId: Long,
        secondPlayerId: Long,
        stake: BigDecimal,
    ): DuelFunds {
        require(firstPlayerId > 0 && secondPlayerId > 0 && firstPlayerId != secondPlayerId) {
            "A duel requires two distinct players"
        }
        val amount = stake.setScale(2, RoundingMode.UNNECESSARY)
        require(amount >= MIN_STAKE) { "Minimum stake is 10.00" }
        val inserted = bankRepository.insertIfAbsent(duelId, chatId, firstPlayerId, secondPlayerId, amount)
        val bank = lockBank(duelId)
        require(
            bank.chatId == chatId && bank.firstPlayerId == firstPlayerId &&
                bank.secondPlayerId == secondPlayerId && bank.stake.compareTo(amount) == 0,
        ) { "Duel ID already belongs to a different commitment" }
        if (inserted == 1) {
            val wallets = lockParticipants(bank)
            check(wallets.all { it.available >= amount }) { "Insufficient available funds" }
            wallets.forEach { wallet ->
                wallet.available -= amount
                wallet.committed += amount
                record(wallet, "DUEL_COMMITMENT", -amount, amount, bank.id)
            }
        }
        return bank.snapshot()
    }

    fun awardWinner(
        duelId: UUID,
        winnerId: Long,
    ): DuelFunds = settle(lockBank(duelId), BankStatus.WON, winnerId)

    fun forfeit(
        duelId: UUID,
        withdrawingPlayerId: Long,
    ): DuelFunds {
        val bank = lockBank(duelId)
        require(withdrawingPlayerId == bank.firstPlayerId || withdrawingPlayerId == bank.secondPlayerId) {
            "Withdrawing player must be a participant"
        }
        val winnerId = if (withdrawingPlayerId == bank.firstPlayerId) bank.secondPlayerId else bank.firstPlayerId
        return settle(bank, BankStatus.FORFEITED, winnerId)
    }

    fun interrupt(duelId: UUID): DuelFunds = settle(lockBank(duelId), BankStatus.INTERRUPTED, null)

    fun claimRecovery(
        chatId: Long,
        playerId: Long,
    ): WalletBalance {
        val wallet = lockWallet(chatId, playerId)
        val today = LocalDate.now(clock.withZone(MOSCOW))
        if (wallet.lastRecoveryDate?.let { it >= today } == true) return wallet.snapshot()
        val wealth = wallet.available + wallet.committed
        if (wealth >= RECOVERY_THRESHOLD) return wallet.snapshot()
        val grant = (RECOVERY_THRESHOLD - wealth).min(RECOVERY_MAX)
        wallet.available += grant
        wallet.lastRecoveryDate = today
        record(wallet, "DAILY_RECOVERY", grant, ZERO)
        return wallet.snapshot()
    }

    private fun settle(
        bank: DuelBank,
        status: BankStatus,
        winnerId: Long?,
    ): DuelFunds {
        require(winnerId == null || winnerId == bank.firstPlayerId || winnerId == bank.secondPlayerId) {
            "Winner must be a participant"
        }
        if (bank.status != BankStatus.LOCKED) {
            check(bank.status == status && bank.winnerId == winnerId) { "Duel has a conflicting settlement" }
            return bank.snapshot()
        }
        lockParticipants(bank).forEach { wallet ->
            check(wallet.committed >= bank.stake) { "Committed balance is inconsistent" }
            val payout =
                when {
                    status == BankStatus.INTERRUPTED -> bank.stake
                    wallet.id.playerId == winnerId -> bank.stake * BigDecimal("2")
                    else -> ZERO
                }
            wallet.committed -= bank.stake
            wallet.available += payout
            record(wallet, status.name, payout, -bank.stake, bank.id)
        }
        bank.status = status
        bank.winnerId = winnerId
        return bank.snapshot()
    }

    private fun lockParticipants(bank: DuelBank): List<PvpWallet> =
        listOf(bank.firstPlayerId, bank.secondPlayerId).sorted().map { lockWallet(bank.chatId, it) }

    private fun lockWallet(
        chatId: Long,
        playerId: Long,
    ): PvpWallet =
        checkNotNull(
            walletRepository.findLockedById(WalletId(chatId, playerId)),
        ) { "Player is not registered in this group" }

    private fun lockBank(duelId: UUID): DuelBank =
        checkNotNull(bankRepository.findLockedById(duelId)) {
            "Duel commitment does not exist"
        }

    private fun record(
        wallet: PvpWallet,
        reason: String,
        availableDelta: BigDecimal,
        committedDelta: BigDecimal,
        duelId: UUID? = null,
    ) {
        ledgerRepository.save(
            PvpLedgerEntry(wallet.id.chatId, wallet.id.playerId, reason, availableDelta, committedDelta, duelId),
        )
    }

    private fun PvpWallet.snapshot() = WalletBalance(id.chatId, id.playerId, available, committed)

    private fun DuelBank.snapshot() = DuelFunds(id, chatId, firstPlayerId, secondPlayerId, stake, status, winnerId)

    companion object {
        private val ZERO = BigDecimal("0.00")
        private val MIN_STAKE = BigDecimal("10.00")
        private val RECOVERY_THRESHOLD = BigDecimal("300.00")
        private val RECOVERY_MAX = BigDecimal("100.00")
        private val MOSCOW = ZoneId.of("Europe/Moscow")
    }
}
