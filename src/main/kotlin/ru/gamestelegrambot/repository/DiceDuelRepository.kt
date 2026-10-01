package ru.gamestelegrambot.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gamestelegrambot.entity.DiceDuelEntity
import java.util.UUID

interface DiceDuelRepository : JpaRepository<DiceDuelEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DiceDuelEntity d where d.id = :id")
    fun findLockedById(
        @Param("id") id: UUID,
    ): DiceDuelEntity?
}
