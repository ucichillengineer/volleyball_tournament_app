package com.volleyball.tournament.domain

import kotlinx.serialization.Serializable

@Serializable
enum class SkillLevel(val label: String, val score: Int) {
    BEGINNER("Beginner", 1),
    MEDIUM("Medium", 2),
    ADVANCED("Advanced", 3);

    companion object {
        fun fromLabel(label: String): SkillLevel =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: BEGINNER
    }
}

@Serializable
data class SkillRatings(
    val setter: SkillLevel = SkillLevel.BEGINNER,
    val lifter: SkillLevel = SkillLevel.BEGINNER,
    val spiker: SkillLevel = SkillLevel.BEGINNER,
    val allRounder: SkillLevel = SkillLevel.BEGINNER
) {
    fun totalScore(): Int =
        setter.score + lifter.score + spiker.score + allRounder.score
}

@Serializable
data class Player(
    val id: String,
    val name: String,
    val ratings: SkillRatings = SkillRatings(),
    val teamId: String? = null,
    val isCaptain: Boolean = false
)

@Serializable
data class Team(
    val id: String,
    val name: String,
    val captainId: String? = null
)

@Serializable
data class AdminCredentials(
    val username: String = "admin",
    val password: String = "volleyball"
)

@Serializable
data class PendingSwitch(
    val playerId: String,
    val fromTeamId: String,
    val toTeamId: String
)

@Serializable
data class TournamentTeam(
    val id: String,
    val name: String,
    val playerIds: List<String> = emptyList(),
    val captainId: String? = null
)

@Serializable
data class TournamentMatch(
    val id: String,
    val teamOneId: String,
    val teamTwoId: String,
    val teamOneScore: Int? = null,
    val teamTwoScore: Int? = null
) {
    fun winnerTeamId(): String? = when {
        teamOneScore == null || teamTwoScore == null || teamOneScore == teamTwoScore -> null
        teamOneScore > teamTwoScore -> teamOneId
        else -> teamTwoId
    }
}

@Serializable
data class Tournament(
    val id: String,
    val name: String,
    /** ISO-style user-entered date, e.g. 2026-08-16. */
    val date: String,
    val participantIds: List<String>,
    val teams: List<TournamentTeam>,
    val matches: List<TournamentMatch> = emptyList()
)

@Serializable
data class AppState(
    val players: List<Player> = emptyList(),
    val teams: List<Team> = emptyList(),
    val tournaments: List<Tournament> = emptyList(),
    val activeTournamentId: String? = null,
    val admin: AdminCredentials = AdminCredentials(),
    val pendingSwitch: PendingSwitch? = null,
    val driveFileId: String = "",
    val driveSyncUrl: String = "",
    val lastSyncedAt: Long = 0L
)

object SeedPlayers {
    val initial: List<Player> = listOf(
        Player(
            id = "player-praveen",
            name = "Praveen Sanigepalli",
            ratings = SkillRatings(
                setter = SkillLevel.MEDIUM,
                lifter = SkillLevel.BEGINNER,
                spiker = SkillLevel.MEDIUM,
                allRounder = SkillLevel.MEDIUM
            )
        ),
        Player(
            id = "player-vikas",
            name = "Vikas Yadlapalli",
            ratings = SkillRatings(
                setter = SkillLevel.ADVANCED,
                lifter = SkillLevel.ADVANCED,
                spiker = SkillLevel.ADVANCED,
                allRounder = SkillLevel.ADVANCED
            )
        )
    )
}
