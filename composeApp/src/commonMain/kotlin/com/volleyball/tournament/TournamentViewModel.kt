package com.volleyball.tournament

import com.volleyball.tournament.data.AppRepository
import com.volleyball.tournament.domain.AppState
import com.volleyball.tournament.domain.Player
import com.volleyball.tournament.domain.SkillLevel
import com.volleyball.tournament.domain.SkillRatings
import com.volleyball.tournament.domain.TeamBalancer
import com.volleyball.tournament.domain.newPlayerId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        // Player can change own ratings; admin can change anyone.
        // Without per-player auth, "self" edits are allowed for all non-locked fields unless admin-only mode.
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
        if (!_isAdmin.value) {
            _message.value = UiMessage("Admin login required to remove players", isError = true)
            return
        }
        repository.update { state ->
            state.copy(players = state.players.filterNot { it.id == playerId })
        }
        _message.value = UiMessage("Player removed")
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

    fun updateDriveConfig(fileId: String, syncUrl: String) {
        if (!_isAdmin.value) {
            _message.value = UiMessage("Admin login required", isError = true)
            return
        }
        repository.update {
            it.copy(driveFileId = fileId.trim(), driveSyncUrl = syncUrl.trim())
        }
        _message.value = UiMessage("Google Drive config saved")
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
        _message.value = UiMessage("Admin password updated (sync to Drive to share)")
    }

    fun pullFromDrive() {
        scope.launch {
            _busy.value = true
            repository.syncFromDrive().fold(
                onSuccess = { _message.value = UiMessage("Pulled latest data from Google Drive") },
                onFailure = { _message.value = UiMessage(it.message ?: "Drive pull failed", isError = true) }
            )
            _busy.value = false
        }
    }

    fun pushToDrive() {
        if (!_isAdmin.value) {
            _message.value = UiMessage("Admin login required to push", isError = true)
            return
        }
        scope.launch {
            _busy.value = true
            repository.syncToDrive().fold(
                onSuccess = { _message.value = UiMessage("Pushed credentials & roster to Google Drive") },
                onFailure = { _message.value = UiMessage(it.message ?: "Drive push failed", isError = true) }
            )
            _busy.value = false
        }
    }

    fun exportSnapshot(): String = repository.exportJson()
}

fun parseLevel(label: String): SkillLevel = SkillLevel.fromLabel(label)
