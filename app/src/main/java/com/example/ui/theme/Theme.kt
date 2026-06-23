package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  lightColorScheme(
    primary = NeonCyan,
    onPrimary = CyberSlate,
    secondary = NeonGreen,
    onSecondary = CyberSlate,
    tertiary = NeonGold,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberSlate,
    onSurface = TextPrimary,
    outline = CardBorder,
    error = NeonRed
  )

private val LightColorScheme = DarkColorScheme // Standardize Bento Grid visual theme across levels

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Default to gorgeous light bento theme
  dynamicColor: Boolean = false, // Force bespoke colors for consistent branding
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
