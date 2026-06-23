package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AlertLog
import com.example.data.database.MarketAlert
import com.example.data.model.LiveCoin
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsManagerView(
    coins: List<LiveCoin>,
    activeRules: List<MarketAlert>,
    alertLogs: List<AlertLog>,
    onAddRule: (String, String, Double) -> Unit,
    onDeleteRule: (Int) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSymbol by remember { mutableStateOf(coins.firstOrNull()?.symbol ?: "BTC") }
    var selectedCondition by remember { mutableStateOf("ABOVE") }
    var targetValueString by remember { mutableStateOf("") }
    var isCreatingRule by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("add_rule_card"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CardBorder),
            colors = CardDefaults.cardColors(containerColor = CyberSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alert logo",
                            tint = NeonGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set Custom Alert Trigger",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    IconButton(
                        onClick = { isCreatingRule = !isCreatingRule },
                        modifier = Modifier.testTag("toggle_add_rule_form")
                    ) {
                        Icon(
                            imageVector = if (isCreatingRule) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Expand",
                            tint = TextSecondary
                        )
                    }
                }

                if (isCreatingRule) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Coin Selector Row
                    Text("Select Crypto Token", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        coins.forEach { coin ->
                            val isSelected = selectedSymbol == coin.symbol
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .background(
                                        color = if (isSelected) NeonGold.copy(alpha = 0.12f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonGold else CardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedSymbol = coin.symbol }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = coin.symbol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonGold else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Type Selector Row
                    Text("Select Conditional Expression", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val conditionOptions = listOf(
                            "ABOVE" to "Price Above",
                            "BELOW" to "Price Below",
                            "FLUCTUATION_PCT" to "Aesthetic %"
                        )
                        conditionOptions.forEach { (type, label) ->
                            val isSelected = selectedCondition == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .background(
                                        color = if (isSelected) NeonGold.copy(alpha = 0.12f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonGold else CardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCondition = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonGold else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Value and Submit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetValueString,
                            onValueChange = { targetValueString = it },
                            label = { Text("Target Threshold (e.g. 68000 or 5.5%)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonGold,
                                unfocusedBorderColor = CardBorder,
                                focusedLabelColor = NeonGold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("alert_threshold_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                val value = targetValueString.toDoubleOrNull()
                                if (value != null && value > 0) {
                                    onAddRule(selectedSymbol, selectedCondition, value)
                                    targetValueString = ""
                                    isCreatingRule = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGold,
                                contentColor = CyberBlack
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("create_alert_rule_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Deploy Rule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Alert Limits List
        Text(
            text = "Active Rules Limits (${activeRules.size})",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (activeRules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active user alert configurations set. Tap Expand above to register alerts.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(activeRules) { rule ->
                    val signStr = when (rule.conditionType) {
                        "ABOVE" -> "Price >= $"
                        "BELOW" -> "Price <= $"
                        else -> "Daily Fluctuation >= %"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSlate),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Active Indicator",
                                    tint = if (rule.isActive) NeonGold else TextSecondary,
                                    modifier = Modifier.size(8.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${rule.coinSymbol}: $signStr${rule.targetValue}",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = { onDeleteRule(rule.id) },
                                modifier = Modifier.size(32.dp).testTag("delete_alert_rule_${rule.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = NeonRed.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // History Log Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Automated Real-Time alerts log",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (alertLogs.isNotEmpty()) {
                Text(
                    text = "Clear Logs",
                    color = NeonRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearLogs() }
                        .testTag("clear_logs_button")
                )
            }
        }

        if (alertLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Silent logs",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real-time alerts logs are empty.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Trigger random fluctuations in sandbox to prompt dynamic triggers.",
                        color = NeonGold,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alertLogs) { log ->
                    val color = when (log.alertType) {
                        "SYSTEM_CRASH" -> NeonRed
                        "SYSTEM_SPIKE" -> NeonCyan
                        else -> NeonGold
                    }
                    val icon = when (log.alertType) {
                        "SYSTEM_CRASH" -> Icons.Default.Warning
                        "SYSTEM_SPIKE" -> Icons.Default.Star
                        else -> Icons.Default.CheckCircle
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberBlack),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Alert classification type icon",
                                tint = color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.title,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = formatTime(log.timestamp),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.message,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(time: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(time))
}
