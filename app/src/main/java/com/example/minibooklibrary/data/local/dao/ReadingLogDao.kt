package com.example.minibooklibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.minibooklibrary.data.local.entity.ReadingLogEntity
import com.example.minibooklibrary.domain.DailyPages
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingLogDao {

    @Insert
    suspend fun insert(entry: ReadingLogEntity): Long

    @Insert
    suspend fun insertAll(entries: List<ReadingLogEntity>)

    /** Pages per day, oldest first. A personal log stays small enough to aggregate in memory. */
    @Query(
        "SELECT epochDay, SUM(pages) AS pages FROM reading_log " +
            "WHERE userId = :userId GROUP BY epochDay ORDER BY epochDay"
    )
    fun observeDailyPages(userId: Long): Flow<List<DailyPages>>

    @Query(
        "SELECT epochDay, SUM(pages) AS pages FROM reading_log " +
            "WHERE userId = :userId GROUP BY epochDay ORDER BY epochDay"
    )
    suspend fun getDailyPages(userId: Long): List<DailyPages>
}
