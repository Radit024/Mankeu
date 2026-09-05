package com.arinahub.umkm.mankeu

import android.app.Application
import com.arinahub.umkm.mankeu.auth.AuthService
import com.arinahub.umkm.mankeu.auth.FirebaseAuthManager
import com.arinahub.umkm.mankeu.backup.EncryptedBackupManager
import com.arinahub.umkm.mankeu.backup.cloud.CloudBackupClient
import com.arinahub.umkm.mankeu.backup.cloud.GoogleDriveBackupClient
import com.arinahub.umkm.mankeu.data.AppDatabase
import com.arinahub.umkm.mankeu.data.FinanceRepository
import com.arinahub.umkm.mankeu.importer.FinanceWorkbookImporter
import java.io.File

class UmkmManKeuApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val workbookImporter: FinanceWorkbookImporter by lazy { FinanceWorkbookImporter() }
    val backupManager: EncryptedBackupManager by lazy {
        EncryptedBackupManager(database, File(noBackupFilesDir, "umkm-before-restore.aabk"))
    }
    val authService: AuthService by lazy { FirebaseAuthManager() }
    val cloudBackupClient: CloudBackupClient by lazy { GoogleDriveBackupClient() }
}
