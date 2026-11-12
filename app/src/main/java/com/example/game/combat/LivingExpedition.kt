package com.example.game.combat

import com.example.data.model.HeroClassType
import com.example.data.model.TacticalOrder
import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import kotlin.math.hypot

/** Exploration directs the existing actors and combat AI; it never simulates a second battle. */
class LivingExpedition internal constructor(
  private val engine: TowerCombatEngine,
  private val courier: CourierCombatUnit,
) {
  private enum class Activity { TRAVEL, DECISION, SCOUT, ASSAULT, RESCUE, PLATES, FORCE, RETURN, FINISHED }
  private var activity = Activity.TRAVEL
  private var selectedSite = "north"
  private val resolved = linkedSetOf<String>()
  private val revealed = linkedSetOf<String>()
  private val completed = linkedSetOf<String>()
  private val spawned = linkedSetOf<String>()
  private val paths = mutableMapOf<String, List<Pair<Float, Float>>>()
  private val pathIndices = mutableMapOf<String, Int>()
  private val history = mutableListOf<ExpeditionConversation>()
  private var conversationSerial = 0
  private var witnessWaypoint = 0
  private var scoutActivated = false
  private val latchedPlates = linkedSetOf<Int>()
  private val breadcrumbs = mutableListOf(LivingExpeditionContent.camp)

  val active: Boolean get() = activity != Activity.FINISHED
  val readyToExtract: Boolean get() = resolved.size >= 2
  var automationEnabled = true
    private set
  val canRedirect: Boolean get() = active && engine.stage == MissionStage.IN_PROGRESS &&
    resolved.size < LivingExpeditionContent.sites.size && activity in setOf(Activity.TRAVEL, Activity.DECISION) &&
    (decision == null || decision?.id == "extraction")
  var decision: ExpeditionDecision? = null
    private set
  var witness: ExpeditionWitness? = null
    private set
  var alarmSilenced = false
    private set
  var gateForced = false
    private set
  var gateSecured = false
    private set
  var statusVi = "Đội tự tìm dấu vết trên lối Bắc. Master có thể đổi hướng khám phá hoặc ra lệnh chiến thuật."
    private set
  val objectiveVi: String get() = when {
    !active -> "Đã về trại • bắt đầu hộ tống"
    activity == Activity.RETURN -> "Đưa toàn đội về trại để hộ tống"
    readyToExtract && decision?.id == "extraction" -> "Đã có hai nguồn tin • rút về hay khám phá tiếp?"
    else -> "Khám phá đồng cỏ • ${resolved.size}/2 nguồn tin"
  }
  val completedIds: Set<String> get() = completed.toSet()
  val conversations: List<ExpeditionConversation> get() = history.toList()
  val sites: List<ExpeditionSiteKnowledge> get() = LivingExpeditionContent.sites.map { site ->
    val state = when (site.id) {
      in resolved -> ExpeditionSiteState.RESOLVED
      in revealed -> ExpeditionSiteState.REVEALED
      else -> ExpeditionSiteState.UNKNOWN
    }
    ExpeditionSiteKnowledge(site.id, if (state == ExpeditionSiteState.UNKNOWN) "Dấu vết chưa rõ" else site.titleVi,
      site.point.first, site.point.second, state,
      if (state == ExpeditionSiteState.UNKNOWN) "Chưa có Anh hùng quan sát khu vực này." else site.detailVi)
  }
  private val living: List<HeroCombatUnit> get() = engine.heroes.filter { !it.knockedOut }
  private val sitePoint: Pair<Float, Float> get() = LivingExpeditionContent.sites.first { it.id == selectedSite }.point
  private val target: Pair<Float, Float> get() = when (activity) {
    Activity.RETURN -> LivingExpeditionContent.camp
    Activity.SCOUT -> LivingExpeditionContent.northSwitch
    Activity.RESCUE -> LivingExpeditionContent.witnessSafeRoute.getOrElse(witnessWaypoint) { LivingExpeditionContent.hub }
    Activity.TRAVEL -> living.firstOrNull()?.let { pathTarget(it) } ?: sitePoint
    else -> sitePoint
  }
  val targetX: Float get() = target.first
  val targetY: Float get() = target.second

  init {
    startTravel("north")
    say(null, "Người đưa thư: Tôi chờ ở trại. Hãy tìm hai nguồn tin; các lối rẽ vẫn còn điều chưa biết.")
  }

  fun suspendAutomation() { automationEnabled = false }

  fun explore(siteId: String? = null): Boolean {
    if (!active || engine.stage != MissionStage.IN_PROGRESS) return false
    if (activity == Activity.RETURN) {
      if (siteId != null) return false
      automationEnabled = true
      return true
    }
    if (siteId != null && LivingExpeditionContent.sites.none { it.id == siteId }) return false
    if (siteId != null && siteId in resolved) return false
    // A tactical interruption resumes the actual unresolved mechanic, rather than restarting it.
    if (siteId == null || siteId == selectedSite) {
      automationEnabled = true
      return true
    }
    if (!canRedirect) return false
    automationEnabled = true
    decision = null
    startTravel(siteId)
    announce("Đội đổi hướng. Anh hùng sẽ tự quan sát và báo cáo khi tìm thấy điều mới.")
    return true
  }

  fun choose(optionId: String): Boolean {
    if (!active || engine.stage != MissionStage.IN_PROGRESS) return false
    val pending = decision ?: return false
    if (pending.options.none { it.id == optionId && it.enabled }) return false
    if (optionId == "south_medicine" && living.none { hero ->
      hero.hp > 0 && distance(hero.x to hero.y, witness?.let { it.x to it.y } ?: sitePoint) <= 260f
    }) return false
    decision = null
    automationEnabled = true
    when (optionId) {
      "north_scout" -> {
        activity = Activity.SCOUT
        spawnNorth(false)
        say(scout()?.id, "Tôi luồn tới dây tù và. Mọi người giữ chân lính gác, rồi tập hợp ở trạm!")
      }
      "north_assault" -> {
        activity = Activity.ASSAULT
        spawnNorth(true)
        say(frontline()?.id, "Đánh thẳng qua trạm! Tín hiệu còn nguyên, phía trước sẽ có thêm xạ thủ.")
      }
      "south_rescue" -> {
        activity = Activity.RESCUE
        say(frontline()?.id, "Đi sát tôi. Cả đội che cho nhân chứng về ngã rẽ an toàn!")
      }
      "south_medicine" -> {
        living.forEach { it.hp = (it.hp + (it.maxHp * .35f).toInt()).coerceAtMost(it.maxHp) }
        completed += "living_medicine_squad"
        say(null, "Đã dùng túi thuốc hồi 35% HP cho đội. Nhân chứng vẫn ở lại lối Nam, không được tính là đã cứu.")
        resolve("south", "Đội ghi lại lời cảnh báo của nhân chứng và mang thuốc đi.")
      }
      "east_cooperate" -> {
        activity = Activity.PLATES
        say(frontline()?.id, if (living.size >= 2) "Hai người giữ hai phiến đá cùng lúc. Tôi giữ bên trái!"
          else "Chỉ còn một người: chốt phụ cho phép gài từng cần riêng. Tôi phải tới đủ cả hai.")
      }
      "east_force" -> {
        activity = Activity.FORCE
        gateForced = true
        spawn("east_0", EnemyArchetype.FIELD_GOBLIN_RAIDER, 2300f, 1035f)
        spawn("east_1", EnemyArchetype.FIELD_GOBLIN_RAIDER, 2340f, 1140f)
        say(frontline()?.id, "Khóa đã vỡ! Hai lính gác nghe tiếng động đang tới.")
      }
      "evacuate" -> {
        activity = Activity.RETURN
        buildPaths(LivingExpeditionContent.camp)
        announce("Đã chọn rút về. Toàn đội phải thực sự tới trại rồi mới đưa người đưa thư lên đường.")
      }
      else -> startTravel(optionId.removePrefix("explore_"))
    }
    return true
  }

  internal fun directiveFor(hero: HeroCombatUnit): ExpeditionDirective? {
    if (!active || !automationEnabled || hero.knockedOut) return null
    fun move(point: Pair<Float, Float>, intent: String) = ExpeditionDirective(TacticalOrder.MOVE_TO, point, intent)
    fun guard(point: Pair<Float, Float>, intent: String) = ExpeditionDirective(TacticalOrder.PROTECT_ALLY, point, intent)
    return when (activity) {
      Activity.TRAVEL, Activity.RETURN -> move(pathTarget(hero), if (activity == Activity.RETURN) "Tự trở về trại" else "Dò đường • quan sát dấu vết")
      Activity.SCOUT -> if (!scoutActivated && hero.id == scout()?.id) move(LivingExpeditionContent.northSwitch, "Luồn tới chốt tù và")
        else guard(sitePoint, if (scoutActivated) "Tập hợp sau khi cắt tín hiệu" else "Giữ chân lính gác cho trinh sát")
      Activity.RESCUE -> {
        val npc = witness ?: return null
        if (hero.id == frontline()?.id) {
          val point = LivingExpeditionContent.witnessSafeRoute.getOrElse(witnessWaypoint) { LivingExpeditionContent.hub }
          move(if (distance(hero.x to hero.y, npc.x to npc.y) > 135f) npc.x to npc.y else point, "Dẫn nhân chứng • chờ người bị thương")
        } else guard(npc.x to npc.y, "Che chắn cho nhân chứng")
      }
      Activity.PLATES -> {
        val actors = plateActors()
        if (living.size == 1) move(LivingExpeditionContent.eastPlates[if (0 in latchedPlates) 1 else 0], "Gài lần lượt hai cần khóa phụ")
        else when (hero.id) {
          actors.getOrNull(0)?.id -> move(LivingExpeditionContent.eastPlates[0], "Giữ phiến đá trái")
          actors.getOrNull(1)?.id -> move(LivingExpeditionContent.eastPlates[1], "Phối hợp • giữ phiến đá phải")
          else -> guard(sitePoint, "Cảnh giới cho đồng đội mở cổng")
        }
      }
      else -> guard(sitePoint, if (decision != null) "Quan sát • chờ quyết định Master" else "Bảo vệ đội • xử lý mối đe dọa")
    }
  }

  internal fun step(dt: Float) {
    if (!dt.isFinite() || dt <= 0f || engine.stage != MissionStage.IN_PROGRESS) return
    for (index in history.indices) {
      val line = history[index]
      if (line.remainingSeconds > 0f) history[index] = line.copy(remainingSeconds = (line.remainingSeconds - dt).coerceAtLeast(0f))
    }
    witness?.let { it.hurtFlash = (it.hurtFlash - dt * 3.5f).coerceAtLeast(0f); it.isMoving = false }
    if (!active) return
    courier.isMoving = false
    // Manual orders replace steering, while world observations and vulnerable NPCs stay live.
    if (activity == Activity.TRAVEL || activity == Activity.RETURN) {
      living.forEach { hero ->
        val path = paths[hero.id] ?: return@forEach
        val index = pathIndices[hero.id] ?: 0
        if (index < path.size && distance(hero.x to hero.y, path[index]) <= 95f) {
          pathIndices[hero.id] = index + 1
          if (breadcrumbs.lastOrNull() != path[index]) {
            breadcrumbs += path[index]
            if (breadcrumbs.size > 32) breadcrumbs.removeAt(0)
          }
        }
      }
      if (activity == Activity.RETURN) {
        if (living.isNotEmpty() && living.all { distance(it.x to it.y, LivingExpeditionContent.camp) <= 175f }) {
          activity = Activity.FINISHED
          completed += "living_returned_to_camp"
          announce("Toàn đội đã về trại. Báo cáo đã đủ; bây giờ bảo vệ người đưa thư tới cổng.")
        }
        return
      }
      val observerArrived = living.any { distance(it.x to it.y, sitePoint) <= 260f }
      val escortArrived = selectedSite != "south" || frontline()?.let { distance(it.x to it.y, sitePoint) <= 200f } == true
      if (observerArrived && escortArrived) revealSite()
    }
    when (activity) {
      Activity.SCOUT -> {
        if (!scoutActivated && living.any { it.id == scout()?.id && distance(it.x to it.y, LivingExpeditionContent.northSwitch) <= 70f }) {
          scoutActivated = true
          alarmSilenced = true
          completed += "living_alarm_silenced"
          say(scout()?.id, "Đã cắt dây tù và. Tôi quay về che đội; đường hộ tống sẽ bớt một xạ thủ.")
        }
        if (scoutActivated && living.isNotEmpty() && living.all { distance(it.x to it.y, sitePoint) <= 170f })
          resolve("north", "Bản đồ trạm Bắc chỉ ra đường cổng và điểm phục kích. Tín hiệu đã tắt.")
      }
      Activity.RESCUE -> moveWitness(dt)
      Activity.PLATES -> observePlates()
      else -> Unit
    }
  }

  internal fun afterStep() {
    if (!active || engine.stage != MissionStage.IN_PROGRESS) return
    if (living.isEmpty()) return
    val npc = witness
    if (npc != null && !npc.alive && "south" !in resolved) {
      completed += "living_witness_lost"
      resolve("south", "Nhân chứng đã gục. Đội chỉ tìm được lời cảnh báo trong nhật ký; không có ai được cứu.")
    }
    if (activity == Activity.ASSAULT && enemiesCleared("north_")) {
      completed += "living_alarm_left"
      resolve("north", "Đã chiếm bản đồ trạm Bắc. Tín hiệu còn nguyên: đường hộ tống sẽ có thêm xạ thủ.")
    }
    if (activity == Activity.FORCE && enemiesCleared("east_")) {
      gateSecured = true
      completed += "living_gate_forced"
      resolve("east", "Đã hạ hai lính gác và mở lối. Tiếng phá khóa khiến lính cổng cảnh giác.")
    }
  }

  internal fun damageWitness(raw: Int) {
    val npc = witness ?: return
    if (!npc.alive || npc.rescued || raw <= 0) return
    npc.hp = (npc.hp - raw).coerceAtLeast(0)
    npc.hurtFlash = 1f
  }

  internal fun retreatWaypoints(): List<Pair<Float, Float>> {
    val lead = living.firstOrNull() ?: return listOf(LivingExpeditionContent.camp)
    return returnCorridor(lead.x to lead.y) + listOf(960f to 1085f, 680f to 1040f, LivingExpeditionContent.camp)
  }

  private fun revealSite() {
    if (!revealed.add(selectedSite)) return
    activity = Activity.DECISION
    when (selectedSite) {
      "north" -> {
        say(scout()?.id, "Có dây tù và phía sau trạm. Tôi có thể luồn qua, nhưng cần người giữ chân lính gác.")
        decision = ExpeditionDecision("north_approach", "Xử lý trạm gác bằng cách nào?", listOf(
          ExpeditionOption("north_scout", "Luồn cắt tín hiệu", "Chia đội giữ 2 lính, cắt tù và để bớt xạ thủ đường về."),
          ExpeditionOption("north_assault", "Đánh thẳng vào trạm", "Đánh 3 địch; để nguyên tù và, đường về có thêm xạ thủ.")))
      }
      "south" -> {
        witness = ExpeditionWitness()
        spawn("south_0", EnemyArchetype.DIRE_WOLF, 1820f, 1490f)
        spawn("south_1", EnemyArchetype.DIRE_WOLF, 1860f, 1610f)
        say(null, "Nhân chứng: Sói đang tới! Dẫn tôi về ngã rẽ… hoặc mang thuốc đi cứu đội của các anh.")
        decision = ExpeditionDecision("south_witness", "Giúp người bị thương hay dùng túi thuốc?", listOf(
          ExpeditionOption("south_rescue", "Dẫn nhân chứng ra ngoài", "Che nhân chứng về ngã rẽ. Nhận tin phục kích và băng bảo vệ."),
          ExpeditionOption("south_medicine", "Dùng thuốc cho đội", "Hồi 35% HP đội; để nhân chứng ở lại giữa bầy sói.")))
      }
      "east" -> {
        say(frontline()?.id, "Hai phiến đá giữ khóa. Chúng ta có thể chia người, hoặc phá khóa gọi lính gác tới.")
        decision = ExpeditionDecision("east_gate", "Mở cổng đá bằng cách nào?", listOf(
          ExpeditionOption("east_cooperate", "Phối hợp giữ hai phiến", "Hai người giữ hai ấn; một người thì gài hai cần phụ."),
          ExpeditionOption("east_force", "Phá khóa cổng", "Đánh 2 lính ở đây; thêm 1 lính gác cùng Kraag.")))
      }
    }
  }

  private fun moveWitness(dt: Float) {
    val npc = witness ?: return
    if (!npc.alive || npc.rescued) return
    val point = LivingExpeditionContent.witnessSafeRoute.getOrNull(witnessWaypoint) ?: return
    if (living.none { distance(it.x to it.y, npc.x to npc.y) <= 210f }) return
    val d = distance(npc.x to npc.y, point)
    if (d <= 60f) {
      witnessWaypoint++
      if (witnessWaypoint == LivingExpeditionContent.witnessSafeRoute.size) {
        npc.rescued = true
        completed += "living_witness_rescued"
        courier.medicineWardHp = maxOf(courier.medicineWardHp, 40)
        say(null, "Nhân chứng: Tôi an toàn rồi! Giữ băng này cho người đưa thư. Cổng phía Đông có quân canh.")
        resolve("south", "Nhân chứng đã được dẫn về ngã rẽ an toàn và cung cấp tin về cổng.")
      }
      return
    }
    val amount = minOf(d, 65f * dt)
    val position = CollisionPhysics.resolvePosition(npc.x + (point.first - npc.x) / d * amount,
      npc.y + (point.second - npc.y) / d * amount, 18f, engine.mapLayout.width, engine.mapLayout.height,
      engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
    npc.facingRight = position.first >= npc.x
    npc.isMoving = distance(npc.x to npc.y, position) > .01f
    npc.x = position.first; npc.y = position.second
  }

  private fun observePlates() {
    val plates = LivingExpeditionContent.eastPlates
    if (living.size == 1) {
      val hero = living.first()
      plates.forEachIndexed { index, point -> if (distance(hero.x to hero.y, point) <= 70f) latchedPlates += index }
      if (latchedPlates.size < 2) return
      completed += "living_gate_solo_latches"
    } else {
      val first = living.filter { distance(it.x to it.y, plates[0]) <= 70f }
      val second = living.filter { distance(it.x to it.y, plates[1]) <= 70f }
      if (first.none { a -> second.any { b -> a.id != b.id } }) return
      completed += "living_gate_cooperation"
    }
    gateSecured = true
    resolve("east", "Khóa đã mở bằng phối hợp. Đội lấy được sơ đồ lối cổng mà không gọi thêm lính.")
  }

  private fun resolve(id: String, message: String) {
    if (!resolved.add(id)) return
    completed += "living_site_$id"
    decision = null
    announce(message)
    if (readyToExtract) {
      activity = Activity.DECISION
      decision = ExpeditionDecision("extraction", "Đã có hai nguồn tin. Master quyết định tiếp tục hay rút về?",
        listOf(ExpeditionOption("evacuate", "Trở về đón người đưa thư", "Đội tự về trại theo đường đã biết; hộ tống vẫn phải vượt trinh sát và Kraag.")) +
          LivingExpeditionContent.sites.filter { it.id !in resolved }.map {
            ExpeditionOption("explore_${it.id}", "Khám phá lối còn lại", "Tiếp tục tìm hiểu khu vực chưa hoàn tất trước khi về trại.")
          })
    } else startTravel(LivingExpeditionContent.sites.first { it.id !in resolved }.id)
  }

  private fun startTravel(id: String) {
    selectedSite = id
    activity = Activity.TRAVEL
    decision = null
    buildPaths(sitePoint)
  }

  private fun buildPaths(destination: Pair<Float, Float>) {
    paths.clear(); pathIndices.clear()
    living.forEach { hero ->
      val prefix = returnCorridor(hero.x to hero.y)
      val route = if (activity == Activity.RETURN) prefix + listOf(960f to 1085f, 680f to 1040f, destination)
        else prefix + LivingExpeditionContent.outwardRoute(selectedSite)
      paths[hero.id] = route
      pathIndices[hero.id] = 0
    }
  }

  private fun returnCorridor(position: Pair<Float, Float>): List<Pair<Float, Float>> = when {
    position.second < 850f -> listOf(1540f to 585f, 1340f to 640f, LivingExpeditionContent.hub)
    position.second > 1350f -> listOf(1480f to 1520f, LivingExpeditionContent.hub)
    position.first > 1550f -> listOf(1780f to 990f, LivingExpeditionContent.hub)
    position.first < 900f -> listOf(680f to 1040f, 960f to 1085f, LivingExpeditionContent.hub)
    else -> listOf(LivingExpeditionContent.hub)
  }

  private fun pathTarget(hero: HeroCombatUnit): Pair<Float, Float> {
    val path = paths[hero.id] ?: return sitePoint
    return path.getOrElse(pathIndices[hero.id] ?: 0) { path.lastOrNull() ?: sitePoint }
  }

  private fun scout(): HeroCombatUnit? = living.minWithOrNull(compareBy<HeroCombatUnit> {
    when (it.heroClass) { HeroClassType.ASSASSIN -> 0; HeroClassType.ARCHER -> 1; HeroClassType.MAGE -> 2; else -> 3 }
  }.thenByDescending { it.effectiveSpeed }.thenBy { it.id })

  private fun frontline(): HeroCombatUnit? = living.minWithOrNull(compareBy<HeroCombatUnit> {
    when (it.heroClass) { HeroClassType.KNIGHT -> 0; HeroClassType.BERSERKER -> 1; HeroClassType.PRIEST -> 2; else -> 3 }
  }.thenBy { it.id })

  private fun plateActors(): List<HeroCombatUnit> = living.sortedWith(compareBy<HeroCombatUnit> {
    if (it.id == frontline()?.id) 0 else 1
  }.thenBy { it.id })

  private fun spawnNorth(assault: Boolean) {
    spawn("north_0", EnemyArchetype.FIELD_GOBLIN_RAIDER, 1490f, 630f)
    spawn("north_1", EnemyArchetype.FIELD_GOBLIN_RAIDER, 1600f, 700f)
    if (assault) spawn("north_2", EnemyArchetype.FIELD_GOBLIN_ARCHER, 1660f, 620f)
  }

  private fun spawn(suffix: String, type: EnemyArchetype, x: Float, y: Float) {
    val id = "expedition_$suffix"
    if (!spawned.add(id)) return
    val point = CollisionPhysics.resolvePosition(x, y, 18f, engine.mapLayout.width, engine.mapLayout.height,
      engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
    engine.enemies += EnemyCombatUnit(id, type, type.baseHp, type.baseHp, type.baseAtk, type.baseDef,
      type.speed, point.first, point.second)
  }

  private fun enemiesCleared(prefix: String): Boolean = engine.enemies.filter { it.id.startsWith("expedition_$prefix") }
    .let { it.isNotEmpty() && it.all { enemy -> enemy.dead } }

  private fun say(speaker: String?, text: String) {
    if (history.count { it.remainingSeconds > 0f } >= 3) {
      val oldest = history.indexOfFirst { it.remainingSeconds > 0f }
      history[oldest] = history[oldest].copy(remainingSeconds = 0f)
    }
    history += ExpeditionConversation("expedition_line_${conversationSerial++}", speaker, text, 8f)
    while (history.size > 8) history.removeAt(0)
    announce(text)
  }

  private fun announce(text: String) { statusVi = text; engine.postFeedback(text) }
  private fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>) = hypot(a.first - b.first, a.second - b.second)
}
