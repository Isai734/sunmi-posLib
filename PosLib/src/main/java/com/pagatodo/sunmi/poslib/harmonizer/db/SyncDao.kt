package com.pagatodo.sunmi.poslib.harmonizer.db

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface SyncDao {

    @Query("SELECT * FROM Sync WHERE status == :status")
    fun selectByStatus(status: String): List<Sync>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sync: Sync): Long

    @Update
    suspend fun update(sync: Sync)

    @Query("DELETE FROM Sync")
    suspend fun deleteSyncData()

    @Query("DELETE FROM Sync WHERE dateTime == :date")
    fun deleteByDate(date: Date?)

    @Query("SELECT * FROM Sync WHERE dateTime == :date")
    fun getByDate(date: Date): Sync
}