package ru.gamestelegrambot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gamestelegrambot.entity.DiceAttemptRequestEntity
import ru.gamestelegrambot.entity.DiceDuelEntity
import ru.gamestelegrambot.mapper.toModel
import ru.gamestelegrambot.model.DiceAttemptRequest
import ru.gamestelegrambot.model.DiceDuel
import ru.gamestelegrambot.model.DiceRound
import ru.gamestelegrambot.model.MatchSide
import ru.gamestelegrambot.repository.DiceAttemptRequestRepository
import ru.gamestelegrambot.repository.DiceDuelRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

/** Internal boundary: adapter verifies native message origin, request correlation and timestamps. */
@Service
@Transactional
class DiceAttemptService(
    private val duels: DiceDuelRepository,
    private val requests: DiceAttemptRequestRepository,
    private val rules: DiceMatchService,
    private val wallets: PvpWalletService,
) {
    fun prepareRequest(
        duelId: UUID,
        chatId: Long,
        roundNumber: Long,
    ): DiceAttemptRequest {
        val duel = lockDuel(duelId, chatId)
        check(duel.winner == null) { "The match has already finished" }
        require(roundNumber == duel.roundsPlayed + 1) { "Unexpected round" }
        val roundRequests = requests.findAllByDuelIdAndRoundNumberOrderByPromptedAtAsc(duelId, roundNumber)
        roundRequests.singleOrNull { it.value == null }?.let { return it.toModel() }
        val side =
            if (roundRequests.isEmpty()) {
                if (Random.nextBoolean()) MatchSide.FIRST else MatchSide.SECOND
            } else {
                check(roundRequests.size == 1) { "The round already has both attempts" }
                if (roundRequests.single().side == MatchSide.FIRST) MatchSide.SECOND else MatchSide.FIRST
            }
        return requests
            .save(
                DiceAttemptRequestEntity(
                    id = UUID.randomUUID(),
                    duelId = duelId,
                    chatId = chatId,
                    roundNumber = roundNumber,
                    side = side,
                ),
            ).toModel()
    }

    /** Called only after successful prompt delivery. Preparation alone never starts a timer. */
    fun confirmPrompt(
        duelId: UUID,
        chatId: Long,
        requestId: UUID,
        promptedAt: Instant,
    ): DiceAttemptRequest {
        val duel = lockDuel(duelId, chatId)
        val request = request(duelId, requestId)
        val timestamp = promptedAt.truncatedTo(ChronoUnit.SECONDS)
        if (request.promptedAt != null) {
            require(request.promptedAt == timestamp) { "Prompt timestamp differs from the original" }
            return request.toModel()
        }
        check(duel.winner == null && request.roundNumber == duel.roundsPlayed + 1) { "Request is no longer current" }
        request.promptedAt = timestamp
        request.deadline = timestamp.plusSeconds(ATTEMPT_TIMEOUT_SECONDS)
        return request.toModel()
    }

    fun recordAttempt(
        duelId: UUID,
        chatId: Long,
        requestId: UUID,
        playerId: Long,
        messageId: Long,
        value: Int,
        sentAt: Instant,
    ): DiceDuel {
        val duel = lockDuel(duelId, chatId)
        val request = request(duelId, requestId)
        val expectedPlayer =
            if (request.side == MatchSide.FIRST) duel.invitation.challengerId else duel.invitation.opponentId
        require(playerId == expectedPlayer) { "Attempt belongs to another player" }
        require(messageId > 0 && value in DiceMatchService.DICE_VALUES) { "Invalid native Dice result" }
        val timestamp = sentAt.truncatedTo(ChronoUnit.SECONDS)
        if (request.value != null) {
            require(request.messageId == messageId && request.value == value && request.sentAt == timestamp) {
                "Request already has another attempt"
            }
            return duel.toModel()
        }
        check(duel.winner == null && request.roundNumber == duel.roundsPlayed + 1) { "Request is no longer current" }
        val promptedAt = checkNotNull(request.promptedAt) { "Prompt has not been sent" }
        val deadline = checkNotNull(request.deadline)
        require(timestamp >= promptedAt && timestamp <= deadline) { "Attempt is outside the request time window" }
        require(requests.findByChatIdAndMessageId(chatId, messageId) == null) { "Native message was already used" }
        request.messageId = messageId
        request.value = value
        request.sentAt = timestamp
        val roundRequests = requests.findAllByDuelIdAndRoundNumberOrderByPromptedAtAsc(duelId, request.roundNumber)
        if (roundRequests.size == PAIRED_ATTEMPTS && roundRequests.all { it.value != null }) {
            val round =
                DiceRound(
                    first = checkNotNull(roundRequests.single { it.side == MatchSide.FIRST }.value),
                    second = checkNotNull(roundRequests.single { it.side == MatchSide.SECOND }.value),
                )
            val match = rules.recordRound(duel.toModel().match, round)
            duel.roundsPlayed = match.roundsPlayed
            duel.firstScore = match.firstScore
            duel.secondScore = match.secondScore
            duel.winner = match.winner
            duel.isTiebreak = match.isTiebreak
            match.winner?.let { winner ->
                val winnerId =
                    if (winner == MatchSide.FIRST) duel.invitation.challengerId else duel.invitation.opponentId
                wallets.awardWinner(duelId, winnerId)
            }
        }
        return duel.toModel()
    }

    @Transactional(readOnly = true)
    fun findRequest(
        duelId: UUID,
        chatId: Long,
        requestId: UUID,
    ): DiceAttemptRequest {
        val duel = checkNotNull(duels.findById(duelId).orElse(null)) { "Dice duel does not exist" }
        require(duel.invitation.chatId == chatId) { "Dice duel belongs to another chat" }
        return request(duelId, requestId).toModel()
    }

    private fun lockDuel(
        duelId: UUID,
        chatId: Long,
    ): DiceDuelEntity {
        val duel = checkNotNull(duels.findLockedById(duelId)) { "Dice duel does not exist" }
        require(duel.invitation.chatId == chatId) { "Dice duel belongs to another chat" }
        return duel
    }

    private fun request(
        duelId: UUID,
        requestId: UUID,
    ): DiceAttemptRequestEntity {
        val request = checkNotNull(requests.findById(requestId).orElse(null)) { "Attempt request does not exist" }
        require(request.duelId == duelId) { "Request belongs to another duel" }
        return request
    }

    companion object {
        const val ATTEMPT_TIMEOUT_SECONDS = 60L
        private const val PAIRED_ATTEMPTS = 2
    }
}
