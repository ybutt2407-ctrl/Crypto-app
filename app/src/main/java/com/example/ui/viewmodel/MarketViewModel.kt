package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AIPrediction
import com.example.data.database.AlertLog
import com.example.data.database.MarketAlert
import com.example.data.model.LiveCoin
import com.example.data.repository.CryptoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

enum class VolatilityLevel {
    LOW, MEDIUM, HIGH
}

class MarketViewModel(private val repository: CryptoRepository) : ViewModel() {
    private val TAG = "MarketViewModel"

    // Exposed Live UI Alerts Flow
    private val _realtimeAlertEvent = MutableSharedFlow<AlertLog>()
    val realtimeAlertEvent: SharedFlow<AlertLog> = _realtimeAlertEvent.asSharedFlow()

    // Database Flows
    val alertRules: StateFlow<List<MarketAlert>> = repository.allAlertRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertLogs: StateFlow<List<AlertLog>> = repository.allAlertLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiPredictions: StateFlow<List<AIPrediction>> = repository.allAIPredictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Configuration States
    private val _isTickerRunning = mutableStateOf(true)
    val isTickerRunning: State<Boolean> = _isTickerRunning

    private val _volatilityLevel = mutableStateOf(VolatilityLevel.MEDIUM)
    val volatilityLevel: State<VolatilityLevel> = _volatilityLevel

    private val _autoForecastMode = mutableStateOf(false)
    val autoForecastMode: State<Boolean> = _autoForecastMode

    private val _selectedCoinForAnalysis = mutableStateOf<LiveCoin?>(null)
    val selectedCoinForAnalysis: State<LiveCoin?> = _selectedCoinForAnalysis

    private val _isAnalyzing = mutableStateOf(false)
    val isAnalyzing: State<Boolean> = _isAnalyzing

    private val _latestSelectedCoinPrediction = mutableStateOf<AIPrediction?>(null)
    val latestSelectedCoinPrediction: State<AIPrediction?> = _latestSelectedCoinPrediction

    // In-memory list representing live crypto markets
    private val _coinsList = mutableStateOf<List<LiveCoin>>(emptyList())
    val coinsList: State<List<LiveCoin>> = _coinsList

    private var tickerJob: Job? = null

    init {
        initializeMockCoins()
        startMarketTicker()
    }

    private fun initializeMockCoins() {
        _coinsList.value = listOf(
            LiveCoin("BTC", "Bitcoin", 64850.0, 1.45, generateHistoricalPrices(64850.0, 12), 65200.0, 63900.0, 24.5e9, 1.28e12, 54.2, 63100.0),
            LiveCoin("ETH", "Ethereum", 3520.0, -0.85, generateHistoricalPrices(3520.0, 12), 3610.0, 3480.0, 13.8e9, 422.5e9, 44.5, 3380.0),
            LiveCoin("SOL", "Solana", 148.5, 5.12, generateHistoricalPrices(148.5, 12), 151.0, 140.5, 3.4e9, 68.2e9, 68.1, 135.0),
            LiveCoin("ADA", "Cardano", 0.46, -2.10, generateHistoricalPrices(0.46, 12), 0.48, 0.45, 320.0e6, 16.4e9, 36.4, 0.49),
            LiveCoin("DOT", "Polkadot", 6.25, 0.35, generateHistoricalPrices(6.25, 12), 6.35, 6.18, 150.0e6, 8.8e9, 48.0, 6.45),
            LiveCoin("DOGE", "Dogecoin", 0.125, 4.30, generateHistoricalPrices(0.125, 12), 0.132, 0.119, 850.0e6, 18.2e9, 59.5, 0.118)
        )
        // Set first item initially selected
        _selectedCoinForAnalysis.value = _coinsList.value.first()
        viewModelScope.launch {
            loadLatestPredictionForSelected()
        }
    }

    private fun generateHistoricalPrices(base: Double, count: Int): List<Double> {
        val list = mutableListOf<Double>()
        var current = base * 0.95
        for (i in 0 until count) {
            current += (Random.nextDouble() - 0.48) * (base * 0.01)
            list.add(current.roundTo2Decimals())
        }
        return list
    }

    fun toggleTicker() {
        _isTickerRunning.value = !_isTickerRunning.value
        if (_isTickerRunning.value) {
            startMarketTicker()
        } else {
            tickerJob?.cancel()
        }
    }

    fun setVolatility(level: VolatilityLevel) {
        _volatilityLevel.value = level
    }

    fun toggleAutoForecastMode() {
        _autoForecastMode.value = !_autoForecastMode.value
    }

    fun selectCoin(coin: LiveCoin) {
        _selectedCoinForAnalysis.value = coin
        viewModelScope.launch {
            loadLatestPredictionForSelected()
        }
    }

