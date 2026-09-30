package ru.gamestelegrambot.repository

import org.springframework.data.repository.Repository
import ru.gamestelegrambot.entity.PvpLedgerEntry

/** The application can append entries but does not expose ledger edits or deletions. */
interface PvpLedgerRepository : Repository<PvpLedgerEntry, Long> {
    fun save(entry: PvpLedgerEntry): PvpLedgerEntry
}
