package ru.gamestelegrambot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gamestelegrambot.entity.DiceDuelEntity
import ru.gamestelegrambot.entity.DuelInvitationEntity
import ru.gamestelegrambot.mapper.toModel
import ru.gamestelegrambot.model.BankStatus
import ru.gamestelegrambot.model.DuelInvitation
import ru.gamestelegrambot.model.InvitationStatus
import ru.gamestelegrambot.model.SeriesFormat
import ru.gamestelegrambot.repository.DiceDuelRepository
import ru.gamestelegrambot.repository.DuelInvitationRepository
import ru.gamestelegrambot.service.PvpWalletService.Companion.CURRENCY_SCALE
import ru.gamestelegrambot.service.PvpWalletService.Companion.MIN_STAKE
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Callers must supply the authenticated Telegram actor and originating chat. */
@Service
@Transactional
class
DuelInvitationService(
    private val repository: DuelInvitationRepository,
    private val walletService: PvpWalletService,
    private val duelRepository: DiceDuelRepository,
) {
    fun challenge(
        invitationId: UUID,
        chatId: Long,
        challengerId: Long,
        opponentId: Long,
        stake: BigDecimal,
        format: SeriesFormat,
    ): DuelInvitation {
        require(challengerId > 0 && opponentId > 0 && challengerId != opponentId) {
            "A duel requires two distinct players"
        }
        val amount = stake.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY)
        require(amount >= MIN_STAKE) { "Minimum stake is $MIN_STAKE" }
        repository.insertIfAbsent(invitationId, chatId, challengerId, opponentId, amount, format.name)
        val invitation = lockInvitation(invitationId)
        require(
            invitation.chatId == chatId && invitation.challengerId == challengerId &&
                invitation.opponentId == opponentId && invitation.stake.compareTo(amount) == 0 &&
                invitation.format == format,
        ) { "Invitation ID already belongs to different terms" }
        return invitation.toModel()
    }

    fun accept(
        invitationId: UUID,
        chatId: Long,
        actorId: Long,
    ): DuelInvitation {
        val invitation = lockInvitation(invitationId)
        require(invitation.chatId == chatId) { "Invitation belongs to another chat" }
        require(invitation.opponentId == actorId) { "Only the invited opponent may accept" }
        if (invitation.status == InvitationStatus.PENDING) {
            val bank =
                walletService.acceptDuel(
                    invitation.id,
                    invitation.chatId,
                    invitation.challengerId,
                    invitation.opponentId,
                    invitation.stake,
                )
            check(bank.status == BankStatus.LOCKED) { "Cannot start a Dice duel with a settled bank" }
            invitation.status = InvitationStatus.ACCEPTED
            duelRepository.saveAndFlush(DiceDuelEntity(id = invitation.id, invitation = invitation))
        }
        return invitation.toModel()
    }

    @Transactional(readOnly = true)
    fun find(
        invitationId: UUID,
        chatId: Long,
    ): DuelInvitation {
        val invitation = checkNotNull(repository.findById(invitationId).orElse(null)) { "Invitation does not exist" }
        require(invitation.chatId == chatId) { "Invitation belongs to another chat" }
        return invitation.toModel()
    }

    private fun lockInvitation(id: UUID): DuelInvitationEntity = checkNotNull(repository.findLockedById(id)) { "Invitation does not exist" }
}
