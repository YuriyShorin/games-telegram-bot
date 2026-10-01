package ru.gamestelegrambot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.gamestelegrambot.entity.DiceDuelEntity
import java.util.UUID

interface DiceDuelRepository : JpaRepository<DiceDuelEntity, UUID>
