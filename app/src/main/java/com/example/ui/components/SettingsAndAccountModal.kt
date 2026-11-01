package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GameSettings
import com.example.data.SessionAccountMode
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.AstralSlate
import com.example.ui.theme.BorderRune
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SummonerGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndAccountModal(
  accountMode: SessionAccountMode,
  accountSummary: String,
  settings: GameSettings,
  onUpdateSettings: (GameSettings) -> Unit,
  onClose: () -> Unit,
  onLinkGoogleAccount: (() -> Unit)? = null,
  isLinkingAccount: Boolean = false,
  linkStatusMessage: String? = null,
  onLogoutOrSwitchAccount: (() -> Unit)? = null,
) {
  BackHandler(onBack = onClose)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.86f))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("settings_account_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = FantasySkin.Panel,
      tonalElevation = 12.dp,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 580.dp)
        .border(1.5.dp, SummonerGold.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = SummonerGold)
            Column {
              Text(
                text = "CÀI ĐẶT & HỒ SƠ TÀI KHOẢN",
                style = MaterialTheme.typography.titleMedium,
                color = SummonerGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Quản lý lưu trữ, tài khoản & hiển thị chiến thuật",
                style = MaterialTheme.typography.labelSmall,
                color = RuneSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).testTag("close_settings_modal_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Đóng cài đặt", tint = ParchmentLight)
          }
        }

        // 1. Account Status & Save Protection Card
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AstralSlate)
            .border(
              1.dp,
              if (accountMode == SessionAccountMode.GOOGLE_CLOUD) EmeraldVitality.copy(alpha = 0.7f)
              else PortalCyan.copy(alpha = 0.65f),
              RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = if (accountMode == SessionAccountMode.GOOGLE_CLOUD) {
                Icons.Default.VerifiedUser
              } else {
                Icons.Default.Person
              },
              contentDescription = null,
              tint = if (accountMode == SessionAccountMode.GOOGLE_CLOUD) EmeraldVitality else PortalCyan,
              modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = when (accountMode) {
                  SessionAccountMode.GOOGLE_CLOUD -> "Tài Khoản Google Cloud • Đồng Bộ Đám Mây"
                  SessionAccountMode.GUEST_LOCAL -> "Hồ Sơ Khách (Guest) • Lưu Cục Bộ Bền Vững (v2)"
                  SessionAccountMode.NONE -> "Chưa Mở Phiên Chơi • Chọn Khách hoặc Google"
                },
                style = MaterialTheme.typography.titleSmall,
                color = ParchmentLight,
              )
              Text(
                text = accountSummary,
                style = MaterialTheme.typography.bodySmall,
                color = SummonerGold,
              )
            }
          }

          if (accountMode == SessionAccountMode.GUEST_LOCAL) {
            Text(
              text = "Dữ liệu Vàng, Anh hùng, Trang bị & Tầng Tháp được lưu tự động kèm bản sao lưu dự phòng (.bak) ngay trên thiết bị. Bạn có thể liên kết sang Google bất kỳ lúc nào mà không mất tiến trình.",
              style = MaterialTheme.typography.bodySmall,
              color = RuneSilver,
            )
          }

          if (linkStatusMessage != null) {
            Text(
              text = linkStatusMessage,
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldVitality,
            )
          }

          if (accountMode == SessionAccountMode.GUEST_LOCAL && onLinkGoogleAccount != null) {
            Button(
              onClick = onLinkGoogleAccount,
              enabled = !isLinkingAccount,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("link_google_account_button"),
              shape = RoundedCornerShape(10.dp),
              colors = FantasySkin.buttonColors(
                containerColor = PortalCyan,
                contentColor = ObsidianVoid,
              ),
            ) {
              if (isLinkingAccount) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = ObsidianVoid,
                  strokeWidth = 2.2.dp,
                )
              } else {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Liên Kết & Đồng Bộ Hồ Sơ Khách Lên Google",
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        // 2. Camera Zoom Preset
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianVoid.copy(alpha = 0.68f))
            .border(1.dp, BorderRune, RoundedCornerShape(14.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Tầm Nhìn Camera Mặc Định",
            style = MaterialTheme.typography.titleSmall,
            color = SummonerGold,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(
              0.76f to "0.76x Toàn Cảnh",
              0.88f to "0.88x Chuẩn RPG",
              1.04f to "1.04x Cận Cảnh",
            ).forEach { (presetZoom, label) ->
              val selected = kotlin.math.abs(settings.defaultZoom - presetZoom) < 0.05f
              Box(
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (selected) ArcaneViolet.copy(alpha = 0.45f) else SanctuaryStone)
                  .border(1.dp, if (selected) SummonerGold else BorderRune, RoundedCornerShape(10.dp))
                  .clickable { onUpdateSettings(settings.copy(defaultZoom = presetZoom)) }
                  .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) SummonerGold else ParchmentLight,
                  maxLines = 1,
                )
              }
            }
          }

          // Toggle 1: Atmospheric Particles
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Hạt Ánh Sáng & Bầu Không Khí",
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentLight,
              )
              Text(
                text = "Đom đóm rừng cổ, tia nắng & tro bụi ma thuật",
                style = MaterialTheme.typography.bodySmall,
                color = RuneSilver,
              )
            }
            Switch(
              checked = settings.particlesEnabled,
              onCheckedChange = { onUpdateSettings(settings.copy(particlesEnabled = it)) },
              modifier = Modifier.testTag("settings_particles_toggle"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = SummonerGold,
              ),
            )
          }

          // Toggle 2: Live Hero AI Intent Labels
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Hiển Thị Ý Định AI Anh Hùng",
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentLight,
              )
              Text(
                text = "Hiện trạng thái hành vi AI tự trị trong chiến đấu",
                style = MaterialTheme.typography.bodySmall,
                color = RuneSilver,
              )
            }
            Switch(
              checked = settings.showHeroAiIntent,
              onCheckedChange = { onUpdateSettings(settings.copy(showHeroAiIntent = it)) },
              modifier = Modifier.testTag("settings_ai_intent_toggle"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = PortalCyan,
              ),
            )
          }
        }

        // 3. Logout / Switch Account
        if (onLogoutOrSwitchAccount != null) {
          OutlinedButton(
            onClick = onLogoutOrSwitchAccount,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("logout_switch_account_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              tint = AmberWarning,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Đăng Xuất / Đổi Tài Khoản (Giữ Nguyên File Lưu Khách)",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
