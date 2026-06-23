package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AlertLog
import com.example.data.model.LiveCoin
import com.example.ui.viewmodel.VolatilityLevel
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardMainView(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val coins by viewModel.coinsList
    val selectedCoin by viewModel.selectedCoinForAnalysis
    val activeRules by viewModel.alertRules.collectAsState()
    val alertLogs by viewModel.alertLogs.collectAsState()
    val predictions by viewModel.aiPredictions.collectAsState()
    val isTickerRunning by viewModel.isTickerRunning
    val isAnalyzing by viewModel.isAnalyzing
    val latestPrediction by viewModel.latestSelectedCoinPrediction
    val autoForecastMode by viewModel.autoForecastMode
    val volatilityLevel by viewModel.volatilityLevel

    var activeTab by remember { mutableStateOf(0) } // 0 = Market & AI, 1 = Limits & Automation

    // Floating Alert banner state
    var currentIncomingAlertLog by remember { mutableStateOf<AlertLog?>(null) }
    var bannerVisible by remember { mutableStateOf(false) }

    // Listen to real-time incoming events
    LaunchedEffect(key1 = Unit) {
        viewModel.realtimeAlertEvent.collect { log ->
            currentIncomingAlertLog = log
            bannerVisible = true
            delay(5000) // Auto hide after 5 seconds
            bannerVisible = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CryptoAlerts AI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isTickerRunning) NeonGreen.copy(alpha = 0.15f) else TextSecondary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Active Indicator dot",
                                        tint = if (isTickerRunning) NeonGreen else TextSecondary,
                                        modifier = Modifier.size(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isTickerRunning) "AUTOMATED LIVE" else "PAUSED",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTickerRunning) NeonGreen else TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Real-time AI Trend Modeling & Limit Alerts",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Quick Toggle Live Ticker Taps
                    IconButton(
                        onClick = { viewModel.toggleTicker() },
                        modifier = Modifier.testTag("toggle_ticker_button")
                    ) {
                        Icon(
                            imageVector = if (isTickerRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Play/pause",
                            tint = if (isTickerRunning) NeonRed else NeonGreen
                        )
                    }
                }

                // Horizontal Navigation Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = CyberBlack,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = NeonCyan
                        )
                    },
                    divider = { HorizontalDivider(color = CardBorder) }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.testTag("tab_market_ai"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Market Board & AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.testTag("tab_limits_automation"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Limits & Automation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Main views layout
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    // Market Board & AI Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // AI Prediction panel
                        item {
                            selectedCoin?.let { coin ->
                                AIPredictionPanel(
                                    coin = coin,
                                    prediction = latestPrediction,
                                    isAnalyzing = isAnalyzing,
                                    onAnalyze = { viewModel.runAITrendAnalysis() }
                                )
                            }
                        }

                        // Real-time market state board header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Automated Live Markets Feed",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "RSI / SMA (50)",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Crypto board assets list
                        items(coins) { coin ->
                            CoinListItem(
                                coin = coin,
                                isSelected = selectedCoin?.symbol == coin.symbol,
                                onSelect = { viewModel.selectCoin(coin) }
                            )
                        }

                        // Sandbox simulation pane
                        item {
                            MarketSandboxControls(
                                isTickerRunning = isTickerRunning,
                                autoAIForecasting = autoForecastMode,
                                volatilityLevel = volatilityLevel,
                                onToggleTicker = { viewModel.toggleTicker() },
                                onToggleAutoAI = { viewModel.toggleAutoForecastMode() },
                                onChangeVolatility = { viewModel.setVolatility(it) },
                                onTriggerSimEvent = { viewModel.triggerSimulatedEvent(it) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                } else {
                    // Limits & Automation Rules Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AlertsManagerView(
                                coins = coins,
                                activeRules = activeRules,
                                alertLogs = alertLogs,
                                onAddRule = { symbol, selection, value ->
                                    viewModel.createAlertRule(symbol, selection, value)
                                },
                                onDeleteRule = { id -> viewModel.removeAlertRule(id) },
                                onClearLogs = { viewModel.clearAllLoggedAlerts() }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            // Real-time Banner Toast Overlay notification
            AnimatedVisibility(
                visible = bannerVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it }
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it }
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .testTag("floating_alert_banner")
            ) {
                currentIncomingAlertLog?.let { alert ->
                    val borderHighlight = when (alert.alertType) {
                        "SYSTEM_CRASH" -> NeonRed
                        "SYSTEM_SPIKE" -> NeonCyan
                        else -> NeonGold
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bannerVisible = false }
                            .border(1.dp, borderHighlight, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberSlate),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (alert.alertType == "SYSTEM_CRASH") Icons.Default.Warning else Icons.Default.Notifications,
                                contentDescription = "Alert notifications symbol",
                                tint = borderHighlight,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    color = borderHighlight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = alert.message,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            IconButton(onClick = { bannerVisible = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close alert dismiss button",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
