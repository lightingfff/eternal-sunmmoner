package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class HeroClassType(
  val labelVi: String,
  val roleSummaryVi: String,
  val skillNameVi: String,
  val skillDescVi: String,
  val preferredRange: Float,
  val skillCooldownSec: Float,
) {
  KNIGHT(
    labelVi = "Hiệp Sĩ (Knight)",
    roleSummaryVi = "Chiến binh tiền tuyến • Chống chịu & chắn đòn bảo vệ đồng đội",
    skillNameVi = "Khiên Chấn Địa",
    skillDescVi = "Nện khiên tạo sóng xung kích tầm gần, gây sát thương và thu hút quái vật xung quanh.",
    preferredRange = 46f,
    skillCooldownSec = 7.5f,
  ),
  ARCHER(
    labelVi = "Cung Thủ (Archer)",
    roleSummaryVi = "Xạ thủ tầm xa • Giữ khoảng cách và bắn tỉa liên hoàn",
    skillNameVi = "Mưa Tên Xuyên Phá",
    skillDescVi = "Bắn chùm 3 mũi tên ma lực xuyên giáp vào các mục tiêu nguy hiểm và lùi bước.",
    preferredRange = 235f,
    skillCooldownSec = 6.5f,
  ),
  MAGE(
    labelVi = "Pháp Sư (Mage)",
    roleSummaryVi = "Pháo đài ma thuật • Gây sát thương diện rộng lên cụm kẻ địch",
    skillNameVi = "Thiên Thạch Tinh Vân",
    skillDescVi = "Triệu hồi vụ nổ cổ ngữ diện rộng thiêu đốt nhóm kẻ địch nguy hiểm nhất.",
    preferredRange = 205f,
    skillCooldownSec = 8.5f,
  ),
  ASSASSIN(
    labelVi = "Sát Thủ (Assassin)",
    roleSummaryVi = "Đột kích tốc độ cao • Áp sát kết liễu mục tiêu máu thấp",
    skillNameVi = "Ảnh Trảm Đoạt Mệnh",
    skillDescVi = "Lướt xuyên bóng tối tới sau lưng mục tiêu, tung nhát chém chí mạng cực mạnh.",
    preferredRange = 42f,
    skillCooldownSec = 6.0f,
  ),
  PRIEST(
    labelVi = "Mục Sư (Priest)",
    roleSummaryVi = "Hỗ trợ thánh quang • Hồi phục HP cho đồng đội suy yếu",
    skillNameVi = "Thánh Quang Cứu Rỗi",
    skillDescVi = "Tạo vòng sáng hồi phục máu cho toàn đội hình ở gần và thanh tẩy sát thương.",
    preferredRange = 185f,
    skillCooldownSec = 8.0f,
  ),
  BERSERKER(
    labelVi = "Cuồng Chiến (Berserker)",
    roleSummaryVi = "Đấu sĩ cuồng nộ • Sát thương cận chiến bùng nổ khi máu giảm",
    skillNameVi = "Huyết Phong Trảm",
    skillDescVi = "Xoay đại đao quét sạch kẻ địch xung quanh, càng thấp máu sát thương càng cao.",
    preferredRange = 50f,
    skillCooldownSec = 7.0f,
  );

  companion object {
    fun fromRaw(raw: String): HeroClassType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: KNIGHT
  }
}

