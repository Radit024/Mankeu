package com.mankeu.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One local change waiting to be pushed. Rows are written by SQLite triggers (see [SyncSchema]),
 * never by app code, so every write path — UI, import, restore, migration — is captured the same
 * way. [seq] orders changes on this device; several entries for one row collapse to the newest.
 */
@Entity(tableName = "sync_outbox", indices = [Index(value = ["tableName", "rowId"])])
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val tableName: String,
    val rowId: String,
    /** `UPSERT` or `DELETE`. */
    val op: String,
    val changedAtEpochMillis: Long,
)

/** Device-local sync bookkeeping (device id, cursors, flags). Never backed up, never synced. */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_outbox ORDER BY seq")
    suspend fun pending(): List<SyncOutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE seq <= :seq")
    suspend fun acknowledge(seq: Long)

    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Query("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES (:key, :value)")
    suspend fun setMeta(key: String, value: String)

    @Query("DELETE FROM sync_meta WHERE `key` = :key")
    suspend fun clearMeta(key: String)
}
