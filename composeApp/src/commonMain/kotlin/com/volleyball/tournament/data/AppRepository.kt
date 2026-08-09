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

    suspend fun syncFromCloud(): Result<Unit> {
        return try {
            val remoteJson = cloudSync.pull()
            if (remoteJson.contains("Blob not found", ignoreCase = true)) {
                return Result.failure(IllegalStateException("Cloud roster not found"))
            }
            val remote = json.decodeFromString<AppState>(remoteJson)
            // Keep any local-only players so a stale/partial cloud pull does not erase newer local ratings.
            val mergedPlayers = mergePlayers(local = _state.value.players, remote = remote.players)
            replace(
                remote.copy(
                    players = mergedPlayers,
                    lastSyncedAt = currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncToCloud(): Result<Unit> = try {
        cloudSync.push(exportJson())
        update { it.copy(lastSyncedAt = currentTimeMillis()) }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun mergePlayers(local: List<com.volleyball.tournament.domain.Player>, remote: List<com.volleyball.tournament.domain.Player>): List<com.volleyball.tournament.domain.Player> {
        val byKey = linkedMapOf<String, com.volleyball.tournament.domain.Player>()
        remote.forEach { byKey[it.name.trim().lowercase()] = it }
        local.forEach { player ->
            val key = player.name.trim().lowercase()
            val existing = byKey[key]
            if (existing == null) {
                byKey[key] = player
            } else {
                // Prefer the copy with a higher total skill score if they differ; otherwise keep remote.
                byKey[key] = if (player.ratings.totalScore() > existing.ratings.totalScore()) player else existing
            }
        }
        return byKey.values.toList()
    }

    private fun persist(state: AppState) {
        storage.saveJson(json.encodeToString(state))
    }
}

expect fun currentTimeMillis(): Long
