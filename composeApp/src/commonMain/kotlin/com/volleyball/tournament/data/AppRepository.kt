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
    private val driveSync: DriveSync = DriveSync()
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

    suspend fun syncFromDrive(): Result<Unit> {
        val syncUrl = _state.value.driveSyncUrl.trim()
        val fileId = _state.value.driveFileId.trim()
        return try {
            val remoteJson = when {
                syncUrl.isNotBlank() -> driveSync.fetchFromAppsScript(syncUrl)
                fileId.isNotBlank() -> driveSync.fetchPublicDriveFile(fileId)
                else -> return Result.failure(IllegalStateException("Configure Drive Sync URL or File ID in Admin"))
            }
            val remote = json.decodeFromString<AppState>(remoteJson)
            // Keep local drive config if remote doesn't have it
            val merged = remote.copy(
                driveFileId = remote.driveFileId.ifBlank { _state.value.driveFileId },
                driveSyncUrl = remote.driveSyncUrl.ifBlank { _state.value.driveSyncUrl },
                lastSyncedAt = currentTimeMillis()
            )
            replace(merged)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncToDrive(): Result<Unit> {
        val syncUrl = _state.value.driveSyncUrl.trim()
        if (syncUrl.isBlank()) {
            return Result.failure(IllegalStateException("Configure Drive Sync URL (Apps Script) in Admin to push data"))
        }
        return try {
            val payload = exportJson()
            driveSync.pushToAppsScript(syncUrl, payload)
            update { it.copy(lastSyncedAt = currentTimeMillis()) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun persist(state: AppState) {
        storage.saveJson(json.encodeToString(state))
    }
}

expect fun currentTimeMillis(): Long
