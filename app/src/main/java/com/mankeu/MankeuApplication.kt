package com.mankeu

import android.app.Application
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.presentation.applyThemeMode
import com.mankeu.presentation.readThemeMode
import java.io.File

class MankeuApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val workbookImporter: FinanceWorkbookImporter by lazy { FinanceWorkbookImporter() }
    val backupManager: EncryptedBackupManager by lazy {
        EncryptedBackupManager(database, File(noBackupFilesDir, "umkm-before-restore.aabk"))
    }
    val authService by lazy { FirebaseAuthManager() }
    val cloudBackupClient by lazy { GoogleDriveBackupClient() }

    override fun onCreate() {
        super.onCreate()
        applyThemeMode(this, readThemeMode())
    }
}
