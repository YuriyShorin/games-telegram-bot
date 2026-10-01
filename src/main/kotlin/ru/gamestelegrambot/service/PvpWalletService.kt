package ru.gamestelegrambot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gamestelegrambot.entity.DuelBankEntity
import ru.gamestelegrambot.entity.PvpLedgerEntity
import ru.gamestelegrambot.entity.PvpWalletEntity
import ru.gamestelegrambot.entity.WalletId
import ru.gamestelegrambot.mapper.toModel
import ru.gamestelegrambot.model.BankStatus
import ru.gamestelegrambot.model.DuelBank
import ru.gamestelegrambot.model.PvpWallet
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
    ): PvpWallet {
        require(playerId > 0) { "Player ID must be positive" }
        val inserted = walletRepository.insertIfAbsent(chatId, playerId, STARTING_GRANT, ZERO)
        val wallet = lockWallet(chatId, playerId)
        if (inserted == 1) {
            record(wallet, "STARTING_GRANT", STARTING_GRANT, ZERO)
        }
        return wallet.toModel()
    }

    @Transactional(readOnly = true)
    fun balance(
        chatId: Long,
        playerId: Long,
    ): PvpWallet =
        checkNotNull(walletRepository.findById(WalletId(chatId, playerId)).orElse(null)) {
            "Player is not registered in this group"
        }.toModel()

    fun acceptDuel(
        duelId: UUID,
        chatId: Long,
        firstPlayerId: Long,
        secondPlayerId: Long,
        stake: BigDecimal,
    ): DuelBank {
        require(firstPlayerId > 0 && secondPlayerId > 0 && firstPlayerId != secondPlayerId) {
            "A duel requires two distinct players"
        }
        val amount = stake.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY)
        require(amount >= MIN_STAKE) { "Minimum stake is $MIN_STAKE" }
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
        return bank.toModel()
    }

    fun awardWinner(
        duelId: UUID,
        winnerId: Long,
    ): DuelBank = settle(lockBank(duelId), BankStatus.WON, winnerId)

    fun forfeit(
        duelId: UUID,
        withdrawingPlayerId: Long,
    ): DuelBank {
        val bank = lockBank(duelId)
        require(withdrawingPlayerId == bank.firstPlayerId || withdrawingPlayerId == bank.secondPlayerId) {
            "Withdrawing player must be a participant"
        }
        val winnerId = if (withdrawingPlayerId == bank.firstPlayerId) bank.secondPlayerId else bank.firstPlayerId
        return settle(bank, BankStatus.FORFEITED, winnerId)
    }

    fun interrupt(duelId: UUID): DuelBank = settle(lockBank(duelId), BankStatus.INTERRUPTED, null)

    fun claimRecovery(
        chatId: Long,
        playerId: Long,
    ): PvpWallet {
        val wallet = lockWallet(chatId, playerId)
        val today = LocalDate.now(clock.withZone(MOSCOW))
        if (wallet.lastRecoveryDate?.let { it >= today } == true) return wallet.toModel()
        val wealth = wallet.available + wallet.committed
        if (wealth >= RECOVERY_THRESHOLD) return wallet.toModel()
        val grant = (RECOVERY_THRESHOLD - wealth).min(RECOVERY_MAX)
        wallet.available += grant
        wallet.lastRecoveryDate = today
        record(wallet, "DAILY_RECOVERY", grant, ZERO)
        return wallet.toModel()
    }

    private fun settle(
        bank: DuelBankEntity,
        status: BankStatus,
        winnerId: Long?,
    ): DuelBank {
        require(winnerId == null || winnerId == bank.firstPlayerId || winnerId == bank.secondPlayerId) {
            "Winner must be a participant"
        }
        if (bank.status != BankStatus.LOCKED) {
            check(bank.status == status && bank.winnerId == winnerId) { "Duel has a conflicting settlement" }
            return bank.toModel()
        }
        lockParticipants(bank).forEach { wallet ->
            check(wallet.committed >= bank.stake) { "Committed balance is inconsistent" }
            val payout =
                when {
                    status == BankStatus.INTERRUPTED -> bank.stake
                    wallet.id.playerId == winnerId -> bank.stake * DUEL_BANK_MULTIPLIER
                    else -> ZERO
                }
            wallet.committed -= bank.stake
            wallet.available += payout
            record(wallet, status.name, payout, -bank.stake, bank.id)
        }
        bank.status = status
        bank.winnerId = winnerId
        return bank.toModel()
    }

    private fun lockParticipants(bank: DuelBankEntity): List<PvpWalletEntity> =
        listOf(bank.firstPlayerId, bank.secondPlayerId).sorted().map { lockWallet(bank.chatId, it) }

    private fun lockWallet(
        chatId: Long,
        playerId: Long,
    ): PvpWalletEntity =
        checkNotNull(
            walletRepository.findLockedById(WalletId(chatId, playerId)),
        ) { "Player is not registered in this group" }

    private fun lockBank(duelId: UUID): DuelBankEntity =
        checkNotNull(bankRepository.findLockedById(duelId)) {
            "Duel commitment does not exist"
        }

    private fun record(
        wallet: PvpWalletEntity,
        reason: String,
        availableDelta: BigDecimal,
        committedDelta: BigDecimal,
        duelId: UUID? = null,
    ) {
        ledgerRepository.save(
            PvpLedgerEntity(wallet.id.chatId, wallet.id.playerId, reason, availableDelta, committedDelta, duelId),
        )
    }

    companion object {
        const val CURRENCY_SCALE = 2
        val MIN_STAKE = BigDecimal("10.00")

        private val ZERO = BigDecimal("0.00")
        private val STARTING_GRANT = BigDecimal("1000.00")
        private val DUEL_BANK_MULTIPLIER = BigDecimal("2")
        private val RECOVERY_THRESHOLD = BigDecimal("300.00")
        private val RECOVERY_MAX = BigDecimal("100.00")
        private val MOSCOW = ZoneId.of("Europe/Moscow")
    }
}
