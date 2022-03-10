package com.pagatodo.sunmi.poslib.harmonizer.db

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface SyncDao {

    @Query("SELECT * FROM Sync WHERE status == :status")
    fun selectByStatus(status: String): LiveData<List<Sync>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sync: Sync)

    @Update
    suspend fun update(sync: Sync)

    @Query("DELETE FROM Sync")
    suspend fun deleteSyncData()

    @Query("SELECT * FROM Sync WHERE dateTime == :date")
    fun getByDate(date: Date): LiveData<Sync>
}