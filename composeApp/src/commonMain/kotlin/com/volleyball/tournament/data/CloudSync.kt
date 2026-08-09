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
        "https://jsonblob.com/api/jsonBlob/019fe58b-6c60-7a87-99b0-a05dfe8465d0"

    /** Durable read fallback (GitHub Gist). */
    const val FALLBACK_ROSTER_URL =
        "https://gist.githubusercontent.com/ucichillengineer/59c6f1256461293d9a7f0513a872dba2/raw/court-balance-data.json"
}

class CloudSync(
    private val client: HttpClient = createHttpClient()
) {
    suspend fun pull(): String =
        try {
            client.get(CloudConfig.SHARED_ROSTER_URL).bodyAsText().also { body ->
                if (body.contains("Blob not found", ignoreCase = true) ||
                    body.contains("\"error\"")
                ) {
                    error("Primary cloud missing")
                }
            }
        } catch (_: Exception) {
            client.get(CloudConfig.FALLBACK_ROSTER_URL).bodyAsText()
        }

    suspend fun push(jsonBody: String) {
        client.put(CloudConfig.SHARED_ROSTER_URL) {
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }.bodyAsText()
    }
}
