package com.volleyball.tournament.data

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private var appContext: Context? = null

fun initAndroidStorage(context: Context) {
    appContext = context.applicationContext
}

actual class PlatformStorage actual constructor() {
    actual fun loadJson(): String? {
        val ctx = appContext ?: return null
        return ctx.getSharedPreferences("court_balance", Context.MODE_PRIVATE)
            .getString("app_state", null)
    }

    actual fun saveJson(json: String) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences("court_balance", Context.MODE_PRIVATE)
            .edit()
            .putString("app_state", json)
            .apply()
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
