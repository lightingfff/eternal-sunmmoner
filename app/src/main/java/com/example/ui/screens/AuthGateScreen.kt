package com.example.ui.screens

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
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SummonerGold

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
      .background(ObsidianVoid),
  ) {
    val isLandscape = maxWidth > maxHeight

    Image(
      painter = painterResource(id = R.drawable.img_sanctuary_gate),
      contentDescription = "Cổng Thánh Địa Eternal Summoner",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
      alpha = 0.60f,
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              ObsidianVoid.copy(alpha = 0.42f),
              ObsidianVoid.copy(alpha = 0.72f),
              ObsidianVoid.copy(alpha = 0.94f),
            )
          )
        ),
    )

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
          .background(SanctuaryStone.copy(alpha = 0.90f))
          .border(1.dp, SummonerGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(18.dp))
            Text("DARK FANTASY PIXEL TOWER RPG", style = MaterialTheme.typography.labelMedium, color = PortalCyan)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "ETERNAL SUMMONER",
            style = MaterialTheme.typography.displaySmall,
            color = SummonerGold,
          )
          Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun AuthSignInCard(
  isLoading: Boolean,
  errorMessage: String?,
  hasExistingGuestSave: Boolean,
  savedGuestSummary: String?,
  onContinueAsGuest: () -> Unit,
  onGoogleSignInClick: () -> Unit,
  modifier: Modifier = Modifier,
  showFullCrestHeader: Boolean = false,
) {
  Surface(
    modifier = modifier
      .border(1.5.dp, SummonerGold.copy(alpha = 0.68f), RoundedCornerShape(20.dp)),
    shape = RoundedCornerShape(20.dp),
    color = SanctuaryStone.copy(alpha = 0.94f),
    tonalElevation = 10.dp,
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (showFullCrestHeader) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(18.dp))
          Text("DARK FANTASY PIXEL TOWER RPG", style = MaterialTheme.typography.labelMedium, color = PortalCyan)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "ETERNAL SUMMONER",
          style = MaterialTheme.typography.headlineMedium,
          color = SummonerGold,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(id = R.string.auth_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianVoid.copy(alpha = 0.74f))
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = if (hasExistingGuestSave) EmeraldVitality else ArcaneViolet,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = if (hasExistingGuestSave && savedGuestSummary != null) {
            "Đã tìm thấy Hồ Sơ Khách trên thiết bị: $savedGuestSummary"
          } else {
            "Vào Thánh Địa ngay với Hồ Sơ Khách (lưu tự động trên máy) hoặc đăng nhập Google để đồng bộ đám mây."
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
