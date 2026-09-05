package com.arinahub.umkm.mankeu.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {
    @Test
    fun `preview counts the user-facing restore scope`() {
        val snapshot = PortableBackup(
            schemaVersion = 2,
            createdAtEpochMillis = 1234,
            tables = listOf(
                BackupTable("businesses", listOf("id"), listOf(listOf(BackupValue.Text("b1")))),
                BackupTable("projects", listOf("id"), listOf(listOf(BackupValue.Text("p1")), listOf(BackupValue.Text("p2")))),
                BackupTable("budget_items", listOf("id"), emptyList()),
                BackupTable("financial_events", listOf("id"), List(3) { listOf(BackupValue.Text("e$it")) }),
            ),
        )

        val preview = snapshot.toPreview()

        assertEquals(4, preview.tableCount)
        assertEquals(6, preview.rowCount)
        assertEquals(1, preview.businessCount)
        assertEquals(2, preview.projectCount)
        assertEquals(0, preview.budgetItemCount)
        assertEquals(3, preview.financialEventCount)
    }

    private val password = "rahasia-kuat".toCharArray()

    @Test
    fun `portable database payload and encryption round trip exactly`() {
        val snapshot = PortableBackup(
            schemaVersion = 2,
            createdAtEpochMillis = 123456789,
            tables = listOf(
                BackupTable(
                    "events",
                    listOf("id", "amount", "note", "optional", "ratio", "blob"),
                    listOf(
                        listOf(
                            BackupValue.Text("event-1"),
                            BackupValue.Integer(7_000_000),
                            BackupValue.Text("Sewa lahan"),
                            BackupValue.Null,
                            BackupValue.Real(1.25),
                            BackupValue.Blob(byteArrayOf(1, 2, 3)),
                        ),
                    ),
                ),
            ),
        )

        val plain = PortableBackupCodec.encode(snapshot)
        val envelope = PasswordBackupCipher.encrypt(plain, password, iterations = 100_000)
        val restored = PortableBackupCodec.decode(PasswordBackupCipher.decrypt(envelope, password))

        assertEquals(snapshot.schemaVersion, restored.schemaVersion)
        assertEquals(snapshot.createdAtEpochMillis, restored.createdAtEpochMillis)
        assertEquals("Sewa lahan", (restored.tables.single().rows.single()[2] as BackupValue.Text).value)
        assertArrayEquals(byteArrayOf(1, 2, 3), (restored.tables.single().rows.single()[5] as BackupValue.Blob).value)
    }

    @Test
    fun `wrong password and tampering fail authentication`() {
        val envelope = PasswordBackupCipher.encrypt("data penting".toByteArray(), password, iterations = 100_000)

        val wrongPassword = runCatching { PasswordBackupCipher.decrypt(envelope, "bukan-password".toCharArray()) }
        assertTrue(wrongPassword.isFailure)
        assertTrue(wrongPassword.exceptionOrNull()?.message.orEmpty().contains("salah", ignoreCase = true))

        val tampered = envelope.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }
        val changed = runCatching { PasswordBackupCipher.decrypt(tampered, password) }
        assertTrue(changed.isFailure)
    }
}
