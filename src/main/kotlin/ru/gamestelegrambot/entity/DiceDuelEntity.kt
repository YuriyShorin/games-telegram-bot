package ru.gamestelegrambot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import ru.gamestelegrambot.model.MatchSide
import java.util.UUID

@Entity
@Table(name = "dice_duel", schema = "games_bot")
class DiceDuelEntity(
    @Id
    var id: UUID,
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", updatable = false)
    var invitation: DuelInvitationEntity,
    @Version
    @Column(nullable = false)
    var version: Long? = null,
    @Column(name = "rounds_played", nullable = false)
    var roundsPlayed: Long = 0,
    @Column(name = "first_score", nullable = false)
    var firstScore: Long = 0,
    @Column(name = "second_score", nullable = false)
    var secondScore: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    var winner: MatchSide? = null,
    @Column(name = "is_tiebreak", nullable = false)
    var isTiebreak: Boolean = false,
)
