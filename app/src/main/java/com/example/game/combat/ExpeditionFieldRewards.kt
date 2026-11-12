package com.example.game.combat

import java.util.Collections

enum class FieldRewardKind { READINESS, WARD, HEALING, VALUABLES }

data class FieldRewardOption(
  val id: String,
  val kind: FieldRewardKind,
  val titleVi: String,
  val detailVi: String,
  val gold: Long = 0L,
  val mana: Long = 0L,
)

data class FieldRewardOffer(
  val siteId: String,
  val titleVi: String,
  val options: List<FieldRewardOption>,
)

data class FieldRewardClaim(
  val siteId: String,
  val option: FieldRewardOption,
  val markerId: String,
  val gold: Long,
  val mana: Long,
  val affectedHeroIds: Set<String>,
)

/** Per-attempt field supplies. The combat engine owns settlement and all observation inputs. */
class ExpeditionFieldRewards {
  private val pending = linkedMapOf<String, FieldRewardOffer>()
  private val consumedSites = linkedSetOf<String>()
  private val markers = linkedSetOf<String>()

  val pendingOffers: List<FieldRewardOffer>
    get() = Collections.unmodifiableList(pending.values.toList())

  /** A detached snapshot: a previous caller cannot mutate or observe later consumption. */
  val claimedIds: Set<String>
    get() = Collections.unmodifiableSet(LinkedHashSet(markers))

  fun observe(sites: List<ExpeditionSiteKnowledge>) {
    for (site in sites) {
      if (site.state != ExpeditionSiteState.RESOLVED || site.id in consumedSites || site.id in pending) continue
      val offer = AUTHORED_OFFERS[site.id] ?: continue
      pending[site.id] = offer
    }
  }

  fun canClaim(
    siteId: String,
    optionId: String,
    heroes: List<HeroCombatUnit>,
    stage: MissionStage,
  ): Boolean {
    if (stage != MissionStage.IN_PROGRESS) return false
    val option = pending[siteId]?.options?.firstOrNull { it.id == optionId } ?: return false
    return heroes.any { isLiving(it) && benefits(option.kind, it) }
  }

  /** Returns resource increments once; callers add them to the existing settlement counters. */
  fun claim(
    siteId: String,
    optionId: String,
    heroes: List<HeroCombatUnit>,
    stage: MissionStage,
  ): FieldRewardClaim? {
    if (!canClaim(siteId, optionId, heroes, stage)) return null
    val option = pending.getValue(siteId).options.first { it.id == optionId }
    val affected = linkedSetOf<String>()
    heroes.asSequence().filter(::isLiving).distinctBy { it.id }.forEach { hero ->
      if (benefits(option.kind, hero)) {
        when (option.kind) {
          FieldRewardKind.READINESS -> hero.skillCooldown =
            (hero.skillCooldown - hero.maxSkillCooldown * .60f).coerceAtLeast(0f)
          FieldRewardKind.WARD -> hero.shieldHp = maxOf(hero.shieldHp, quarterHp(hero))
          FieldRewardKind.HEALING -> hero.hp = minOf(
            hero.maxHp.toLong(), hero.hp.toLong() + quarterHp(hero).toLong(),
          ).toInt()
          FieldRewardKind.VALUABLES -> Unit
        }
        if (option.kind != FieldRewardKind.VALUABLES) affected += hero.id
      }
    }
    pending.remove(siteId)
    consumedSites += siteId
    val marker = "field_${siteId}_${option.kind.name.lowercase(java.util.Locale.ROOT)}"
    markers += marker
    return FieldRewardClaim(
      siteId = siteId,
      option = option,
      markerId = marker,
      gold = option.gold,
      mana = option.mana,
      affectedHeroIds = Collections.unmodifiableSet(LinkedHashSet(affected)),
    )
  }

  private fun isLiving(hero: HeroCombatUnit): Boolean = !hero.knockedOut && hero.hp > 0 && hero.maxHp > 0

  private fun quarterHp(hero: HeroCombatUnit): Int = (hero.maxHp / 4).coerceAtLeast(1)

  private fun benefits(kind: FieldRewardKind, hero: HeroCombatUnit): Boolean = when (kind) {
    FieldRewardKind.READINESS -> hero.skillCooldown.isFinite() && hero.skillCooldown > 0f
    FieldRewardKind.WARD -> hero.shieldHp < quarterHp(hero)
    FieldRewardKind.HEALING -> hero.hp < hero.maxHp
    FieldRewardKind.VALUABLES -> true
  }

  companion object {
    private fun options(vararg values: FieldRewardOption): List<FieldRewardOption> =
      Collections.unmodifiableList(values.toList())

    private fun valuables(site: String, gold: Long, mana: Long) = FieldRewardOption(
      id = "${site}_valuables",
      kind = FieldRewardKind.VALUABLES,
      titleVi = "Giữ chiến lợi phẩm",
      detailVi = "+$gold Vàng • +$mana Mana khi kết thúc chuyến, kể cả thua hoặc rút lui. Không hỗ trợ giao tranh hiện tại.",
      gold = gold,
      mana = mana,
    )

    private val AUTHORED_OFFERS = mapOf(
      "north" to FieldRewardOffer("north", "Túi tiếp tế trạm Bắc", options(
        FieldRewardOption("north_readiness", FieldRewardKind.READINESS, "Chỉnh đốn kỹ năng",
          "Dùng ngay: giảm hồi kỹ năng bằng 60% thời gian hồi tối đa cho Anh hùng còn sống. Không bỏ qua khóa lệnh Master."),
        valuables("north", 60L, 10L),
      )),
      "south" to FieldRewardOffer("south", "Di vật phòng hộ lối Nam", options(
        FieldRewardOption("south_ward", FieldRewardKind.WARD, "Chuẩn bị khiên hộ mệnh",
          "Dùng ngay: nâng khiên của Anh hùng còn sống lên 25% HP tối đa trong chuyến này. Giữ khiên đang mạnh hơn; không hồi sinh."),
        valuables("south", 65L, 15L),
      )),
      "east" to FieldRewardOffer("east", "Hòm cứu thương cổng Đông", options(
        FieldRewardOption("east_healing", FieldRewardKind.HEALING, "Băng bó tại chỗ",
          "Dùng ngay: hồi 25% HP tối đa cho Anh hùng còn sống, không vượt HP tối đa. Không hồi sinh."),
        valuables("east", 75L, 20L),
      )),
    )
  }
}
