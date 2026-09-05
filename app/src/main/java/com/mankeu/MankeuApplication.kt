package com.mankeu

import android.app.Application
import com.mankeu.auth.AuthService
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.CloudBackupClient
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.importer.FinanceWorkbookImporter
import java.io.File

class MankeuApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val workbookImporter: FinanceWorkbookImporter by lazy { FinanceWorkbookImporter() }
    val backupManager: EncryptedBackupManager by lazy {
        EncryptedBackupManager(database, File(noBackupFilesDir, "umkm-before-restore.aabk"))
    }
    val authService: AuthService by lazy { FirebaseAuthManager() }
    val cloudBackupClient: CloudBackupClient by lazy { GoogleDriveBackupClient() }
}
