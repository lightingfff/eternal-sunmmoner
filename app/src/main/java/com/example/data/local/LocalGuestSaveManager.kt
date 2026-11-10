package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.FeastBuffHelper
import com.example.data.GameSettings
import com.example.data.GuestSaveSnapshot
import com.example.data.LostCourierProgression
import com.example.data.SanctuaryProgressionHelper
import com.example.data.SessionAccountMode
import com.example.data.SummonerDataSource
import com.example.data.firebase.ExpeditionOutcomeType
import com.example.data.firebase.ExpeditionSettlementResult
import com.example.data.firebase.SummonerRepository
import com.example.data.model.EquipmentRecord
import com.example.data.model.CourierOutcome
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.PersonalityType
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import java.io.File
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reliable, versioned, offline-first local save manager for Guest Profiles & Game Settings.
 * - Uses atomic temp-file writes + automatic `.bak` backup recovery so transient read/write
 *   failures NEVER overwrite a valid player save with an empty profile.
 * - Supports schema version migration (v1 -> v2) and full validation/clamping.
 */
class LocalGuestSaveManager(context: Context) {

  private val storageDir: File = File(context.applicationContext.filesDir, "eternal_summoner_saves").apply {
    if (!exists()) mkdirs()
  }

  private val primarySaveFile = File(storageDir, "guest_save_v2.json")
  private val backupSaveFile = File(storageDir, "guest_save_v2.bak")
  private val tempSaveFile = File(storageDir, "guest_save_v2.tmp")
  private val sessionFile = File(storageDir, "session_state.json")

  private val ioMutex = Mutex()

  private val _sessionMode = MutableStateFlow(readInitialSessionMode())
  val sessionMode: StateFlow<SessionAccountMode> = _sessionMode.asStateFlow()

  private val _settings = MutableStateFlow(readInitialSettings())
  val settings: StateFlow<GameSettings> = _settings.asStateFlow()

  private val _guestSnapshot = MutableStateFlow(loadValidSnapshotFromDisk())
  val guestSnapshot: StateFlow<GuestSaveSnapshot?> = _guestSnapshot.asStateFlow()

  fun hasSavedGuestProfile(): Boolean = _guestSnapshot.value != null

  fun activeGuestIdOrNull(): String? = _guestSnapshot.value?.guestPlayerId

  /**
   * Starts or resumes a Guest session in one tap without requiring email, password, or network.
   */
  suspend fun startOrResumeGuestSession(preferredMasterName: String = "Master"): GuestSaveSnapshot =
    ioMutex.withLock {
      val existing = _guestSnapshot.value ?: loadValidSnapshotFromDisk()
      val activeSnapshot = if (existing != null) {
        existing.copy(lastSavedEpochSec = System.currentTimeMillis() / 1000L)
      } else {
        createStarterGuestSnapshot(preferredMasterName)
      }
      writeSnapshotAtomically(activeSnapshot)
      _guestSnapshot.value = activeSnapshot
      _sessionMode.value = SessionAccountMode.GUEST_LOCAL
      persistSessionState(SessionAccountMode.GUEST_LOCAL, _settings.value)
      activeSnapshot
    }

  fun markGoogleCloudSessionActive() {
    _sessionMode.value = SessionAccountMode.GOOGLE_CLOUD
    persistSessionState(SessionAccountMode.GOOGLE_CLOUD, _settings.value)
  }

  fun logoutToTitleGate() {
    // Keeps local guest save intact on disk so returning guest never loses progress!
    _sessionMode.value = SessionAccountMode.NONE
    persistSessionState(SessionAccountMode.NONE, _settings.value)
  }

  fun updateSettings(newSettings: GameSettings) {
    val snap = _guestSnapshot.value
    if (snap != null) {
      val updated = snap.copy(
        settings = newSettings,
        lastSavedEpochSec = System.currentTimeMillis() / 1000L,
      )
      writeSnapshotAtomically(updated)
      _guestSnapshot.value = updated
    }
    _settings.value = newSettings
    persistSessionState(_sessionMode.value, newSettings)
  }

  suspend fun mutateSnapshot(
    transform: (GuestSaveSnapshot) -> GuestSaveSnapshot,
  ): GuestSaveSnapshot = ioMutex.withLock {
    val current = _guestSnapshot.value
      ?: loadValidSnapshotFromDisk()
      ?: createStarterGuestSnapshot("Master")
    val transformed = transform(current)
    val next = transformed.copy(
      heroes = transformed.heroes.map { hero ->
        if (!hero.hiddenTrait.unlocked && hero.hiddenTrait.canUnlock(hero.level.toInt(), hero.morale.toInt(), com.example.data.model.HeroSanityState.fromRaw(hero.sanityState)))
          hero.copy(hiddenTrait = hero.hiddenTrait.copy(unlocked = true)) else hero
      },
      schemaVersion = GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION,
      lastSavedEpochSec = System.currentTimeMillis() / 1000L,
    )
    writeSnapshotAtomically(next)
    _guestSnapshot.value = next
    next
  }

  private fun readInitialSessionMode(): SessionAccountMode {
    if (!sessionFile.exists()) {
      // Auto-resume if a valid guest save already exists from an earlier session
      return if (primarySaveFile.exists() || backupSaveFile.exists()) {
        SessionAccountMode.GUEST_LOCAL
      } else {
        SessionAccountMode.NONE
      }
    }
    return try {
      val root = JSONObject(sessionFile.readText(Charsets.UTF_8))
      val raw = root.optString("sessionMode", SessionAccountMode.NONE.name)
      val mode = SessionAccountMode.entries.firstOrNull { it.name == raw } ?: SessionAccountMode.NONE
      if (mode == SessionAccountMode.GUEST_LOCAL && !primarySaveFile.exists() && !backupSaveFile.exists()) {
        SessionAccountMode.NONE
      } else {
        mode
      }
    } catch (e: Exception) {
      Log.w(TAG, "Session file unreadable, checking guest save fallback", e)
      if (primarySaveFile.exists() || backupSaveFile.exists()) SessionAccountMode.GUEST_LOCAL else SessionAccountMode.NONE
    }
  }

  private fun readInitialSettings(): GameSettings {
    if (!sessionFile.exists()) return GameSettings()
    return try {
      val root = JSONObject(sessionFile.readText(Charsets.UTF_8))
      val sObj = root.optJSONObject("settings") ?: return GameSettings()
      parseSettings(sObj)
    } catch (_: Exception) {
      GameSettings()
    }
  }

  private fun persistSessionState(mode: SessionAccountMode, settings: GameSettings) {
    try {
      val root = JSONObject().apply {
        put("sessionMode", mode.name)
        put("updatedEpochSec", System.currentTimeMillis() / 1000L)
        put("settings", serializeSettings(settings))
      }
      sessionFile.writeText(root.toString(), Charsets.UTF_8)
    } catch (e: Exception) {
      Log.e(TAG, "Failed persisting session state", e)
    }
  }