    private suspend fun loadLatestPredictionForSelected() {
        val coin = _selectedCoinForAnalysis.value ?: return
        _latestSelectedCoinPrediction.value = repository.getLatestPrediction(coin.symbol)
    }

    // continuous update loop
    private fun startMarketTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(3000) // Update metrics every 3 seconds
                if (_isTickerRunning.value) {
                    fluctuateCryptoPrices()
                }
            }
        }
    }

    private suspend fun fluctuateCryptoPrices() {
        val multiplier = when (_volatilityLevel.value) {
            VolatilityLevel.LOW -> 0.001  // Max 0.1% change
            VolatilityLevel.MEDIUM -> 0.005 // Max 0.5% change
            VolatilityLevel.HIGH -> 0.02   // Max 2.0% change
        }

        val updatedList = _coinsList.value.map { coin ->
            val changePercent = (Random.nextDouble() - 0.5) * 2.0 * multiplier
            val delta = coin.price * changePercent
            val newPrice = (coin.price + delta).coerceAtLeast(0.01).roundTo2Decimals()
            
            // Recompute 24h metrics
            val newHigh = if (newPrice > coin.high24h) newPrice else coin.high24h
            val newLow = if (newPrice < coin.low24h) newPrice else coin.low24h
            val rawPct = coin.percentChange24h + (changePercent * 100.0)
            val newPct24h = rawPct.roundTo2Decimals()

            // Update sparkline history
            val sparkline = coin.historicalPrices.toMutableList()
            sparkline.add(newPrice)
            if (sparkline.size > 15) {
                sparkline.removeAt(0)
            }

            // Adjust indicators dynamically
            val rsiDelta = (changePercent * 350.0)
            val newRsi = (coin.rsi + rsiDelta).coerceIn(10.0, 95.0).roundTo2Decimals()
            val newMA50 = (coin.movingAverage50 * 0.999 + newPrice * 0.001).roundTo2Decimals()

            val updatedCoin = coin.copy(
                price = newPrice,
                percentChange24h = newPct24h,
                historicalPrices = sparkline,
                high24h = newHigh,
                low24h = newLow,
                rsi = newRsi,
                movingAverage50 = newMA50,
                lastUpdated = System.currentTimeMillis()
            )

            // Automate: Check and trigger rule alerts
            evaluateAutomatedAlerts(updatedCoin)

            // Automate: Check if extreme fluctuation triggers automated AI forecasting
            if (_autoForecastMode.value && (changePercent * 100.0).coerceAtLeast(0.0) > 1.2 && !_isAnalyzing.value) {
                triggerAutoAIForecasting(updatedCoin)
            }

            updatedCoin
        }

        _coinsList.value = updatedList
        // Keep selected coin synchronized
        val selected = _selectedCoinForAnalysis.value
        if (selected != null) {
            _selectedCoinForAnalysis.value = updatedList.firstOrNull { it.symbol == selected.symbol }
        }
    }

    private suspend fun evaluateAutomatedAlerts(coin: LiveCoin) {
        val rules = repository.getActiveAlertsForCoin(coin.symbol)
        rules.forEach { rule ->
            var isTriggered = false
            var message = ""
            var title = ""

            when (rule.conditionType) {
                "ABOVE" -> {
                    if (coin.price >= rule.targetValue) {
                        isTriggered = true
                        title = "🚀 Price Limit Breached"
                        message = "${coin.name} (${coin.symbol}) has surcharged above your target threshold of $${rule.targetValue}. Live price: $${coin.price}."
                    }
                }
                "BELOW" -> {
                    if (coin.price <= rule.targetValue) {
                        isTriggered = true
                        title = "⚠️ Price Support Breached"
                        message = "${coin.name} (${coin.symbol}) has dropped below your active target threshold of $${rule.targetValue}. Live price: $${coin.price}."
                    }
                }
                "FLUCTUATION_PCT" -> {
                    if (kotlin.math.abs(coin.percentChange24h) >= rule.targetValue) {
                        isTriggered = true
                        title = "⚡ Alert: High Dynamic Swing"
                        message = "${coin.name} (${coin.symbol}) has undergone extreme dynamic fluctuations over 24h reaching ${coin.percentChange24h}%. Threshold: ${rule.targetValue}%."
                    }
                }
            }

            if (isTriggered) {
                // Set rule inactive to avoid repetitive rapid trigger
                repository.updateAlertRule(rule.copy(isActive = false))

                // Log the alert
                val alertLog = AlertLog(
                    coinSymbol = coin.symbol,
                    title = title,
                    message = message,
                    alertType = "CUSTOM_ALERT",
                    priceAtTrigger = coin.price
                )
                repository.insertAlertLog(alertLog)

                // Dispatch to Live UI listener
                _realtimeAlertEvent.emit(alertLog)
            }
        }
    }

    private fun triggerAutoAIForecasting(coin: LiveCoin) {
        viewModelScope.launch {
            Log.d(TAG, "Automated alert: High volatility detected on ${coin.symbol}. Injecting automated AI prediction...")
            
            val alertInfo = AlertLog(
                coinSymbol = coin.symbol,
                title = "🤖 Automated AI Analysis Triggered",
                message = "The forecasting engine detected high-volatility price swings of ${coin.percentChange24h}% on ${coin.symbol}. Automated AI ML re-prediction dispatched.",
                alertType = "SYSTEM_SPIKE",
                priceAtTrigger = coin.price
            )
            repository.insertAlertLog(alertInfo)
            _realtimeAlertEvent.emit(alertInfo)

            _isAnalyzing.value = true
            try {
                val prediction = repository.generateMarketPrediction(coin)
                if (coin.symbol == _selectedCoinForAnalysis.value?.symbol) {
                    _latestSelectedCoinPrediction.value = prediction
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-AI modeling query failed: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Users can trigger simulated sandboxed Market Events (Surge / Crash) for quick and beautiful testing
     */
    fun triggerSimulatedEvent(isCrash: Boolean) {
        viewModelScope.launch {
            val direction = if (isCrash) "Flash Crash" else "Bullish Breakthrough"
            val multiplier = if (isCrash) 0.88 else 1.12 // -12% or +12%
            
            // Format Event alert
            val systemLog = AlertLog(
                coinSymbol = "GLOBAL",
                title = "🔥 Simulated Market Event: $direction",
                message = "A simulated black-swan sentiment event has been triggered in the sandbox. Global automated limits compiling...",
                alertType = if (isCrash) "SYSTEM_CRASH" else "SYSTEM_SPIKE",
                priceAtTrigger = 0.0
            )
            repository.insertAlertLog(systemLog)
            _realtimeAlertEvent.emit(systemLog)

            _coinsList.value = _coinsList.value.map { coin ->
                val newPrice = (coin.price * multiplier).roundTo2Decimals()
                val deltaPct = if (isCrash) -12.0 else 12.0
                val newPct = (coin.percentChange24h + deltaPct).roundTo2Decimals()
                
                val sparkline = coin.historicalPrices.toMutableList()
                sparkline.add(newPrice)
                if (sparkline.size > 15) sparkline.removeAt(0)

                val newHigh = if (newPrice > coin.high24h) newPrice else coin.high24h
                val newLow = if (newPrice < coin.low24h) newPrice else coin.low24h
                
                val adjustedRsi = if (isCrash) 18.0 else 88.0

                val updatedCoin = coin.copy(
                    price = newPrice,
                    percentChange24h = newPct,
                    historicalPrices = sparkline,
                    high24h = newHigh,
                    low24h = newLow,
                    rsi = adjustedRsi,
                    lastUpdated = System.currentTimeMillis()
                )

                // Trigger alerts on bounds
                evaluateAutomatedAlerts(updatedCoin)

                updatedCoin
            }

            // Sync selections
            val selected = _selectedCoinForAnalysis.value
            if (selected != null) {
                _selectedCoinForAnalysis.value = _coinsList.value.firstOrNull { it.symbol == selected.symbol }
            }
        }
    }

    /**
     * User clicks "Analyze AI Predictions" (starts Gemini chain)
     */
    fun runAITrendAnalysis() {
        val currentCoin = _selectedCoinForAnalysis.value ?: return
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val prediction = repository.generateMarketPrediction(currentCoin)
                _latestSelectedCoinPrediction.value = prediction
            } catch (e: Exception) {
                Log.e(TAG, "Manual AI trend model query failed: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Add customizable price limit alerts
     */
    fun createAlertRule(symbol: String, type: String, value: Double) {
        viewModelScope.launch {
            val alert = MarketAlert(
                coinSymbol = symbol,
                conditionType = type,
                targetValue = value,
                isActive = true
            )
            repository.insertAlertRule(alert)
        }
    }

    fun removeAlertRule(id: Int) {
        viewModelScope.launch {
            repository.deleteAlertRule(id)
        }
    }

    fun clearAllLoggedAlerts() {
        viewModelScope.launch {
            repository.clearAlertLogs()
        }
    }

    private fun Double.roundTo2Decimals(): Double {
        return (this * 100.0).roundToInt() / 100.0
    }
}

class MarketViewModelFactory(private val repository: CryptoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class Exception: " + modelClass.name)
    }
}
