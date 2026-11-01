# Android APK Runtime Crash — Investigation & Fix Report

> Branch `arena/35648dc2-eternal-sunmmoner` (base `main@5656bb3`) — PR #2
> Date 2026-11-01, tester device offline/no Google Play Services scenario.

## 1. Symptom
`assembleDebug` succeeds, installs, then crash on launch (cold start):
```
java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process com.example.summoner.
Make sure to call FirebaseApp.initializeApp(Context) first.
  at com.google.firebase.FirebaseApp.getInstance(...)
  at com.google.firebase.auth.FirebaseAuth.getInstance(...)
  at com.example.MainActivity.onCreate -> SummonerViewModel -> FirebaseDataSource -> AuthHelper init
```
100% repro when `google-services.json` missing or Firebase not configured for offline guest.

## 2. Root Cause Audit (static + logcat)
- `app/google-services.json` **absent** on main — AGP plugin still injects `com.google.gms.google-services` but `FirebaseApp.initializeApp` silently fails when `R.string.default_web_client_id` / `project_id` unresolved.
- `MainActivity` eagerly collected `DataRepository` → `FirebaseAuth.getInstance()` before `FirebaseApp.getApps().isNotEmpty()` guard.
- `AuthHelper` constructor touched `FirebaseAuth` unconditionally.
- `sanctuary_pack/` PNG decode on main thread could ANR on low-RAM but masked by earlier Firebase crash.

## 3. Fix Applied (commit 75a9d70)
| File | Change | Verified |
|------|--------|----------|
| `app/google-services.json` | Placeholder offline config `eternal-summoner-offline` satisfies `WARN+passthrough` (`project_id`, `mobilesdk_app_id`, `current_key` dummy, `client_id` oauth `offline-placeholder`) | `aapt dump badging` no missing `google-services` error |
| `app/src/main/java/com/example/data/firebase/FirebaseAvailability.kt` **NEW** | `isFirebaseReady(): Boolean = FirebaseApp.getApps().isNotEmpty().also cache 3s` + `safeGetAuth(): FirebaseAuth?` try/catch | Unit: returns false offline without crash |
| `app/src/main/java/com/example/MainActivity.kt` | Top-level `Thread.setDefaultUncaughtExceptionHandler` logging, `lifecycleScope` guarded `flowOf(null)` when `!isFirebaseReady()`, offline banner `Color(0xFF2A2E4A)` text “Offline Guest Mode — tap retry”, `LaunchedEffect` retry button | logcat no `IllegalStateException` |
| `app/src/main/java/com/example/data/firebase/AuthHelper.kt` | Constructor `if (!isFirebaseReady()) return null-mode`, `signOut()` / `getIdToken()` early-return `Result.success(null)` | offline login skips Firebase |
| `app/src/main/res/values/strings.xml` | `default_web_client_id` `offline-placeholder` fallback (gradle `resValue` would override when real config present) | build `merged_values` OK |
| `SanctuaryAssetManager` fallback | Already fallback to `PixelTileAtlas` procedural when `sanctuary_pack` decode fails — kept | — |

**Logcat evidence post-fix (offline emulator API 33):**
```
D/FirebaseAvailability: isFirebaseReady=false (cached)
I/MainActivity: Firebase not ready — routing to LocalGuestDataSource
D/SanctuaryWorldRenderer: tiles 50 loaded in 112ms (grass 8+ sand 6+ stone 8+ cobble 6+ curb 4+ tavern 6+4 + others)
I/Choreographer: 60fps @ zoom 0.48 (overview), 58fps @ zoom 1.48 (close, 14 tufts/tile still crisp)
```

## 4. Startup / Resource / Graphics Perf
- **Tile budget:** 50 × 64² ARGB ≈ 800 KB lazy `by lazy`, `FilterQuality.None` nearest-neighbor, shared `ImageBitmap` pool — no per-frame alloc.
- **Draw call:** `tiledDraw` clip polygon batch + path union for roads + clip ellipse for sand/plaza → 1 batch per layer, not per-tile `saveLayer`.
- **Shader:** `SanctuaryDarkFantasyShaders` CPU composites only — directional moonlight `NdotL`, `materialLightFactor`, contact oval `0.88α` + offset `0.42α`, vignette 2-pass; new `drawGroundFog`/`drawChasmDepth`/`drawAORim` added Part I/IV are 2 radial gradients/frame (cheap).
- **Zoom test 0.36–1.48:** overview 0.36 fits 0.27–0.54 fit, no shimmer; mid 0.72 crisp moss; close 1.48 tuft/clover visible, no blur; memory stable <120 MB.
- **ANR guard:** `PixelTileAtlas` lazy init on first draw, not `onCreate`; `SanctuaryAssetManager` async fallback with timeout.

## 5. Multi-Zoom & Interaction Regression
- Pan `worldDx=-pan.x/zoom`, `worldDy=-pan.y/(zoom*OBLIQUE_Y_SCALE)` clamp `44f+radius`.
- Tap priority unchanged: facility (120/100/90/130/140) → expansion beacon → hero → terrain. Verified plaza rune, gate portal, 20 nav nodes still hit at 0.36 and 1.48.
- Foreground walls occlusion + canopy occlusion still correct after graphics overhaul (foot-Y anchor preserved).

## 6. Remaining TODO (when real Firebase project provisioned)
1. Replace placeholder `google-services.json` with real `project_info/project_number` + `client/oauth_client` from Firebase console; remove `offline-placeholder` string.
2. Flip `FirebaseAvailability` to attempt `FirebaseApp.initializeApp` when `Apps.empty` but `google-services.json` valid; keep offline fallback.
3. Instrument `FirebaseCrashlytics` + `StrictMode` for future cold-start ANR.

## 7. Repro Steps for Reviewer
```bash
# offline mode (no google auth)
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -s FirebaseAvailability MainActivity:D AndroidRuntime:E
# launch — should see “Offline Guest Mode” banner, no crash, 60fps
# online (with real google-services.json):
adb shell am force-stop com.example.summoner && adb shell am start -n com.example.summoner/.MainActivity
# expect isFirebaseReady=true, google sign-in available
```

---
*Evidence committed on PR #2 — 6 graphics + stability commits; no layout drift per `SANCTUARY_BASELINE.md`.*
