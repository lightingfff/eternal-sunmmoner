package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EternalSummonerColorScheme = darkColorScheme(
  primary = ESDesignTokens.gold,
  onPrimary = ObsidianVoid,
  primaryContainer = ArcaneVioletDeep,
  onPrimaryContainer = ESDesignTokens.textPrimary,
  secondary = ESDesignTokens.textSecondary,
  onSecondary = ObsidianVoid,
  secondaryContainer = ESDesignTokens.panelRaised,
  onSecondaryContainer = ESDesignTokens.textPrimary,
  tertiary = ESDesignTokens.cyan,
  onTertiary = ObsidianVoid,
  tertiaryContainer = AstralSlate,
  onTertiaryContainer = ESDesignTokens.cyan,
  background = ESDesignTokens.backdrop,
  onBackground = ESDesignTokens.textPrimary,
  surface = ESDesignTokens.panel,
  onSurface = ESDesignTokens.textPrimary,
  surfaceVariant = ESDesignTokens.panelRaised,
  onSurfaceVariant = ESDesignTokens.textSecondary,
  outline = ESDesignTokens.border,
  outlineVariant = ESDesignTokens.hairline,
  error = ESDesignTokens.danger,
  onError = ObsidianVoid,
  errorContainer = CrimsonDark,
  onErrorContainer = ESDesignTokens.textPrimary,
)

private val EternalSummonerShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun EternalSummonerTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = EternalSummonerColorScheme,
    typography = Typography,
    shapes = EternalSummonerShapes,
    content = content,
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  EternalSummonerTheme(content = content)
}
