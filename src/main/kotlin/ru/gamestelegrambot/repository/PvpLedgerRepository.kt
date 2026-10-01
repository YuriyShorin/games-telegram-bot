package ru.gamestelegrambot.repository

import org.springframework.data.repository.Repository
import ru.gamestelegrambot.entity.PvpLedgerEntity

/** The application can append entries but does not expose ledger edits or deletions. */
interface PvpLedgerRepository : Repository<PvpLedgerEntity, Long> {
    fun save(entry: PvpLedgerEntity): PvpLedgerEntity
}
