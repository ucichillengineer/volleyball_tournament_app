package com.volleyball.tournament

import com.volleyball.tournament.data.AppRepository
import com.volleyball.tournament.domain.AppState
import com.volleyball.tournament.domain.Player
import com.volleyball.tournament.domain.SeedPlayers
import com.volleyball.tournament.domain.SkillLevel
import com.volleyball.tournament.domain.SkillRatings
import com.volleyball.tournament.domain.TeamBalancer
import com.volleyball.tournament.domain.newPlayerId
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

    fun exportSnapshot(): String = repository.exportJson()
}

fun parseLevel(label: String): SkillLevel = SkillLevel.fromLabel(label)
