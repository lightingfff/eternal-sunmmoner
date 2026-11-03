package com.example.game.combat

import kotlin.math.hypot

enum class RescueFieldActionType {
  READ_TRACKS, RECON_SIGNS, RECOVER_DISPATCH, SIGNAL_MIRRORS, RELEASE_WINCH, RESCUE_STRAGGLER, SEAL_SEQUENCE,
}

data class RescueFieldTarget(val id: String, val labelVi: String, val x: Float, val y: Float)

/** Physical field work: each step needs an explicit command by a living hero at the right place. */
class RescueFieldAction(
  val type: RescueFieldActionType,
  val targets: List<RescueFieldTarget>,
  val hintVi: String,
) {
  init {
    require(targets.isNotEmpty())
    require(targets.map { it.id }.distinct().size == targets.size)
  }
  var completedSteps: Int = 0
    private set
  val completed: Boolean get() = completedSteps == targets.size
  val currentTarget: RescueFieldTarget? get() = targets.getOrNull(completedSteps)

  fun canInteract(heroPositions: List<Pair<Float, Float>>): Boolean {
    val point = currentTarget ?: return false
    return heroPositions.any { (x, y) -> hypot(x - point.x, y - point.y) <= INTERACTION_RADIUS }
  }

  fun interact(heroPositions: List<Pair<Float, Float>>): Boolean =
    currentTarget?.let { interact(it.id, heroPositions) } ?: false

  fun interact(targetId: String, heroPositions: List<Pair<Float, Float>>): Boolean {
    if (currentTarget?.id != targetId || !canInteract(heroPositions)) return false
    completedSteps++
    return true
  }

  companion object { // The widest valid one-survivor formation slot is about109 units from a Move-To beacon.
    // Allow that slot plus its22-unit arrival tolerance; the actor must still physically leave escort cover.
    const val INTERACTION_RADIUS = 140f }
}

/** Authored actions use existing meadow corridors and the additive eastern/southern field. */
object RescueFieldActions {
  fun forChapter(id: String, site: Pair<Float, Float>): RescueFieldAction? {
    fun point(key: String, label: String, dx: Float, dy: Float) =
      RescueFieldTarget("${id}_$key", label, site.first + dx, site.second + dy)
    return when (id) {
      "archives" -> RescueFieldAction(RescueFieldActionType.READ_TRACKS, listOf(
        RescueFieldTarget("archives_cart", "Đọc vết bánh xe", 1340f, 640f),
        RescueFieldTarget("archives_cloth", "Đối chiếu dải khăn đẫm bùn", 2120f, 1030f),
      ), "Vết bánh xe đi trước, dải khăn ở lối rẽ sau: đưa một Anh hùng tới từng dấu rồi đối chiếu. Dấu vết xác nhận đoàn xe đã chuyển về Đông.")
      "recon" -> RescueFieldAction(RescueFieldActionType.RECON_SIGNS, listOf(
        point("ruin_tracks", "Xem dấu giày tại vòm đá", -220f, 120f),
      ), "Dấu giày mới bên vòm đá cho thấy đội gác đã đi về gờ Đông. Đưa một Anh hùng tới xem trước khi đưa đoàn tiếp tục.")
      "wardens" -> RescueFieldAction(RescueFieldActionType.RESCUE_STRAGGLER, listOf(
        point("rope", "Lấy dây cứu hộ của toán gác", 380f, 190f),
        point("scout", "Đưa dây cho người gác mắc kẹt", 80f, 490f),
      ), "Người gác còn ở chân dốc: lấy cuộn dây trước rồi đưa tới chỗ anh ấy. Tách một nhóm đi cứu hộ, sau đó quay lại người đưa thư.")
      "signal" -> RescueFieldAction(RescueFieldActionType.SIGNAL_MIRRORS, listOf(
        point("west_mirror", "Xoay gương Tây về cờ đồng minh", -400f, 220f),
        point("east_mirror", "Xác nhận ánh phản chiếu ở gương Đông", 200f, 450f),
      ), "Hai gương phải khớp nhau: chỉnh gương Tây trước, kiểm tra gương Đông sau. Chỉ khi đọc được tín hiệu mới quyết định phá tù và hay cứu trinh sát.")
      "crossing" -> RescueFieldAction(RescueFieldActionType.RELEASE_WINCH, listOf(
        point("brake", "Gài chốt hãm phía Bắc", -400f, -160f),
        point("winch", "Nhả tời phía Nam để hạ ván cầu", -320f, 340f),
      ), "Cầu mất ván: gài chốt hãm rồi mới nhả tời. Hai cơ cấu ở hai bên đầu cầu; di chuyển đội tới đúng điểm để mở lối cho người bị thương.")
      "medicine" -> RescueFieldAction(RescueFieldActionType.RECOVER_DISPATCH, listOf(
        point("ledger", "Đọc sổ thuốc còn dùng được", -380f, -200f),
        point("satchel", "Lấy túi thuốc theo dấu trong sổ", -80f, -490f),
      ), "Đọc sổ để tránh thuốc hỏng, rồi thu đúng túi còn niêm phong. Mang thuốc về phân cho những người còn đứng vững.")
      "seal" -> RescueFieldAction(RescueFieldActionType.SEAL_SEQUENCE, listOf(
        point("sun", "Ấn Mặt Trời", -380f, -220f),
        point("moon", "Ấn Mặt Trăng", 120f, -480f),
        point("star", "Ấn Ngôi Sao", 400f, -220f),
      ), "Minh văn: 'Ngày nhường đêm, đêm dẫn lối sao'. Chạm Mặt Trời → Mặt Trăng → Ngôi Sao để mở hộp ấn; sai thứ tự không mở được khóa.")
      else -> null
    }
  }
}
