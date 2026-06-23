package com.example.data.model

data class LiveCoin(
    val symbol: String,
    val name: String,
    val price: Double,
    val percentChange24h: Double,
    val historicalPrices: List<Double>, // 12 elements for a Sparkline
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val marketCap: Double,
    val rsi: Double, // 14-day RSI (relative strength index)
    val movingAverage50: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)
