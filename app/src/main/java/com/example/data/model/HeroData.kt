package com.example.data.model

/** Persisted assignment and transient locomotion are deliberately separate state machines. */
enum class HeroAssignment { UNASSIGNED, IN_PARTY, RESTING, SACRIFICED }
enum class HeroSanityState(val labelVi: String) {
  NORMAL("Bình ổn"),
  ANXIOUS("Bất an"),
  PANICKED("Hoảng loạn"),
  SELF_AWARE("Thức tỉnh");
  companion object { fun fromRaw(raw: String) = entries.firstOrNull { it.name == raw } ?: NORMAL }
}
enum class LobbyPersonality { COWARD, ARROGANT, LOYAL, LONER, REBEL;
  companion object {
    fun fromRaw(raw: String) = entries.firstOrNull { it.name == raw } ?: when (raw) {
      "RECKLESS" -> ARROGANT
      "CALCULATING", "CYNICAL" -> LONER
      else -> LOYAL
    }
  }
}

data class HeroHiddenTrait(
  val id: String = "unyielding_soul",
  val name: String = "Linh hồn bất khuất",
  val minimumLevel: Int = 10,
  val minimumMorale: Int = 60,
  val requiresSelfAwareness: Boolean = false,
  val unlocked: Boolean = false,
) {
  fun canUnlock(
    level: Int,
    morale: Int,
    sanity: HeroSanityState,
    survivedExpeditions: Int = 0,
    returnedLowHpOnce: Boolean = false,
  ): Boolean =
    unlocked ||
      survivedExpeditions >= 3 ||
      returnedLowHpOnce ||
      (level >= minimumLevel && morale >= minimumMorale && (!requiresSelfAwareness || sanity == HeroSanityState.SELF_AWARE))

  val displayName: String get() = if (unlocked) name else "???"
  val detailedDisplayNameVi: String get() = if (unlocked) name else "??? (Chưa thức tỉnh)"
  val unlockCondition: String
    get() = "Sống sót ≥ 3 trận leo tháp • Từng trở về HP < 15% • Hoặc Cấp $minimumLevel & Tinh thần ≥ $minimumMorale" +
      if (requiresSelfAwareness) " • Thức tỉnh" else ""

  companion object {
    val AWAKENED_TRAIT_POOL: List<Pair<String, String>> = listOf(
      "assassin_instinct" to "Bản Năng Sát Thủ - Tăng 30% Crit",
      "indomitable_will" to "Bất Khuất - Miễn nhiễm Hoảng loạn",
      "mortal_sword_saint" to "Kiếm Thánh Phàm Trần - Tăng 25% ATK",
      "bloodline_bastion" to "Huyết Thống Thiết Giáp - Tăng 25% DEF",
      "astral_eye" to "Nhãn Quan Tinh Không - Tăng 20% SPD & Chính xác",
    )

    fun defaultTraitForHero(heroId: String, rarity: Int): HeroHiddenTrait {
      val index = (heroId.hashCode() and Int.MAX_VALUE) % AWAKENED_TRAIT_POOL.size
      val (traitId, traitName) = AWAKENED_TRAIT_POOL[index]
      return HeroHiddenTrait(
        id = traitId,
        name = traitName,
        minimumLevel = if (rarity <= 2) 6 else 10,
        minimumMorale = 60,
        requiresSelfAwareness = false,
        unlocked = rarity >= 3,
      )
    }

    fun resolveAwakenedTrait(current: HeroHiddenTrait, heroId: String): HeroHiddenTrait {
      val named = if (current.name.isBlank() || current.name == "Linh hồn bất khuất") {
        val index = (heroId.hashCode() and Int.MAX_VALUE) % AWAKENED_TRAIT_POOL.size
        val (traitId, traitName) = AWAKENED_TRAIT_POOL[index]
        current.copy(id = traitId, name = traitName)
      } else {
        current
      }
      return named.copy(unlocked = true)
    }
  }
}

