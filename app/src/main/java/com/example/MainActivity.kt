package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SessionAccountMode
import com.example.data.firebase.FirebaseAvailability
import com.example.data.firebase.SummonerRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.performInteractiveGoogleSignIn
import com.example.data.firebase.signOutSummoner
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.ui.GameScreenMode
import com.example.ui.SummonerViewModel
import com.example.ui.components.SettingsAndAccountModal
import com.example.ui.components.p28.ESLoadingOverlay
import com.example.ui.components.p28.ESLoadingState
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.FloorSelectionScreen
import com.example.ui.screens.SanctuaryLobbyScreen
import com.example.ui.screens.SquadPreparationScreen
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.SummonerGold
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.game.pixel.SanctuaryArtAssets.init(applicationContext)
    // Crash fix: Thread.setDefaultUncaughtExceptionHandler to log FATAL without silent exit
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
      Log.e("MainActivity", "FATAL uncaught on ${t.name}", e)
      defaultHandler?.uncaughtException(t, e)
    }
    enableEdgeToEdge()
    setContent {
      EternalSummonerTheme {
        EternalSummonerRootApp()
      }
    }
  }
}

@Composable
fun EternalSummonerRootApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val saveManager = remember(context) { LocalGuestSaveManager(context) }

  // ---- ANDROID STABILITY FIX: guard Firebase availability ----
  // Build can succeed with google-services.json missing (WARN+passthrough) but at runtime
  // FirebaseApp is not initialized → Firebase.auth throws IllegalStateException and app crashes
  // on launch before any UI appears. We check availability once and fall back to pure offline guest mode.
  val isFirebaseReady = remember(context) {
    val ready = FirebaseAvailability.isFirebaseReady(context)
    if (!ready) {
      Log.w("EternalSummoner", "Firebase not ready — forcing offline guest mode. Cloud sync disabled.")
    }
    ready
  }

  // Safe auth flow: only subscribe if Firebase is actually initialized
  val firebaseUserFlow: Flow<FirebaseUser?> = remember(isFirebaseReady) {
    if (isFirebaseReady) {
      try {
        Firebase.auth.authStateFlow()
      } catch (e: Exception) {
        Log.w("EternalSummoner", "authStateFlow failed, fallback to null flow", e)
        flowOf(null)
      }
    } else {
      flowOf(null)
    }
  }
  val initialUser: FirebaseUser? = remember(isFirebaseReady) {
    if (isFirebaseReady) {
      try { Firebase.auth.currentUser } catch (_: Exception) { null }
    } else null
  }
  val currentUser by firebaseUserFlow
    .collectAsStateWithLifecycle(initialValue = initialUser)
  val sessionMode by saveManager.sessionMode.collectAsStateWithLifecycle()
  val gameSettings by saveManager.settings.collectAsStateWithLifecycle()
  val guestSnapshot by saveManager.guestSnapshot.collectAsStateWithLifecycle()

  LaunchedEffect(gameSettings.performanceMode) {
    com.example.game.pixel.DepthPresentation.quality =
      if(gameSettings.performanceMode == com.example.data.PerformanceMode.HIGH)
        com.example.game.pixel.DepthPresentation.Quality.BALANCED
      else com.example.game.pixel.DepthPresentation.Quality.REDUCED
  }
  var showSettingsModal by remember { mutableStateOf(false) }
  var isLinkingAccount by remember { mutableStateOf(false) }
  var linkStatusMessage by remember { mutableStateOf<String?>(null) }
  var allowSilentGoogleOnGate by remember { mutableStateOf(true) }

  val cloudUser = if (isFirebaseReady) currentUser else null
  LaunchedEffect(cloudUser?.uid) {
    if (cloudUser != null) {
      saveManager.markGoogleCloudSessionActive()
    }
  }

  val isGuestSessionActive = cloudUser == null && sessionMode == SessionAccountMode.GUEST_LOCAL

  if (cloudUser == null && !isGuestSessionActive) {
    val savedSummary = guestSnapshot?.let { snap ->
      "${snap.profile.masterName} (Lv.${snap.profile.masterLevel}) • Tầng ${snap.profile.highestClearedFloor} • ${snap.heroes.size} Anh hùng"
    }

    Box(modifier = Modifier.fillMaxSize()) {
      AuthGateScreen(
        hasExistingGuestSave = guestSnapshot != null,
        savedGuestSummary = savedSummary,
        allowSilentAutoSignIn = allowSilentGoogleOnGate && isFirebaseReady,
        onContinueAsGuest = {
          scope.launch {
            saveManager.startOrResumeGuestSession("Master")
          }
        },
        onAuthenticated = {
          saveManager.markGoogleCloudSessionActive()
        },
        onOpenSettings = {
          linkStatusMessage = null
          showSettingsModal = true
        },
      )

      if (showSettingsModal) {
        SettingsAndAccountModal(
          accountMode = SessionAccountMode.NONE,
          accountSummary = savedSummary ?: "Chưa có hồ sơ đang mở trên thiết bị",
          settings = gameSettings,
          onUpdateSettings = { next ->
            try { saveManager.updateSettings(next) }
            catch (error: Exception) { linkStatusMessage = "Không lưu được cài đặt; dữ liệu cũ được giữ lại. ${error.localizedMessage.orEmpty()}" }
          },
          onClose = { showSettingsModal = false },
        )
      }
    }
    return
  }

  val activeUserId = cloudUser?.uid ?: (guestSnapshot?.guestPlayerId ?: "guest_local_session")
  val vmKey = if (cloudUser != null) "cloud_${cloudUser.uid}" else "guest_$activeUserId"

  val vm: SummonerViewModel = viewModel(
    key = vmKey,
    factory = viewModelFactory {
      initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
        if (cloudUser != null && isFirebaseReady) {
          try {
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            SummonerViewModel(
              repository = SummonerRepository(db = db, auth = Firebase.auth),
              currentUserId = cloudUser.uid,
              preferredDisplayName = cloudUser.displayName?.takeIf { it.isNotBlank() } ?: "Master",
            )
          } catch (e: Exception) {
            Log.e("EternalSummoner", "Firestore init failed, fallback to guest", e)
            SummonerViewModel(
              repository = LocalGuestDataSource(saveManager),
              currentUserId = activeUserId,
              preferredDisplayName = guestSnapshot?.profile?.masterName ?: "Master",
            )
          }
        } else {
          SummonerViewModel(
            repository = LocalGuestDataSource(saveManager),
            currentUserId = activeUserId,
            preferredDisplayName = guestSnapshot?.profile?.masterName ?: "Master",
          )
        }
      }
    },
  )

  val uiState by vm.uiState.collectAsStateWithLifecycle()

  // Auto-dismiss transient toast banners after 4 seconds
  LaunchedEffect(uiState.bannerFeedback) {
    if (uiState.bannerFeedback != null) {
      delay(4000L)
      vm.dismissBanner()
    }
  }

  if (uiState.isBootstrapping) {
    // P28: cinematic loading overlay with the original backdrop/sigil/progress assets.
    // Stage text is real bootstrap copy; guest/offline mode never claims a server connection.
    val stage = "Đang triệu hồi Thánh Địa (The Sanctuary)..." + " • " + if (cloudUser != null) {
      "Đồng bộ khế ước Google Cloud & đội hình Anh hùng"
    } else {
      "Khôi phục Hồ Sơ Khách & dữ liệu Thánh Địa trên thiết bị" +
        if (!isFirebaseReady) " • ⚡ Chế độ Khách ngoại tuyến — đám mây chưa cấu hình" else ""
    }
    ESLoadingOverlay(
      state = ESLoadingState.Indeterminate(stage),
      modifier = Modifier.fillMaxSize(),
    )
    return
  }

  val handleLogoutOrSwitch: () -> Unit = {
    allowSilentGoogleOnGate = false
    showSettingsModal = false
    linkStatusMessage = null
    saveManager.logoutToTitleGate()
    if (isFirebaseReady) {
      try {
        if (Firebase.auth.currentUser != null) {
          signOutSummoner(
            credentialManager = credentialManager,
            scope = scope,
            onComplete = {},
          )
        }
      } catch (e: Exception) {
        Log.w("EternalSummoner", "signOut failed", e)
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    when (uiState.screenMode) {
      GameScreenMode.SANCTUARY_LOBBY -> {
        SanctuaryLobbyScreen(
          state = uiState,
          sceneActive = !showSettingsModal,
          settings = gameSettings,
          onOpenFacility = vm::openFacility,
          onCloseFacility = vm::closeFacilityModal,
          onSummonHero = vm::performHeroSummon,
          onTrainHero = vm::trainHero,
          onEquipItem = vm::equipItem,
          onSynthesizeGear = vm::synthesizeNewGear,
          onEnhanceGear = vm::enhanceGear,
          onRecoverHero = vm::recoverHero,
          onHostMeal = vm::hostRestaurantMeal,
          onToggleSquad = vm::toggleSquadHero,
          onCompletePrologue = vm::completePrologueCutscene,
          onOpenSettings = {
            linkStatusMessage = null
            showSettingsModal = true
          },
          onSignOut = handleLogoutOrSwitch,
          onTrainHeroWithFocus = vm::trainHeroWithFocus,
          onUnequipItem = vm::unequipItem,
          onAssignHeroActivity = vm::assignHeroActivity,
          onDropHero = vm::assignLobbyHero,
          onHostMealWithRecipe = vm::hostRestaurantMealWithRecipe,
          onUnlockExpansion = vm::unlockIslandExpansion,
          onHealHeroInfirmary = vm::healHeroAtInfirmary,
          onServeHeartyMeal = vm::serveHeartyMealToHero,
          onCruelSynthesis = vm::performCruelHeroSynthesis,
          onSocialDisdainPenalty = { heroId -> vm.applySocialDisdainPenalty(heroId, 2) },
        )
      }

      GameScreenMode.FLOOR_SELECTION -> {
        FloorSelectionScreen(
          state = uiState,
          onSelectFloor = vm::selectFloorForPrep,
          onBackToLobby = vm::returnToSanctuaryLobby,
          playEntryCutscene = true,
        )
      }

      GameScreenMode.SQUAD_PREPARATION -> {
        SquadPreparationScreen(
          state = uiState,
          onToggleHero = vm::toggleSquadHero,
          onOpenFacility = vm::openFacility,
          onCloseFacility = vm::closeFacilityModal,
          onSummonHero = vm::performHeroSummon,
          onTrainHero = vm::trainHero,
          onEquipItem = vm::equipItem,
          onSynthesizeGear = vm::synthesizeNewGear,
          onEnhanceGear = vm::enhanceGear,
          onRecoverHero = vm::recoverHero,
          onHostMeal = vm::hostRestaurantMeal,
          onDeploy = vm::launchTowerExpedition,
          onBack = vm::backToFloorSelection,
          onTrainHeroWithFocus = vm::trainHeroWithFocus,
          onUnequipItem = vm::unequipItem,
          onAssignHeroActivity = vm::assignHeroActivity,
          onHostMealWithRecipe = vm::hostRestaurantMealWithRecipe,
          onHealHeroInfirmary = vm::healHeroAtInfirmary,
          onServeHeartyMeal = vm::serveHeartyMealToHero,
          onCruelSynthesis = vm::performCruelHeroSynthesis,
        )
      }

      GameScreenMode.TOWER_COMBAT -> {
        TowerBattleScreen(
          state = uiState,
          sceneActive = !showSettingsModal,
          settings = gameSettings,
          onFinishExpedition = { victory, fallen -> vm.finishTowerExpedition(victory, fallen) },
          onReturnToSanctuary = vm::returnToSanctuaryLobby,
          onPrepareRetry = vm::retryTowerExpedition,
          onFinishExpeditionDetailed = vm::finishTowerExpedition,
          onEquipItem = vm::equipItem,
        )
      }
    }

    if (showSettingsModal) {
      val activeMode = if (cloudUser != null) SessionAccountMode.GOOGLE_CLOUD else SessionAccountMode.GUEST_LOCAL
      val profile = uiState.profile
      val summaryText = if (cloudUser != null) {
        "${cloudUser.displayName ?: profile?.masterName ?: "Master"} • Tầng ${profile?.highestClearedFloor ?: 0} • ${uiState.heroes.size} Anh hùng"
      } else {
        "ID: ${guestSnapshot?.guestPlayerId ?: activeUserId} • Tầng ${profile?.highestClearedFloor ?: 0} • ${uiState.heroes.size} Anh hùng"
      }

      SettingsAndAccountModal(
        accountMode = activeMode,
        accountSummary = summaryText,
        settings = gameSettings,
        onUpdateSettings = { next ->
            try { saveManager.updateSettings(next) }
            catch (error: Exception) { linkStatusMessage = "Không lưu được cài đặt; dữ liệu cũ được giữ lại. ${error.localizedMessage.orEmpty()}" }
          },
        onClose = { showSettingsModal = false },
        isLinkingAccount = isLinkingAccount,
        linkStatusMessage = linkStatusMessage,
        onLinkGoogleAccount = if (activeMode == SessionAccountMode.GUEST_LOCAL && isFirebaseReady) {
          {
            val snapToMigrate = saveManager.guestSnapshot.value
            if (snapToMigrate != null && !isLinkingAccount) {
              isLinkingAccount = true
              linkStatusMessage = null
              performInteractiveGoogleSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {
                  scope.launch {
                    try {
                      val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                      val cloudRepo = SummonerRepository(db = db, auth = Firebase.auth)
                      val msg = cloudRepo.linkGuestSaveToCloudAccount(snapToMigrate)
                      saveManager.markGoogleCloudSessionActive()
                      linkStatusMessage = msg
                    } catch (e: Exception) {
                      linkStatusMessage = e.localizedMessage ?: "Liên kết đám mây gặp sự cố."
                    } finally {
                      isLinkingAccount = false
                    }
                  }
                },
                onAuthError = { err ->
                  isLinkingAccount = false
                  linkStatusMessage = err
                },
                onAuthCancelled = {
                  isLinkingAccount = false
                },
              )
            }
          }
        } else {
          null
        },
        onLogoutOrSwitchAccount = handleLogoutOrSwitch,
      )
    }
  }
}
