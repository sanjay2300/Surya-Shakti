package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EnergyDao {
    @Insert
    suspend fun insert(record: EnergyRecord)

    @Update
    suspend fun update(record: EnergyRecord)

    @Delete
    suspend fun delete(record: EnergyRecord)

    @Query("SELECT * FROM energy_records ORDER BY date DESC")
    fun getAllRecords(): LiveData<List<EnergyRecord>>

    @Query("SELECT SUM(savings) FROM energy_records WHERE date >= :startDate")
    fun getTotalSavings(startDate: Long): LiveData<Double?>

    @Query("SELECT * FROM energy_records WHERE date >= :startOfDay AND date <= :endOfDay LIMIT 1")
    suspend fun getRecordForDay(startOfDay: Long, endOfDay: Long): EnergyRecord?
}