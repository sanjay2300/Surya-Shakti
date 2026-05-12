package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.*

class EnergyViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).energyDao()
    private val unitRate = 5.0 // ₹ per kWh

    val allRecords: LiveData<List<EnergyRecord>> = dao.getAllRecords()

    private val _todayRecord = MutableLiveData<EnergyRecord?>()
    val todayRecord: LiveData<EnergyRecord?> = _todayRecord

    init {
        fetchTodayRecord()
    }

    fun fetchTodayRecord() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            _todayRecord.value = dao.getRecordForDay(startOfDay, endOfDay)
        }
    }

    fun getLast30DaysSavings(): LiveData<Double?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        return dao.getTotalSavings(calendar.timeInMillis)
    }

    fun simulate(gen: Double, weather: String): Double {
        return when (weather) {
            "Sunny" -> gen * 1.4
            "Cloudy" -> gen * 0.7
            "Rainy" -> gen * 0.3
            else -> gen
        }
    }

    fun calculateAndSave(inputGen: Double, con: Double, weather: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            val existing = dao.getRecordForDay(startOfDay, endOfDay)
            if (existing != null) {
                onComplete(false) // Already entered for today
                return@launch
            }

            val simulatedGen = simulate(inputGen, weather)
            val exported = if (simulatedGen > con) simulatedGen - con else 0.0
            val savings = simulatedGen * unitRate

            val record = EnergyRecord(
                date = System.currentTimeMillis(),
                generation = simulatedGen,
                consumption = con,
                savings = savings,
                exported = exported,
                weather = weather
            )

            dao.insert(record)
            fetchTodayRecord()
            onComplete(true)
        }
    }

    fun updateRecord(record: EnergyRecord) {
        viewModelScope.launch {
            dao.update(record)
            fetchTodayRecord()
        }
    }

    fun deleteRecord(record: EnergyRecord) {
        viewModelScope.launch {
            dao.delete(record)
            fetchTodayRecord()
        }
    }

    fun getAnalysis(simulatedGen: Double, con: Double): Pair<Int, String> {
        val independence = if (con == 0.0) 100 else ((minOf(simulatedGen, con) / con) * 100).toInt()
        
        val message = when {
            simulatedGen > con -> "Excellent! Over-generation: Exporting ${"%.1f".format(simulatedGen - con)} kWh to grid. ⚡"
            independence >= 80 -> "Great! Almost grid-independent today. 👍"
            independence >= 50 -> "Good solar usage. Try moving more loads to noon. ☀️"
            else -> "High grid dependency. Run heavy pumps/washers during peak sun!"
        }
        return Pair(independence, message)
    }
}