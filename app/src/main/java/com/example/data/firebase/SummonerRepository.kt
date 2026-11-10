package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.FeastBuffHelper
import com.example.data.GuestSaveSnapshot
import com.example.data.LostCourierProgression
import com.example.data.SanctuaryProgressionHelper
import com.example.data.SummonerDataSource
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class SummonableTemplate(
  val name: String,
  val title: String,
  val heroClass: HeroClassType,
  val rarity: Long,
  val hp: Long,
  val atk: Long,
  val def: Long,
  val spd: Long,
  val personality: PersonalityType,
  val bond: String,
)

enum class ExpeditionOutcomeType(val titleVi: String) {
  VICTORY("CHIẾN THẮNG HOÀN HẢO"),
  TACTICAL_RETREAT("RÚT LUI CHIẾN THUẬT AN TOÀN"),
  DEFEAT("NHIỆM VỤ THẤT BẠI — ĐỘI HÌNH BỊ THƯƠNG"),
}

data class ExpeditionSettlementResult(
  val victory: Boolean,
  val floorNumber: Int,
  val goldEarned: Long,
  val manaEarned: Long,
  val shardsEarned: Long,
  val oreEarned: Long,
  val essenceEarned: Long,
  val injuredHeroNames: List<String>,
  val leveledUpHeroNames: List<String>,
  val lootedItemName: String?,
  val outcomeType: ExpeditionOutcomeType = if (victory) ExpeditionOutcomeType.VICTORY else ExpeditionOutcomeType.DEFEAT,
  val bonusDiscoveryGold: Long = 0L,
  val bonusDiscoveryMana: Long = 0L,
  val discoveredNodesCount: Int = 0,
  val courierOutcome: CourierOutcome? = null,
  val courierForgeUnlocked: Boolean = false,
  val courierMemoryHeroName: String? = null,
)

class SummonerRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) : SummonerDataSource {
  override val isGuestMode: Boolean = false

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Người chơi cần đăng nhập Google trước khi lưu dữ liệu.")
  }

  override fun observeMasterProfile(userId: String): Flow<MasterProfile?> {
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(MasterProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  override fun observeHeroes(userId: String): Flow<List<HeroRecord>> {
    val path = "users/$userId/heroes"
    return db.collection("users").document(userId).collection("heroes")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(HeroRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  override fun observeEquipment(userId: String): Flow<List<EquipmentRecord>> {
    val path = "users/$userId/equipment"
    return db.collection("users").document(userId).collection("equipment")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(
          EquipmentRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun getHeroById(ownerUid: String, heroId: String): HeroRecord? {
    val docRef = db.collection("users").document(ownerUid).collection("heroes").document(heroId)
    return try {
      val snap = docRef.get().await()
      snap.toObject(HeroRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  override suspend fun bootstrapMasterIfNeeded(preferredName: String) {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    try {
      val existing = userRef.get().await()
      if (existing.exists()) return

      val cleanName = preferredName.trim()
        .replace(Regex("[^\\p{L}\\p{N} _\\-]"), "")
        .take(28)
        .ifBlank { "Master" }

      val batch = db.batch()
      val masterPayload = mapOf(
        "userId" to uid,
        "masterName" to cleanName,
        "masterLevel" to 1L,
        "masterExp" to 0L,
        "gold" to 650L,
        "manaCrystals" to 160L,
        "soulShards" to 45L,
        "rations" to 12L,
        "ironOre" to 8L,
        "arcaneEssence" to 6L,
        "highestClearedFloor" to 0L,
        "totalExpeditions" to 0L,
        "unlockedFacilities" to listOf(
          FacilityType.TOWER_GATE.name,
          FacilityType.SUMMONING_HALL.name,
          FacilityType.TRAINING_GROUND.name,
          FacilityType.ARMORY.name,
          FacilityType.HERO_QUARTERS.name,
          FacilityType.SYNTHESIS_LAB.name,
          FacilityType.RESTAURANT.name,
        ),
        "storyFlags" to emptyList<String>(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      batch.set(userRef, masterPayload)

      val swordId = "eq_starter_sword"
      val bowId = "eq_starter_bow"
      val staffId = "eq_starter_staff"
      val armorId = "eq_starter_plate"

      val kaelenId = "hero_starter_kaelen"
      val lyraId = "hero_starter_lyra"
      val vesperId = "hero_starter_vesper"

      val heroesRef = userRef.collection("heroes")
      batch.set(
        heroesRef.document(kaelenId),
        heroMap(
          uid = uid,
          name = "Kaelen",
          title = "Khiên Thép Tiền Phong",
          heroClass = HeroClassType.KNIGHT.name,
          rarity = 3L,
          maxHp = 480L,
          attack = 42L,
          defense = 32L,
          speed = 94L,
          personality = PersonalityType.STOIC.name,
          weaponId = swordId,
          inSquad = true,
          bond = "Tin tưởng tuyệt đối vào mũi tên yểm trợ của Lyra.",
        ),
      )
      batch.set(
        heroesRef.document(lyraId),
        heroMap(
          uid = uid,
          name = "Lyra",
          title = "Mắt Ưng Gió Đêm",
          heroClass = HeroClassType.ARCHER.name,
          rarity = 3L,
          maxHp = 320L,
          attack = 54L,
          defense = 16L,
          speed = 118L,
          personality = PersonalityType.CYNICAL.name,
          weaponId = bowId,
          inSquad = true,
          bond = "Hay tranh luận chiến thuật với Vesper nhưng luôn bọc lót chuẩn xác.",
        ),
      )
      batch.set(
        heroesRef.document(vesperId),
        heroMap(
          uid = uid,
          name = "Vesper",
          title = "Học Giả Tinh Vân",
          heroClass = HeroClassType.MAGE.name,
          rarity = 3L,
          maxHp = 290L,
          attack = 60L,
          defense = 14L,
          speed = 102L,
          personality = PersonalityType.CALCULATING.name,
          weaponId = staffId,
          inSquad = true,
          bond = "Đánh giá cao sự vững chãi của Kaelen nơi tuyến đầu.",
        ),
      )

      val equipRef = userRef.collection("equipment")
      batch.set(
        equipRef.document(swordId),
        equipmentMap(
          uid = uid,
          name = "Trường Kiếm Tiên Phong",
          slot = "WEAPON",
          rarity = 2L,
          bonusHp = 35L,
          bonusAtk = 12L,
          bonusDef = 6L,
          bonusSpd = 0L,
          iconType = "LONGSWORD",
          equippedByHeroId = kaelenId,
        ),
      )
      batch.set(
        equipRef.document(bowId),
        equipmentMap(
          uid = uid,
          name = "Cung Gỗ Tần Bì",
          slot = "WEAPON",
          rarity = 2L,
          bonusHp = 10L,
          bonusAtk = 15L,
          bonusDef = 0L,
          bonusSpd = 8L,
          iconType = "LONGBOW",
          equippedByHeroId = lyraId,
        ),
      )
      batch.set(
        equipRef.document(staffId),
        equipmentMap(
          uid = uid,
          name = "Trượng Tinh Thạch",
          slot = "WEAPON",
          rarity = 2L,
          bonusHp = 20L,
          bonusAtk = 18L,
          bonusDef = 2L,
          bonusSpd = 0L,
          iconType = "ARCANE_STAFF",
          equippedByHeroId = vesperId,
        ),
      )
      batch.set(
        equipRef.document(armorId),
        equipmentMap(
          uid = uid,
          name = "Giáp Hộ Vệ Cổ Ngữ",
          slot = "ARMOR",
          rarity = 2L,
          bonusHp = 75L,
          bonusAtk = 0L,
          bonusDef = 12L,
          bonusSpd = 0L,
          iconType = "PLATE_MAIL",
          equippedByHeroId = "",
        ),
      )

      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      throw e
    }
  }

  override suspend fun summonNewHero(
    profile: MasterProfile,
    existingHeroes: List<HeroRecord>,
  ): HeroRecord {
    val uid = requireUserId()
    val shardCost = 20L
    val manaCost = 40L
    require(profile.soulShards >= shardCost && profile.manaCrystals >= manaCost) {
      "Không đủ Mảnh Linh Hồn (20) hoặc Tinh Thể Mana (40)."
    }

    val ownedNames = existingHeroes.map { it.name }.toSet()
    val candidate = SUMMON_POOL.filterNot { it.name in ownedNames }.randomOrNull()
      ?: SUMMON_POOL.random().let { base ->
        val suffix = RomanNumeral(existingHeroes.count { it.name.startsWith(base.name) } + 2)
        base.copy(name = "${base.name} $suffix")
      }

    val heroId = "hero_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    var squadAutoJoin = false
    var partyJoinOrder = 0L

    val userRef = db.collection("users").document(uid)
    val heroRef = userRef.collection("heroes").document(heroId)

    try {
      var committed = false
      for (attempt in 0 until 4) {
        val before = userRef.get(com.google.firebase.firestore.Source.SERVER).await()
        val docs = userRef.collection("heroes").whereEqualTo("userId", uid)
          .get(com.google.firebase.firestore.Source.SERVER).await().documents
        require(docs.size < 400) { "Danh sách Anh hùng đã đạt giới hạn." }
        try {
          squadAutoJoin = db.runTransaction { tx ->
            val current = tx.get(userRef)
            if (current.get("updatedAt") != before.get("updatedAt") || current.getLong("lobbyRosterRevision") != before.getLong("lobbyRosterRevision")) throw LobbyRosterChanged()
            val roster = docs.mapNotNull { tx.get(it.reference).toObject(HeroRecord::class.java) }
            val shards = current.getLong("soulShards") ?: 0L
            val mana = current.getLong("manaCrystals") ?: 0L
            require(shards >= shardCost && mana >= manaCost) { "Không đủ tài nguyên triệu hồi." }
            val join = roster.count { it.inActiveSquad } < 4
            partyJoinOrder = if (join) ((roster.maxOfOrNull { it.partyJoinedOrder } ?: 0L).coerceAtMost(Long.MAX_VALUE-1) + 1L) else 0L
            tx.update(userRef, mapOf("soulShards" to shards - shardCost, "manaCrystals" to mana - manaCost,
              "lobbyRosterRevision" to ((current.getLong("lobbyRosterRevision") ?: 0L) + 1L), "updatedAt" to FieldValue.serverTimestamp()))
            tx.set(heroRef, heroMap(uid = uid, name = candidate.name, title = candidate.title,
              heroClass = candidate.heroClass.name, rarity = candidate.rarity, maxHp = candidate.hp,
              attack = candidate.atk, defense = candidate.def, speed = candidate.spd,
              personality = candidate.personality.name, weaponId = "", inSquad = join, bond = candidate.bond) + mapOf("partyJoinedOrder" to partyJoinOrder))
            join
          }.await()
          committed = true
          break
        } catch (e: Exception) {
          var cause: Throwable? = e
          while (cause != null && cause !is LobbyRosterChanged) cause = cause.cause
          if (cause == null) throw e
        }
      }
      check(committed) { "Danh sách Anh hùng đang thay đổi, hãy triệu hồi lại." }

      return HeroRecord(
        id = heroId,
        userId = uid,
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
        partyJoinedOrder = partyJoinOrder,
        bondSummary = candidate.bond,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, heroRef.path)
      throw e
    }
  }

  override suspend fun trainHeroWithFocus(
    profile: MasterProfile,
    hero: HeroRecord,
    focus: TrainingFocus,
  ) {
    val uid = requireUserId()
    require(!hero.isCurrentlyInjured()) {
      "${hero.name} đang dưỡng thương! Hãy hồi phục tại Khu Nghỉ Ngơi trước khi huấn luyện."
    }
    val userRef = db.collection("users").document(uid)
    val heroRef = userRef.collection("heroes").document(hero.id)

    try {
      val batch = db.batch()
      if (focus.isSkillUpgrade) {
        val manaCost = 25L + hero.skillLevel * 10L
        val essenceCost = 1L
        require(profile.manaCrystals >= manaCost && profile.arcaneEssence >= essenceCost) {
          "Cần $manaCost Mana và $essenceCost Tinh Hoa Cổ Ngữ để nâng kỹ năng."
        }
        require(hero.skillLevel < 20L) { "Kỹ năng đã đạt cấp tối đa." }
        batch.update(
          userRef,
          mapOf(
            "manaCrystals" to (profile.manaCrystals - manaCost),
            "arcaneEssence" to (profile.arcaneEssence - essenceCost),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.update(
          heroRef,
          mapOf(
            "skillLevel" to (hero.skillLevel + 1L).coerceAtMost(20L),
            "status" to HeroStatusType.TRAINING.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        val goldCost = 80L + hero.level * 35L
        require(profile.gold >= goldCost) { "Cần $goldCost Vàng để huấn luyện." }
        require(hero.level < 100L) { "Anh hùng đã đạt cấp tối đa." }

        val baseHpGain = if (hero.classType == HeroClassType.KNIGHT || hero.classType == HeroClassType.BERSERKER) 36L else 24L
        val baseAtkGain = if (hero.classType == HeroClassType.MAGE || hero.classType == HeroClassType.ARCHER || hero.classType == HeroClassType.ASSASSIN) 8L else 5L
        val baseDefGain = if (hero.classType == HeroClassType.KNIGHT) 4L else 2L
        val baseSpdGain = if (hero.classType == HeroClassType.ASSASSIN || hero.classType == HeroClassType.ARCHER) 2L else 1L

        val hpGain = baseHpGain + focus.hpBonus
        val atkGain = baseAtkGain + focus.atkBonus
        val defGain = baseDefGain + focus.defBonus
        val spdGain = baseSpdGain + focus.spdBonus

        batch.update(
          userRef,
          mapOf(
            "gold" to (profile.gold - goldCost),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.update(
          heroRef,
          mapOf(
            "level" to (hero.level + 1L).coerceAtMost(100L),
            "maxHp" to (hero.maxHp + hpGain).coerceAtMost(100000L),
            "currentHp" to if (hero.currentHp < 0L) -1L else (hero.currentHp + hpGain).coerceAtMost(100000L),
            "hiddenTrait" to com.example.data.LobbyHeroPersistenceFields.traitFields(hero.hiddenTrait.copy(unlocked = hero.hiddenTrait.unlocked || hero.hiddenTrait.canUnlock((hero.level + 1L).toInt(), hero.morale.toInt(), com.example.data.model.HeroSanityState.fromRaw(hero.sanityState)))),
            "attack" to (hero.attack + atkGain).coerceAtMost(50000L),
            "defense" to (hero.defense + defGain).coerceAtMost(50000L),
            "speed" to (hero.speed + spdGain).coerceAtMost(500L),
            "status" to HeroStatusType.TRAINING.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, heroRef.path)
      throw e
    }
  }

  override suspend fun equipItemToHero(
    hero: HeroRecord,
    item: EquipmentRecord,
    allHeroes: List<HeroRecord>,
    allEquipment: List<EquipmentRecord>,
  ) {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val heroesRef = userRef.collection("heroes")
    val equipRef = userRef.collection("equipment")

    try {
      val batch = db.batch()
      val isWeapon = item.slot == "WEAPON"

      if (item.equippedByHeroId.isNotBlank() && item.equippedByHeroId != hero.id) {
        val prevHolder = allHeroes.firstOrNull { it.id == item.equippedByHeroId }
        if (prevHolder != null) {
          val field = if (isWeapon) "equippedWeaponId" else "equippedArmorId"
          batch.update(
            heroesRef.document(prevHolder.id),
            mapOf(
              field to "",
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }

      val prevItemId = if (isWeapon) hero.equippedWeaponId else hero.equippedArmorId
      if (prevItemId.isNotBlank() && prevItemId != item.id) {
        val prevItem = allEquipment.firstOrNull { it.id == prevItemId }
        if (prevItem != null) {
          batch.update(
            equipRef.document(prevItem.id),
            mapOf(
              "equippedByHeroId" to "",
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }

      val heroSlotField = if (isWeapon) "equippedWeaponId" else "equippedArmorId"
      batch.update(
        heroesRef.document(hero.id),
        mapOf(
          heroSlotField to item.id,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        equipRef.document(item.id),
        mapOf(
          "equippedByHeroId" to hero.id,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/equipment/${item.id}")
      throw e
    }
  }

  override suspend fun unequipItemFromHero(
    hero: HeroRecord,
    slot: String,
    allEquipment: List<EquipmentRecord>,
  ) {
    val uid = requireUserId()
    val isWeapon = slot != "ARMOR"
    val itemId = if (isWeapon) hero.equippedWeaponId else hero.equippedArmorId
    if (itemId.isBlank()) return

    val userRef = db.collection("users").document(uid)
    val heroRef = userRef.collection("heroes").document(hero.id)
    val equipRef = userRef.collection("equipment").document(itemId)

    try {
      val batch = db.batch()
      val heroSlotField = if (isWeapon) "equippedWeaponId" else "equippedArmorId"
      batch.update(
        heroRef,
        mapOf(
          heroSlotField to "",
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      if (allEquipment.any { it.id == itemId }) {
        batch.update(
          equipRef,
          mapOf(
            "equippedByHeroId" to "",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, heroRef.path)
      throw e
    }
  }

  override suspend fun assignHeroActivity(
    hero: HeroRecord,
    targetStatus: HeroStatusType,
  ) {
    val uid = requireUserId()
    val nowSec = System.currentTimeMillis() / 1000L
    require(!hero.isCurrentlyInjured(nowSec)) {
      "${hero.name} đang bị thương! Cần hoàn tất trị thương tại Khu Nghỉ Ngơi trước."
    }
    require(targetStatus != HeroStatusType.INJURED) {
      "Không thể gán trạng thái chấn thương thủ công."
    }
    val heroRef = db.collection("users").document(uid).collection("heroes").document(hero.id)
    val moraleBoost = if (targetStatus == HeroStatusType.RESTING) 8L else 0L
    try {
      heroRef.update(
        mapOf(
          "status" to targetStatus.name,
          "currentAssignment" to if (targetStatus == HeroStatusType.RESTING) "RESTING" else if (hero.inActiveSquad) "IN_PARTY" else "UNASSIGNED",
          "inActiveSquad" to (hero.inActiveSquad && targetStatus != HeroStatusType.RESTING),
          "restStartedAtEpochSec" to if (targetStatus == HeroStatusType.RESTING) nowSec else 0L,
          "currentHp" to com.example.data.model.HeroData.fromRecord(hero, nowSec).currentHP.toLong(),
          "morale" to (hero.morale + moraleBoost).coerceIn(0L, 100L),
          "hiddenTrait" to com.example.data.LobbyHeroPersistenceFields.traitFields(hero.hiddenTrait.copy(unlocked = hero.hiddenTrait.unlocked || hero.hiddenTrait.canUnlock(hero.level.toInt(), (hero.morale + moraleBoost).coerceIn(0L,100L).toInt(), com.example.data.model.HeroSanityState.fromRaw(hero.sanityState)))),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, heroRef.path)
      throw e
    }
  }

  override suspend fun synthesizeEquipment(profile: MasterProfile): EquipmentRecord {
    val uid = requireUserId()
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
    val blueprint = SYNTHESIS_POOL.random()
    val scale = rarity.toInt()
    val itemId = "eq_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    val bonusHp = blueprint.baseHp * scale
    val bonusAtk = blueprint.baseAtk * scale
    val bonusDef = blueprint.baseDef * scale
    val bonusSpd = blueprint.baseSpd * scale
    val itemName = "${blueprint.nameVi} +$rarity"

    val userRef = db.collection("users").document(uid)
    val itemRef = userRef.collection("equipment").document(itemId)

    try {
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "ironOre" to (profile.ironOre - oreCost),
          "arcaneEssence" to (profile.arcaneEssence - essenceCost),
          "gold" to (profile.gold - goldCost),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        itemRef,
        equipmentMap(
          uid = uid,
          name = itemName,
          slot = blueprint.slot,
          rarity = rarity,
          bonusHp = bonusHp,
          bonusAtk = bonusAtk,
          bonusDef = bonusDef,
          bonusSpd = bonusSpd,
          iconType = blueprint.iconType,
          equippedByHeroId = "",
        ),
      )
      batch.commit().await()

      return EquipmentRecord(
        id = itemId,
        userId = uid,
        name = itemName,
        slot = blueprint.slot,
        rarity = rarity,
        level = 1L,
        bonusHp = bonusHp,
        bonusAttack = bonusAtk,
        bonusDefense = bonusDef,
        bonusSpeed = bonusSpd,
        iconType = blueprint.iconType,
        equippedByHeroId = "",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, itemRef.path)
      throw e
    }
  }

  override suspend fun enhanceEquipment(profile: MasterProfile, item: EquipmentRecord) {
    val uid = requireUserId()
    val goldCost = 90L + item.level * 45L
    val oreCost = 1L
    require(profile.gold >= goldCost && profile.ironOre >= oreCost) {
      "Cần $goldCost Vàng và 1 Quặng Sắt để cường hóa."
    }
    require(item.level < 50L) { "Trang bị đã đạt cấp tối đa." }

    val userRef = db.collection("users").document(uid)
    val itemRef = userRef.collection("equipment").document(item.id)

    try {
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "gold" to (profile.gold - goldCost),
          "ironOre" to (profile.ironOre - oreCost),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        itemRef,
        mapOf(
          "level" to (item.level + 1L),
          "bonusHp" to (item.bonusHp + if (item.slot == "ARMOR") 22L else 8L).coerceAtMost(50000L),
          "bonusAttack" to (item.bonusAttack + if (item.slot == "WEAPON") 6L else 1L).coerceAtMost(10000L),
          "bonusDefense" to (item.bonusDefense + if (item.slot == "ARMOR") 5L else 1L).coerceAtMost(10000L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, itemRef.path)
      throw e
    }
  }

  override suspend fun recoverInjuredHero(
    profile: MasterProfile,
    hero: HeroRecord,
    instantWithResources: Boolean,
  ) {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val heroRef = userRef.collection("heroes").document(hero.id)
    val nowSec = System.currentTimeMillis() / 1000L

    try {
      val batch = db.batch()
      if (instantWithResources && hero.remainingInjurySeconds(nowSec) > 0L) {
        val rationCost = 2L
        val manaCost = 15L
        require(profile.rations >= rationCost && profile.manaCrystals >= manaCost) {
          "Cần 2 Khẩu Phần và 15 Tinh Thể Mana để trị thương tức thì."
        }
        batch.update(
          userRef,
          mapOf(
            "rations" to (profile.rations - rationCost),
            "manaCrystals" to (profile.manaCrystals - manaCost),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.update(
        heroRef,
        mapOf(
          "status" to HeroStatusType.READY.name,
          "currentHp" to hero.maxHp,
          "currentAssignment" to if (hero.inActiveSquad) "IN_PARTY" else "UNASSIGNED",
          "restStartedAtEpochSec" to 0L,
          "injuryRecoveryUntilEpochSec" to 0L,
          "morale" to (hero.morale + 15L).coerceAtMost(100L),
          "hiddenTrait" to com.example.data.LobbyHeroPersistenceFields.traitFields(hero.hiddenTrait.copy(unlocked = hero.hiddenTrait.unlocked || hero.hiddenTrait.canUnlock(hero.level.toInt(), (hero.morale + 15L).coerceAtMost(100L).toInt(), com.example.data.model.HeroSanityState.fromRaw(hero.sanityState)))),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, heroRef.path)
      throw e
    }
  }

  override suspend fun hostSanctuaryMealWithRecipe(
    profile: MasterProfile,
    heroes: List<HeroRecord>,
    recipe: SanctuaryMealRecipe,
  ) {
    val uid = requireUserId()
    require(profile.rations >= recipe.rationCost && profile.gold >= recipe.goldCost) {
      "Cần ${recipe.rationCost} Khẩu Phần và ${recipe.goldCost} Vàng để phục vụ [${recipe.titleVi}]."
    }
    val nowSec = System.currentTimeMillis() / 1000L
    val feastUntil = nowSec + recipe.durationSec
    val updatedFlags = FeastBuffHelper.buildUpdatedStoryFlagsWithFeast(
      existingFlags = profile.storyFlags,
      untilEpochSec = feastUntil,
      recipe = recipe,
    )

    val userRef = db.collection("users").document(uid)
    try {
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "rations" to (profile.rations - recipe.rationCost),
          "gold" to (profile.gold - recipe.goldCost),
          "storyFlags" to updatedFlags,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      heroes.forEach { hero ->
        val trimmedRecovery = if (hero.isCurrentlyInjured(nowSec)) {
          (hero.injuryRecoveryUntilEpochSec - recipe.recoveryTrimSec).coerceAtLeast(nowSec)
        } else {
          hero.injuryRecoveryUntilEpochSec
        }
        val healedStatus = if (hero.status == HeroStatusType.INJURED.name && trimmedRecovery <= nowSec) {
          HeroStatusType.READY.name
        } else {
          hero.status
        }
        batch.update(
          userRef.collection("heroes").document(hero.id),
          mapOf(
            "morale" to (hero.morale + recipe.moraleGain).coerceIn(0L, 100L),
            "hiddenTrait" to com.example.data.LobbyHeroPersistenceFields.traitFields(hero.hiddenTrait.copy(unlocked = hero.hiddenTrait.unlocked || hero.hiddenTrait.canUnlock(hero.level.toInt(), (hero.morale + recipe.moraleGain).coerceIn(0L,100L).toInt(), com.example.data.model.HeroSanityState.fromRaw(hero.sanityState)))),
            "injuryRecoveryUntilEpochSec" to if (healedStatus == HeroStatusType.READY.name) 0L else trimmedRecovery,
            "status" to healedStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  override suspend fun unlockIslandExpansion(
    profile: MasterProfile,
    milestone: IslandExpansionMilestone,
  ) {
    val uid = requireUserId()
    require(!SanctuaryProgressionHelper.isExpansionUnlocked(profile.storyFlags, milestone)) {
      "[${milestone.titleVi}] đã được mở khóa trước đó."
    }
    require(SanctuaryProgressionHelper.canMeetRequirements(profile, milestone)) {
      "Cần đạt Master Lv.${milestone.reqMasterLevel} và vượt Tầng ${milestone.reqClearedFloor} để mở khóa [${milestone.titleVi}]."
    }
    require(SanctuaryProgressionHelper.canAffordExpansion(profile, milestone)) {
      "Cần ${milestone.goldCost} Vàng, ${milestone.oreCost} Quặng Sắt và ${milestone.essenceCost} Tinh Hoa Cổ Ngữ."
    }
    val userRef = db.collection("users").document(uid)
    val nextFlags = SanctuaryProgressionHelper.compactStoryFlags(profile.storyFlags + milestone.flagKey)
    try {
      userRef.update(
        mapOf(
          "gold" to (profile.gold - milestone.goldCost).coerceAtLeast(0L),
          "ironOre" to (profile.ironOre - milestone.oreCost).coerceAtLeast(0L),
          "arcaneEssence" to (profile.arcaneEssence - milestone.essenceCost).coerceAtLeast(0L),
          "storyFlags" to nextFlags,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  private class LobbyRosterChanged : RuntimeException()

  private suspend fun mutateLobbyAssignment(heroId: String, requested: com.example.data.model.HeroAssignment?, replace: Boolean) {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    // Read the query between two checks of the profile revision/timestamp. All roster writers
    // touch that profile in the same transaction/batch, so a stale membership list is retried.
    repeat(4) {
      val before = userRef.get(com.google.firebase.firestore.Source.SERVER).await()
      val docs = userRef.collection("heroes").whereEqualTo("userId", uid)
        .get(com.google.firebase.firestore.Source.SERVER).await().documents
      require(docs.size <= 400) { "Danh sách Anh hùng vượt giới hạn giao dịch an toàn." }
      try {
        db.runTransaction { tx ->
          val profile = tx.get(userRef)
          if (profile.get("updatedAt") != before.get("updatedAt") ||
            profile.getLong("lobbyRosterRevision") != before.getLong("lobbyRosterRevision")) throw LobbyRosterChanged()
          val current = docs.mapNotNull { doc -> tx.get(doc.reference).toObject(HeroRecord::class.java) }
          val hero = requireNotNull(current.find { it.id == heroId }) { "Không tìm thấy Anh hùng sở hữu." }
          val assignment = requested ?: if (hero.inActiveSquad) com.example.data.model.HeroAssignment.UNASSIGNED else com.example.data.model.HeroAssignment.IN_PARTY
          val next = com.example.data.LobbyHeroAssignments.assign(current, heroId, assignment, System.currentTimeMillis() / 1000L, replace)
          next.zip(current).filter { (a, b) -> a != b }.forEach { (h, _) ->
            tx.update(userRef.collection("heroes").document(h.id), mapOf(
              "inActiveSquad" to h.inActiveSquad, "currentAssignment" to h.currentAssignment,
              "status" to h.status, "currentHp" to h.currentHp, "partyJoinedOrder" to h.partyJoinedOrder,
              "restStartedAtEpochSec" to h.restStartedAtEpochSec,
              "hiddenTrait" to mapOf("id" to h.hiddenTrait.id, "name" to h.hiddenTrait.name,
                "minimumLevel" to h.hiddenTrait.minimumLevel, "minimumMorale" to h.hiddenTrait.minimumMorale,
                "requiresSelfAwareness" to h.hiddenTrait.requiresSelfAwareness, "unlocked" to h.hiddenTrait.unlocked),
              "updatedAt" to FieldValue.serverTimestamp()))
          }
          tx.update(userRef, mapOf("lobbyRosterRevision" to ((profile.getLong("lobbyRosterRevision") ?: 0L) + 1L),
            "updatedAt" to FieldValue.serverTimestamp()))
          true
        }.await()
        return
      } catch (e: Exception) {
        var cause: Throwable? = e
        while (cause != null && cause !is LobbyRosterChanged) cause = cause.cause
        if (cause == null) throw e
      }
    }
    error("Danh sách Anh hùng đang thay đổi, hãy thử thả lại.")
  }

  override suspend fun assignLobbyHero(heroId: String, assignment: com.example.data.model.HeroAssignment) {
    mutateLobbyAssignment(heroId, assignment, true)
  }

  override suspend fun toggleHeroInSquad(hero: HeroRecord, currentSquadSize: Int) {
    mutateLobbyAssignment(hero.id, null, false)
  }

  override suspend fun markStoryFlag(profile: MasterProfile, flag: String) {
    if (flag in profile.storyFlags) return
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val updated = SanctuaryProgressionHelper.compactStoryFlags(profile.storyFlags + flag)
    try {
      userRef.update(
        mapOf(
          "storyFlags" to updated,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
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
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val escortOutcome = courierOutcome?.let {
      if (floorNumber == 1 && victory && it == CourierOutcome.RESCUED) CourierOutcome.RESCUED else CourierOutcome.FAILED
    }
    try {
      // Read before writing: a retry or a stale UI profile cannot grant the service/memory twice.
      if (escortOutcome != null) {
        return db.runTransaction { transaction ->
          val currentProfile = transaction.get(userRef).toObject(MasterProfile::class.java)
            ?: error("Không tìm thấy tiến trình Master để lưu nhiệm vụ.")
          val currentHeroes = squadHeroes.mapNotNull { hero ->
            transaction.get(userRef.collection("heroes").document(hero.id)).toObject(HeroRecord::class.java)
          }
          buildExpeditionSettlement(
            uid, currentProfile, currentHeroes, fallenHeroIds, floorNumber, victory, retreatUsed,
            bonusGold, bonusMana, discoveredNodeIds, escortOutcome, courierMemoryHeroId,
            updateDocument = { ref, fields -> transaction.update(ref, fields); Unit },
            setDocument = { ref, fields -> transaction.set(ref, fields); Unit },
          )
        }.await()
      }
      val batch = db.batch()
      val result = buildExpeditionSettlement(
        uid, profile, squadHeroes, fallenHeroIds, floorNumber, victory, retreatUsed,
        bonusGold, bonusMana, discoveredNodeIds, null, null,
        updateDocument = { ref, fields -> batch.update(ref, fields); Unit },
        setDocument = { ref, fields -> batch.set(ref, fields); Unit },
      )
      batch.commit().await()
      return result
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      throw e
    }
  }

  private fun buildExpeditionSettlement(
    uid: String,
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
    updateDocument: (com.google.firebase.firestore.DocumentReference, Map<String, Any>) -> Unit,
    setDocument: (com.google.firebase.firestore.DocumentReference, Map<String, Any>) -> Unit,
  ): ExpeditionSettlementResult {
    val userRef = db.collection("users").document(uid)
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

    val outcomeType = when {
      victory -> ExpeditionOutcomeType.VICTORY
      retreatUsed -> ExpeditionOutcomeType.TACTICAL_RETREAT
      else -> ExpeditionOutcomeType.DEFEAT
    }

    val totalMasterExp = profile.masterExp + masterExpGain
    val newMasterLevel = (1L + totalMasterExp / 250L).coerceAtMost(100L)
    val newHighestFloor = if (victory) {
      maxOf(profile.highestClearedFloor, floorNumber.toLong())
    } else {
      profile.highestClearedFloor
    }

    val injuredNames = mutableListOf<String>()
    val leveledNames = mutableListOf<String>()
    var lootedName: String? = null

    val nodeFlags = discoveredNodeIds.map { "DISC_${it.take(36)}" }
    val courierForgeUnlocked = LostCourierProgression.isFirstRescue(profile.storyFlags, courierOutcome)
    val squadIds = squadHeroes.map { it.id }.toSet()
    val memoryHeroId = courierMemoryHeroId?.takeIf { it in squadIds } ?: squadHeroes.firstOrNull()?.id
    var courierMemoryHeroName: String? = null
    val nextFlags = SanctuaryProgressionHelper.compactStoryFlags(
      LostCourierProgression.outcomeFlags(profile.storyFlags, courierOutcome) + nodeFlags,
    )

    updateDocument(
      userRef,
      mapOf(
        "masterLevel" to newMasterLevel,
        "masterExp" to totalMasterExp.coerceAtMost(10000000L),
        "gold" to (profile.gold + goldEarned).coerceAtMost(99999999L),
        "manaCrystals" to (profile.manaCrystals + manaEarned).coerceAtMost(9999999L),
        "soulShards" to (profile.soulShards + shardsEarned).coerceAtMost(9999999L),
        "rations" to (profile.rations + if (victory) 2L else 0L).coerceAtMost(99999L),
        "ironOre" to (profile.ironOre + oreEarned).coerceAtMost(99999L),
        "arcaneEssence" to (profile.arcaneEssence + essenceEarned).coerceAtMost(99999L),
        "highestClearedFloor" to newHighestFloor,
        "totalExpeditions" to (profile.totalExpeditions + 1L),
        "storyFlags" to nextFlags,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    val heroExpGain = when {
      victory -> 90L + floorNumber * 40L
      retreatUsed -> 40L
      else -> 25L
    }
    for (hero in squadHeroes) {
      val heroRef = userRef.collection("heroes").document(hero.id)
      val totalHeroExp = hero.exp + heroExpGain
      val expNeeded = hero.level * 100L
      val didLevelUp = victory && totalHeroExp >= expNeeded && hero.level < 100L
      val nextLevel = if (didLevelUp) hero.level + 1L else hero.level
      val remExp = if (didLevelUp) (totalHeroExp - expNeeded).coerceAtLeast(0L) else totalHeroExp
      if (didLevelUp) leveledNames += hero.name

      val wasKnockedOut = hero.id in fallenHeroIds || (!victory && !retreatUsed)
      val nextStatus = if (wasKnockedOut) HeroStatusType.INJURED.name else HeroStatusType.READY.name
      val recoveryUntil = if (wasKnockedOut) (nowSec + recoveryDurationSec) else 0L
      if (wasKnockedOut) injuredNames += hero.name

      val nextMorale = when {
        victory -> (hero.morale - 6L).coerceIn(maxOf(20L, moraleFloor), 100L)
        retreatUsed -> (hero.morale - 10L).coerceIn(maxOf(25L, moraleFloor), 100L)
        else -> (hero.morale - 22L).coerceIn(moraleFloor, 100L)
      }

      updateDocument(
        heroRef,
        mapOf(
          "level" to nextLevel,
          "exp" to remExp.coerceAtMost(10000000L),
          "maxHp" to (hero.maxHp + if (didLevelUp) 32L else 0L).coerceAtMost(100000L),
          "attack" to (hero.attack + if (didLevelUp) 7L else 0L).coerceAtMost(50000L),
          "defense" to (hero.defense + if (didLevelUp) 4L else 0L).coerceAtMost(50000L),
          "speed" to (hero.speed + if (didLevelUp) 2L else 0L).coerceAtMost(500L),
          "status" to nextStatus,
          "injuryRecoveryUntilEpochSec" to recoveryUntil,
          "morale" to nextMorale,
          "hiddenTrait" to com.example.data.LobbyHeroPersistenceFields.traitFields(hero.hiddenTrait.copy(unlocked = hero.hiddenTrait.unlocked || hero.hiddenTrait.canUnlock(nextLevel.toInt(), nextMorale.toInt(), com.example.data.model.HeroSanityState.fromRaw(hero.sanityState)))),
          "inActiveSquad" to if (wasKnockedOut) false else hero.inActiveSquad,
          "currentHp" to if (wasKnockedOut) hero.maxHp / 4L else if (hero.currentHp < 0L) -1L else (hero.currentHp + if (didLevelUp) 32L else 0L).coerceAtMost(100000L),
          "currentAssignment" to if (wasKnockedOut) "RESTING" else if (hero.inActiveSquad) "IN_PARTY" else "UNASSIGNED",
          "restStartedAtEpochSec" to if (wasKnockedOut) nowSec else 0L,
          "updatedAt" to FieldValue.serverTimestamp(),
        ).let { fields ->
          if (courierForgeUnlocked && hero.id == memoryHeroId) {
            courierMemoryHeroName = hero.name
            fields + ("bondSummary" to LostCourierProgression.rememberRescue(hero.bondSummary))
          } else fields
        },
      )
    }

    if (victory) {
      val dropBlueprint = SYNTHESIS_POOL[(floorNumber - 1) % SYNTHESIS_POOL.size]
      val dropRarity = (2L + floorNumber / 2L).coerceIn(2L, 5L)
      val dropId = "eq_drop_${UUID.randomUUID().toString().replace("-", "").take(14)}"
      lootedName = "${dropBlueprint.nameVi} Tầng $floorNumber"
      setDocument(
        userRef.collection("equipment").document(dropId),
        equipmentMap(
          uid = uid,
          name = lootedName,
          slot = dropBlueprint.slot,
          rarity = dropRarity,
          bonusHp = dropBlueprint.baseHp * dropRarity,
          bonusAtk = dropBlueprint.baseAtk * dropRarity,
          bonusDef = dropBlueprint.baseDef * dropRarity,
          bonusSpd = dropBlueprint.baseSpd * dropRarity,
          iconType = dropBlueprint.iconType,
          equippedByHeroId = "",
        ),
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
      courierOutcome = courierOutcome,
      courierForgeUnlocked = courierForgeUnlocked,
      courierMemoryHeroName = courierMemoryHeroName,
    )
  }

  private fun heroMap(
    uid: String,
    name: String,
    title: String,
    heroClass: String,
    rarity: Long,
    maxHp: Long,
    attack: Long,
    defense: Long,
    speed: Long,
    personality: String,
    weaponId: String,
    inSquad: Boolean,
    bond: String,
  ): Map<String, Any> = mapOf(
    "userId" to uid,
    "name" to name,
    "title" to title,
    "heroClass" to heroClass,
    "rarity" to rarity,
    "level" to 1L,
    "exp" to 0L,
    "maxHp" to maxHp,
    "attack" to attack,
    "defense" to defense,
    "speed" to speed,
    "skillLevel" to 1L,
    "personality" to personality,
    "status" to HeroStatusType.READY.name,
    "injuryRecoveryUntilEpochSec" to 0L,
    "morale" to 100L,
    "equippedWeaponId" to weaponId,
    "equippedArmorId" to "",
    "inActiveSquad" to inSquad,
    "bondSummary" to bond,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  private fun equipmentMap(
    uid: String,
    name: String,
    slot: String,
    rarity: Long,
    bonusHp: Long,
    bonusAtk: Long,
    bonusDef: Long,
    bonusSpd: Long,
    iconType: String,
    equippedByHeroId: String,
  ): Map<String, Any> = mapOf(
    "userId" to uid,
    "name" to name,
    "slot" to slot,
    "rarity" to rarity,
    "level" to 1L,
    "bonusHp" to bonusHp,
    "bonusAttack" to bonusAtk,
    "bonusDefense" to bonusDef,
    "bonusSpeed" to bonusSpd,
    "iconType" to iconType,
    "equippedByHeroId" to equippedByHeroId,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  private fun RomanNumeral(num: Int): String = when (num) {
    2 -> "II"
    3 -> "III"
    4 -> "IV"
    5 -> "V"
    else -> "$num"
  }

  /**
   * Safely links and migrates a local Guest profile into the authenticated Google Cloud account:
   * - If the Google account has no profile yet, uploads the entire Guest profile, roster, and gear.
   * - If the Google account already exists, preserves the higher progress/resources and imports any
   *   unique Guest heroes or equipment without duplicating IDs or overwriting cloud progress.
   */
  suspend fun linkGuestSaveToCloudAccount(snapshot: GuestSaveSnapshot): String {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    try {
      val existingUserSnap = userRef.get().await()
      val batch = db.batch()
      val gp = snapshot.profile

      if (!existingUserSnap.exists()) {
        batch.set(
          userRef,
          mapOf(
            "userId" to uid,
            "masterName" to gp.masterName.ifBlank { "Master" }.take(40),
            "masterLevel" to gp.masterLevel.coerceIn(1L, 100L),
            "masterExp" to gp.masterExp.coerceIn(0L, 10000000L),
            "gold" to gp.gold.coerceIn(0L, 99999999L),
            "manaCrystals" to gp.manaCrystals.coerceIn(0L, 9999999L),
            "soulShards" to gp.soulShards.coerceIn(0L, 9999999L),
            "rations" to gp.rations.coerceIn(0L, 99999L),
            "ironOre" to gp.ironOre.coerceIn(0L, 99999L),
            "arcaneEssence" to gp.arcaneEssence.coerceIn(0L, 99999L),
            "highestClearedFloor" to gp.highestClearedFloor.coerceIn(0L, 100L),
            "totalExpeditions" to gp.totalExpeditions.coerceIn(0L, 999999L),
            "unlockedFacilities" to gp.unlockedFacilities.ifEmpty { FacilityType.entries.map { it.name } }.take(10),
            "storyFlags" to SanctuaryProgressionHelper.compactStoryFlags(gp.storyFlags),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        snapshot.heroes.forEach { h ->
          val cleanId = h.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "").ifBlank { "hero_mig_${UUID.randomUUID().toString().take(8)}" }
          batch.set(
            userRef.collection("heroes").document(cleanId),
            mapOf(
              "userId" to uid,
              "name" to h.name.ifBlank { "Kaelen" }.take(50),
              "title" to h.title.ifBlank { "Chiến Binh" }.take(80),
              "heroClass" to h.classType.name,
              "rarity" to h.rarity.coerceIn(1L, 5L),
              "level" to h.level.coerceIn(1L, 100L),
              "exp" to h.exp.coerceIn(0L, 10000000L),
              "maxHp" to h.maxHp.coerceIn(50L, 100000L),
              "attack" to h.attack.coerceIn(5L, 50000L),
              "defense" to h.defense.coerceIn(0L, 50000L),
              "speed" to h.speed.coerceIn(20L, 500L),
              "skillLevel" to h.skillLevel.coerceIn(1L, 20L),
              "personality" to h.personalityType.name,
              "status" to HeroStatusType.fromRaw(h.status).name,
              "injuryRecoveryUntilEpochSec" to h.injuryRecoveryUntilEpochSec.coerceIn(0L, 4102444800L),
              "morale" to h.morale.coerceIn(0L, 100L),
              "equippedWeaponId" to h.equippedWeaponId.take(128),
              "equippedArmorId" to h.equippedArmorId.take(128),
              "inActiveSquad" to h.inActiveSquad,
              "bondSummary" to h.bondSummary.take(200),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ) + com.example.data.LobbyHeroPersistenceFields.fields(h),
          )
        }
        snapshot.equipment.forEach { eq ->
          val cleanId = eq.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "").ifBlank { "eq_mig_${UUID.randomUUID().toString().take(8)}" }
          batch.set(
            userRef.collection("equipment").document(cleanId),
            mapOf(
              "userId" to uid,
              "name" to eq.name.ifBlank { "Binh Khí" }.take(60),
              "slot" to if (eq.slot == "ARMOR") "ARMOR" else "WEAPON",
              "rarity" to eq.rarity.coerceIn(1L, 5L),
              "level" to eq.level.coerceIn(1L, 50L),
              "bonusHp" to eq.bonusHp.coerceIn(0L, 50000L),
              "bonusAttack" to eq.bonusAttack.coerceIn(0L, 10000L),
              "bonusDefense" to eq.bonusDefense.coerceIn(0L, 10000L),
              "bonusSpeed" to eq.bonusSpeed.coerceIn(0L, 200L),
              "iconType" to eq.iconType.ifBlank { "LONGSWORD" }.take(40),
              "equippedByHeroId" to eq.equippedByHeroId.take(128),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
        return "Đã liên kết & đồng bộ toàn bộ tiến trình Khách lên Google Cloud!"
      } else {
        val cloudProfile = existingUserSnap.toObject(MasterProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: MasterProfile()
        val existingHeroesSnap = userRef.collection("heroes").whereEqualTo("userId", uid).get().await()
        val existingHeroIds = existingHeroesSnap.documents.map { it.id }.toSet()
        val existingHeroNames = existingHeroesSnap.documents.mapNotNull { it.getString("name") }.toSet()

        val existingEquipSnap = userRef.collection("equipment").whereEqualTo("userId", uid).get().await()
        val existingEquipIds = existingEquipSnap.documents.map { it.id }.toSet()

        batch.update(
          userRef,
          mapOf(
            "masterLevel" to maxOf(cloudProfile.masterLevel, gp.masterLevel).coerceIn(1L, 100L),
            "masterExp" to maxOf(cloudProfile.masterExp, gp.masterExp).coerceIn(0L, 10000000L),
            "gold" to maxOf(cloudProfile.gold, gp.gold).coerceIn(0L, 99999999L),
            "manaCrystals" to maxOf(cloudProfile.manaCrystals, gp.manaCrystals).coerceIn(0L, 9999999L),
            "soulShards" to maxOf(cloudProfile.soulShards, gp.soulShards).coerceIn(0L, 9999999L),
            "rations" to maxOf(cloudProfile.rations, gp.rations).coerceIn(0L, 99999L),
            "ironOre" to maxOf(cloudProfile.ironOre, gp.ironOre).coerceIn(0L, 99999L),
            "arcaneEssence" to maxOf(cloudProfile.arcaneEssence, gp.arcaneEssence).coerceIn(0L, 99999L),
            "highestClearedFloor" to maxOf(cloudProfile.highestClearedFloor, gp.highestClearedFloor).coerceIn(0L, 100L),
            "totalExpeditions" to maxOf(cloudProfile.totalExpeditions, gp.totalExpeditions).coerceIn(0L, 999999L),
            "storyFlags" to SanctuaryProgressionHelper.compactStoryFlags(cloudProfile.storyFlags + gp.storyFlags),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        snapshot.heroes.forEach { h ->
          if (h.id !in existingHeroIds && h.name !in existingHeroNames) {
            batch.set(
              userRef.collection("heroes").document(h.id),
              mapOf(
                "userId" to uid,
                "name" to h.name.take(50),
                "title" to h.title.take(80),
                "heroClass" to h.classType.name,
                "rarity" to h.rarity.coerceIn(1L, 5L),
                "level" to h.level.coerceIn(1L, 100L),
                "exp" to h.exp.coerceIn(0L, 10000000L),
                "maxHp" to h.maxHp.coerceIn(50L, 100000L),
                "attack" to h.attack.coerceIn(5L, 50000L),
                "defense" to h.defense.coerceIn(0L, 50000L),
                "speed" to h.speed.coerceIn(20L, 500L),
                "skillLevel" to h.skillLevel.coerceIn(1L, 20L),
                "personality" to h.personalityType.name,
                "status" to HeroStatusType.READY.name,
                "injuryRecoveryUntilEpochSec" to 0L,
                "morale" to h.morale.coerceIn(0L, 100L),
                "equippedWeaponId" to "",
                "equippedArmorId" to "",
                "inActiveSquad" to false,
                "bondSummary" to h.bondSummary.take(200),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ) + com.example.data.LobbyHeroPersistenceFields.fields(h.copy(inActiveSquad = false)),
            )
          } else if (LostCourierProgression.hasRescueMemory(h.bondSummary)) {
            val cloudHero = existingHeroesSnap.documents.firstOrNull { it.id == h.id }
              ?: existingHeroesSnap.documents.firstOrNull { it.getString("name") == h.name }
            if (cloudHero != null) {
              batch.update(
                cloudHero.reference,
                mapOf(
                  "bondSummary" to LostCourierProgression.rememberRescue(cloudHero.getString("bondSummary").orEmpty()),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
          }
        }

        snapshot.equipment.forEach { eq ->
          if (eq.id !in existingEquipIds) {
            batch.set(
              userRef.collection("equipment").document(eq.id),
              mapOf(
                "userId" to uid,
                "name" to eq.name.take(60),
                "slot" to if (eq.slot == "ARMOR") "ARMOR" else "WEAPON",
                "rarity" to eq.rarity.coerceIn(1L, 5L),
                "level" to eq.level.coerceIn(1L, 50L),
                "bonusHp" to eq.bonusHp.coerceIn(0L, 50000L),
                "bonusAttack" to eq.bonusAttack.coerceIn(0L, 10000L),
                "bonusDefense" to eq.bonusDefense.coerceIn(0L, 10000L),
                "bonusSpeed" to eq.bonusSpeed.coerceIn(0L, 200L),
                "iconType" to eq.iconType.take(40),
                "equippedByHeroId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }
        batch.commit().await()
        return "Đã hợp nhất tiến trình Khách với hồ sơ Google Cloud hiện có!"
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      throw e
    }
  }

  data class EquipBlueprint(
    val nameVi: String,
    val slot: String,
    val iconType: String,
    val baseHp: Long,
    val baseAtk: Long,
    val baseDef: Long,
    val baseSpd: Long,
  )

  companion object {
    val SYNTHESIS_POOL = listOf(
      EquipBlueprint("Đại Kiếm Hắc Diện", "WEAPON", "LONGSWORD", 18L, 11L, 4L, 0L),
      EquipBlueprint("Cung Phong Linh", "WEAPON", "LONGBOW", 8L, 13L, 0L, 5L),
      EquipBlueprint("Quyền Trượng Hư Không", "WEAPON", "ARCANE_STAFF", 12L, 15L, 2L, 0L),
      EquipBlueprint("Song Đoản Đao Bóng Đêm", "WEAPON", "DAGGER", 6L, 14L, 0L, 7L),
      EquipBlueprint("Giáp Trọng Kỵ Thánh Điện", "ARMOR", "PLATE_MAIL", 48L, 0L, 9L, 0L),
      EquipBlueprint("Áo Choàng Linh Vũ", "ARMOR", "RANGER_CLOAK", 30L, 3L, 5L, 6L),
      EquipBlueprint("Pháp Bào Tinh Tú", "ARMOR", "MYSTIC_ROBE", 34L, 6L, 5L, 2L),
    )

    val SUMMON_POOL = listOf(
      SummonableTemplate(
        name = "Seraphina",
        title = "Thánh Nữ Ánh Trăng",
        heroClass = HeroClassType.PRIEST,
        rarity = 4L,
        hp = 340L,
        atk = 44L,
        def = 22L,
        spd = 104L,
        personality = PersonalityType.DEVOTED,
        bond = "Luôn băng bó vết thương cho cả đội tại Khu Nghỉ Ngơi.",
      ),
      SummonableTemplate(
        name = "Ragnar",
        title = "Cuồng Long Phá Giáp",
        heroClass = HeroClassType.BERSERKER,
        rarity = 4L,
        hp = 520L,
        atk = 62L,
        def = 20L,
        spd = 106L,
        personality = PersonalityType.RECKLESS,
        bond = "Thường tỉ thí nảy lửa với Kaelen tại Sân Huấn Luyện.",
      ),
      SummonableTemplate(
        name = "Noctis",
        title = "Lưỡi Gặt Hư Ảnh",
        heroClass = HeroClassType.ASSASSIN,
        rarity = 4L,
        hp = 305L,
        atk = 68L,
        def = 15L,
        spd = 132L,
        personality = PersonalityType.CYNICAL,
        bond = "Tôn trọng sự quyết đoán của Master trong thời khắc sinh tử.",
      ),
      SummonableTemplate(
        name = "Garrick",
        title = "Thành Lũy Bất Diệt",
        heroClass = HeroClassType.KNIGHT,
        rarity = 3L,
        hp = 510L,
        atk = 38L,
        def = 36L,
        spd = 88L,
        personality = PersonalityType.LOYAL,
        bond = "Nguyện làm lá chắn cuối cùng trước Cổng Tháp.",
      ),
      SummonableTemplate(
        name = "Sylphie",
        title = "Tiễn Vũ Rừng Xanh",
        heroClass = HeroClassType.ARCHER,
        rarity = 3L,
        hp = 315L,
        atk = 56L,
        def = 15L,
        spd = 122L,
        personality = PersonalityType.LOYAL,
        bond = "Học hỏi kỹ thuật bắn tỉa từ Lyra mỗi buổi chiều.",
      ),
      SummonableTemplate(
        name = "Malakor",
        title = "Pháp Sư Hỏa Ngục",
        heroClass = HeroClassType.MAGE,
        rarity = 5L,
        hp = 330L,
        atk = 76L,
        def = 18L,
        spd = 108L,
        personality = PersonalityType.CALCULATING,
        bond = "Dành hàng giờ nghiên cứu cổ ngữ cùng Vesper ở Phòng Tổng Hợp.",
      ),
      SummonableTemplate(
        name = "isolde".replaceFirstChar { it.uppercase() },
        title = "Kiếm Ảnh Bạc",
        heroClass = HeroClassType.ASSASSIN,
        rarity = 5L,
        hp = 335L,
        atk = 74L,
        def = 18L,
        spd = 138L,
        personality = PersonalityType.STOIC,
        bond = "Chỉ xuất kiếm khi Master chỉ định mục tiêu nguy hiểm nhất.",
      ),
    )
  }
}
