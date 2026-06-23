package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AIPrediction
import com.example.data.model.LiveCoin
import com.example.ui.viewmodel.VolatilityLevel
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SparklineChart(
    prices: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val strokeColor = if (isPositive) NeonGreen else NeonRed
    val gradientColor = if (isPositive) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        val priceRange = maxPrice - minPrice
        val adjustedRange = if (priceRange == 0.0) 1.0 else priceRange

        val points = prices.mapIndexed { index, price ->
            val x = index * (width / (prices.size - 1))
            val percentY = (price - minPrice) / adjustedRange
            val y = height - (percentY * height).toFloat()
            Offset(x, y)
        }

        // Draw background gradient fill
        val fillPath = Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw smooth main stroke
        val strokePath = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    // Bezier interpolation
                    val controlX = (p0.x + p1.x) / 2
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }
        }
        drawPath(
            path = strokePath,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun CoinListItem(
    coin: LiveCoin,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = coin.percentChange24h >= 0
    val trendColor = if (isPositive) NeonGreen else NeonRed
    val changeSign = if (isPositive) "+" else ""

    val borderAndBgColor = animateColorAsState(
        targetValue = if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "bgColorTransition"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("coin_card_${coin.symbol}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyberGray else CyberSlate
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) NeonCyan else CardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asset Symbol & Badging
            Column(modifier = Modifier.weight(1.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = coin.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (coin.symbol == "BTC" || coin.symbol == "SOL") NeonGold.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (coin.symbol == "BTC" || coin.symbol == "SOL") "PRO" else "ALT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (coin.symbol == "BTC" || coin.symbol == "SOL") NeonGold else NeonCyan
                        )
                    }
                }
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Real-time Mini Sparkline Graph
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(36.dp)
                    .padding(horizontal = 8.dp)
            ) {
                SparklineChart(
                    prices = coin.historicalPrices,
                    isPositive = isPositive,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live Price details
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$${formatPrice(coin.price)}",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                        contentDescription = "Price trend symbol",
                        tint = trendColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$changeSign${coin.percentChange24h}%",
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AIPredictionPanel(
    coin: LiveCoin,
    prediction: AIPrediction?,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .testTag("ai_prediction_panel"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "AI symbol",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Trend Predictor Terminal",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Automated neural modeling on technical indicators",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Run Live analysis trigger
                Button(
                    onClick = onAnalyze,
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = CyberBlack
                    ),
                    modifier = Modifier.testTag("run_analysis_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            color = CyberBlack,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Modeling...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Analyze",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAnalyzing) {
                // Interactive loading terminal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = ">> INIT NEURAL_NET_FORECASTER_3.5",
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                    Text(
                        text = ">> EXTRACTING HISTORICAL SERIOUS: ${coin.historicalPrices.joinToString(", ")}",
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = ">> COMPUTING TECHNICAL VIBRATIONS (RSI: ${coin.rsi}, MA50: ${coin.movingAverage50})",
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = ">> CORRELATING VOLATILITY SPECTRUMS...",
                        fontFamily = FontFamily.Monospace,
                        color = NeonGold,
                        fontSize = 11.sp
                    )
                }
            } else if (prediction != null) {
                // Production level ML Analysis output
                val trend = prediction.predictedTrend.uppercase()
                val trendColor = when (trend) {
                    "BULLISH" -> NeonGreen
                    "BEARISH" -> NeonRed
                    else -> NeonGold
                }

                val trendIcon = when (trend) {
                    "BULLISH" -> Icons.Default.ArrowForward
                    "BEARISH" -> Icons.Default.ArrowBack
                    else -> Icons.Default.ArrowForward
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prediction trend badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(trendColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = "Trend direction",
                                tint = trendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PREDICTED: $trend",
                                color = trendColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Sentiment score gauge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Market Sentiment Index",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${prediction.sentimentScore.roundToInt()}/100",
                                fontWeight = FontWeight.Bold,
                                color = if (prediction.sentimentScore >= 50.0) NeonGreen else NeonRed,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rationale text in a beautiful retro border box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = prediction.analysisText,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quantitative target stats table
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Projected Range (24h)", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${formatPrice(prediction.targetMin)} - $${formatPrice(prediction.targetMax)}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Actionable Alert Pivot", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${formatPrice(prediction.actionableThreshold)}",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty analysis",
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active ML projections for ${coin.symbol}.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Tap 'Analyze' to invoke predictive modelling.",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onAnalyze() }
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketSandboxControls(
    isTickerRunning: Boolean,
    autoAIForecasting: Boolean,
    volatilityLevel: VolatilityLevel,
    onToggleTicker: () -> Unit,
    onToggleAutoAI: () -> Unit,
    onChangeVolatility: (VolatilityLevel) -> Unit,
    onTriggerSimEvent: (isCrash: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sandbox_controls"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardBorder),
        colors = CardDefaults.cardColors(containerColor = CyberSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Automation controller icon",
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Automated Engine Panel",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "Simulate real-time indicators & automate trend warnings",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto ticker & auto AI toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isTickerRunning,
                        onCheckedChange = { onToggleTicker() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonGreen
                        ),
                        modifier = Modifier
                            .graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                            .testTag("toggle_ticker_switch")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Live Market Updates", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (isTickerRunning) "Simulating ticker active" else "Ticker paused", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = autoAIForecasting,
                        onCheckedChange = { onToggleAutoAI() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonCyan
                        ),
                        modifier = Modifier
                            .graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                            .testTag("toggle_auto_ai_switch")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Auto AI Mode", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (autoAIForecasting) "AI modeling on swing" else "Manual analysis only", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volatility selector
            Text("Automated Volatility Setting", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VolatilityLevel.values().forEach { level ->
                    val isSelected = volatilityLevel == level
                    val bcolor = if (isSelected) NeonGreen else CardBorder
                    val textColor = if (isSelected) NeonGreen else TextSecondary
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .background(
                                color = if (isSelected) NeonGreen.copy(alpha = 0.08f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, bcolor, RoundedCornerShape(8.dp))
                            .clickable { onChangeVolatility(level) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Black swan market dynamic controls
            Text("Sandbox Black Swan Injectors", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onTriggerSimEvent(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGray, contentColor = NeonGreen),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .testTag("trigger_surge_button"),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Rally", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Rally", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onTriggerSimEvent(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGray, contentColor = NeonRed),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                        .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .testTag("trigger_crash_button"),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Crash", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Crash", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


private fun formatPrice(price: Double): String {
    return if (price >= 1.0) {
        String.format("%,.2f", price)
    } else {
        String.format("%.4f", price)
    }
}