  /**
   * Loads primary save file; if missing or corrupted, automatically falls back to `.bak` backup
   * WITHOUT overwriting valid backup data with an empty profile.
   */
  fun loadValidSnapshotFromDisk(): GuestSaveSnapshot? {
    parseFileSafely(primarySaveFile)?.let { return it }
    val recovered = parseFileSafely(backupSaveFile)
    if (recovered != null) {
      Log.w(TAG, "Primary save corrupted/missing — restored intact Guest Save from .bak backup!")
      // A valid backup remains usable even when repairing the primary file is impossible.
      runCatching { writeSnapshotAtomically(recovered) }
      return recovered
    }
    return null
  }

  private fun parseFileSafely(file: File): GuestSaveSnapshot? {
    if (!file.exists() || file.length() == 0L) return null
    return try {
      val text = file.readText(Charsets.UTF_8)
      val root = JSONObject(text)
      deserializeAndMigrateSnapshot(root)
    } catch (e: Exception) {
      Log.e(TAG, "Corrupted save file ${file.name}", e)
      null
    }
  }

  private fun writeSnapshotAtomically(snapshot: GuestSaveSnapshot) {
    try {
      val json = serializeSnapshot(snapshot).toString()
      tempSaveFile.writeText(json, Charsets.UTF_8)
      if (primarySaveFile.exists() && primarySaveFile.length() > 0L) {
        // Keep last known good primary as backup before replacing
        if (parseFileSafely(primarySaveFile) != null) {
          primarySaveFile.copyTo(backupSaveFile, overwrite = true)
        }
      }
      if (!tempSaveFile.renameTo(primarySaveFile)) {
        tempSaveFile.copyTo(primarySaveFile, overwrite = true)
        tempSaveFile.delete()
      }
      if (!backupSaveFile.exists()) {
        primarySaveFile.copyTo(backupSaveFile, overwrite = true)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Atomic guest save failed", e)
      throw e
    }
  }

  private fun createStarterGuestSnapshot(preferredName: String): GuestSaveSnapshot {
    val guestUid = "guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val cleanName = preferredName.trim()
      .replace(Regex("[^\\p{L}\\p{N} _\\-]"), "")
      .take(28)
      .ifBlank { "Master" }

    val profile = MasterProfile(
      id = guestUid,
      userId = guestUid,
      masterName = cleanName,
      masterLevel = 1L,
      masterExp = 0L,
      gold = 650L,
      manaCrystals = 160L,
      soulShards = 45L,
      rations = 12L,
      ironOre = 8L,
      arcaneEssence = 6L,
      highestClearedFloor = 0L,
      totalExpeditions = 0L,
      unlockedFacilities = FacilityType.entries.map { it.name },
      storyFlags = emptyList(),
    )

    val swordId = "eq_starter_sword"
    val bowId = "eq_starter_bow"
    val staffId = "eq_starter_staff"
    val armorId = "eq_starter_plate"

    val kaelenId = "hero_starter_kaelen"
    val lyraId = "hero_starter_lyra"
    val vesperId = "hero_starter_vesper"

    val heroes = listOf(
      HeroRecord(
        id = kaelenId,
        userId = guestUid,
        name = "Kaelen",
        title = "Khiên Thép Tiền Phong",
        heroClass = HeroClassType.KNIGHT.name,
        rarity = 3L,
        level = 1L,
        exp = 0L,
        maxHp = 480L,
        attack = 42L,
        defense = 32L,
        speed = 94L,
        skillLevel = 1L,
        personality = PersonalityType.STOIC.name,
        status = HeroStatusType.READY.name,
        morale = 100L,
        equippedWeaponId = swordId,
        inActiveSquad = true,
        bondSummary = "Tin tưởng tuyệt đối vào mũi tên yểm trợ của Lyra.",
      ),
      HeroRecord(
        id = lyraId,
        userId = guestUid,
        name = "Lyra",
        title = "Mắt Ưng Gió Đêm",
        heroClass = HeroClassType.ARCHER.name,
        rarity = 3L,
        level = 1L,
        exp = 0L,
        maxHp = 320L,
        attack = 54L,
        defense = 16L,
        speed = 118L,
        skillLevel = 1L,
        personality = PersonalityType.CYNICAL.name,
        status = HeroStatusType.READY.name,
        morale = 100L,
        equippedWeaponId = bowId,
        inActiveSquad = true,
        bondSummary = "Hay tranh luận chiến thuật với Vesper nhưng luôn bọc lót chuẩn xác.",
      ),
      HeroRecord(
        id = vesperId,
        userId = guestUid,
        name = "Vesper",
        title = "Học Giả Tinh Vân",
        heroClass = HeroClassType.MAGE.name,
        rarity = 3L,
        level = 1L,
        exp = 0L,
        maxHp = 290L,
        attack = 60L,
        defense = 14L,
        speed = 102L,
        skillLevel = 1L,
        personality = PersonalityType.CALCULATING.name,
        status = HeroStatusType.READY.name,
        morale = 100L,
        equippedWeaponId = staffId,
        inActiveSquad = true,
        bondSummary = "Đánh giá cao sự vững chãi của Kaelen nơi tuyến đầu.",
      ),
    )

    val equipment = listOf(
      EquipmentRecord(
        id = swordId,
        userId = guestUid,
        name = "Trường Kiếm Tiên Phong",
        slot = "WEAPON",
        rarity = 2L,
        level = 1L,
        bonusHp = 35L,
        bonusAttack = 12L,
        bonusDefense = 6L,
        bonusSpeed = 0L,
        iconType = "LONGSWORD",
        equippedByHeroId = kaelenId,
      ),
      EquipmentRecord(
        id = bowId,
        userId = guestUid,
        name = "Cung Gỗ Tần Bì",
        slot = "WEAPON",
        rarity = 2L,
        level = 1L,
        bonusHp = 10L,
        bonusAttack = 15L,
        bonusDefense = 0L,
        bonusSpeed = 8L,
        iconType = "LONGBOW",
        equippedByHeroId = lyraId,
      ),
      EquipmentRecord(
        id = staffId,
        userId = guestUid,
        name = "Trượng Tinh Thạch",
        slot = "WEAPON",
        rarity = 2L,
        level = 1L,
        bonusHp = 20L,
        bonusAttack = 18L,
        bonusDefense = 2L,
        bonusSpeed = 0L,
        iconType = "ARCANE_STAFF",
        equippedByHeroId = vesperId,
      ),
      EquipmentRecord(
        id = armorId,
        userId = guestUid,
        name = "Giáp Hộ Vệ Cổ Ngữ",
        slot = "ARMOR",
        rarity = 2L,
        level = 1L,
        bonusHp = 75L,
        bonusAttack = 0L,
        bonusDefense = 12L,
        bonusSpeed = 0L,
        iconType = "PLATE_MAIL",
        equippedByHeroId = "",
      ),
    )

    return GuestSaveSnapshot(
      schemaVersion = GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION,
      guestPlayerId = guestUid,
      profile = profile,
      heroes = heroes,
      equipment = equipment,
      settings = _settings.value,
      lastSavedEpochSec = System.currentTimeMillis() / 1000L,
    )
  }

  private fun serializeSettings(s: GameSettings): JSONObject = JSONObject().apply {
    put("defaultZoom", s.defaultZoom.toDouble())
    put("particlesEnabled", s.particlesEnabled)
    put("showHeroAiIntent", s.showHeroAiIntent)
    put("compactHud", s.compactHud)
    put("performanceMode", s.performanceMode.name)
    put("performanceOverlay", s.performanceOverlay)
  }

  private fun parseSettings(obj: JSONObject): GameSettings = GameSettings(
    defaultZoom = obj.optDouble("defaultZoom", 0.88).toFloat().coerceIn(0.65f, 1.35f),
    particlesEnabled = obj.optBoolean("particlesEnabled", true),
    showHeroAiIntent = obj.optBoolean("showHeroAiIntent", true),
    compactHud = obj.optBoolean("compactHud", false),
    performanceMode = com.example.data.PerformanceMode.entries.firstOrNull {
      it.name == obj.optString("performanceMode")
    } ?: com.example.data.PerformanceMode.BALANCED,
    performanceOverlay = obj.optBoolean("performanceOverlay", false),
  )

  private fun serializeSnapshot(s: GuestSaveSnapshot): JSONObject = JSONObject().apply {
    put("schemaVersion", s.schemaVersion)
    put("guestPlayerId", s.guestPlayerId)
    put("lastSavedEpochSec", s.lastSavedEpochSec)
    put("settings", serializeSettings(s.settings))

    val p = s.profile
    put(
      "profile",
      JSONObject().apply {
        put("id", p.id.ifBlank { s.guestPlayerId })
        put("userId", p.userId.ifBlank { s.guestPlayerId })
        put("masterName", p.masterName)
        put("masterLevel", p.masterLevel)
        put("masterExp", p.masterExp)
        put("gold", p.gold)
        put("manaCrystals", p.manaCrystals)
        put("soulShards", p.soulShards)
        put("rations", p.rations)
        put("ironOre", p.ironOre)
        put("arcaneEssence", p.arcaneEssence)
        put("highestClearedFloor", p.highestClearedFloor)
        put("totalExpeditions", p.totalExpeditions)
        put("unlockedFacilities", JSONArray(p.unlockedFacilities))
        put("storyFlags", JSONArray(p.storyFlags))
      },
    )

    val heroesArr = JSONArray()
    s.heroes.forEach { h ->
      heroesArr.put(
        JSONObject().apply {
          put("id", h.id)
          put("userId", h.userId.ifBlank { s.guestPlayerId })
          put("name", h.name)
          put("title", h.title)
          put("heroClass", h.heroClass)
          put("rarity", h.rarity)
          put("level", h.level)
          put("exp", h.exp)
          put("maxHp", h.maxHp)
          put("attack", h.attack)
          put("defense", h.defense)
          put("speed", h.speed)
          put("skillLevel", h.skillLevel)
          put("personality", h.personality)
          put("status", h.status)
          put("injuryRecoveryUntilEpochSec", h.injuryRecoveryUntilEpochSec)
          put("morale", h.morale)
          put("equippedWeaponId", h.equippedWeaponId)
          put("equippedArmorId", h.equippedArmorId)
          put("inActiveSquad", h.inActiveSquad)
          put("bondSummary", h.bondSummary)
          put("currentHp", h.currentHp)
          put("sanityState", h.sanityState)
          put("currentAssignment", h.currentAssignment)
          put("partyJoinedOrder", h.partyJoinedOrder)
          put("restStartedAtEpochSec", h.restStartedAtEpochSec)
          put("hiddenTrait", JSONObject().apply {
            put("id", h.hiddenTrait.id); put("name", h.hiddenTrait.name)
            put("minimumLevel", h.hiddenTrait.minimumLevel); put("minimumMorale", h.hiddenTrait.minimumMorale)
            put("requiresSelfAwareness", h.hiddenTrait.requiresSelfAwareness); put("unlocked", h.hiddenTrait.unlocked)
          })
        },
      )
    }
    put("heroes", heroesArr)

    val equipArr = JSONArray()
    s.equipment.forEach { eq ->
      equipArr.put(
        JSONObject().apply {
          put("id", eq.id)
          put("userId", eq.userId.ifBlank { s.guestPlayerId })
          put("name", eq.name)
          put("slot", eq.slot)
          put("rarity", eq.rarity)
          put("level", eq.level)
          put("bonusHp", eq.bonusHp)
          put("bonusAttack", eq.bonusAttack)
          put("bonusDefense", eq.bonusDefense)
          put("bonusSpeed", eq.bonusSpeed)
          put("iconType", eq.iconType)
          put("equippedByHeroId", eq.equippedByHeroId)
        },
      )
    }
    put("equipment", equipArr)
  }

  private fun deserializeAndMigrateSnapshot(root: JSONObject): GuestSaveSnapshot {
    val version = root.optInt("schemaVersion", 1)
    val guestId = root.optString("guestPlayerId", "").takeIf { it.isNotBlank() }
      ?: throw IllegalArgumentException("Missing guestPlayerId")
    val pObj = root.getJSONObject("profile")

    val unlocked = mutableListOf<String>()
    val uArr = pObj.optJSONArray("unlockedFacilities")
    if (uArr != null) {
      for (i in 0 until uArr.length()) unlocked += uArr.optString(i)
    }
    if (unlocked.isEmpty()) {
      unlocked.addAll(FacilityType.entries.map { it.name })
    } else if (version < 2) {
      // Schema v1 -> v2 migration: ensure all 7 Sanctuary facilities are present
      FacilityType.entries.forEach { fac ->
        if (fac.name !in unlocked) unlocked += fac.name
      }
    }

    val flags = mutableListOf<String>()
    val fArr = pObj.optJSONArray("storyFlags")
    if (fArr != null) {
      for (i in 0 until fArr.length()) {
        val fl = fArr.optString(i)
        if (fl.isNotBlank()) flags += fl
      }
    }

    val profile = MasterProfile(
      id = pObj.optString("id", guestId),
      userId = guestId,
      masterName = pObj.optString("masterName", "Master").ifBlank { "Master" }.take(40),
      masterLevel = pObj.optLong("masterLevel", 1L).coerceIn(1L, 100L),
      masterExp = pObj.optLong("masterExp", 0L).coerceIn(0L, 10000000L),
      gold = pObj.optLong("gold", 650L).coerceIn(0L, 99999999L),
      manaCrystals = pObj.optLong("manaCrystals", 160L).coerceIn(0L, 9999999L),
      soulShards = pObj.optLong("soulShards", 45L).coerceIn(0L, 9999999L),
      rations = pObj.optLong("rations", 12L).coerceIn(0L, 99999L),
      ironOre = pObj.optLong("ironOre", 8L).coerceIn(0L, 99999L),
      arcaneEssence = pObj.optLong("arcaneEssence", 6L).coerceIn(0L, 99999L),
      highestClearedFloor = pObj.optLong("highestClearedFloor", 0L).coerceIn(0L, 100L),
      totalExpeditions = pObj.optLong("totalExpeditions", 0L).coerceIn(0L, 999999L),
      unlockedFacilities = unlocked.distinct().take(10),
      storyFlags = SanctuaryProgressionHelper.compactStoryFlags(flags),
    )

    val heroes = mutableListOf<HeroRecord>()
    val hArr = root.optJSONArray("heroes")
    if (hArr != null) {
      for (i in 0 until hArr.length()) {
        val h = hArr.getJSONObject(i)
        heroes += HeroRecord(
          id = h.optString("id", "hero_$i"),
          userId = guestId,
          name = h.optString("name", "Anh Hùng").take(50),
          title = h.optString("title", "Chiến Binh").take(80),
          heroClass = HeroClassType.fromRaw(h.optString("heroClass", "KNIGHT")).name,
          rarity = h.optLong("rarity", 3L).coerceIn(1L, 5L),
          level = h.optLong("level", 1L).coerceIn(1L, 100L),
          exp = h.optLong("exp", 0L).coerceIn(0L, 10000000L),
          maxHp = h.optLong("maxHp", 350L).coerceIn(50L, 100000L),
          attack = h.optLong("attack", 35L).coerceIn(5L, 50000L),
          defense = h.optLong("defense", 20L).coerceIn(0L, 50000L),
          speed = h.optLong("speed", 100L).coerceIn(20L, 500L),
          skillLevel = h.optLong("skillLevel", 1L).coerceIn(1L, 20L),
          personality = h.optString("personality", "STOIC").let { raw ->
            if (com.example.data.model.LobbyPersonality.entries.any { it.name == raw }) raw else PersonalityType.fromRaw(raw).name
          },
          status = HeroStatusType.fromRaw(h.optString("status", "READY")).name,
          injuryRecoveryUntilEpochSec = h.optLong("injuryRecoveryUntilEpochSec", 0L).coerceAtLeast(0L),
          morale = h.optLong("morale", 100L).coerceIn(0L, 100L),
          equippedWeaponId = h.optString("equippedWeaponId", ""),
          equippedArmorId = h.optString("equippedArmorId", ""),
          inActiveSquad = h.optBoolean("inActiveSquad", false),
          bondSummary = h.optString("bondSummary", "Chiến đấu vì Thánh Địa.").take(200),
          currentHp = h.optLong("currentHp", -1L).coerceIn(-1L, h.optLong("maxHp", 350L).coerceIn(50L, 100000L)),
          sanityState = com.example.data.model.HeroSanityState.fromRaw(h.optString("sanityState")).name,
          currentAssignment = h.optString("currentAssignment").takeIf { raw -> com.example.data.model.HeroAssignment.entries.any { it.name == raw } }.orEmpty(),
          partyJoinedOrder = h.optLong("partyJoinedOrder", 0L).coerceAtLeast(0L),
          restStartedAtEpochSec = h.optLong("restStartedAtEpochSec", 0L).coerceAtLeast(0L),
          hiddenTrait = h.optJSONObject("hiddenTrait")?.let { t -> com.example.data.model.HeroHiddenTrait(
            id = t.optString("id", "unyielding_soul").take(80), name = t.optString("name", "Linh hồn bất khuất").take(100),
            minimumLevel = t.optInt("minimumLevel", 10).coerceIn(1, 100), minimumMorale = t.optInt("minimumMorale", 60).coerceIn(0, 100),
            requiresSelfAwareness = t.optBoolean("requiresSelfAwareness"), unlocked = t.optBoolean("unlocked")) } ?: com.example.data.model.HeroHiddenTrait(),
        )
      }
    }
    require(heroes.isNotEmpty()) { "Save file contains zero heroes" }

    val equipment = mutableListOf<EquipmentRecord>()
    val eArr = root.optJSONArray("equipment")
    if (eArr != null) {
      for (i in 0 until eArr.length()) {
        val eq = eArr.getJSONObject(i)
        equipment += EquipmentRecord(
          id = eq.optString("id", "eq_$i"),
          userId = guestId,
          name = eq.optString("name", "Binh Khí").take(60),
          slot = if (eq.optString("slot") == "ARMOR") "ARMOR" else "WEAPON",
          rarity = eq.optLong("rarity", 2L).coerceIn(1L, 5L),
          level = eq.optLong("level", 1L).coerceIn(1L, 50L),
          bonusHp = eq.optLong("bonusHp", 0L).coerceIn(0L, 50000L),
          bonusAttack = eq.optLong("bonusAttack", 0L).coerceIn(0L, 10000L),
          bonusDefense = eq.optLong("bonusDefense", 0L).coerceIn(0L, 10000L),
          bonusSpeed = eq.optLong("bonusSpeed", 0L).coerceIn(0L, 200L),
          iconType = eq.optString("iconType", "LONGSWORD").take(40),
          equippedByHeroId = eq.optString("equippedByHeroId", ""),
        )
      }
    }

    val settings = root.optJSONObject("settings")?.let(::parseSettings) ?: GameSettings()

    return GuestSaveSnapshot(
      schemaVersion = GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION,
      guestPlayerId = guestId,
      profile = profile,
      heroes = heroes,
      equipment = equipment,
      settings = settings,
      lastSavedEpochSec = root.optLong("lastSavedEpochSec", System.currentTimeMillis() / 1000L),
    )
  }

  // Test-only helper to inspect save file paths in unit tests
  internal fun primaryFileForTest(): File = primarySaveFile
  internal fun backupFileForTest(): File = backupSaveFile

  companion object {
    private const val TAG = "LocalGuestSave"
  }
}

/**
 * Full offline-first SummonerDataSource backed by LocalGuestSaveManager.
 */
class LocalGuestDataSource(
  private val saveManager: LocalGuestSaveManager,
) : SummonerDataSource {

  override val isGuestMode: Boolean = true

  override fun observeMasterProfile(userId: String): Flow<MasterProfile?> =
    saveManager.guestSnapshot.map { it?.profile }

  override fun observeHeroes(userId: String): Flow<List<HeroRecord>> =
    saveManager.guestSnapshot.map { it?.heroes ?: emptyList() }

  override fun observeEquipment(userId: String): Flow<List<EquipmentRecord>> =
    saveManager.guestSnapshot.map { it?.equipment ?: emptyList() }

  override suspend fun bootstrapMasterIfNeeded(preferredName: String) {
    saveManager.startOrResumeGuestSession(preferredName)
  }

  override suspend fun summonNewHero(
    profile: MasterProfile,
    existingHeroes: List<HeroRecord>,
  ): HeroRecord {
    val shardCost = 20L
    val manaCost = 40L
    require(profile.soulShards >= shardCost && profile.manaCrystals >= manaCost) {
      "Không đủ Mảnh Linh Hồn (20) hoặc Tinh Thể Mana (40)."
    }

    val ownedNames = existingHeroes.map { it.name }.toSet()
    val candidate = SummonerRepository.SUMMON_POOL.filterNot { it.name in ownedNames }.randomOrNull()
      ?: SummonerRepository.SUMMON_POOL.random().let { base ->
        val count = existingHeroes.count { it.name.startsWith(base.name) } + 2
        val suffix = when (count) {
          2 -> "II"
          3 -> "III"
          4 -> "IV"
          5 -> "V"
          else -> "$count"
        }
        base.copy(name = "${base.name} $suffix")
      }

    val heroId = "hero_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    lateinit var createdHero: HeroRecord
    saveManager.mutateSnapshot { snap ->
      require(snap.profile.soulShards >= shardCost && snap.profile.manaCrystals >= manaCost) { "Không đủ tài nguyên triệu hồi." }
      val squadAutoJoin = snap.heroes.count { it.inActiveSquad } < 4
      createdHero = HeroRecord(
        id = heroId,
        userId = snap.guestPlayerId,
        name = candidate.name,
        title = candidate.title,
        heroClass = candidate.heroClass.name,
        rarity = candidate.rarity,
        level = 1L,
        exp = 0L,
        maxHp = candidate.hp,
        attack = candidate.atk,
        defense = candidate.def,
        speed = candidate.spd,
        skillLevel = 1L,
        personality = candidate.personality.name,
        status = HeroStatusType.READY.name,
        injuryRecoveryUntilEpochSec = 0L,
        morale = 100L,
        inActiveSquad = squadAutoJoin,
        partyJoinedOrder = if (squadAutoJoin) ((snap.heroes.maxOfOrNull { it.partyJoinedOrder } ?: 0L).coerceAtMost(Long.MAX_VALUE-1) + 1L) else 0L,
        bondSummary = candidate.bond,
      )
      snap.copy(
        profile = snap.profile.copy(
          soulShards = (snap.profile.soulShards - shardCost).coerceAtLeast(0L),
          manaCrystals = (snap.profile.manaCrystals - manaCost).coerceAtLeast(0L),
        ),
        heroes = snap.heroes + createdHero,
      )
    }
    return createdHero
  }

  override suspend fun trainHeroWithFocus(
    profile: MasterProfile,
    hero: HeroRecord,
    focus: TrainingFocus,
  ) {
    saveManager.mutateSnapshot { snap ->
      val curProfile = snap.profile
      val target = snap.heroes.firstOrNull { it.id == hero.id } ?: return@mutateSnapshot snap
      require(!target.isCurrentlyInjured()) {
        "${target.name} đang dưỡng thương! Hãy hồi phục tại Khu Nghỉ Ngơi trước khi huấn luyện."
      }
      if (focus.isSkillUpgrade) {
        val manaCost = 25L + target.skillLevel * 10L
        val essenceCost = 1L
        require(curProfile.manaCrystals >= manaCost && curProfile.arcaneEssence >= essenceCost) {
          "Cần $manaCost Mana và $essenceCost Tinh Hoa Cổ Ngữ để nâng kỹ năng."
        }
        require(target.skillLevel < 20L) { "Kỹ năng đã đạt cấp tối đa." }
        snap.copy(
          profile = curProfile.copy(
            manaCrystals = curProfile.manaCrystals - manaCost,
            arcaneEssence = curProfile.arcaneEssence - essenceCost,
          ),
          heroes = snap.heroes.map {
            if (it.id == target.id) {
              it.copy(
                skillLevel = (it.skillLevel + 1L).coerceAtMost(20L),
                status = HeroStatusType.TRAINING.name,
              )
            } else {
              it
            }
          },
        )
      } else {
        val goldCost = 80L + target.level * 35L
        require(curProfile.gold >= goldCost) { "Cần $goldCost Vàng để huấn luyện." }
        require(target.level < 100L) { "Anh hùng đã đạt cấp tối đa." }
        val baseHpGain = if (target.classType == HeroClassType.KNIGHT || target.classType == HeroClassType.BERSERKER) 36L else 24L
        val baseAtkGain = if (target.classType == HeroClassType.MAGE || target.classType == HeroClassType.ARCHER || target.classType == HeroClassType.ASSASSIN) 8L else 5L
        val baseDefGain = if (target.classType == HeroClassType.KNIGHT) 4L else 2L
        val baseSpdGain = if (target.classType == HeroClassType.ASSASSIN || target.classType == HeroClassType.ARCHER) 2L else 1L

        val hpGain = baseHpGain + focus.hpBonus
        val atkGain = baseAtkGain + focus.atkBonus
        val defGain = baseDefGain + focus.defBonus
        val spdGain = baseSpdGain + focus.spdBonus

        snap.copy(
          profile = curProfile.copy(gold = curProfile.gold - goldCost),
          heroes = snap.heroes.map {
            if (it.id == target.id) {
              it.copy(
                level = (it.level + 1L).coerceAtMost(100L),
                maxHp = (it.maxHp + hpGain).coerceAtMost(100000L),
                currentHp = if (it.currentHp < 0L) -1L else (it.currentHp + hpGain).coerceAtMost(100000L),
                attack = (it.attack + atkGain).coerceAtMost(50000L),
                defense = (it.defense + defGain).coerceAtMost(50000L),
                speed = (it.speed + spdGain).coerceAtMost(500L),
                status = HeroStatusType.TRAINING.name,
              )
            } else {
              it
            }
          },
        )
      }
    }
  }

  override suspend fun equipItemToHero(
    hero: HeroRecord,
    item: EquipmentRecord,
    allHeroes: List<HeroRecord>,
    allEquipment: List<EquipmentRecord>,
  ) {
    saveManager.mutateSnapshot { snap ->
      val isWeapon = item.slot == "WEAPON"
      val prevItemId = if (isWeapon) hero.equippedWeaponId else hero.equippedArmorId

      val nextHeroes = snap.heroes.map { h ->
        when {
          h.id == hero.id -> if (isWeapon) h.copy(equippedWeaponId = item.id) else h.copy(equippedArmorId = item.id)
          item.equippedByHeroId.isNotBlank() && h.id == item.equippedByHeroId ->
            if (isWeapon) h.copy(equippedWeaponId = "") else h.copy(equippedArmorId = "")
          else -> h
        }
      }

      val nextEquip = snap.equipment.map { eq ->
        when {
          eq.id == item.id -> eq.copy(equippedByHeroId = hero.id)
          prevItemId.isNotBlank() && eq.id == prevItemId -> eq.copy(equippedByHeroId = "")
          else -> eq
        }
      }

      snap.copy(heroes = nextHeroes, equipment = nextEquip)
    }
  }

  override suspend fun unequipItemFromHero(
    hero: HeroRecord,
    slot: String,
    allEquipment: List<EquipmentRecord>,
  ) {
    saveManager.mutateSnapshot { snap ->
      val isWeapon = slot != "ARMOR"
      val itemId = if (isWeapon) hero.equippedWeaponId else hero.equippedArmorId
      if (itemId.isBlank()) return@mutateSnapshot snap

      val nextHeroes = snap.heroes.map { h ->
        if (h.id == hero.id) {
          if (isWeapon) h.copy(equippedWeaponId = "") else h.copy(equippedArmorId = "")
        } else {
          h
        }
      }
      val nextEquip = snap.equipment.map { eq ->
        if (eq.id == itemId) eq.copy(equippedByHeroId = "") else eq
      }
      snap.copy(heroes = nextHeroes, equipment = nextEquip)
    }
  }

  override suspend fun assignHeroActivity(
    hero: HeroRecord,
    targetStatus: HeroStatusType,
  ) {
    val nowSec = System.currentTimeMillis() / 1000L
    require(!hero.isCurrentlyInjured(nowSec)) {
      "${hero.name} đang bị thương! Cần hoàn tất trị thương tại Khu Nghỉ Ngơi trước."
    }
    require(targetStatus != HeroStatusType.INJURED) {
      "Không thể gán trạng thái chấn thương thủ công."
    }
    saveManager.mutateSnapshot { snap ->
      snap.copy(
        heroes = snap.heroes.map { h ->
          if (h.id == hero.id) {
            val moraleBoost = if (targetStatus == HeroStatusType.RESTING) 8L else 0L
            h.copy(
              status = targetStatus.name,
              currentAssignment = if (targetStatus == HeroStatusType.RESTING) "RESTING" else if (h.inActiveSquad) "IN_PARTY" else "UNASSIGNED",
              inActiveSquad = h.inActiveSquad && targetStatus != HeroStatusType.RESTING,
              restStartedAtEpochSec = if (targetStatus == HeroStatusType.RESTING) nowSec else 0L,
              currentHp = com.example.data.model.HeroData.fromRecord(h, nowSec).currentHP.toLong(),
              morale = (h.morale + moraleBoost).coerceIn(0L, 100L),
            )
          } else {
            h
          }
        },
      )
    }
  }

  override suspend fun synthesizeEquipment(profile: MasterProfile): EquipmentRecord {
    val oreCost = 3L
    val essenceCost = 2L
    val goldCost = LostCourierProgression.synthesisGoldCost(profile.storyFlags)
    require(
      profile.ironOre >= oreCost &&
        profile.arcaneEssence >= essenceCost &&
        profile.gold >= goldCost
    ) {
      "Cần 3 Quặng Sắt, 2 Tinh Hoa Cổ Ngữ và $goldCost Vàng để chế tác."
    }

    val roll = Random.nextInt(100)
    val rarity = when {
      roll >= 82 -> 4L
      roll >= 40 -> 3L
      else -> 2L
    }
    val bp = SummonerRepository.SYNTHESIS_POOL.random()
    val itemId = "eq_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    lateinit var crafted: EquipmentRecord
    saveManager.mutateSnapshot { snap ->
      val currentGoldCost = LostCourierProgression.synthesisGoldCost(snap.profile.storyFlags)
      require(snap.profile.ironOre >= oreCost && snap.profile.arcaneEssence >= essenceCost && snap.profile.gold >= currentGoldCost) {
        "Không đủ nguyên liệu chế tác."
      }
      crafted = EquipmentRecord(
        id = itemId,
        userId = snap.guestPlayerId,
        name = "${bp.nameVi} +$rarity",
        slot = bp.slot,
        rarity = rarity,
        level = 1L,
        bonusHp = bp.baseHp * rarity,
        bonusAttack = bp.baseAtk * rarity,
        bonusDefense = bp.baseDef * rarity,
        bonusSpeed = bp.baseSpd * rarity,
        iconType = bp.iconType,
        equippedByHeroId = "",
      )
      snap.copy(
        profile = snap.profile.copy(
          ironOre = snap.profile.ironOre - oreCost,
          arcaneEssence = snap.profile.arcaneEssence - essenceCost,
          gold = snap.profile.gold - currentGoldCost,
        ),
        equipment = snap.equipment + crafted,
      )
    }
    return crafted
  }

  override suspend fun enhanceEquipment(profile: MasterProfile, item: EquipmentRecord) {
    val goldCost = 90L + item.level * 45L
    val oreCost = 1L
    require(profile.gold >= goldCost && profile.ironOre >= oreCost) {
      "Cần $goldCost Vàng và 1 Quặng Sắt để cường hóa."
    }
    require(item.level < 50L) { "Trang bị đã đạt cấp tối đa." }

    saveManager.mutateSnapshot { snap ->
      snap.copy(
        profile = snap.profile.copy(
          gold = snap.profile.gold - goldCost,
          ironOre = snap.profile.ironOre - oreCost,
        ),
        equipment = snap.equipment.map { eq ->
          if (eq.id == item.id) {
            eq.copy(
              level = eq.level + 1L,
              bonusHp = (eq.bonusHp + if (eq.slot == "ARMOR") 22L else 8L).coerceAtMost(50000L),
              bonusAttack = (eq.bonusAttack + if (eq.slot == "WEAPON") 6L else 1L).coerceAtMost(10000L),
              bonusDefense = (eq.bonusDefense + if (eq.slot == "ARMOR") 5L else 1L).coerceAtMost(10000L),
            )
          } else {
            eq
          }
        },
      )
    }
  }

  override suspend fun recoverInjuredHero(
    profile: MasterProfile,
    hero: HeroRecord,
    instantWithResources: Boolean,
  ) {
    val nowSec = System.currentTimeMillis() / 1000L
    saveManager.mutateSnapshot { snap ->
      var nextProfile = snap.profile
      if (instantWithResources && hero.remainingInjurySeconds(nowSec) > 0L) {
        val rationCost = 2L
        val manaCost = 15L
        require(nextProfile.rations >= rationCost && nextProfile.manaCrystals >= manaCost) {
          "Cần 2 Khẩu Phần và 15 Tinh Thể Mana để trị thương tức thì."
        }
        nextProfile = nextProfile.copy(
          rations = nextProfile.rations - rationCost,
          manaCrystals = nextProfile.manaCrystals - manaCost,
        )
      }
      snap.copy(
        profile = nextProfile,
        heroes = snap.heroes.map { h ->
          if (h.id == hero.id) {
            h.copy(
              status = HeroStatusType.READY.name,
              currentHp = h.maxHp,
              currentAssignment = if (h.inActiveSquad) "IN_PARTY" else "UNASSIGNED",
              restStartedAtEpochSec = 0L,
              injuryRecoveryUntilEpochSec = 0L,
              morale = (h.morale + 15L).coerceAtMost(100L),
            )
          } else {
            h
          }
        },
      )
    }
  }

  override suspend fun hostSanctuaryMealWithRecipe(
    profile: MasterProfile,
    heroes: List<HeroRecord>,
    recipe: SanctuaryMealRecipe,
  ) {
    require(profile.rations >= recipe.rationCost && profile.gold >= recipe.goldCost) {
      "Cần ${recipe.rationCost} Khẩu Phần và ${recipe.goldCost} Vàng để phục vụ [${recipe.titleVi}]."
    }
    val nowSec = System.currentTimeMillis() / 1000L
    val feastUntil = nowSec + recipe.durationSec
    saveManager.mutateSnapshot { snap ->
      snap.copy(
        profile = snap.profile.copy(
          rations = snap.profile.rations - recipe.rationCost,
          gold = snap.profile.gold - recipe.goldCost,
          storyFlags = FeastBuffHelper.buildUpdatedStoryFlagsWithFeast(
            existingFlags = snap.profile.storyFlags,
            untilEpochSec = feastUntil,
            recipe = recipe,
          ),
        ),
        heroes = snap.heroes.map { h ->
          val trimmedRecovery = if (h.isCurrentlyInjured(nowSec)) {
            (h.injuryRecoveryUntilEpochSec - recipe.recoveryTrimSec).coerceAtLeast(nowSec)
          } else {
            h.injuryRecoveryUntilEpochSec
          }
          val healedStatus = if (h.status == HeroStatusType.INJURED.name && trimmedRecovery <= nowSec) {
            HeroStatusType.READY.name
          } else {
            h.status
          }
          h.copy(
            morale = (h.morale + recipe.moraleGain).coerceIn(0L, 100L),
            injuryRecoveryUntilEpochSec = if (healedStatus == HeroStatusType.READY.name) 0L else trimmedRecovery,
            status = healedStatus,
          )
        },
      )
    }
  }

  override suspend fun unlockIslandExpansion(
    profile: MasterProfile,
    milestone: IslandExpansionMilestone,
  ) {
    require(!SanctuaryProgressionHelper.isExpansionUnlocked(profile.storyFlags, milestone)) {
      "[${milestone.titleVi}] đã được mở khóa trước đó."
    }
    require(SanctuaryProgressionHelper.canMeetRequirements(profile, milestone)) {
      "Cần đạt Master Lv.${milestone.reqMasterLevel} và vượt Tầng ${milestone.reqClearedFloor} để mở khóa [${milestone.titleVi}]."
    }
    require(SanctuaryProgressionHelper.canAffordExpansion(profile, milestone)) {
      "Cần ${milestone.goldCost} Vàng, ${milestone.oreCost} Quặng Sắt và ${milestone.essenceCost} Tinh Hoa Cổ Ngữ."
    }
    saveManager.mutateSnapshot { snap ->
      val cur = snap.profile
      val nextFlags = SanctuaryProgressionHelper.compactStoryFlags(cur.storyFlags + milestone.flagKey)
      snap.copy(
        profile = cur.copy(
          gold = (cur.gold - milestone.goldCost).coerceAtLeast(0L),
          ironOre = (cur.ironOre - milestone.oreCost).coerceAtLeast(0L),
          arcaneEssence = (cur.arcaneEssence - milestone.essenceCost).coerceAtLeast(0L),
          storyFlags = nextFlags,
        ),
      )
    }
  }

  override suspend fun assignLobbyHero(heroId: String, assignment: com.example.data.model.HeroAssignment) {
    saveManager.mutateSnapshot { snap ->
      snap.copy(heroes = com.example.data.LobbyHeroAssignments.assign(snap.heroes, heroId, assignment, System.currentTimeMillis() / 1000L))
    }
  }

  override suspend fun toggleHeroInSquad(hero: HeroRecord, currentSquadSize: Int) {
    saveManager.mutateSnapshot { snap ->
      val current = requireNotNull(snap.heroes.find { it.id == hero.id })
      val assignment = if (current.inActiveSquad) com.example.data.model.HeroAssignment.UNASSIGNED else com.example.data.model.HeroAssignment.IN_PARTY
      snap.copy(heroes = com.example.data.LobbyHeroAssignments.assign(snap.heroes, hero.id, assignment,
        System.currentTimeMillis() / 1000L, replaceWhenFull = false))
    }
  }

  override suspend fun markStoryFlag(profile: MasterProfile, flag: String) {
    if (flag in profile.storyFlags) return
    saveManager.mutateSnapshot { snap ->
      val updated = SanctuaryProgressionHelper.compactStoryFlags(snap.profile.storyFlags + flag)
      snap.copy(profile = snap.profile.copy(storyFlags = updated))
    }
  }

  override suspend fun settleExpedition(
    profile: MasterProfile,
    squadHeroes: List<HeroRecord>,
    fallenHeroIds: Set<String>,
    floorNumber: Int,
    victory: Boolean,
    retreatUsed: Boolean,
    bonusGold: Long,
    bonusMana: Long,
    discoveredNodeIds: Set<String>,
    courierOutcome: CourierOutcome?,
    courierMemoryHeroId: String?,
  ): ExpeditionSettlementResult {
    val nowSec = System.currentTimeMillis() / 1000L
    val hasAstralDock = SanctuaryProgressionHelper.isExpansionUnlocked(
      profile.storyFlags,
      IslandExpansionMilestone.NORTH_ASTRAL_DOCK,
    )
    val hasEastGarden = SanctuaryProgressionHelper.isExpansionUnlocked(
      profile.storyFlags,
      IslandExpansionMilestone.EAST_CELESTIAL_GARDEN,
    )
    val dockMult = if (hasAstralDock) 1.20f else 1.0f

    val baseGold = when {
      victory -> 180L + floorNumber * 95L
      retreatUsed -> 60L + floorNumber * 20L
      else -> 35L + floorNumber * 15L
    }
    val baseMana = when {
      victory -> 45L + floorNumber * 20L
      retreatUsed -> 18L + floorNumber * 5L
      else -> 10L
    }
    val goldEarned = ((baseGold + bonusGold) * dockMult).toLong()
    val manaEarned = ((baseMana + bonusMana) * dockMult).toLong()
    val shardsEarned = when {
      victory -> 15L + floorNumber * 8L
      retreatUsed -> 5L + floorNumber * 2L
      else -> 3L
    }
    val oreEarned = if (victory) (2L + floorNumber) else 1L
    val essenceEarned = if (victory) (1L + floorNumber / 2L) else 0L
    val masterExpGain = when {
      victory -> 120L + floorNumber * 50L
      retreatUsed -> 45L
      else -> 25L
    }
    val recoveryDurationSec = if (hasEastGarden) 55L else 75L
    val moraleFloor = if (hasEastGarden) 30L else 15L

    val injuredNames = mutableListOf<String>()
    val leveledNames = mutableListOf<String>()
    var lootedName: String? = null
    val escortOutcome = courierOutcome?.let {
      if (floorNumber == 1 && victory && it == CourierOutcome.RESCUED) CourierOutcome.RESCUED else CourierOutcome.FAILED
    }
    var courierForgeUnlocked = false
    var courierMemoryHeroName: String? = null

    val outcomeType = when {
      victory -> ExpeditionOutcomeType.VICTORY
      retreatUsed -> ExpeditionOutcomeType.TACTICAL_RETREAT
      else -> ExpeditionOutcomeType.DEFEAT
    }

    saveManager.mutateSnapshot { snap ->
      val cur = snap.profile
      val totalMasterExp = cur.masterExp + masterExpGain
      val newMasterLevel = (1L + totalMasterExp / 250L).coerceAtMost(100L)
      val newHighestFloor = if (victory) maxOf(cur.highestClearedFloor, floorNumber.toLong()) else cur.highestClearedFloor

      val squadIds = squadHeroes.map { it.id }.toSet()
      courierForgeUnlocked = LostCourierProgression.isFirstRescue(cur.storyFlags, escortOutcome)
      val memoryHeroId = courierMemoryHeroId?.takeIf { it in squadIds }
        ?: snap.heroes.firstOrNull { it.id in squadIds }?.id
      val heroExpGain = when {
        victory -> 90L + floorNumber * 40L
        retreatUsed -> 40L
        else -> 25L
      }

      val nextHeroes = snap.heroes.map { hero ->
        if (hero.id !in squadIds) {
          hero
        } else {
          val totalHeroExp = hero.exp + heroExpGain
          val expNeeded = hero.level * 100L
          val didLevelUp = victory && totalHeroExp >= expNeeded && hero.level < 100L
          val nextLevel = if (didLevelUp) hero.level + 1L else hero.level
          val remExp = if (didLevelUp) (totalHeroExp - expNeeded).coerceAtLeast(0L) else totalHeroExp
          if (didLevelUp) leveledNames += hero.name

          // Tactical Retreat protects surviving heroes from automatic injury!
          val wasKnockedOut = hero.id in fallenHeroIds || (!victory && !retreatUsed)
          val nextStatus = if (wasKnockedOut) HeroStatusType.INJURED.name else HeroStatusType.READY.name
          val recoveryUntil = if (wasKnockedOut) (nowSec + recoveryDurationSec) else 0L
          if (wasKnockedOut) injuredNames += hero.name

          val nextMorale = when {
            victory -> (hero.morale - 6L).coerceIn(maxOf(20L, moraleFloor), 100L)
            retreatUsed -> (hero.morale - 10L).coerceIn(maxOf(25L, moraleFloor), 100L)
            else -> (hero.morale - 22L).coerceIn(moraleFloor, 100L)
          }

          hero.copy(
            level = nextLevel,
            exp = remExp.coerceAtMost(10000000L),
            maxHp = (hero.maxHp + if (didLevelUp) 32L else 0L).coerceAtMost(100000L),
            attack = (hero.attack + if (didLevelUp) 7L else 0L).coerceAtMost(50000L),
            defense = (hero.defense + if (didLevelUp) 4L else 0L).coerceAtMost(50000L),
            speed = (hero.speed + if (didLevelUp) 2L else 0L).coerceAtMost(500L),
            status = nextStatus,
            injuryRecoveryUntilEpochSec = recoveryUntil,
            morale = nextMorale,
            inActiveSquad = if (wasKnockedOut) false else hero.inActiveSquad,
            currentHp = if (wasKnockedOut) hero.maxHp / 4L else if (hero.currentHp < 0L) -1L else (hero.currentHp + if (didLevelUp) 32L else 0L).coerceAtMost(100000L),
            currentAssignment = if (wasKnockedOut) "RESTING" else if (hero.inActiveSquad) "IN_PARTY" else "UNASSIGNED",
            restStartedAtEpochSec = if (wasKnockedOut) nowSec else 0L,
            bondSummary = if (courierForgeUnlocked && hero.id == memoryHeroId) {
              courierMemoryHeroName = hero.name
              LostCourierProgression.rememberRescue(hero.bondSummary)
            } else hero.bondSummary,
          )
        }
      }

      val nextEquipment = snap.equipment.toMutableList()
      if (victory) {
        val dropBp = SummonerRepository.SYNTHESIS_POOL[(floorNumber - 1) % SummonerRepository.SYNTHESIS_POOL.size]
        val dropRarity = (2L + floorNumber / 2L).coerceIn(2L, 5L)
        val dropId = "eq_drop_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        lootedName = "${dropBp.nameVi} Tầng $floorNumber"
        nextEquipment += EquipmentRecord(
          id = dropId,
          userId = snap.guestPlayerId,
          name = lootedName!!,
          slot = dropBp.slot,
          rarity = dropRarity,
          level = 1L,
          bonusHp = dropBp.baseHp * dropRarity,
          bonusAttack = dropBp.baseAtk * dropRarity,
          bonusDefense = dropBp.baseDef * dropRarity,
          bonusSpeed = dropBp.baseSpd * dropRarity,
          iconType = dropBp.iconType,
          equippedByHeroId = "",
        )
      }

      val nodeFlags = discoveredNodeIds.map { "DISC_${it.take(36)}" }
      val nextFlags = SanctuaryProgressionHelper.compactStoryFlags(
        LostCourierProgression.outcomeFlags(cur.storyFlags, escortOutcome) + nodeFlags,
      )

      snap.copy(
        profile = cur.copy(
          masterLevel = newMasterLevel,
          masterExp = totalMasterExp.coerceAtMost(10000000L),
          gold = (cur.gold + goldEarned).coerceAtMost(99999999L),
          manaCrystals = (cur.manaCrystals + manaEarned).coerceAtMost(9999999L),
          soulShards = (cur.soulShards + shardsEarned).coerceAtMost(9999999L),
          rations = (cur.rations + if (victory) 2L else 0L).coerceAtMost(99999L),
          ironOre = (cur.ironOre + oreEarned).coerceAtMost(99999L),
          arcaneEssence = (cur.arcaneEssence + essenceEarned).coerceAtMost(99999L),
          highestClearedFloor = newHighestFloor,
          totalExpeditions = cur.totalExpeditions + 1L,
          storyFlags = nextFlags,
        ),
        heroes = nextHeroes,
        equipment = nextEquipment,
      )
    }

    return ExpeditionSettlementResult(
      victory = victory,
      floorNumber = floorNumber,
      goldEarned = goldEarned,
      manaEarned = manaEarned,
      shardsEarned = shardsEarned,
      oreEarned = oreEarned,
      essenceEarned = essenceEarned,
      injuredHeroNames = injuredNames,
      leveledUpHeroNames = leveledNames,
      lootedItemName = lootedName,
      outcomeType = outcomeType,
      bonusDiscoveryGold = bonusGold,
      bonusDiscoveryMana = bonusMana,
      discoveredNodesCount = discoveredNodeIds.size,
      courierOutcome = escortOutcome,
      courierForgeUnlocked = courierForgeUnlocked,
      courierMemoryHeroName = courierMemoryHeroName,
    )
  }
}
