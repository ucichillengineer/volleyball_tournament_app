package com.volleyball.tournament

import com.volleyball.tournament.data.AppRepository
import com.volleyball.tournament.domain.AppState
import com.volleyball.tournament.domain.Player
import com.volleyball.tournament.domain.SeedPlayers
import com.volleyball.tournament.domain.SkillLevel
import com.volleyball.tournament.domain.SkillRatings
import com.volleyball.tournament.domain.TeamBalancer
import com.volleyball.tournament.domain.Tournament
import com.volleyball.tournament.domain.TournamentMatch
import com.volleyball.tournament.domain.TournamentTeam
import com.volleyball.tournament.domain.newMatchId
import com.volleyball.tournament.domain.newPlayerId
import com.volleyball.tournament.domain.newTournamentId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiMessage(val text: String, val isError: Boolean = false)

class TournamentViewModel(
    private val repository: AppRepository = AppRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    val appState: StateFlow<AppState> = repository.state

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val whatsAppText: StateFlow<String> = repository.state
        .map { TeamBalancer.formatWhatsAppMessage(it) }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val tournamentWhatsAppText: StateFlow<String> = repository.state
        .map { state ->
            state.tournaments.firstOrNull { it.id == state.activeTournamentId }
                ?.let { formatTournamentWhatsApp(it, state.players) }
                ?: "No active tournament yet."
        }
        .stateIn(scope, SharingStarted.Eagerly, "")

    private var pushJob: Job? = null

    init {
        // Load shared cloud roster, then keep cloud updated when local state changes.
        scope.launch {
            _busy.value = true
            repository.syncFromCloud().fold(
                onSuccess = { _message.value = UiMessage("Synced shared roster from cloud") },
                onFailure = {
                    // Do not overwrite cloud with bare seed data. Only republish if this
                    // device already has real roster changes beyond the default seed.
                    val local = repository.state.value
                    val hasRealData =
                        local.teams.isNotEmpty() ||
                            local.players.size > SeedPlayers.initial.size ||
                            local.players.any { player ->
                                SeedPlayers.initial.none { it.id == player.id || it.name == player.name }
                            }
                    if (hasRealData) {
                        repository.syncToCloud()
                    }
                }
            )
            _busy.value = false
        }
        scope.launch {
            repository.state.drop(1).collect {
                scheduleCloudPush()
            }
        }
    }

    private fun scheduleCloudPush() {
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(800)
            repository.syncToCloud()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun loginAdmin(username: String, password: String): Boolean {
        val admin = repository.state.value.admin
        val ok = username.trim() == admin.username && password == admin.password
        _isAdmin.value = ok
        _message.value = if (ok) {
            UiMessage("Admin unlocked")
        } else {
            UiMessage("Invalid admin credentials", isError = true)
        }
        return ok
    }

    fun logoutAdmin() {
        _isAdmin.value = false
        _message.value = UiMessage("Admin locked")
    }

    fun addPlayer(
        name: String,
        ratings: SkillRatings,
        asSelf: Boolean = false
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _message.value = UiMessage("Player name is required", isError = true)
            return
        }
        if (!_isAdmin.value && !asSelf) {
            _message.value = UiMessage("Only admin can add other players. Use Add Myself.", isError = true)
            return
        }
        repository.update { state ->
            state.copy(
                players = state.players + Player(
                    id = newPlayerId(),
                    name = trimmed,
                    ratings = ratings
                )
            )
        }
        _message.value = UiMessage("$trimmed added")
    }

    fun updateRatings(playerId: String, ratings: SkillRatings) {
        val player = repository.state.value.players.firstOrNull { it.id == playerId } ?: return
        repository.update { state ->
            state.copy(
                players = state.players.map {
                    if (it.id == playerId) it.copy(ratings = ratings) else it
                }
            )
        }
        _message.value = UiMessage("${player.name} ratings updated")
    }

    fun removePlayer(playerId: String) {
        val player = repository.state.value.players.firstOrNull { it.id == playerId }
        repository.update { state ->
            val remaining = state.players.filterNot { it.id == playerId }
            val teams = state.teams.mapNotNull { team ->
                val roster = remaining.filter { it.teamId == team.id }
                if (roster.isEmpty()) {
                    null
                } else {
                    val captain = roster.firstOrNull { it.isCaptain } ?: roster.maxByOrNull { it.ratings.totalScore() }
                    team.copy(captainId = captain?.id)
                }
            }
            val withCaptains = remaining.map { p ->
                p.copy(isCaptain = teams.any { it.captainId == p.id })
            }
            state.copy(
                players = withCaptains,
                teams = teams,
                pendingSwitch = state.pendingSwitch?.takeIf { it.playerId != playerId }
            )
        }
        _message.value = UiMessage("${player?.name ?: "Player"} removed")
    }

    fun createTeams(teamCount: Int, customNames: List<String> = emptyList()) {
        val players = repository.state.value.players
        if (players.size < teamCount) {
            _message.value = UiMessage("Need at least $teamCount players", isError = true)
            return
        }
        runCatching {
            val (teams, assigned) = TeamBalancer.createBalancedTeams(players, teamCount, customNames)
            repository.update {
                it.copy(players = assigned, teams = teams, pendingSwitch = null)
            }
            _message.value = UiMessage("Created ${teams.size} balanced teams")
        }.onFailure {
            _message.value = UiMessage(it.message ?: "Could not create teams", isError = true)
        }
    }

    fun requestTeamSwitch(playerId: String, toTeamId: String) {
        val result = TeamBalancer.requestSwitch(repository.state.value, playerId, toTeamId)
        result.fold(
            onSuccess = {
                repository.replace(it)
                _message.value = UiMessage("Switch pending — confirm to apply (one change at a time)")
            },
            onFailure = {
                _message.value = UiMessage(it.message ?: "Switch failed", isError = true)
            }
        )
    }

    fun confirmSwitch() {
        TeamBalancer.confirmPendingSwitch(repository.state.value).fold(
            onSuccess = {
                repository.replace(it)
                _message.value = UiMessage("Team switch applied and captains reshuffled")
            },
            onFailure = {
                _message.value = UiMessage(it.message ?: "Confirm failed", isError = true)
            }
        )
    }

    fun cancelSwitch() {
        repository.replace(TeamBalancer.cancelPendingSwitch(repository.state.value))
        _message.value = UiMessage("Pending switch cancelled")
    }

    fun createTournament(
        name: String,
        date: String,
        participantIds: List<String>,
        teamCount: Int,
        manualAssignments: Map<String, String> = emptyMap()
    ) {
        val title = name.trim()
        if (title.isEmpty() || date.trim().isEmpty()) {
            _message.value = UiMessage("Tournament name and date are required", isError = true)
            return
        }
        if (teamCount < 2 || participantIds.size < teamCount) {
            _message.value = UiMessage("Select at least one player for each team", isError = true)
            return
        }
        val roster = repository.state.value.players.filter { it.id in participantIds }
        val teams = if (manualAssignments.isEmpty()) {
            val (balancedTeams, assigned) = TeamBalancer.createBalancedTeams(roster, teamCount)
            balancedTeams.map { team ->
                val members = assigned.filter { it.teamId == team.id }
                TournamentTeam(team.id, team.name, members.map { it.id }, team.captainId)
            }
        } else {
            val ids = (0 until teamCount).map { "manual-team-$it" }
            if (participantIds.any { manualAssignments[it] !in ids }) {
                _message.value = UiMessage("Assign every selected player to a team", isError = true)
                return
            }
            if (ids.any { teamId -> participantIds.none { manualAssignments[it] == teamId } }) {
                _message.value = UiMessage("Add at least one player to every team", isError = true)
                return
            }
            ids.mapIndexed { index, id ->
                val members = participantIds.filter { manualAssignments[it] == id }
                TournamentTeam(
                    id = id,
                    name = "Team ${index + 1}",
                    playerIds = members,
                    captainId = members.maxByOrNull { playerId ->
                        roster.firstOrNull { it.id == playerId }?.ratings?.totalScore() ?: 0
                    }
                )
            }
        }
        val tournament = Tournament(
            id = newTournamentId(),
            name = title,
            date = date.trim(),
            participantIds = participantIds,
            teams = teams
        )
        repository.update { state ->
            state.copy(
                tournaments = state.tournaments + tournament,
                activeTournamentId = tournament.id
            )
        }
        _message.value = UiMessage("${tournament.name} created with ${teams.size} teams")
    }

    fun selectTournament(tournamentId: String) {
        repository.update { it.copy(activeTournamentId = tournamentId) }
    }

    fun deleteTournament(tournamentId: String) {
        repository.update { state ->
            val remaining = state.tournaments.filterNot { it.id == tournamentId }
            state.copy(
                tournaments = remaining,
                activeTournamentId = state.activeTournamentId
                    ?.takeIf { it != tournamentId }
                    ?: remaining.lastOrNull()?.id
            )
        }
        _message.value = UiMessage("Tournament removed")
    }

    fun addMatch(teamOneId: String, teamTwoId: String, teamOneScore: String, teamTwoScore: String) {
        val active = repository.state.value.tournaments
            .firstOrNull { it.id == repository.state.value.activeTournamentId }
            ?: run {
                _message.value = UiMessage("Create or select a tournament first", isError = true)
                return
            }
        if (teamOneId == teamTwoId) {
            _message.value = UiMessage("Choose two different teams", isError = true)
            return
        }
        val first = teamOneScore.toIntOrNull()
        val second = teamTwoScore.toIntOrNull()
        if (first == null || second == null || first < 0 || second < 0 || first == second) {
            _message.value = UiMessage("Enter two different non-negative scores", isError = true)
            return
        }
        val match = TournamentMatch(newMatchId(), teamOneId, teamTwoId, first, second)
        repository.update { state ->
            state.copy(tournaments = state.tournaments.map {
                if (it.id == active.id) it.copy(matches = it.matches + match) else it
            })
        }
        _message.value = UiMessage("Match score saved")
    }

    fun removeMatch(matchId: String) {
        repository.update { state ->
            state.copy(tournaments = state.tournaments.map { tournament ->
                if (tournament.id == state.activeTournamentId) {
                    tournament.copy(matches = tournament.matches.filterNot { it.id == matchId })
                } else tournament
            })
        }
    }

    fun updateAdminPassword(newPassword: String) {
        if (!_isAdmin.value) {
            _message.value = UiMessage("Admin login required", isError = true)
            return
        }
        if (newPassword.length < 4) {
            _message.value = UiMessage("Password must be at least 4 characters", isError = true)
            return
        }
        repository.update { it.copy(admin = it.admin.copy(password = newPassword)) }
        _message.value = UiMessage("Admin password updated (syncing to cloud)")
    }

    fun refreshFromCloud() {
        scope.launch {
            _busy.value = true
            repository.syncFromCloud().fold(
                onSuccess = { _message.value = UiMessage("Refreshed shared roster from cloud") },
                onFailure = { _message.value = UiMessage(it.message ?: "Cloud refresh failed", isError = true) }
            )
            _busy.value = false
        }
    }

    fun restoreFromGitHubBackup() {
        if (!_isAdmin.value) {
            _message.value = UiMessage("Admin login required", isError = true)
            return
        }
        scope.launch {
            _busy.value = true
            repository.restoreFromGitHubBackup().fold(
                onSuccess = {
                    _message.value = UiMessage("Restored roster from GitHub data/roster.json and published to live cloud")
                },
                onFailure = {
                    _message.value = UiMessage(it.message ?: "GitHub restore failed", isError = true)
                }
            )
            _busy.value = false
        }
    }

    fun exportSnapshot(): String = repository.exportJson()

    private fun formatTournamentWhatsApp(tournament: Tournament, players: List<Player>): String = buildString {
        appendLine("*${tournament.name}*")
        appendLine("📅 ${tournament.date}")
        appendLine()
        appendLine("*Teams*")
        tournament.teams.forEach { team ->
            appendLine("*${team.name}*")
            team.playerIds.forEach { playerId ->
                val player = players.firstOrNull { it.id == playerId } ?: return@forEach
                appendLine("• ${player.name}${if (player.id == team.captainId) " (C)" else ""}")
            }
            appendLine()
        }
        appendLine("*Match results*")
        if (tournament.matches.isEmpty()) {
            appendLine("No match scores recorded yet.")
        } else {
            tournament.matches.forEachIndexed { index, match ->
                val first = tournament.teams.firstOrNull { it.id == match.teamOneId }?.name ?: "Team 1"
                val second = tournament.teams.firstOrNull { it.id == match.teamTwoId }?.name ?: "Team 2"
                val winner = tournament.teams.firstOrNull { it.id == match.winnerTeamId() }?.name
                appendLine("${index + 1}. $first ${match.teamOneScore} – ${match.teamTwoScore} $second")
                appendLine("   Winner: ${winner ?: "Not decided"} · Loser: ${
                    if (winner == first) second else if (winner == second) first else "—"
                }")
            }
        }
    }.trim()
}

fun parseLevel(label: String): SkillLevel = SkillLevel.fromLabel(label)
