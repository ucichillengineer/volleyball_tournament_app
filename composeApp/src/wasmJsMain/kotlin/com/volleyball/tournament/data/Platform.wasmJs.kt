package com.volleyball.tournament.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

actual class PlatformStorage actual constructor() {
    actual fun loadJson(): String? = localStorage.getItem(KEY)

    actual fun saveJson(json: String) {
        localStorage.setItem(KEY, json)
    }

    companion object {
        private const val KEY = "court_balance_app_state"
    }
}

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMillis(): Long = jsDateNow().toLong()

actual fun createHttpClient(): HttpClient = HttpClient(Js) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