enum class PersonalityType(
  val labelVi: String,
  val emotionTagVi: String,
  val lobbyHabitVi: String,
  val sampleQuoteVi: String,
) {
  STOIC(
    labelVi = "Điềm Tĩnh",
    emotionTagVi = "[Điềm tĩnh]",
    lobbyHabitVi = "Thường đứng rèn kiếm ở Sân Tập hoặc canh gác trước Cổng Tháp.",
    sampleQuoteVi = "Chỉ cần Master ra lệnh, tấm khiên này sẽ không lùi nửa bước.",
  ),
  RECKLESS(
    labelVi = "Nóng Nảy",
    emotionTagVi = "[Hào hứng]",
    lobbyHabitVi = "Luôn tập kích hình nộm ở Sân Tập và nóng lòng leo tầng mới.",
    sampleQuoteVi = "Đứng mãi ở Sảnh Thánh Địa ngứa tay quá! Khi nào xuất chinh đây Master?",
  ),
  CALCULATING(
    labelVi = "Điềm Đạm Phân Tích",
    emotionTagVi = "[Cẩn trọng]",
    lobbyHabitVi = "Hay lui tới Phòng Tổng Hợp và quan sát cổ ngữ không gian.",
    sampleQuoteVi = "Quái vật tầng trên không chỉ mạnh hơn... chúng biết phối hợp phục kích đấy.",
  ),
  DEVOTED(
    labelVi = "Tận Tụy",
    emotionTagVi = "[Kính trọng]",
    lobbyHabitVi = "Chăm hỏi thăm đồng đội ở Khu Nghỉ Ngơi và Nhà Ăn.",
    sampleQuoteVi = "Mọi người trở về bình an là chiến thắng lớn nhất rồi.",
  ),
  CYNICAL(
    labelVi = "Lạnh Lùng",
    emotionTagVi = "[Thực dụng]",
    lobbyHabitVi = "Ngồi góc vắng Nhà Ăn hoặc kiểm tra vũ khí tại Kho Trang Bị.",
    sampleQuoteVi = "Trong tòa tháp này, kẻ do dự một giây sẽ hóa thành tro bụi.",
  ),
  LOYAL(
    labelVi = "Trung Thành",
    emotionTagVi = "[Quyết tâm]",
    lobbyHabitVi = "Tuần tra quanh Quảng Trường Thánh Địa bảo vệ Master.",
    sampleQuoteVi = "Dù là tầng 1 hay tầng 100, đội hình của chúng ta nhất định sẽ sống sót!",
  );

  companion object {
    fun fromRaw(raw: String): PersonalityType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: STOIC
  }
}

enum class HeroStatusType(val labelVi: String) {
  READY("Sẵn Sàng"),
  INJURED("Đang Bị Thương"),
  RESTING("Đang Nghỉ Ngơi"),
  TRAINING("Đang Tập Luyện");

  companion object {
    fun fromRaw(raw: String): HeroStatusType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: READY
  }
}

enum class FacilityType(
  val titleVi: String,
  val subtitleVi: String,
  val unlockFloorReq: Int,
) {
  TOWER_GATE(
    titleVi = "Tower Gate • Cổng Tháp",
    subtitleVi = "Cổng không gian khổng lồ dẫn lên các tầng của Tòa Tháp Vĩnh Hằng",
    unlockFloorReq = 0,
  ),
  SUMMONING_HALL(
    titleVi = "Summoning Hall • Điện Triệu Hồi",
    subtitleVi = "Nghi thức cổ ngữ chiêu mộ Anh hùng mới bằng Mảnh Linh Hồn & Tinh Thể Mana",
    unlockFloorReq = 0,
  ),
  TRAINING_GROUND(
    titleVi = "Training Ground • Sân Huấn Luyện",
    subtitleVi = "Rèn luyện thực chiến, đột phá cấp độ và kỹ năng chiến đấu",
    unlockFloorReq = 0,
  ),
  ARMORY(
    titleVi = "Armory • Kho Vũ Khí",
    subtitleVi = "Quản lý binh khí, giáp trụ và trang bị trực tiếp cho từng Anh hùng",
    unlockFloorReq = 0,
  ),
  HERO_QUARTERS(
    titleVi = "Hero Quarters • Khu Nghỉ Ngơi",
    subtitleVi = "Phòng sinh hoạt hồi phục chấn thương sau nhiệm vụ thất bại hoặc khốc liệt",
    unlockFloorReq = 0,
  ),
  SYNTHESIS_LAB(
    titleVi = "Synthesis Lab • Phòng Tổng Hợp",
    subtitleVi = "Dung hợp Quặng Sắt và Tinh Hoa Cổ Ngữ để rèn và cường hóa thần binh",
    unlockFloorReq = 0,
  ),
  RESTAURANT(
    titleVi = "Sanctuary Bistro • Nhà Ăn",
    subtitleVi = "Bữa ăn nóng hồi phục Sĩ Khí (Morale) và gắn kết tình đồng đội",
    unlockFloorReq = 0,
  ),
}

