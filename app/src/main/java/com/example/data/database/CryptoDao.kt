package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketAlertDao {
    @Query("SELECT * FROM market_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<MarketAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: MarketAlert)

    @Update
    suspend fun updateAlert(alert: MarketAlert)

    @Query("DELETE FROM market_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)

    @Query("SELECT * FROM market_alerts WHERE coinSymbol = :symbol AND isActive = 1")
    suspend fun getActiveAlertsForCoin(symbol: String): List<MarketAlert>
}

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AlertLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AlertLog)

    @Query("UPDATE alert_logs SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM alert_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM alert_logs")
    suspend fun clearAllLogs()
}

@Dao
interface AIPredictionDao {
    @Query("SELECT * FROM ai_predictions ORDER BY timestamp DESC")
    fun getAllPredictions(): Flow<List<AIPrediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: AIPrediction)

    @Query("SELECT * FROM ai_predictions WHERE coinSymbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPredictionForCoin(symbol: String): AIPrediction?

    @Query("DELETE FROM ai_predictions WHERE id = :id")
    suspend fun deletePredictionById(id: Int)
}
