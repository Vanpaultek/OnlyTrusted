// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.onlytrusted.app.data.db.entities.SecurityEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: SecurityEvent): Long

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<SecurityEvent>>

    @Query("SELECT COUNT(*) FROM security_events WHERE type = :type AND timestamp >= :since")
    fun countOfTypeSince(type: String, since: Long): Flow<Int>

    /** One-shot variant of [countOfTypeSince] for the Weekly Digest PDF generator. */
    @Query("SELECT COUNT(*) FROM security_events WHERE type = :type AND timestamp >= :since")
    suspend fun countOfTypeSinceOnce(type: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE timestamp >= :since")
    fun countSince(since: Long): Flow<Int>

    /** One-shot variant of [countSince]. */
    @Query("SELECT COUNT(*) FROM security_events WHERE timestamp >= :since")
    suspend fun countSinceOnce(since: Long): Int

    @Query("DELETE FROM security_events")
    suspend fun clearAll()

    @Query("DELETE FROM security_events WHERE timestamp < :cutoff")
    suspend fun clearBefore(cutoff: Long)

    @Query("SELECT rowHash FROM security_events ORDER BY id DESC LIMIT 1")
    suspend fun lastRowHash(): String?

    @Query("SELECT * FROM security_events ORDER BY id ASC")
    suspend fun allOldestFirst(): List<SecurityEvent>

    @Query("SELECT * FROM security_events ORDER BY id DESC LIMIT :limit")
    suspend fun tail(limit: Int): List<SecurityEvent>

    /**
     * Blocking (non-suspend) read of recent call-related events for the
     * CallScreeningService storm detector. The service runs on a binder thread
     * where synchronous DB reads are safe — see [SecurityRepository.isCallBlockedSync]
     * for the same pattern.
     */
    @Query("""
        SELECT * FROM security_events
         WHERE type IN (:types) AND timestamp >= :since
         ORDER BY timestamp DESC LIMIT :limit
    """)
    fun recentOfTypesSinceSync(
        types: List<String>,
        since: Long,
        limit: Int = 50
    ): List<SecurityEvent>

    @Query("SELECT COUNT(*) FROM security_events")
    suspend fun totalCount(): Int

    /** Delete everything older than the newest [keep] rows. */
    @Query("""
        DELETE FROM security_events
        WHERE id NOT IN (SELECT id FROM security_events ORDER BY id DESC LIMIT :keep)
    """)
    suspend fun trimToNewest(keep: Int)
}
