package com.example.game.combat

/** Authored encounters on the existing meadow's northern, southern and eastern roads. */
object LivingExpeditionContent {
  val camp = 600f to 1049f
  val hub = 1180f to 1125f
  val northSwitch = 1740f to 630f
  val eastPlates = listOf(2140f to 1005f, 2250f to 1120f)
  val witnessSafeRoute = listOf(1480f to 1520f, hub)

  internal data class Site(val id: String, val titleVi: String, val point: Pair<Float, Float>, val detailVi: String)
  internal val sites = listOf(
    Site("north", "Trạm gác Bắc", 1540f to 585f, "Dây tù và nối với lính gác bên đường. Một người có thể luồn tới chốt tín hiệu."),
    Site("south", "Tiếng kêu ở lối Nam", 1660f to 1565f, "Một nhân chứng bị thương đang bị sói đánh hơi. Túi thuốc chỉ đủ dùng một lần."),
    Site("east", "Cổng đá phía Đông", 2180f to 1075f, "Hai phiến đá cách nhau giữ khóa cổng. Cần hai người đứng đồng thời, hoặc phá khóa gây tiếng động."),
  )

  internal fun outwardRoute(id: String): List<Pair<Float, Float>> = when (id) {
    "north" -> listOf(1340f to 640f, 1540f to 585f)
    "south" -> listOf(1480f to 1520f, 1660f to 1565f)
    else -> listOf(1780f to 990f, 2120f to 1030f, 2180f to 1075f)
  }
}
