package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GameSettings
import com.example.data.PerformanceMode
import com.example.data.SessionAccountMode
import com.example.ui.theme.ESDesignTokens
import com.example.ui.theme.ObsidianVoid

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
      // This modal is a barrier: blank panel/scrim taps must never reach the world below it.
      .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
      .background(ESDesignTokens.scrim)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("settings_account_modal"),
    contentAlignment = Alignment.Center,
  ) {
    FantasySurface(
      shape = RoundedCornerShape(20.dp),
      color = ESDesignTokens.panel,
      tonalElevation = 12.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .border(1.5.dp, ESDesignTokens.gold.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
        .testTag("settings_account_modal_surface"),
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
            Icon(Icons.Default.Settings, contentDescription = null, tint = ESDesignTokens.gold)
            Column {
              Text(
                text = "CÀI ĐẶT & HỒ SƠ TÀI KHOẢN",
                style = MaterialTheme.typography.titleMedium,
                color = ESDesignTokens.gold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Quản lý lưu trữ, tài khoản & hiển thị chiến thuật",
                style = MaterialTheme.typography.labelSmall,
                color = ESDesignTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          FantasyIconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).testTag("close_settings_modal_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Đóng cài đặt", tint = ESDesignTokens.textPrimary)
          }
        }

        // 1. Account status and local-save protection.
        val accountAccent = if (accountMode == SessionAccountMode.GOOGLE_CLOUD) {
          ESDesignTokens.success
        } else {
          ESDesignTokens.cyan
        }
        ESSectionCard(
          modifier = Modifier.fillMaxWidth(),
          title = "Hồ sơ và lưu trữ",
          eyebrow = when (accountMode) {
            SessionAccountMode.GOOGLE_CLOUD -> "ĐỒNG BỘ GOOGLE CLOUD"
            SessionAccountMode.GUEST_LOCAL -> "HỒ SƠ KHÁCH • LƯU CỤC BỘ"
            SessionAccountMode.NONE -> "CHƯA MỞ PHIÊN CHƠI"
          },
          subtitle = accountSummary,
          accent = accountAccent,
          padding = 12.dp,
        ) {
          ESStatePill(
            text = when (accountMode) {
              SessionAccountMode.GOOGLE_CLOUD -> "Đã kết nối tài khoản Google"
              SessionAccountMode.GUEST_LOCAL -> "Tiến trình được giữ trên thiết bị"
              SessionAccountMode.NONE -> "Chọn Khách hoặc Google để tiếp tục"
            },
            tone = if (accountMode == SessionAccountMode.GOOGLE_CLOUD) ESUiTone.SUCCESS else ESUiTone.ACCENT,
            icon = if (accountMode == SessionAccountMode.GOOGLE_CLOUD) Icons.Default.VerifiedUser else Icons.Default.Person,
          )

          if (accountMode == SessionAccountMode.GUEST_LOCAL) {
            Text(
              text = "Vàng, anh hùng, trang bị và tiến trình Tháp được lưu tự động trên thiết bị kèm bản sao lưu. Có thể liên kết Google sau mà không xóa hồ sơ khách.",
              style = MaterialTheme.typography.bodySmall,
              color = ESDesignTokens.textSecondary,
            )
          }

          if (linkStatusMessage != null) {
            Text(
              text = linkStatusMessage,
              style = MaterialTheme.typography.bodySmall,
              color = ESDesignTokens.success,
              modifier = Modifier.testTag("account_link_status"),
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
              shape = RoundedCornerShape(12.dp),
              colors = FantasySkin.buttonColors(
                containerColor = ESDesignTokens.cyan,
                contentColor = ObsidianVoid,
              ),
            ) {
              if (isLinkingAccount) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = ESDesignTokens.gold,
                  strokeWidth = 2.2.dp,
                )
              } else {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Liên kết hồ sơ khách với Google",
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        // 2. Camera, frame pacing and visual toggles.
        ESSectionCard(
          modifier = Modifier.fillMaxWidth(),
          title = "Hiển thị & hiệu năng",
          eyebrow = "CÀI ĐẶT THÁNH ĐỊA",
          subtitle = "Chọn góc nhìn, nhịp khung hình và hiệu ứng môi trường.",
          accent = ESDesignTokens.cyan,
          padding = 12.dp,
        ) {
          Text(
            text = "Tầm Nhìn Camera Mặc Định",
            style = MaterialTheme.typography.titleSmall,
            color = ESDesignTokens.gold,
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
              FantasyChoiceButton(
                onClick = { onUpdateSettings(settings.copy(defaultZoom = presetZoom)) },
                selected = selected,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) ESDesignTokens.gold else ESDesignTokens.textPrimary,
                  maxLines = 1,
                )
              }
            }
          }

          Text(
            text = "Nhịp Khung Hình",
            style = MaterialTheme.typography.titleSmall,
            color = ESDesignTokens.gold,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PerformanceMode.entries.forEach { mode ->
              val selected = settings.performanceMode == mode
              FantasyChoiceButton(
                onClick = { onUpdateSettings(settings.copy(performanceMode = mode)) },
                selected = selected,
                modifier = Modifier.heightIn(min = 48.dp).testTag("settings_performance_${mode.name.lowercase()}"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Text(
                  text = if (mode == PerformanceMode.BALANCED) "Cân bằng • 30 FPS" else "Cao • 60 FPS",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) ESDesignTokens.gold else ESDesignTokens.textPrimary,
                )
              }
            }
          }
          Text(
            text = "Cân bằng tiết kiệm pin, giữ nguyên hình ảnh và tốc độ mô phỏng. Chế độ Cao dành cho máy chạy ổn định.",
            style = MaterialTheme.typography.bodySmall,
            color = ESDesignTokens.textSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text("Đo hiệu năng (nhà phát triển)", style = MaterialTheme.typography.labelLarge, color = ESDesignTokens.textPrimary)
              Text("Cập nhật số liệu mỗi giây; mặc định tắt", style = MaterialTheme.typography.bodySmall, color = ESDesignTokens.textSecondary)
            }
            Switch(
              checked = settings.performanceOverlay,
              onCheckedChange = { onUpdateSettings(settings.copy(performanceOverlay = it)) },
              modifier = Modifier.testTag("settings_performance_overlay_toggle"),
              colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = ESDesignTokens.cyan),
            )
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
                color = ESDesignTokens.textPrimary,
              )
              Text(
                text = "Đom đóm rừng cổ, tia nắng & tro bụi ma thuật",
                style = MaterialTheme.typography.bodySmall,
                color = ESDesignTokens.textSecondary,
              )
            }
            Switch(
              checked = settings.particlesEnabled,
              onCheckedChange = { onUpdateSettings(settings.copy(particlesEnabled = it)) },
              modifier = Modifier.testTag("settings_particles_toggle"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = ESDesignTokens.gold,
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
                color = ESDesignTokens.textPrimary,
              )
              Text(
                text = "Hiện trạng thái hành vi AI tự trị trong chiến đấu",
                style = MaterialTheme.typography.bodySmall,
                color = ESDesignTokens.textSecondary,
              )
            }
            Switch(
              checked = settings.showHeroAiIntent,
              onCheckedChange = { onUpdateSettings(settings.copy(showHeroAiIntent = it)) },
              modifier = Modifier.testTag("settings_ai_intent_toggle"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = ESDesignTokens.cyan,
              ),
            )
          }
        }

        // 3. Logout / Switch Account
        if (onLogoutOrSwitchAccount != null) {
          FantasyChoiceButton(
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
              tint = ESDesignTokens.warning,
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
