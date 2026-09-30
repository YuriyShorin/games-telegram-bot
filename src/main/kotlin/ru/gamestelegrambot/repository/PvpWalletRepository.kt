package ru.gamestelegrambot.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gamestelegrambot.entity.PvpWallet
import ru.gamestelegrambot.entity.WalletId

interface PvpWalletRepository : JpaRepository<PvpWallet, WalletId> {
    @Modifying
    @Query(
        value = """
            INSERT INTO games_bot.pvp_wallet (chat_id, player_id, available, committed)
            VALUES (:chat, :player, 1000.00, 0.00)
            ON CONFLICT (chat_id, player_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("chat") chatId: Long,
        @Param("player") playerId: Long,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PvpWallet w where w.id = :id")
    fun findLockedById(
        @Param("id") id: WalletId,
    ): PvpWallet?
}
