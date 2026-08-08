package com.volleyball.tournament.data

import com.volleyball.tournament.domain.AppState
import com.volleyball.tournament.domain.SeedPlayers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect class PlatformStorage() {
    fun loadJson(): String?
    fun saveJson(json: String)
}

class AppRepository(
    private val storage: PlatformStorage = PlatformStorage(),
    private val cloudSync: CloudSync = CloudSync()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun loadInitial(): AppState {
        val local = storage.loadJson()?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() }
        return when {
            local != null && local.players.isNotEmpty() -> local
            else -> AppState(players = SeedPlayers.initial).also { persist(it) }
        }
    }

    fun update(transform: (AppState) -> AppState) {
        _state.update { current ->
            val next = transform(current)
            persist(next)
            next
        }
    }

    fun replace(state: AppState) {
        _state.value = state
        persist(state)
    }

    fun exportJson(): String = json.encodeToString(_state.value)

    suspend fun syncFromCloud(): Result<Unit> = try {
        val remote = json.decodeFromString<AppState>(cloudSync.pull())
        replace(remote.copy(lastSyncedAt = currentTimeMillis()))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun syncToCloud(): Result<Unit> = try {
        cloudSync.push(exportJson())
        update { it.copy(lastSyncedAt = currentTimeMillis()) }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun persist(state: AppState) {
        storage.saveJson(json.encodeToString(state))
    }
}

expect fun currentTimeMillis(): Long
