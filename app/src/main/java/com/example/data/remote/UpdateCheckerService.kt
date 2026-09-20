package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val changelog: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean,
    val publishedAt: String
)

object UpdateCheckerService {

    // Default repository path on GitHub
    const val DEFAULT_REPO_PATH = "cybermasters/ebbinghaus-android"

    val currentVersionName: String
        get() = BuildConfig.VERSION_NAME

    val currentVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    suspend fun checkLatestRelease(repoPath: String = DEFAULT_REPO_PATH): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$repoPath/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "Hermann-Memo-App")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "v1.0.0")
                val releaseName = json.optString("name", tagName)
                val body = json.optString("body", "Нет описания изменений.")
                val publishedAt = json.optString("published_at", "")

                var apkDownloadUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", apkDownloadUrl)
                            break
                        }
                    }
                }

                val remoteVersionClean = tagName.removePrefix("v").trim()
                val isUpdateAvailable = isRemoteVersionHigher(remoteVersionClean, currentVersionName)

                Result.success(
                    AppReleaseInfo(
                        tagName = tagName,
                        releaseName = releaseName,
                        changelog = body,
                        downloadUrl = apkDownloadUrl,
                        isUpdateAvailable = isUpdateAvailable,
                        publishedAt = publishedAt
                    )
                )
            } else if (responseCode == 404) {
                // Repository releases not published yet or private
                Result.success(
                    AppReleaseInfo(
                        tagName = "v$currentVersionName",
                        releaseName = "Релиз v$currentVersionName (Текущая)",
                        changelog = "Вы используете самую актуальную версию приложения.",
                        downloadUrl = "",
                        isUpdateAvailable = false,
                        publishedAt = "Актуально"
                    )
                )
            } else {
                Result.failure(Exception("Сервер GitHub вернул код ответа: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isRemoteVersionHigher(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