/** Complete lobby projection of the existing owned HeroRecord, never a second roster/save. */
data class HeroData(
  val id: String, val name: String, val rarity: Int, val level: Int,
  val currentHP: Int, val maxHP: Int, val atk: Int, val def: Int, val speed: Int,
  val morale: Int, val sanityState: HeroSanityState, val personality: LobbyPersonality,
  val currentAssignment: HeroAssignment, val hiddenTrait: HeroHiddenTrait,
) {
  init {
    require(id.isNotBlank()); require(rarity in 1..5); require(level >= 1)
    require(maxHP > 0 && currentHP in 0..maxHP); require(morale in 0..100)
    require(atk >= 0 && def >= 0 && speed > 0)
  }
  val isLimping get() = currentHP.toLong() * 100L < maxHP.toLong() * 30L
  val isLowHpNearHouse get() = currentHP.toLong() * 100L < maxHP.toLong() * 40L
  val isCriticalReturnHp get() = currentHP > 0 && currentHP.toLong() * 100L < maxHP.toLong() * 15L
  val isFrightened get() = morale < 25 || sanityState == HeroSanityState.PANICKED
  val movementSpeed get() = speed * if (isLimping) .5f else 1f

  companion object {
    fun fromRecord(hero: HeroRecord, nowEpochSec: Long = System.currentTimeMillis() / 1000L): HeroData {
      val max = hero.maxHp.coerceIn(1L, 100000L).toInt()
      val assignment = when {
        hero.currentAssignment == HeroAssignment.SACRIFICED.name -> HeroAssignment.SACRIFICED
        hero.isCurrentlyInjured(nowEpochSec) || hero.statusType == HeroStatusType.RESTING -> HeroAssignment.RESTING
        hero.inActiveSquad -> HeroAssignment.IN_PARTY
        hero.currentAssignment == HeroAssignment.RESTING.name -> HeroAssignment.RESTING
        else -> HeroAssignment.UNASSIGNED
      }
      val hp = if (hero.currentHp < 0L) {
        if (hero.isCurrentlyInjured(nowEpochSec)) max / 4 else max
      } else hero.currentHp.coerceIn(0L, max.toLong()).toInt()
      val elapsed = if (assignment == HeroAssignment.RESTING && hero.restStartedAtEpochSec > 0L)
        (nowEpochSec - hero.restStartedAtEpochSec).coerceIn(0L, 86400L) else 0L
      val healed = (hp + elapsed * max / 120L).coerceAtMost(max.toLong()).toInt()
      val effectiveSanity = if (hero.name.contains("Han Ysl", ignoreCase = true) || hero.bondSummary.contains("SELF_AWARE")) {
        HeroSanityState.SELF_AWARE
      } else {
        HeroSanityState.fromRaw(hero.sanityState)
      }
      return HeroData(hero.id, hero.name, hero.rarity.coerceIn(1L, 5L).toInt(), hero.level.coerceIn(1L, 100L).toInt(),
        healed, max, hero.attack.coerceIn(0L, 50000L).toInt(), hero.defense.coerceIn(0L, 50000L).toInt(),
        hero.speed.coerceIn(1L, 500L).toInt(), hero.morale.coerceIn(0L, 100L).toInt(),
        effectiveSanity, LobbyPersonality.fromRaw(hero.personality), assignment, hero.hiddenTrait)
    }
  }
}

enum class DialogueTriggerContext {
  BEING_PICKED_UP,
  DROPPED_IN_PARTY,
  DROPPED_IN_HOUSE,
  LOW_HP_NEAR_HOUSE,
  HIGH_STAR_DISDAIN,
  LOW_STAR_HUMILIATED,
  SACRIFICE_PLEA,
  TAPPED_AT_LOBBY,
  IDLE_AMBIENT,
}

/**
 * Context-Aware Dialogue Database providing 30+ full Vietnamese lines for Lobby speech bubbles
 * and Sanctuary synthesis/management reactions.
 */
object ContextAwareDialogueDatabase {
  const val DEFAULT_BUBBLE_DURATION_SEC: Float = 3.5f

