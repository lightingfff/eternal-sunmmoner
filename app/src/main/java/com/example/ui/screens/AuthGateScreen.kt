package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptSilentGoogleSignIn
import com.example.data.firebase.performInteractiveGoogleSignIn
import com.example.data.model.HeroClassType
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.performance.SceneFrameLoop
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SummonerGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuthGateScreen(
  hasExistingGuestSave: Boolean = false,
  savedGuestSummary: String? = null,
  allowSilentAutoSignIn: Boolean = true,
  onContinueAsGuest: () -> Unit = {},
  onAuthenticated: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var animTimeSec by remember { mutableFloatStateOf(1.25f) }

  SceneFrameLoop(active = true, targetFps = 60) { dt, _ ->
    animTimeSec += dt.coerceAtMost(0.1f)
  }

  LaunchedEffect(allowSilentAutoSignIn, hasExistingGuestSave) {
    if (allowSilentAutoSignIn && !hasExistingGuestSave) {
      attemptSilentGoogleSignIn(
        context = context,
        credentialManager = credentialManager,
        scope = scope,
        onAuthSuccess = onAuthenticated,
        onUnauthenticated = { /* Stay on interactive Dark Fantasy gate */ },
      )
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF091526)),
  ) {
    val isLandscape = maxWidth > maxHeight

    // 1. Ken-Burns Dynamic Panning & Breathing Sanctuary Gate Artwork
    val zoomPulse = 1.07f + 0.035f * sin(animTimeSec * 0.42f)
    val panX = 14f * cos(animTimeSec * 0.28f)
    val panY = 9f * sin(animTimeSec * 0.35f)

    Image(
      painter = painterResource(id = R.drawable.img_sanctuary_gate),
      contentDescription = "Cổng Thánh Địa Eternal Summoner",
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          scaleX = zoomPulse
          scaleY = zoomPulse
          translationX = panX
          translationY = panY
        },
      contentScale = ContentScale.Crop,
      alpha = 0.94f,
    )

    // 2. Live 60fps 2.5D Cinematic Opening Scene Canvas:
    //    - Volumetric Sun & Portal God-Rays
    //    - Rotating 3D Foreshortened Astral Tower Seal in the Monolith Cleft
    //    - Crackling Dimensional Lightning Bolts
    //    - Drifting Nebula Mist & 48 Rising Golden/Cyan Rune Embers
    //    - Foreground Obsidian Overlook with Animated HD-128 Hero Squad Gazing at the Tower Gate
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCinematicOpeningScene(
        timeSec = animTimeSec,
        isLandscape = isLandscape,
      )
    }

    // Accessible Settings Icon Button in Top-Right Safe Area
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xC20E1E36))
          .border(1.2.dp, PortalCyan.copy(alpha = 0.80f), RoundedCornerShape(12.dp))
          .testTag("login_settings_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Cài đặt & Tài khoản",
          tint = SummonerGold,
        )
      }
    }

    if (isLandscape) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Column(
          modifier = Modifier.weight(1.05f),
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xB80B1B32),
            modifier = Modifier.border(1.dp, PortalCyan.copy(alpha = 0.78f), RoundedCornerShape(8.dp)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(16.dp))
              Text(
                text = "[ SYSTEM AWAKENING • DARK FANTASY 2.5D ]",
                style = MaterialTheme.typography.labelSmall,
                color = PortalCyan,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "ETERNAL SUMMONER",
            style = MaterialTheme.typography.displaySmall,
            color = SummonerGold,
            fontWeight = FontWeight.ExtraBold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(id = R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentLight,
          )
        }

        AuthSignInCard(
          isLoading = isLoading,
          errorMessage = errorMessage,
          hasExistingGuestSave = hasExistingGuestSave,
          savedGuestSummary = savedGuestSummary,
          animTimeSec = animTimeSec,
          onContinueAsGuest = onContinueAsGuest,
          onGoogleSignInClick = {
            isLoading = true
            errorMessage = null
            performInteractiveGoogleSignIn(
              context = context,
              credentialManager = credentialManager,
              scope = scope,
              onAuthSuccess = {
                isLoading = false
                onAuthenticated()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              onAuthCancelled = { isLoading = false },
            )
          },
          modifier = Modifier.weight(1f).widthIn(max = 440.dp),
        )
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(start = 18.dp, end = 18.dp, top = 62.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
      ) {
        AuthSignInCard(
          isLoading = isLoading,
          errorMessage = errorMessage,
          hasExistingGuestSave = hasExistingGuestSave,
          savedGuestSummary = savedGuestSummary,
          animTimeSec = animTimeSec,
          showFullCrestHeader = true,
          onContinueAsGuest = onContinueAsGuest,
          onGoogleSignInClick = {
            isLoading = true
            errorMessage = null
            performInteractiveGoogleSignIn(
              context = context,
              credentialManager = credentialManager,
              scope = scope,
              onAuthSuccess = {
                isLoading = false
                onAuthenticated()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              onAuthCancelled = { isLoading = false },
            )
          },
          modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        )
      }
    }
  }
}

