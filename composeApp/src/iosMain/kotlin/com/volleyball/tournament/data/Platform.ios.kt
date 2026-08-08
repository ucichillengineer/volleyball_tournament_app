package com.volleyball.tournament.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

actual class PlatformStorage actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun loadJson(): String? =
        defaults.stringForKey(KEY)

    actual fun saveJson(json: String) {
        defaults.setObject(json, KEY)
        defaults.synchronize()
    }

    companion object {
        private const val KEY = "court_balance_app_state"
    }
}

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
