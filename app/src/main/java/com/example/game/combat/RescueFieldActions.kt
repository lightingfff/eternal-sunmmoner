package com.example.game.combat

import kotlin.math.hypot

enum class RescueFieldActionType { READ_TRACKS, RECOVER_DISPATCH, SIGNAL_MIRRORS }
data class RescueFieldTarget(val id: String, val labelVi: String, val x: Float, val y: Float)

/** An explicit interaction by a living hero supplied by the caller, never a timer or commander marker. */
class RescueFieldAction(
  val type: RescueFieldActionType,
  val targets: List<RescueFieldTarget>,
  val hintVi: String,
) {
  init {
    require(targets.isNotEmpty())
    require(targets.map { it.id }.distinct().size == targets.size)
  }
  var completedSteps = 0
    private set
  val completed: Boolean get() = completedSteps == targets.size
  val currentTarget: RescueFieldTarget? get() = targets.getOrNull(completedSteps)
  fun canInteract(heroPositions: List<Pair<Float, Float>>): Boolean {
    val target = currentTarget ?: return false
    return heroPositions.any { hypot(it.first - target.x, it.second - target.y) <= INTERACTION_RADIUS }
  }
  fun interact(heroPositions: List<Pair<Float, Float>>): Boolean =
    currentTarget?.let { interact(it.id, heroPositions) } ?: false
  fun interact(targetId: String, heroPositions: List<Pair<Float, Float>>): Boolean {
    if (currentTarget?.id != targetId || !canInteract(heroPositions)) return false
    completedSteps++
    return true
  }
  companion object { const val INTERACTION_RADIUS = 140f }
}

object RescueFieldActions {
  fun forChapter(id: String, site: Pair<Float, Float>): RescueFieldAction? {
    fun target(key: String, label: String) = RescueFieldTarget("${id}_$key", label, site.first, site.second)
    return when (id) {
      "watchpost" -> RescueFieldAction(RescueFieldActionType.READ_TRACKS,
        listOf(target("letter", "Đọc thư cầu cứu của người đưa thư")),
        "Đưa một Anh hùng tới người đưa thư ở trại rồi đọc thư để xác định tuyến khảo sát.")
      "lookout" -> RescueFieldAction(RescueFieldActionType.READ_TRACKS, listOf(
        RescueFieldTarget("lookout_tracks", "Đọc dấu bánh xe ở lối Bắc", 1340f, 640f),
        target("map", "Đối chiếu bản đồ trong rương Bắc")),
        "Sau khi trạm an toàn: đối chiếu dấu bánh xe với rương để xác định đúng lối về cổng.")
      "signal" -> RescueFieldAction(RescueFieldActionType.SIGNAL_MIRRORS,
        listOf(target("horn", "Kiểm tra dấu tù và trên lối Bắc")),
        "Kiểm tra dấu tại đây rồi chọn phá tín hiệu hay lấy băng dự phòng. Tù và trinh sát trên chặng hộ tống vẫn là mối đe dọa riêng.")
      "medicine" -> RescueFieldAction(RescueFieldActionType.RECOVER_DISPATCH,
        listOf(target("medicine", "Lấy túi băng còn niêm phong ở đài Nam")),
        "Lấy thuốc sau khi đài an toàn. Chọn bọc lót người đưa thư (hấp thụ 60 sát thương) hoặc hồi 35% HP cho Anh hùng còn đứng vững.")
      else -> null
    }
  }
}
