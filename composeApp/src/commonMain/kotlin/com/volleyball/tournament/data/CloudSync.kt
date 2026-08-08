package com.volleyball.tournament.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

expect fun createHttpClient(): HttpClient

/**
 * Shared cloud roster — no Google Drive setup required.
 * Hosted on jsonblob (CORS-friendly). Auto pull/push from the app.
 */
object CloudConfig {
    const val SHARED_ROSTER_URL =
        "https://jsonblob.com/api/jsonBlob/019fdfdf-413b-7c69-9c01-7edb3fab9a20"
}

class CloudSync(
    private val client: HttpClient = createHttpClient()
) {
    suspend fun pull(): String =
        client.get(CloudConfig.SHARED_ROSTER_URL).bodyAsText()

    suspend fun push(jsonBody: String) {
        client.put(CloudConfig.SHARED_ROSTER_URL) {
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }.bodyAsText()
    }
}
