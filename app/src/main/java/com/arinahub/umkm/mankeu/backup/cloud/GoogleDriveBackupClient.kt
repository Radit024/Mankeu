package com.arinahub.umkm.mankeu.backup.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter

data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTimeEpochMillis: Long,
    val sha256Hex: String?,
)

interface CloudBackupClient {
    suspend fun listBackups(accessToken: String): List<DriveBackupFile>
    suspend fun uploadBackup(accessToken: String, filename: String, encryptedBytes: ByteArray, sha256Hex: String): DriveBackupFile
    suspend fun downloadBackup(accessToken: String, fileId: String): ByteArray
    suspend fun deleteBackup(accessToken: String, fileId: String): Boolean
}

class GoogleDriveBackupClient(
    private val apiBaseUrl: String = "https://www.googleapis.com/drive/v3",
    private val uploadBaseUrl: String = "https://www.googleapis.com/upload/drive/v3",
    private val maxRetainedBackups: Int = 5,
) : CloudBackupClient {

    override suspend fun listBackups(accessToken: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val url = URL("$apiBaseUrl/files?spaces=appDataFolder&fields=files(id,name,size,createdTime,appProperties)&orderBy=createdTime%20desc")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            error("Gagal mengambil daftar cadangan dari Google Drive: $errorBody")
        }

        val jsonText = conn.inputStream.bufferedReader().readText()
        parseBackupListJson(jsonText)
    }

    override suspend fun uploadBackup(
        accessToken: String,
        filename: String,
        encryptedBytes: ByteArray,
        sha256Hex: String,
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        val boundary = "==_ArinaBackupBoundary_${System.currentTimeMillis()}_=="
        val metadataJson = """{"name":"$filename","parents":["appDataFolder"],"appProperties":{"sha256":"$sha256Hex"}}"""

        val url = URL("$uploadBaseUrl/files?uploadType=multipart")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connectTimeout = 30000
            readTimeout = 30000
        }

        val headerPart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/octet-stream\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)

        val footerPart = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        conn.setFixedLengthStreamingMode(headerPart.size + encryptedBytes.size + footerPart.size)

        conn.outputStream.use { output ->
            output.write(headerPart)
            output.write(encryptedBytes)
            output.write(footerPart)
            output.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            error("Gagal mengunggah cadangan ke Google Drive: $errorBody")
        }

        val responseJson = conn.inputStream.bufferedReader().readText()
        val created = parseSingleFilePure(responseJson, fallbackBytes = encryptedBytes.size.toLong()) ?: DriveBackupFile(
            id = "drive-${System.currentTimeMillis()}",
            name = filename,
            sizeBytes = encryptedBytes.size.toLong(),
            createdTimeEpochMillis = System.currentTimeMillis(),
            sha256Hex = sha256Hex,
        )

        // Prune older backups beyond maxRetainedBackups
        runCatching {
            val all = listBackups(accessToken)
            if (all.size > maxRetainedBackups) {
                for (i in maxRetainedBackups until all.size) {
                    deleteBackup(accessToken, all[i].id)
                }
            }
        }

        created
    }

    override suspend fun downloadBackup(accessToken: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val url = URL("$apiBaseUrl/files/$fileId?alt=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 30000
            readTimeout = 30000
        }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            error("Gagal mengunduh cadangan dari Google Drive: $errorBody")
        }

        conn.inputStream.use { input -> input.readBytes() }
    }

    override suspend fun deleteBackup(accessToken: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$apiBaseUrl/files/$fileId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val responseCode = conn.responseCode
        responseCode in 200..299 || responseCode == 404
    }

    companion object {
        fun parseBackupListJson(jsonText: String): List<DriveBackupFile> {
            val filesList = mutableListOf<DriveBackupFile>()
            val filesIndex = jsonText.indexOf("\"files\"")
            if (filesIndex == -1) return emptyList()

            val openBracket = jsonText.indexOf('[', filesIndex)
            if (openBracket == -1) return emptyList()

            var depth = 0
            var objStart = -1
            for (i in openBracket until jsonText.length) {
                val c = jsonText[i]
                if (c == '{') {
                    if (depth == 0) objStart = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        val objText = jsonText.substring(objStart, i + 1)
                        parseSingleFilePure(objText)?.let { filesList.add(it) }
                        objStart = -1
                    }
                } else if (c == ']' && depth == 0) {
                    break
                }
            }
            return filesList
        }

        fun parseSingleFilePure(json: String, fallbackBytes: Long = 0L): DriveBackupFile? {
            val id = extractJsonString(json, "id") ?: return null
            val name = extractJsonString(json, "name") ?: "arina-backup.aabk"
            val size = extractJsonLong(json, "size") ?: fallbackBytes
            val createdTimeStr = extractJsonString(json, "createdTime")
            val createdTime = if (createdTimeStr != null) {
                runCatching {
                    Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(createdTimeStr)).toEpochMilli()
                }.getOrDefault(System.currentTimeMillis())
            } else {
                System.currentTimeMillis()
            }
            val sha256 = extractJsonString(json, "sha256")

            return DriveBackupFile(
                id = id,
                name = name,
                sizeBytes = size,
                createdTimeEpochMillis = createdTime,
                sha256Hex = sha256,
            )
        }

        private fun extractJsonString(json: String, key: String): String? {
            val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            return pattern.find(json)?.groupValues?.get(1)
        }

        private fun extractJsonLong(json: String, key: String): Long? {
            val pattern = Regex("\"$key\"\\s*:\\s*\"?(\\d+)\"?")
            return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
        }
    }
}
