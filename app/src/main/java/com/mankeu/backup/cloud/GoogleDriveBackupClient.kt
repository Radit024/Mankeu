package com.mankeu.backup.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTimeEpochMillis: Long,
    val sha256Hex: String?,
)

/**
 * Drive v3 REST over `HttpURLConnection`, JSON through the platform's `org.json`.
 *
 * JSON used to be read with regular expressions, which break on any escaped quote, and written by
 * string interpolation. A non-2xx response surfaced its raw body in the UI. Every call now goes
 * through [call], which always disconnects and reports failures as a short status message.
 */
class GoogleDriveBackupClient(
    private val apiBaseUrl: String = "https://www.googleapis.com/drive/v3",
    private val uploadBaseUrl: String = "https://www.googleapis.com/upload/drive/v3",
    private val maxRetainedBackups: Int = 5,
) {

    suspend fun listBackups(accessToken: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        call(
            "$apiBaseUrl/files?spaces=appDataFolder&fields=files(id,name,size,createdTime,appProperties)&orderBy=createdTime%20desc",
            "GET", accessToken, SHORT_TIMEOUT_MS, "mengambil daftar cadangan",
        ) { conn -> parseBackupListJson(conn.readBody()) }
    }

    suspend fun uploadBackup(
        accessToken: String,
        filename: String,
        encryptedBytes: ByteArray,
        sha256Hex: String,
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        val boundary = "==_MankeuBackupBoundary_${System.currentTimeMillis()}_=="
        val metadata = JSONObject()
            .put("name", filename)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("appProperties", JSONObject().put("sha256", sha256Hex))
        val headerPart = (
            "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata" +
                "\r\n--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        val footerPart = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        val created = call(
            "$uploadBaseUrl/files?uploadType=multipart", "POST", accessToken, LONG_TIMEOUT_MS, "mengunggah cadangan",
            send = { conn ->
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.setFixedLengthStreamingMode(headerPart.size + encryptedBytes.size + footerPart.size)
                conn.outputStream.use { output ->
                    output.write(headerPart)
                    output.write(encryptedBytes)
                    output.write(footerPart)
                }
            },
        ) { conn -> parseFile(JSONObject(conn.readBody()), fallbackBytes = encryptedBytes.size.toLong()) }
            ?: DriveBackupFile(
                id = "drive-${System.currentTimeMillis()}",
                name = filename,
                sizeBytes = encryptedBytes.size.toLong(),
                createdTimeEpochMillis = System.currentTimeMillis(),
                sha256Hex = sha256Hex,
            )

        // Retention is best effort: the upload already succeeded, and a failed prune must not undo that.
        runCatching {
            listBackups(accessToken).drop(maxRetainedBackups).forEach { deleteBackup(accessToken, it.id) }
        }
        created
    }

    suspend fun downloadBackup(accessToken: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        call("$apiBaseUrl/files/${fileId.urlEncoded()}?alt=media", "GET", accessToken, LONG_TIMEOUT_MS, "mengunduh cadangan") { conn ->
            conn.inputStream.use { it.readBytes() }
        }
    }

    suspend fun deleteBackup(accessToken: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val conn = (URL("$apiBaseUrl/files/${fileId.urlEncoded()}").openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = SHORT_TIMEOUT_MS
            conn.readTimeout = SHORT_TIMEOUT_MS
            conn.responseCode.let { it in 200..299 || it == 404 }
        } finally {
            conn.disconnect()
        }
    }

    private inline fun <T> call(
        url: String,
        method: String,
        accessToken: String,
        timeoutMs: Int,
        action: String,
        send: (HttpURLConnection) -> Unit = {},
        read: (HttpURLConnection) -> T,
    ): T {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            send(conn)
            val status = conn.responseCode
            if (status !in 200..299) {
                conn.errorStream?.close()
                error(
                    if (status == 401) "Sesi Google berakhir. Masuk ulang, lalu coba $action lagi."
                    else "Gagal $action di Google Drive (HTTP $status).",
                )
            }
            return read(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun HttpURLConnection.readBody(): String = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }

    private fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

    companion object {
        private const val SHORT_TIMEOUT_MS = 15_000
        private const val LONG_TIMEOUT_MS = 30_000

        fun parseBackupListJson(jsonText: String): List<DriveBackupFile> {
            val files = JSONObject(jsonText).optJSONArray("files") ?: return emptyList()
            return (0 until files.length()).mapNotNull { index -> files.optJSONObject(index)?.let { parseFile(it) } }
        }

        private fun parseFile(json: JSONObject, fallbackBytes: Long = 0L): DriveBackupFile? {
            val id = json.optString("id").ifBlank { return null }
            return DriveBackupFile(
                id = id,
                name = json.optString("name").ifBlank { "mankeu-backup.aabk" },
                // Drive encodes int64 fields such as `size` as JSON strings.
                sizeBytes = json.optString("size").toLongOrNull() ?: fallbackBytes,
                createdTimeEpochMillis = runCatching { Instant.parse(json.getString("createdTime")).toEpochMilli() }
                    .getOrDefault(System.currentTimeMillis()),
                sha256Hex = json.optJSONObject("appProperties")?.optString("sha256")?.ifBlank { null },
            )
        }
    }
}