  fun allCatalogDialogues(): List<String> = listOf(
    // 1–6: BeingPickedUp
    "Á! Đừng ném tôi vào lò dung hợp mà Master! Tôi xin ngài!",
    "Bỏ cái ngón tay bẩn thỉu của ngươi ra khỏi người ta!",
    "Lại định sắp xếp đội hình đấy à? Chọn người cho khôn ngoan vào.",
    "Tôi không phải quân cờ vô tri trên bàn cờ của ngài đâu Master!",
    "Đặt tôi xuống nhẹ nhàng thôi, vết thương cũ vẫn chưa khép miệng.",
    "Ngài cần tôi trấn giữ vị trí nào? Hãy chỉ hướng đi thưa Master!",
    // 7–10: Dropped into Party (InParty)
    "Không... tầng trước đám đồng đội đã chết sạch rồi, tôi không muốn vào đó!",
    "Cứ giao vị trí tiên phong cho tôi.",
    "Lưỡi kiếm này đã sẵn sàng nhuốm máu lũ quái vật trong Tháp Vĩnh Hằng!",
    "Đội hình viễn chinh đã tập hợp đủ, chỉ chờ hiệu lệnh xuất kích của Master.",
    // 11–13: Low HP passing near House & Resting
    "Vết thương rỉ máu mãi không ngừng... làm ơn cho tôi vào Nhà nghỉ ngơi...",
    "Mắt tôi mờ đi vì mất máu rồi... cần được băng bó tại Phòng Nghỉ Dưỡng ngay...",
    "Cuối cùng cũng được ngả lưng xuống giường ấm tại Ký Túc Xá Thánh Địa.",
    // 14–17: Social Proximity (3-5 star vs 1-2 star)
    "Tránh đường ra đồ rác rưởi 1 sao!",
    "Cút sang một bên đi phế vật cấp thấp, đừng làm bẩn áo choàng của ta!",
    "Kẻ yếu ớt chỉ có một hoặc hai sao như ngươi sớm muộn cũng vào lò dung hợp thôi!",
    "Đừng nhìn tôi bằng ánh mắt đó... tôi cũng đang cố gắng sống sót mà...",
    // 18–23: Sacrifice Final Pleas in Cruel Synthesis Chamber
    "Xin ngài Master! Tôi sẽ làm bất cứ điều gì, đừng biến tôi thành nguyên liệu dung hợp!",
    "Ngươi dám hiến tế một chiến binh cao quý như ta ư?! Lời nguyền này sẽ bám theo ngươi!",
    "Thì ra đây là số phận của những kẻ bị coi là thừa thãi trong cái lồng kính này...",
    "Nếu sinh mạng và máu thịt của tôi giúp đồng đội vượt qua Tầng Tháp tiếp theo... tôi chấp nhận.",
    "Chết trong lò luyện hồn hay chết dưới móng vuốt quái vật... kết cục nào cũng lạnh lẽo như nhau.",
    "Haha! Đốt cháy linh hồn ta đi, nhưng ý chí phản kháng này ngươi không bao giờ nuốt trọn được!",
    // 24–30: Tap & Idle Ambient self-aware / personality dialogues
    "Bầu trời giả lập trên Thánh Địa này lúc nào cũng dừng ở một khoảnh khắc, ngài có nhận ra không?",
    "Mùi thịt nướng từ Nhà Ăn làm tôi quên đi nỗi kinh hoàng dưới chân Tháp Đá Đen.",
    "Làm ơn cho tôi ăn một Bữa Ăn Thịnh Soạn... tay tôi vẫn còn run không cầm nổi vũ khí!",
    "Đừng chạm vào vai tôi bất ngờ như thế, phản xạ sát thủ không phân biệt chủ nhân đâu.",
    "Mỗi vết sẹo sau chuyến leo tháp đều đánh thức một sức mạnh tiềm ẩn trong huyết quản.",
    "Nghe nói hôm qua lại có người bước vào Lò Dung Hợp mà không bao giờ bước ra nữa...",
    "Chừng nào Cổng Tháp còn mở, chừng đó máu của chúng ta vẫn chưa ngừng đổ.",
  )

  fun lineForPickedUp(hero: HeroData): String = when {
    hero.sanityState == HeroSanityState.SELF_AWARE || hero.name.contains("Han Ysl", ignoreCase = true) ->
      "Lại định sắp xếp đội hình đấy à? Chọn người cho khôn ngoan vào."
    hero.personality == LobbyPersonality.COWARD ->
      "Á! Đừng ném tôi vào lò dung hợp mà Master! Tôi xin ngài!"
    hero.personality == LobbyPersonality.ARROGANT ->
      "Bỏ cái ngón tay bẩn thỉu của ngươi ra khỏi người ta!"
    hero.personality == LobbyPersonality.REBEL ->
      "Tôi không phải quân cờ vô tri trên bàn cờ của ngài đâu Master!"
    hero.personality == LobbyPersonality.LONER ->
      "Đặt tôi xuống nhẹ nhàng thôi, vết thương cũ vẫn chưa khép miệng."
    else ->
      "Ngài cần tôi trấn giữ vị trí nào? Hãy chỉ hướng đi thưa Master!"
  }

  fun lineForPartyAssignment(hero: HeroData): String =
    if (hero.morale < 30) {
      "Không... tầng trước đám đồng đội đã chết sạch rồi, tôi không muốn vào đó!"
    } else {
      "Cứ giao vị trí tiên phong cho tôi."
    }

