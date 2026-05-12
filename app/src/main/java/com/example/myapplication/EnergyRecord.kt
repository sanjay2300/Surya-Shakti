package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "energy_records")
data class EnergyRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val generation: Double,
    val consumption: Double,
    val savings: Double,
    val exported: Double,
    val weather: String
)