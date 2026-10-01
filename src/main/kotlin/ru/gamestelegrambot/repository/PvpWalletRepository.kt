package ru.gamestelegrambot.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gamestelegrambot.entity.PvpWalletEntity
import ru.gamestelegrambot.entity.WalletId
import java.math.BigDecimal

interface PvpWalletRepository : JpaRepository<PvpWalletEntity, WalletId> {
    @Modifying
    @Query(
        value = """
            INSERT INTO games_bot.pvp_wallet (chat_id, player_id, available, committed)
            VALUES (:chat, :player, :grant, :zero)
            ON CONFLICT (chat_id, player_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("chat") chatId: Long,
        @Param("player") playerId: Long,
        @Param("grant") startingGrant: BigDecimal,
        @Param("zero") initialCommitted: BigDecimal,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PvpWalletEntity w where w.id = :id")
    fun findLockedById(
        @Param("id") id: WalletId,
    ): PvpWalletEntity?
}
