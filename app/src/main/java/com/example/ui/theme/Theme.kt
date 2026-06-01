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
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedIceBlue,
    onPrimary = SophisticatedDarkBlue,
    primaryContainer = SophisticatedBlueActive,
    onPrimaryContainer = Color.White,
    secondary = SophisticatedAccentBlue,
    onSecondary = SophisticatedDarkBlue,
    tertiary = SophisticatedRedAccent,
    background = SophisticatedBg,
    onBackground = SophisticatedText,
    surface = SophisticatedSurface,
    onSurface = SophisticatedText,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedSubtext,
    outline = SophisticatedBorder,
  )

private val LightColorScheme = DarkColorScheme // Always premium dark mode for Sophisticated Dark theme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default
  dynamicColor: Boolean = false, // Disable default system tinting
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
