package com.arinahub.arinaagri.calculator

import android.app.Application
import com.arinahub.arinaagri.calculator.auth.AuthService
import com.arinahub.arinaagri.calculator.auth.FirebaseAuthManager
import com.arinahub.arinaagri.calculator.backup.EncryptedBackupManager
import com.arinahub.arinaagri.calculator.backup.cloud.CloudBackupClient
import com.arinahub.arinaagri.calculator.backup.cloud.GoogleDriveBackupClient
import com.arinahub.arinaagri.calculator.data.AppDatabase
import com.arinahub.arinaagri.calculator.data.FinanceRepository
import com.arinahub.arinaagri.calculator.importer.FinanceWorkbookImporter
import java.io.File

class ArinaCalculatorApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val workbookImporter: FinanceWorkbookImporter by lazy { FinanceWorkbookImporter() }
    val backupManager: EncryptedBackupManager by lazy {
        EncryptedBackupManager(database, File(noBackupFilesDir, "arina-before-restore.aabk"))
    }
    val authService: AuthService by lazy { FirebaseAuthManager() }
    val cloudBackupClient: CloudBackupClient by lazy { GoogleDriveBackupClient() }
}
