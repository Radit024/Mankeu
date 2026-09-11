package com.mankeu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BusinessEntity::class,
        ProjectEntity::class,
        BookEntity::class,
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
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        /**
         * Schema 4 drops the `units` master-data table and the `categories.defaultUnitId` column
         * that pointed into it (D-064). A unit is a word the owner types next to a quantity —
         * "kg", "botol", "porsi" — and keeping a per-business registry of those words bought
         * nothing: nothing read `defaultUnitId`, and the registry's only job was to populate a
         * dropdown the entry form does not use.
         *
         * Written as a real migration rather than a destructive fallback because v0.0.4 is on the
         * Play Store: from here on, a schema change must carry a user's books forward, not delete
         * them. Financial data is not something an app gets to drop because a table was retired.
         *
         * `categories` is recreated because SQLite before 3.35 cannot `DROP COLUMN`, and the
         * minimum supported API (26) ships older versions than that. Every remaining column is
         * copied across by name, and the two indices are rebuilt exactly as the entity declares.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `units`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories_new` (
                        `id` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        `aliases` TEXT NOT NULL,
                        `templateOrigin` TEXT,
                        `active` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `categories_new`
                        (`id`, `businessId`, `role`, `name`, `normalizedName`, `aliases`,
                         `templateOrigin`, `active`, `createdAtEpochMillis`)
                    SELECT `id`, `businessId`, `role`, `name`, `normalizedName`, `aliases`,
                           `templateOrigin`, `active`, `createdAtEpochMillis`
                    FROM `categories`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `categories`")
                db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_businessId` ON `categories` (`businessId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_businessId_role_normalizedName` " +
                        "ON `categories` (`businessId`, `role`, `normalizedName`)",
                )
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "mankeu.db",
        )
            .addMigrations(MIGRATION_3_4)
            .build()
    }
}