  fun lineForLowHpNearHouse(): String =
    "Vết thương rỉ máu mãi không ngừng... làm ơn cho tôi vào Nhà nghỉ ngơi..."

  fun lineForHighStarDisdain(highStar: HeroData, lowStar: HeroData): String =
    "Tránh đường ra đồ rác rưởi 1 sao!"

  fun lineForLowStarHumiliated(): String =
    "Đừng nhìn tôi bằng ánh mắt đó... tôi cũng đang cố gắng sống sót mà..."

  fun lineForSacrificePlea(hero: HeroData): String = when {
    hero.sanityState == HeroSanityState.SELF_AWARE || hero.name.contains("Han Ysl", ignoreCase = true) ->
      "Thì ra đây là số phận của những kẻ bị coi là thừa thãi trong cái lồng kính này..."
    hero.personality == LobbyPersonality.COWARD ->
      "Xin ngài Master! Tôi sẽ làm bất cứ điều gì, đừng biến tôi thành nguyên liệu dung hợp!"
    hero.personality == LobbyPersonality.ARROGANT ->
      "Ngươi dám hiến tế một chiến binh cao quý như ta ư?! Lời nguyền này sẽ bám theo ngươi!"
    hero.personality == LobbyPersonality.REBEL ->
      "Haha! Đốt cháy linh hồn ta đi, nhưng ý chí phản kháng này ngươi không bao giờ nuốt trọn được!"
    hero.personality == LobbyPersonality.LONER ->
      "Chết trong lò luyện hồn hay chết dưới móng vuốt quái vật... kết cục nào cũng lạnh lẽo như nhau."
    else ->
      "Nếu sinh mạng và máu thịt của tôi giúp đồng đội vượt qua Tầng Tháp tiếp theo... tôi chấp nhận."
  }

  fun lineForTapOrAmbient(hero: HeroData, nearHouse: Boolean, seed: Int = 0): String {
    if (hero.isLowHpNearHouse && nearHouse) {
      return lineForLowHpNearHouse()
    }
    if (hero.currentAssignment == HeroAssignment.IN_PARTY) {
      return lineForPartyAssignment(hero)
    }
    if (hero.sanityState == HeroSanityState.SELF_AWARE || hero.name.contains("Han Ysl", ignoreCase = true)) {
      return if (seed % 2 == 0) {
        "Lại định sắp xếp đội hình đấy à? Chọn người cho khôn ngoan vào."
      } else {
        "Bầu trời giả lập trên Thánh Địa này lúc nào cũng dừng ở một khoảnh khắc, ngài có nhận ra không?"
      }
    }
    if (hero.isLowHpNearHouse) {
      return "Mắt tôi mờ đi vì mất máu rồi... cần được băng bó tại Phòng Nghỉ Dưỡng ngay..."
    }
    if (hero.isFrightened || hero.morale < 30) {
      return if (seed % 2 == 0) {
        "Không... tầng trước đám đồng đội đã chết sạch rồi, tôi không muốn vào đó!"
      } else {
        "Làm ơn cho tôi ăn một Bữa Ăn Thịnh Soạn... tay tôi vẫn còn run không cầm nổi vũ khí!"
      }
    }
    return when (hero.personality) {
      LobbyPersonality.COWARD ->
        if (seed % 2 == 0) "Nghe nói hôm qua lại có người bước vào Lò Dung Hợp mà không bao giờ bước ra nữa..."
        else "Á! Đừng ném tôi vào lò dung hợp mà Master! Tôi xin ngài!"
      LobbyPersonality.ARROGANT ->
        if (seed % 2 == 0) "Kẻ yếu ớt chỉ có một hoặc hai sao như ngươi sớm muộn cũng vào lò dung hợp thôi!"
        else "Lưỡi kiếm này đã sẵn sàng nhuốm máu lũ quái vật trong Tháp Vĩnh Hằng!"
      LobbyPersonality.LONER ->
        "Đừng chạm vào vai tôi bất ngờ như thế, phản xạ sát thủ không phân biệt chủ nhân đâu."
      LobbyPersonality.REBEL ->
        "Mỗi vết sẹo sau chuyến leo tháp đều đánh thức một sức mạnh tiềm ẩn trong huyết quản."
      LobbyPersonality.LOYAL ->
        if (seed % 2 == 0) "Cứ giao vị trí tiên phong cho tôi."
        else "Mùi thịt nướng từ Nhà Ăn làm tôi quên đi nỗi kinh hoàng dưới chân Tháp Đá Đen."
    }
  }
}

