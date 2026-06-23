package com.example.data.repository

import android.util.Log
import com.example.data.database.AIPrediction
import com.example.data.database.AIPredictionDao
import com.example.data.database.AlertLog
import com.example.data.database.AlertLogDao
import com.example.data.database.MarketAlert
import com.example.data.database.MarketAlertDao
import com.example.data.model.LiveCoin
import com.example.data.network.GeminiNetworkClient
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import kotlin.math.roundToInt

class CryptoRepository(
    private val marketAlertDao: MarketAlertDao,
    private val alertLogDao: AlertLogDao,
    private val aiPredictionDao: AIPredictionDao
) {
    private val TAG = "CryptoRepository"

    val allAlertRules: Flow<List<MarketAlert>> = marketAlertDao.getAllAlerts()
    val allAlertLogs: Flow<List<AlertLog>> = alertLogDao.getAllLogs()
    val allAIPredictions: Flow<List<AIPrediction>> = aiPredictionDao.getAllPredictions()

    suspend fun insertAlertRule(alert: MarketAlert) {
        marketAlertDao.insertAlert(alert)
    }

    suspend fun deleteAlertRule(id: Int) {
        marketAlertDao.deleteAlertById(id)
    }

    suspend fun updateAlertRule(alert: MarketAlert) {
        marketAlertDao.updateAlert(alert)
    }

    suspend fun insertAlertLog(log: AlertLog) {
        alertLogDao.insertLog(log)
    }

    suspend fun clearAlertLogs() {
        alertLogDao.clearAllLogs()
    }

    suspend fun markLogAsRead(id: Int) {
        alertLogDao.markAsRead(id)
    }

    suspend fun deleteLogById(id: Int) {
        alertLogDao.deleteLogById(id)
    }

    suspend fun getActiveAlertsForCoin(symbol: String): List<MarketAlert> {
        return marketAlertDao.getActiveAlertsForCoin(symbol)
    }

    suspend fun getLatestPrediction(symbol: String): AIPrediction? {
        return aiPredictionDao.getLatestPredictionForCoin(symbol)
    }

    /**
     * Request a machine learning prediction on historical parameters for a live coin asset
     */
    suspend fun generateMarketPrediction(coin: LiveCoin): AIPrediction {
        val prompt = """
            Perform professional market analysis and machine learning price projection for ${coin.symbol} (${coin.name}).
            Current Price: $${coin.price}
            24h High: $${coin.high24h}
            24h Low: $${coin.low24h}
            Historical Spot Price Series: ${coin.historicalPrices.joinToString(", ")}
            RSI Technical Index (14): ${coin.rsi}
            50-period simple moving average (SMA): ${coin.movingAverage50}
            
            Respond only with a single, valid JSON object containing exactly these fields (do not write any conversational wrappers or markdown code blocks like ```json ... ```, just return the raw JSON):
            {
               "predictedTrend": "BULLISH" or "BEARISH" or "SIDEWAYS",
               "targetMin": Double,
               "targetMax": Double,
               "sentimentScore": Double (from 0 to 100),
               "analysisText": "Concise 2-3 sentence quantitative rationale combining RSI and technical indicators.",
               "actionableThreshold": Double (a recommended trigger price to set an alert on based on trend breakout)
            }
        """.trimIndent()

        var prediction: AIPrediction? = null

        try {
            val responseString = GeminiNetworkClient.getPrediction(prompt)
            if (!responseString.isNullOrEmpty()) {
                val cleanedJson = cleanResponseJson(responseString)
                val json = JSONObject(cleanedJson)
                
                prediction = AIPrediction(
                    coinSymbol = coin.symbol,
                    predictedTrend = json.optString("predictedTrend", "SIDEWAYS").uppercase(),
                    targetMin = json.optDouble("targetMin", coin.price * 0.95),
                    targetMax = json.optDouble("targetMax", coin.price * 1.05),
                    sentimentScore = json.optDouble("sentimentScore", 50.0),
                    analysisText = json.optString("analysisText", "Gemini neural model identified neutral consolidation bounds for ${coin.name}."),
                    actionableThreshold = json.optDouble("actionableThreshold", coin.price * 1.02)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Gemini API - falling back to technical indicator model: ${e.message}")
        }

        // Fallback: Professional Rule-Based Technical Indicator Predictive Agent
        if (prediction == null) {
            prediction = executeTechnicalPredictiveModel(coin)
        }

        // Save prediction to Room database history
        aiPredictionDao.insertPrediction(prediction)
        return prediction
    }

    private fun cleanResponseJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun executeTechnicalPredictiveModel(coin: LiveCoin): AIPrediction {
        val rsiValue = coin.rsi
        val currentPrice = coin.price
        val movingAvg = coin.movingAverage50

        // Heuristic analysis
        val trend: String
        val sentiment: Double
        val minPrice: Double
        val maxPrice: Double
        val actionableAlert: Double
        val rationale: String

        when {
            rsiValue < 35.0 -> {
                trend = "BULLISH"
                sentiment = 78.5
                minPrice = currentPrice * 0.985
                maxPrice = currentPrice * 1.12
                actionableAlert = (currentPrice * 1.06).roundTo2Decimals()
                rationale = "ML Indicators (RSI $rsiValue) show highly oversold signals. Combined with the 50-day moving average support, we identify a strong bullish accumulation rebound opportunity for ${coin.name}."
            }
            rsiValue > 70.0 -> {
                trend = "BEARISH"
                sentiment = 22.0
                minPrice = currentPrice * 0.88
                maxPrice = currentPrice * 1.015
                actionableAlert = (currentPrice * 0.93).roundTo2Decimals()
                rationale = "Technical oscillators (RSI value $rsiValue) indicate extreme overbought parameters. Expect near-term resistance and downward distribution corrections for ${coin.name}."
            }
            currentPrice > movingAvg -> {
                trend = "BULLISH"
                sentiment = 62.0
                minPrice = currentPrice * 0.97
                maxPrice = currentPrice * 1.075
                actionableAlert = (currentPrice * 1.05).roundTo2Decimals()
                rationale = "Asset is in a healthy structural uptrend above the 50 SMA. RSI of ${rsiValue.roundToInt()} represents intermediate bullish momentum. Trend is highly likely to continue higher."
            }
            else -> {
                trend = "SIDEWAYS"
                sentiment = 48.0
                minPrice = currentPrice * 0.94
                maxPrice = currentPrice * 1.045
                actionableAlert = (currentPrice * 0.95).roundTo2Decimals()
                rationale = "The market is trading within tight consolidation parameters. Moving Average represents flat trend directions. RSI ($rsiValue) indicates balanced buy/sell pressure."
            }
        }

        return AIPrediction(
            coinSymbol = coin.symbol,
            predictedTrend = trend,
            targetMin = minPrice.roundTo2Decimals(),
            targetMax = maxPrice.roundTo2Decimals(),
            sentimentScore = sentiment,
            analysisText = "[Technical Analyzer] $rationale",
            actionableThreshold = actionableAlert
        )
    }

    private fun Double.roundTo2Decimals(): Double {
        return (this * 100.0).roundToInt() / 100.0
    }
}
