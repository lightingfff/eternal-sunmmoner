package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EternalSummonerColorScheme = darkColorScheme(
  primary = androidx.compose.ui.graphics.Color(0xffc6b27f),
  onPrimary = ObsidianVoid,
  primaryContainer = ArcaneVioletDeep,
  onPrimaryContainer = ParchmentLight,
  secondary = androidx.compose.ui.graphics.Color(0xff9286ad),
  onSecondary = ObsidianVoid,
  secondaryContainer = ElevatedCrypt,
  onSecondaryContainer = ParchmentLight,
  tertiary = androidx.compose.ui.graphics.Color(0xff83aeb7),
  onTertiary = ObsidianVoid,
  tertiaryContainer = AstralSlate,
  onTertiaryContainer = PortalCyanGlow,
  background = ObsidianVoid,
  onBackground = ParchmentLight,
  surface = com.example.ui.components.FantasySkin.Panel,
  onSurface = ParchmentLight,
  surfaceVariant = AstralSlate,
  onSurfaceVariant = RuneSilver,
  outline = BorderRune,
  outlineVariant = ElevatedCrypt,
  error = CrimsonBlood,
  onError = ParchmentLight,
  errorContainer = CrimsonDark,
  onErrorContainer = ParchmentLight,
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
