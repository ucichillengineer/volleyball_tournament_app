package com.volleyball.tournament.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

expect fun createHttpClient(): HttpClient

class DriveSync(
    private val client: HttpClient = createHttpClient()
) {
    /**
     * Reads a publicly shared Google Drive file.
     * Share the JSON file as "Anyone with the link can view", then paste the file ID.
     */
    suspend fun fetchPublicDriveFile(fileId: String): String {
        val url = "https://drive.google.com/uc"
        return client.get(url) {
            parameter("export", "download")
            parameter("id", fileId)
        }.bodyAsText()
    }

    /**
     * Google Apps Script web app that stores JSON in Drive.
     * Deploy as web app (Anyone can access) — see scripts/GoogleDriveSync.gs
     */
    suspend fun fetchFromAppsScript(syncUrl: String): String {
        return client.get(syncUrl) {
            parameter("action", "load")
        }.bodyAsText()
    }

    suspend fun pushToAppsScript(syncUrl: String, jsonBody: String) {
        client.post(syncUrl) {
            parameter("action", "save")
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }.bodyAsText()
    }
}