enum class TacticalOrder(
  val labelVi: String,
  val shortVi: String,
  val hotkey: String,
  val descriptionVi: String,
) {
  FOLLOW("Theo Đội Hình", "Follow", "1", "Di chuyển bám sát đội hình quanh Master và tự động đánh trả."),
  HOLD_POSITION("Giữ Vị Trí", "Hold", "2", "Chốt chặn tại vị trí hiện tại, không tự ý đuổi xa."),
  ATTACK_TARGET("Tấn Công Mục Tiêu", "Attack", "3", "Ưu tiên công kích mục tiêu đã chọn hoặc mối đe dọa gần nhất."),
  FOCUS_FIRE("Tập Trung Hỏa Lực", "Focus", "4", "Toàn bộ đội hình dồn sát thương và kỹ năng vào 1 kẻ địch chỉ định."),
  PROTECT_ALLY("Bảo Vệ Đồng Đội", "Protect", "5", "Thu hẹp đội hình bảo vệ thành viên yếu máu nhất và Master."),
  REGROUP("Tập Hợp", "Regroup", "6", "Lập tức rút về sát bên Master để chỉnh đốn đội ngũ."),
  RETREAT("Rút Lui", "Retreat", "7", "Ngừng truy kích, tăng tốc lùi về hướng an toàn phía sau."),
  MOVE_TO("Di Chuyển Tới", "Move To", "8", "Chạm lên bản đồ để cắm cờ điều động cả đội tiến tới vị trí đó."),
}

enum class TrainingFocus(
  val titleVi: String,
  val shortVi: String,
  val descVi: String,
  val hpBonus: Long,
  val atkBonus: Long,
  val defBonus: Long,
  val spdBonus: Long,
  val isSkillUpgrade: Boolean = false,
) {
  BALANCED(
    titleVi = "Huấn Luyện Toàn Diện",
    shortVi = "Toàn Diện",
    descVi = "Tăng cân bằng Sinh Lực, Công Kích, Phòng Thủ và Tốc Độ theo hệ phái.",
    hpBonus = 6L,
    atkBonus = 2L,
    defBonus = 1L,
    spdBonus = 1L,
  ),
  STRENGTH_ATK(
    titleVi = "Rèn Binh Khí • Cường Công",
    shortVi = "Cường Công",
    descVi = "Tập trung đòn đánh uy lực: ưu tiên tăng mạnh ATK (+6 ATK, +10 HP).",
    hpBonus = 10L,
    atkBonus = 6L,
    defBonus = 0L,
    spdBonus = 1L,
  ),
  VITALITY_HP(
    titleVi = "Thể Lực Bền Bỉ • Sinh Lực",
    shortVi = "Sinh Lực",
    descVi = "Rèn luyện sức chịu đựng dẻo dai: tăng mạnh HP (+34 HP, +2 DEF).",
    hpBonus = 34L,
    atkBonus = 1L,
    defBonus = 2L,
    spdBonus = 0L,
  ),
  GUARD_DEF(
    titleVi = "Thiết Giáp Trận • Phòng Ngự",
    shortVi = "Phòng Ngự",
    descVi = "Luyện thế thủ thuẫn giáp vững chắc: tăng mạnh DEF (+5 DEF, +18 HP).",
    hpBonus = 18L,
    atkBonus = 1L,
    defBonus = 5L,
    spdBonus = 0L,
  ),
  AGILITY_SPD(
    titleVi = "Bộ Pháp Tinh Linh • Tốc Độ",
    shortVi = "Tốc Độ",
    descVi = "Tăng tốc độ di chuyển và phản xạ chiến trường (+5 SPD, +3 ATK).",
    hpBonus = 8L,
    atkBonus = 3L,
    defBonus = 0L,
    spdBonus = 5L,
  ),
  ARCANE_SKILL(
    titleVi = "Lĩnh Ngộ Cổ Ngữ • Kỹ Năng",
    shortVi = "Kỹ Năng",
    descVi = "Dùng Tinh Thể Mana & Tinh Hoa Cổ Ngữ đột phá cấp kỹ năng đặc trưng.",
    hpBonus = 0L,
    atkBonus = 0L,
    defBonus = 0L,
    spdBonus = 0L,
    isSkillUpgrade = true,
  ),
}

