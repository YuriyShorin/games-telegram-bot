package ru.gamestelegrambot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gamestelegrambot.mapper.toModel
import ru.gamestelegrambot.model.DiceDuel
import ru.gamestelegrambot.repository.DiceDuelRepository
import java.util.UUID

/** Internal read boundary; callers verify Telegram access to the originating chat. */
@Service
@Transactional(readOnly = true)
class DiceDuelService(
    private val repository: DiceDuelRepository,
) {
    fun find(
        duelId: UUID,
        chatId: Long,
    ): DiceDuel {
        val duel = checkNotNull(repository.findById(duelId).orElse(null)) { "Dice duel does not exist" }
        require(duel.invitation.chatId == chatId) { "Dice duel belongs to another chat" }
        return duel.toModel()
    }
}
