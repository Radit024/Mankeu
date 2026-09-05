package com.arinahub.umkm.mankeu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusinessEntity::class,
        ProjectEntity::class,
        BookEntity::class,
        UnitEntity::class,
        CategoryEntity::class,
        BudgetItemEntity::class,
        FinancialEventEntity::class,
        JournalEntryEntity::class,
        PostingEntity::class,
        ObligationEntity::class,
        SettlementEntity::class,
        ScenarioAssumptionEntity::class,
        ImportBatchEntity::class,
        ImportNoticeEntity::class,
        ProvenanceEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "umkm-mankeu.db",
        )
            // The owner confirmed that the pre-release MVP contains no data that
            // needs migration. Remove this before the first real user-data build.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