enum class SanctuaryMealRecipe(
  val code: String,
  val titleVi: String,
  val subtitleVi: String,
  val rationCost: Long,
  val goldCost: Long,
  val moraleGain: Long,
  val durationSec: Long,
  val atkBuffPct: Int,
  val hpBuffPct: Int,
  val recoveryTrimSec: Long,
) {
  HERB_RATION_SOUP(
    code = "SOUP",
    titleVi = "Súp Thảo Dược Ấm",
    subtitleVi = "Hồi +35 Sĩ Khí toàn đội, giảm 15s hồi thương & +6% ATK / +5% HP (15 phút)",
    rationCost = 1L,
    goldCost = 20L,
    moraleGain = 35L,
    durationSec = 900L,
    atkBuffPct = 6,
    hpBuffPct = 5,
    recoveryTrimSec = 15L,
  ),
  ROAST_BEAST_PLATTER(
    code = "ROAST",
    titleVi = "Đại Tiệc Thịt Nướng Thánh Địa",
    subtitleVi = "Hồi 100% Sĩ Khí, giảm 25s hồi thương & Buff +12% ATK / +10% HP (30 phút)",
    rationCost = 2L,
    goldCost = 40L,
    moraleGain = 100L,
    durationSec = 1800L,
    atkBuffPct = 12,
    hpBuffPct = 10,
    recoveryTrimSec = 25L,
  ),
  ROYAL_ASTRAL_BANQUET(
    code = "BANQUET",
    titleVi = "Yến Tiệc Tinh Quang Hoàng Gia",
    subtitleVi = "Hồi 100% Sĩ Khí, giảm 45s hồi thương & Buff +18% ATK / +15% HP (45 phút)",
    rationCost = 3L,
    goldCost = 75L,
    moraleGain = 100L,
    durationSec = 2700L,
    atkBuffPct = 18,
    hpBuffPct = 15,
    recoveryTrimSec = 45L,
  );

  companion object {
    fun fromCode(code: String): SanctuaryMealRecipe =
      entries.firstOrNull { it.code == code } ?: ROAST_BEAST_PLATTER
  }
}

enum class IslandExpansionMilestone(
  val flagKey: String,
  val titleVi: String,
  val subtitleVi: String,
  val benefitVi: String,
  val reqMasterLevel: Long,
  val reqClearedFloor: Long,
  val goldCost: Long,
  val oreCost: Long,
  val essenceCost: Long,
) {
  EAST_CELESTIAL_GARDEN(
    flagKey = "EXP_EAST_GARDEN",
    titleVi = "Đài Vườn Tinh Quang Đông Bộ",
    subtitleVi = "Bắc cầu đá vòm từ tường thành phía Đông sang đảo phụ có suối thánh hồi phục và vườn anh đào.",
    benefitVi = "Giảm 25% thời gian hồi thương tại Khu Nghỉ Ngơi & giữ tối thiểu 30 Sĩ Khí sau viễn chinh.",
    reqMasterLevel = 1L,
    reqClearedFloor = 0L,
    goldCost = 180L,
    oreCost = 2L,
    essenceCost = 1L,
  ),
  NORTH_ASTRAL_DOCK(
    flagKey = "EXP_NORTH_DOCK",
    titleVi = "Bến Phi Thuyền Tinh Hà Đông Bắc",
    subtitleVi = "Mở rộng bến đá lơ lửng phía Đông Bắc để neo đậu thuyền trinh sát thu gom chiến lợi phẩm.",
    benefitVi = "Tăng +20% Vàng & Tinh Thể Mana thu hoạch từ mọi cuộc viễn chinh Tháp.",
    reqMasterLevel = 2L,
    reqClearedFloor = 1L,
    goldCost = 280L,
    oreCost = 3L,
    essenceCost = 2L,
  ),
}

