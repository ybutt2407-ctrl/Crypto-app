package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_alerts")
data class MarketAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinSymbol: String,
    val conditionType: String, // "ABOVE", "BELOW", "FLUCTUATION_PCT"
    val targetValue: Double,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alert_logs")
data class AlertLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinSymbol: String,
    val title: String,
    val message: String,
    val alertType: String, // "CUSTOM_ALERT", "SYSTEM_SPIKE", "SYSTEM_CRASH"
    val priceAtTrigger: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "ai_predictions")
data class AIPrediction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinSymbol: String,
    val predictedTrend: String, // "BULLISH", "BEARISH", "SIDEWAYS"
    val targetMin: Double,
    val targetMax: Double,
    val sentimentScore: Double, // 0.0 to 100.0 or 0 to 1.0
    val analysisText: String,
    val actionableThreshold: Double,
    val timestamp: Long = System.currentTimeMillis()
)