private fun DrawScope.drawCinematicOpeningScene(
  timeSec: Float,
  isLandscape: Boolean,
) {
  val w = size.width
  val h = size.height

  // Focal point of the Northern Obsidian Tower Gate Rift
  val gateCenterX = if (isLandscape) w * 0.34f else w * 0.50f
  val gateCenterY = if (isLandscape) h * 0.34f else h * 0.25f

  // 1. Atmospheric Depth Gradient (leaves the Tower Gate bright while adding cinematic sky & foreground contrast)
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0x36081528),
        Color(0x180C1D36),
        Color(0x420A182E),
        Color(0x88071120),
      ),
    ),
  )

  // 2. Sweeping Volumetric God-Rays & Astral Portal Shafts from the Gate Cleft
  val rayCount = 7
  for (i in 0 until rayCount) {
    val phase = timeSec * 0.55f + i * 0.92f
    val sway = sin(phase) * 0.08f
    val baseAngle = -0.48f + (i.toFloat() / (rayCount - 1)) * 0.96f + sway
    val spread = 0.055f + 0.018f * cos(phase * 1.3f)
    val length = maxOf(w, h) * 1.15f
    val endX1 = gateCenterX + sin(baseAngle - spread) * length
    val endY1 = gateCenterY + cos(baseAngle - spread) * length
    val endX2 = gateCenterX + sin(baseAngle + spread) * length
    val endY2 = gateCenterY + cos(baseAngle + spread) * length

    val rayAlpha = (0.11f + 0.06f * sin(timeSec * 1.2f + i * 1.1f)).coerceIn(0.04f, 0.20f)
    val rayColor = if (i % 2 == 0) SummonerGold else PortalCyan
    val path = Path().apply {
      moveTo(gateCenterX - 14f, gateCenterY)
      lineTo(gateCenterX + 14f, gateCenterY)
      lineTo(endX2, endY2)
      lineTo(endX1, endY1)
      close()
    }
    drawPath(
      path = path,
      brush = Brush.verticalGradient(
        colors = listOf(
          rayColor.copy(alpha = rayAlpha * 1.45f),
          rayColor.copy(alpha = rayAlpha * 0.65f),
          Color.Transparent,
        ),
        startY = gateCenterY,
        endY = gateCenterY + length * 0.85f,
      ),
    )
  }

  // 3. Pulsing Dimensional Portal Vortex & Counter-Rotating Astral Seal Rings at Gate Cleft
  val portalPulse = 1f + 0.08f * sin(timeSec * 2.2f)
  val baseRadius = minOf(w, h) * (if (isLandscape) 0.24f else 0.22f) * portalPulse

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xAAFFF7D6),
        PortalCyan.copy(alpha = 0.48f),
        ArcaneViolet.copy(alpha = 0.24f),
        Color.Transparent,
      ),
      center = Offset(gateCenterX, gateCenterY),
      radius = baseRadius * 1.45f,
    ),
    radius = baseRadius * 1.45f,
    center = Offset(gateCenterX, gateCenterY),
  )

  // Outer & inner foreshortened 2.5D arcane rings
  val ringRx = baseRadius * 0.92f
  val ringRy = ringRx * 0.48f
  drawOval(
    color = SummonerGold.copy(alpha = 0.62f),
    topLeft = Offset(gateCenterX - ringRx, gateCenterY - ringRy),
    size = Size(ringRx * 2f, ringRy * 2f),
    style = Stroke(width = 2.2f),
  )
  val innerRx = ringRx * 0.68f
  val innerRy = ringRy * 0.68f
  drawOval(
    color = PortalCyan.copy(alpha = 0.72f),
    topLeft = Offset(gateCenterX - innerRx, gateCenterY - innerRy),
    size = Size(innerRx * 2f, innerRy * 2f),
    style = Stroke(width = 1.6f),
  )

  // 12 Orbiting Astral Glyphs around the Gate Vortex
  for (g in 0 until 12) {
    val angle = timeSec * 0.65f + (g * 2.0 * PI / 12.0).toFloat()
    val gx = gateCenterX + cos(angle) * ringRx
    val gy = gateCenterY + sin(angle) * ringRy
    val glyphPulse = 0.55f + 0.45f * sin(timeSec * 3.1f + g)
    drawCircle(
      color = (if (g % 2 == 0) SummonerGold else PortalCyan).copy(alpha = 0.78f * glyphPulse),
      radius = 4.2f,
      center = Offset(gx, gy),
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.85f * glyphPulse),
      radius = 1.8f,
      center = Offset(gx, gy),
    )
  }

  // 4. Crackling Dimensional Lightning Bolts across the Tower Gate Cleft
  for (boltIdx in 0 until 3) {
    val boltCycle = (timeSec * 1.8f + boltIdx * 1.37f) % 2.4f
    if (boltCycle < 0.85f) {
      val boltAlpha = (1f - boltCycle / 0.85f).coerceIn(0f, 1f)
      val startX = gateCenterX + (boltIdx - 1) * baseRadius * 0.34f
      val startY = gateCenterY - baseRadius * 0.55f
      var prevX = startX
      var prevY = startY
      val segments = 6
      for (seg in 1..segments) {
        val frac = seg.toFloat() / segments
        val jitter = sin(timeSec * 19f + boltIdx * 7f + seg * 3.1f) * 18f
        val nextX = startX + jitter + sin(boltIdx + seg.toFloat()) * 10f
        val nextY = startY + frac * baseRadius * 1.35f
        drawLine(
          color = PortalCyan.copy(alpha = boltAlpha * 0.65f),
          start = Offset(prevX, prevY),
          end = Offset(nextX, nextY),
          strokeWidth = 4.2f,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = Color.White.copy(alpha = boltAlpha * 0.92f),
          start = Offset(prevX, prevY),
          end = Offset(nextX, nextY),
          strokeWidth = 1.6f,
          cap = StrokeCap.Round,
        )
        prevX = nextX
        prevY = nextY
      }
    }
  }

  // 5. Foreground 2.5D Obsidian Cliff Overlook & Animated HD-128 Hero Party Silhouettes
  val cliffCenterX = if (isLandscape) w * 0.28f else w * 0.50f
  val cliffCenterY = if (isLandscape) h * 0.82f else h * 0.44f
  val cliffRx = if (isLandscape) w * 0.24f else w * 0.36f
  val cliffRy = cliffRx * 0.26f

  // Glowing 2.5D Summoning Terrace under the Heroes
  drawOval(
    brush = Brush.radialGradient(
      colors = listOf(
        PortalCyan.copy(alpha = 0.34f),
        SummonerGold.copy(alpha = 0.18f),
        Color(0xCC0D1B2E),
        Color.Transparent,
      ),
      center = Offset(cliffCenterX, cliffCenterY),
      radius = cliffRx * 1.15f,
    ),
    topLeft = Offset(cliffCenterX - cliffRx, cliffCenterY - cliffRy),
    size = Size(cliffRx * 2f, cliffRy * 2f),
  )
  drawOval(
    color = SummonerGold.copy(alpha = 0.58f),
    topLeft = Offset(cliffCenterX - cliffRx * 0.82f, cliffCenterY - cliffRy * 0.82f),
    size = Size(cliffRx * 1.64f, cliffRy * 1.64f),
    style = Stroke(width = 1.8f),
  )

  // Render 3 Animated HD-128 Heroes on the Overlook facing North toward the Colossal Tower Gate
  val spriteZoom = (minOf(w, h) / 540f).coerceIn(0.56f, 0.92f)
  val squadClasses = listOf(
    Triple(HeroClassType.KNIGHT, -78f, 8f),
    Triple(HeroClassType.MAGE, 0f, -6f),
    Triple(HeroClassType.ASSASSIN, 78f, 10f),
  )
  squadClasses.forEachIndexed { idx, (heroClass, offsetX, offsetY) ->
    val heroWorldX = 1000f + offsetX
    val heroWorldY = 1000f + offsetY
    val pulseAura = if (idx == 1) SummonerGold.copy(alpha = 0.72f) else PortalCyan.copy(alpha = 0.52f)
    drawCharacterSprite(
      worldX = heroWorldX,
      worldY = heroWorldY,
      camX = 1000f - (cliffCenterX - w / 2f) / spriteZoom,
      camY = 1000f - (cliffCenterY - h / 2f) / (spriteZoom * 0.72f),
      zoom = spriteZoom,
      isMaster = idx == 1,
      heroClass = heroClass,
      facingRight = idx <= 1,
      isMoving = false,
      attackAnimProgress = 0f,
      hurtFlash = 0f,
      hpFraction = 1f,
      shieldOrAuraColor = pulseAura,
      isInjuredResting = false,
      timeSec = timeSec + idx * 0.45f,
      facingNorth = true,
    )
  }

  // 6. 48 Rising Golden Embers, Astral Runes & Sakura Light Motes
  for (p in 0 until 48) {
    val seed = p * 37.13f
    val speed = 18f + (p % 7) * 6.5f
    val px = ((seed * 43.7f + sin(timeSec * 0.7f + p) * 26f) % w + w) % w
    val rawY = h - ((timeSec * speed + seed * 29.3f) % (h * 1.15f))
    val alpha = (0.25f + 0.55f * sin(timeSec * 2.1f + p)).coerceIn(0.10f, 0.85f)
    val particleColor = when (p % 3) {
      0 -> SummonerGold
      1 -> PortalCyan
      else -> Color(0xFFF472B6)
    }
    val radius = 1.8f + (p % 4) * 0.95f
    drawCircle(
      color = particleColor.copy(alpha = alpha * 0.32f),
      radius = radius * 2.8f,
      center = Offset(px, rawY),
    )
    drawCircle(
      color = particleColor.copy(alpha = alpha),
      radius = radius,
      center = Offset(px, rawY),
    )
  }
}