@IgnoreExtraProperties
data class MasterProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val masterName: String = "Master",
  val masterLevel: Long = 1L,
  val masterExp: Long = 0L,
  val gold: Long = 600L,
  val manaCrystals: Long = 150L,
  val soulShards: Long = 40L,
  val rations: Long = 12L,
  val ironOre: Long = 8L,
  val arcaneEssence: Long = 6L,
  val highestClearedFloor: Long = 0L,
  val totalExpeditions: Long = 0L,
  val unlockedFacilities: List<String> = listOf(
    FacilityType.TOWER_GATE.name,
    FacilityType.SUMMONING_HALL.name,
    FacilityType.TRAINING_GROUND.name,
    FacilityType.ARMORY.name,
    FacilityType.HERO_QUARTERS.name,
    FacilityType.SYNTHESIS_LAB.name,
    FacilityType.RESTAURANT.name,
  ),
  val storyFlags: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class HeroRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val title: String = "",
  val heroClass: String = HeroClassType.KNIGHT.name,
  val rarity: Long = 1L,
  val level: Long = 1L,
  val exp: Long = 0L,
  val maxHp: Long = 350L,
  val attack: Long = 35L,
  val defense: Long = 20L,
  val speed: Long = 100L,
  val skillLevel: Long = 1L,
  val personality: String = PersonalityType.STOIC.name,
  val status: String = HeroStatusType.READY.name,
  val injuryRecoveryUntilEpochSec: Long = 0L,
  val morale: Long = 100L,
  val equippedWeaponId: String = "",
  val equippedArmorId: String = "",
  val inActiveSquad: Boolean = false,
  val bondSummary: String = "",
  val currentHp: Long = -1L,
  val sanityState: String = HeroSanityState.NORMAL.name,
  val currentAssignment: String = "",
  val partyJoinedOrder: Long = 0L,
  val restStartedAtEpochSec: Long = 0L,
  val hiddenTrait: HeroHiddenTrait = HeroHiddenTrait(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val classType: HeroClassType
    get() = HeroClassType.fromRaw(heroClass)

  val personalityType: PersonalityType
    get() = PersonalityType.fromRaw(personality)

  val statusType: HeroStatusType
    get() = HeroStatusType.fromRaw(status)

  fun isCurrentlyInjured(nowEpochSec: Long = System.currentTimeMillis() / 1000L): Boolean {
    return status == HeroStatusType.INJURED.name && injuryRecoveryUntilEpochSec > nowEpochSec
  }

  fun remainingInjurySeconds(nowEpochSec: Long = System.currentTimeMillis() / 1000L): Long {
    return (injuryRecoveryUntilEpochSec - nowEpochSec).coerceAtLeast(0L)
  }

  fun consistentActivityLabelVi(nowEpochSec: Long = System.currentTimeMillis() / 1000L): String {
    return when {
      isCurrentlyInjured(nowEpochSec) -> "Đang dưỡng thương tại Khu Nghỉ Ngơi (${remainingInjurySeconds(nowEpochSec)}s)"
      status == HeroStatusType.INJURED.name -> "Vừa bình phục • Chờ nhận lệnh tại Khu Nghỉ Ngơi"
      status == HeroStatusType.TRAINING.name -> "Đang rèn luyện tại Sân Tập"
      status == HeroStatusType.RESTING.name -> "Đang nghỉ ngơi hồi phục tại Khu Nghỉ Ngơi"
      inActiveSquad -> "Trực chiến Đội Viễn Chinh • Tập hợp chờ tại Cổng Tháp"
      else -> "Sinh hoạt tự do tại Thánh Địa"
    }
  }

  fun previewWithCandidateItem(candidate: EquipmentRecord): HeroRecord {
    return if (candidate.slot == "ARMOR") {
      copy(equippedArmorId = candidate.id)
    } else {
      copy(equippedWeaponId = candidate.id)
    }
  }

  fun totalMaxHp(equipment: List<EquipmentRecord>): Int {
    val bonus = equipment.filter { it.id == equippedWeaponId || it.id == equippedArmorId }
      .sumOf { it.bonusHp }
    return (maxHp + bonus).toInt()
  }

  fun totalAttack(equipment: List<EquipmentRecord>): Int {
    val bonus = equipment.filter { it.id == equippedWeaponId || it.id == equippedArmorId }
      .sumOf { it.bonusAttack }
    val moraleMultiplier = 0.85f + (morale.coerceIn(0L, 100L) / 100f) * 0.25f
    return ((attack + bonus) * moraleMultiplier).toInt().coerceAtLeast(5)
  }

  fun totalDefense(equipment: List<EquipmentRecord>): Int {
    val bonus = equipment.filter { it.id == equippedWeaponId || it.id == equippedArmorId }
      .sumOf { it.bonusDefense }
    return (defense + bonus).toInt()
  }

  fun totalSpeed(equipment: List<EquipmentRecord>): Int {
    val bonus = equipment.filter { it.id == equippedWeaponId || it.id == equippedArmorId }
      .sumOf { it.bonusSpeed }
    return (speed + bonus).toInt()
  }
}

@IgnoreExtraProperties
data class EquipmentRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val slot: String = "WEAPON",
  val rarity: Long = 1L,
  val level: Long = 1L,
  val bonusHp: Long = 0L,
  val bonusAttack: Long = 10L,
  val bonusDefense: Long = 0L,
  val bonusSpeed: Long = 0L,
  val iconType: String = "LONGSWORD",
  val equippedByHeroId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
