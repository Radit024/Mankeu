package com.mankeu.backup.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveBackupClientTest {

    @Test
    fun parseBackupListJson_validResponse_parsesCorrectly() {
        val sampleJson = """
            {
              "files": [
                {
                  "id": "drive-file-123",
                  "name": "mankeu-backup-2026-08-29.aabk",
                  "size": "45678",
                  "createdTime": "2026-08-29T00:30:00.000Z",
                  "appProperties": {
                    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                  }
                },
                {
                  "id": "drive-file-456",
                  "name": "mankeu-backup-2026-08-28.aabk",
                  "size": "41234",
                  "createdTime": "2026-08-28T12:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val parsed = GoogleDriveBackupClient.parseBackupListJson(sampleJson)

        assertEquals(2, parsed.size)

        val first = parsed[0]
        assertEquals("drive-file-123", first.id)
        assertEquals("mankeu-backup-2026-08-29.aabk", first.name)
        assertEquals(45678L, first.sizeBytes)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", first.sha256Hex)
        assertTrue(first.createdTimeEpochMillis > 0)

        val second = parsed[1]
        assertEquals("drive-file-456", second.id)
        assertEquals("mankeu-backup-2026-08-28.aabk", second.name)
        assertEquals(41234L, second.sizeBytes)
        assertEquals(null, second.sha256Hex)
    }

    @Test
    fun parseBackupListJson_emptyFiles_returnsEmptyList() {
        val sampleJson = """{ "files": [] }"""
        val parsed = GoogleDriveBackupClient.parseBackupListJson(sampleJson)
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun parseBackupListJson_missingFilesArray_returnsEmptyList() {
        val sampleJson = """{ "kind": "drive#fileList" }"""
        val parsed = GoogleDriveBackupClient.parseBackupListJson(sampleJson)
        assertTrue(parsed.isEmpty())
    }
}