@Composable
private fun AuthSignInCard(
  isLoading: Boolean,
  errorMessage: String?,
  hasExistingGuestSave: Boolean,
  savedGuestSummary: String?,
  animTimeSec: Float,
  onContinueAsGuest: () -> Unit,
  onGoogleSignInClick: () -> Unit,
  modifier: Modifier = Modifier,
  showFullCrestHeader: Boolean = false,
) {
  Surface(
    modifier = modifier
      .drawWithContent {
        drawContent()
        // Holographic Manhwa System Window Corner Brackets & Scanline Shimmer
        val bracketLen = 22.dp.toPx()
        val strokeW = 2.4.dp.toPx()
        val pad = 4.dp.toPx()
        val w = size.width
        val h = size.height
        val glowCol = PortalCyan.copy(alpha = 0.88f)
        val goldCol = SummonerGold.copy(alpha = 0.92f)

        // Top-Left Bracket
        drawLine(glowCol, Offset(pad, pad), Offset(pad + bracketLen, pad), strokeW)
        drawLine(glowCol, Offset(pad, pad), Offset(pad, pad + bracketLen), strokeW)
        // Top-Right Bracket
        drawLine(goldCol, Offset(w - pad - bracketLen, pad), Offset(w - pad, pad), strokeW)
        drawLine(goldCol, Offset(w - pad, pad), Offset(w - pad, pad + bracketLen), strokeW)
        // Bottom-Left Bracket
        drawLine(goldCol, Offset(pad, h - pad - bracketLen), Offset(pad, h - pad), strokeW)
        drawLine(goldCol, Offset(pad, h - pad), Offset(pad + bracketLen, h - pad), strokeW)
        // Bottom-Right Bracket
        drawLine(glowCol, Offset(w - pad - bracketLen, h - pad), Offset(w - pad, h - pad), strokeW)
        drawLine(glowCol, Offset(w - pad, h - pad - bracketLen), Offset(w - pad, h - pad), strokeW)

        // Animated Horizontal Holographic Scanline
        val scanY = ((animTimeSec * 0.28f) % 1f) * h
        drawRoundRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              PortalCyan.copy(alpha = 0.10f),
              Color.Transparent,
            ),
            startY = (scanY - 24f).coerceAtLeast(0f),
            endY = (scanY + 24f).coerceAtMost(h),
          ),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
        )
      }
      .border(1.5.dp, PortalCyan.copy(alpha = 0.78f), RoundedCornerShape(22.dp)),
    shape = RoundedCornerShape(22.dp),
    color = Color(0xD40B1A30),
    tonalElevation = 12.dp,
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Manhwa System Awakening Header Strip
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x98102644),
        modifier = Modifier.border(1.dp, PortalCyan.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(14.dp))
          Text(
            text = "SYSTEM NOTIFICATION • KHỞI ĐỘNG HỆ THỐNG",
            style = MaterialTheme.typography.labelSmall,
            color = PortalCyan,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }

      if (showFullCrestHeader) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "ETERNAL SUMMONER",
          style = MaterialTheme.typography.headlineMedium,
          color = SummonerGold,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(id = R.string.auth_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
      } else {
        Spacer(modifier = Modifier.height(10.dp))
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xA4132846))
          .border(1.dp, PortalCyan.copy(alpha = 0.52f), RoundedCornerShape(12.dp))
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = if (hasExistingGuestSave) EmeraldVitality else SummonerGold,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = if (hasExistingGuestSave && savedGuestSummary != null) {
            "[HỒ SƠ ĐÃ LƯU] $savedGuestSummary • Sẵn sàng kết nối Cổng Tháp."
          } else {
            "[THỨC TỈNH] Bước vào Thánh Địa ngay với Hồ Sơ Khách hoặc đồng bộ Khế Ước Google Cloud."
          },
          style = MaterialTheme.typography.bodySmall,
          color = ParchmentLight,
        )
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = CrimsonBlood,
          textAlign = TextAlign.Center,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 1. ONE-TAP GUEST LOGIN (Primary Instant Entry — Zero Email/Password Required!)
      Button(
        onClick = onContinueAsGuest,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 50.dp)
          .testTag("continue_as_guest_button"),
        shape = RoundedCornerShape(12.dp),
        colors = FantasySkin.buttonColors(
          containerColor = SummonerGold,
          contentColor = ObsidianVoid,
        ),
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (hasExistingGuestSave) {
            "Continue as Guest • Tiếp Tục Hồ Sơ Khách"
          } else {
            "Continue as Guest • Chơi Ngay (Khách)"
          },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. GOOGLE CLOUD SIGN-IN
      OutlinedButton(
        onClick = onGoogleSignInClick,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 50.dp)
          .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = PortalCyan,
            strokeWidth = 2.4.dp,
          )
        } else {
          Icon(Icons.Default.CloudSync, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Sign in with Google • Đồng Bộ Đám Mây",
            style = MaterialTheme.typography.labelLarge,
            color = ParchmentLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
