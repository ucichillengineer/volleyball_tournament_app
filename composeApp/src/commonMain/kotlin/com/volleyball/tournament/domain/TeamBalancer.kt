package com.volleyball.tournament.domain

object TeamBalancer {

    fun createBalancedTeams(
        players: List<Player>,
        teamCount: Int,
        teamNames: List<String> = emptyList()
    ): Pair<List<Team>, List<Player>> {
        require(teamCount >= 2) { "Need at least 2 teams" }
        require(players.isNotEmpty()) { "Need at least one player" }

        val names = (0 until teamCount).map { index ->
            teamNames.getOrNull(index)?.takeIf { it.isNotBlank() }
                ?: defaultTeamName(index)
        }

        val teams = names.mapIndexed { index, name ->
            Team(id = "team-$index", name = name)
        }

        val sorted = players.sortedByDescending { it.ratings.totalScore() }
        val buckets = Array(teamCount) { mutableListOf<Player>() }
        val scores = IntArray(teamCount)

        // Snake draft for balance
        var forward = true
        var index = 0
        sorted.forEach { player ->
            buckets[index].add(player)
            scores[index] += player.ratings.totalScore()
            if (forward) {
                if (index == teamCount - 1) forward = false else index++
            } else {
                if (index == 0) forward = true else index--
            }
        }

        val assignedPlayers = mutableListOf<Player>()
        teams.forEachIndexed { teamIndex, team ->
            val roster = buckets[teamIndex]
            val captain = roster.maxByOrNull { it.ratings.totalScore() }
            roster.forEach { player ->
                assignedPlayers += player.copy(
                    teamId = team.id,
                    isCaptain = captain?.id == player.id
                )
            }
        }

        val teamsWithCaptains = teams.map { team ->
            val captainId = assignedPlayers.firstOrNull { it.teamId == team.id && it.isCaptain }?.id
            team.copy(captainId = captainId)
        }

        return teamsWithCaptains to assignedPlayers
    }

    fun requestSwitch(
        state: AppState,
        playerId: String,
        toTeamId: String
    ): Result<AppState> {
        if (state.pendingSwitch != null) {
            return Result.failure(
                IllegalStateException("Only one team change is allowed at a time. Resolve the pending switch first.")
            )
        }
        val player = state.players.firstOrNull { it.id == playerId }
            ?: return Result.failure(IllegalArgumentException("Player not found"))
        val fromTeamId = player.teamId
            ?: return Result.failure(IllegalStateException("Player is not on a team yet"))
        if (fromTeamId == toTeamId) {
            return Result.failure(IllegalArgumentException("Player is already on that team"))
        }
        if (state.teams.none { it.id == toTeamId }) {
            return Result.failure(IllegalArgumentException("Target team not found"))
        }

        return Result.success(
            state.copy(
                pendingSwitch = PendingSwitch(
                    playerId = playerId,
                    fromTeamId = fromTeamId,
                    toTeamId = toTeamId
                )
            )
        )
    }

    fun confirmPendingSwitch(state: AppState): Result<AppState> {
        val pending = state.pendingSwitch
            ?: return Result.failure(IllegalStateException("No pending switch"))

        val updatedPlayers = state.players.map { player ->
            when {
                player.id == pending.playerId -> {
                    val wasCaptain = player.isCaptain
                    player.copy(teamId = pending.toTeamId, isCaptain = false).also {
                        // captain flag cleared; reassigned below if needed
                    }.let { moved ->
                        if (wasCaptain) moved else moved
                    }
                }
                else -> player
            }
        }

        // If captain left a team, promote highest remaining player
        val afterMove = updatedPlayers.map { it }.toMutableList()
        val teamsUpdated = state.teams.map { team ->
            val roster = afterMove.filter { it.teamId == team.id }
            val currentCaptain = roster.firstOrNull { it.isCaptain }
            if (currentCaptain != null) {
                team.copy(captainId = currentCaptain.id)
            } else {
                val promoted = roster.maxByOrNull { it.ratings.totalScore() }
                if (promoted != null) {
                    val idx = afterMove.indexOfFirst { it.id == promoted.id }
                    if (idx >= 0) {
                        afterMove[idx] = afterMove[idx].copy(isCaptain = true)
                    }
                    team.copy(captainId = promoted.id)
                } else {
                    team.copy(captainId = null)
                }
            }
        }

        // Clear captain flags for players not matching team captains
        val finalPlayers = afterMove.map { player ->
            val team = teamsUpdated.firstOrNull { it.id == player.teamId }
            player.copy(isCaptain = team?.captainId == player.id)
        }

        return Result.success(
            state.copy(
                players = finalPlayers,
                teams = teamsUpdated,
                pendingSwitch = null
            )
        )
    }

    fun cancelPendingSwitch(state: AppState): AppState =
        state.copy(pendingSwitch = null)

    fun formatWhatsAppMessage(state: AppState): String {
        if (state.teams.isEmpty()) return "No teams created yet."
        val builder = StringBuilder()
        builder.appendLine("*Volleyball Tournament Teams*")
        builder.appendLine()
        state.teams.forEach { team ->
            val roster = state.players.filter { it.teamId == team.id }
                .sortedWith(compareByDescending<Player> { it.isCaptain }.thenBy { it.name })
            builder.appendLine("*${team.name}*")
            roster.forEach { player ->
                val mark = if (player.isCaptain) " (C)" else ""
                builder.appendLine("• ${player.name}$mark")
            }
            val strength = roster.sumOf { it.ratings.totalScore() }
            builder.appendLine("_Balance score: ${strength}_")
            builder.appendLine()
        }
        state.pendingSwitch?.let { pending ->
            val name = state.players.firstOrNull { it.id == pending.playerId }?.name ?: "Player"
            val from = state.teams.firstOrNull { it.id == pending.fromTeamId }?.name ?: "?"
            val to = state.teams.firstOrNull { it.id == pending.toTeamId }?.name ?: "?"
            builder.appendLine("_Pending switch: $name ($from → $to)_")
        }
        return builder.toString().trim()
    }

    private fun defaultTeamName(index: Int): String {
        val names = listOf("Spike Squad", "Net Ninjas", "Court Kings", "Ace Alliance", "Block Party", "Serve Storm")
        return names.getOrElse(index) { "Team ${index + 1}" }
    }
}

fun newPlayerId(): String =
    "player-${kotlin.random.Random.nextLong().toULong().toString(16)}"

fun newTeamId(): String =
    "team-${kotlin.random.Random.nextLong().toULong().toString(16)}"
