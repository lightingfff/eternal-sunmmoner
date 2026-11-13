package com.example.game.combat

import com.example.game.map.EnemyArchetype

data class RescueJourneyChapter(
  val id: String,
  val titleVi: String,
  val briefingVi: String,
  val resolutionVi: String,
  val routeEnd: Int,
  val enemies: List<EnemyArchetype> = emptyList(),
)

/** All sites follow the existing meadow's roads. There is no duration or enlarged map requirement. */
object RescueJourneyContent {
  const val PRELUDE_CHAPTERS = 5
  val routePoints = listOf(
    600f to 1049f,
    680f to 1040f, 960f to 1085f, 1340f to 640f, 1540f to 585f,
    1740f to 630f,
    2120f to 1030f, 2240f to 1075f, 1820f to 1530f, 1660f to 1565f,
    1480f to 1520f, 1180f to 1125f, 680f to 1040f, 600f to 1049f,
    2120f to 1030f, 2860f to 1066f,
  )
  val chapters = listOf(
    RescueJourneyChapter("watchpost", "Đọc thư cầu cứu tại trại",
      "Người đưa thư ở lại trại đã an toàn. Đưa một Anh hùng tới đọc thư; đội cần tìm dấu đường ở rương Bắc và thuốc ở đài Nam trước khi hộ tống.",
      "Thư chỉ lối Bắc: sói vòng sau, xạ thủ giữ rương. Dồn hỏa lực vào xạ thủ hoặc tập hợp để che tuyến sau.", 0),
    RescueJourneyChapter("lookout", "Thu hồi bản đồ ở rương Bắc",
      "Theo lối Bắc. Sói săn cung thủ và pháp sư; xạ thủ lùi khi bị áp sát. Dọn trạm rồi đối chiếu dấu bánh xe với bản đồ trong rương.",
      "Bản đồ xác nhận đường về cổng và một ổ phục kích ở đoạn giao nhau. Tín hiệu trên đường Bắc sẽ quyết định quân tiếp viện ở đó.", 4,
      listOf(EnemyArchetype.DIRE_WOLF, EnemyArchetype.DIRE_WOLF, EnemyArchetype.FIELD_GOBLIN_ARCHER)),
    RescueJourneyChapter("signal", "Quyết định tại tín hiệu Bắc",
      "Đọc dấu tù và ở cuối lối Bắc. Phá tín hiệu sẽ bớt một xạ thủ tại điểm giao; lấy băng dự phòng trong hòm gác sẽ hồi 20% HP đội đang đứng vững nhưng để lại tín hiệu.",
      "Đã xử lý tín hiệu. Đài thuốc ở lối Nam còn một túi băng: dùng cho người đưa thư hay đội hộ tống là quyết định tiếp theo.", 5),
    RescueJourneyChapter("medicine", "Lấy thuốc tại đài Nam",
      "Ra ngã rẽ phía Đông rồi theo đường Nam tới đài. Một bộ binh và một sói canh thuốc: giữ đội hình, xử lý sói áp tuyến sau trước.",
      "Thuốc đã được phân một lần. Mang bản đồ và tin tức về trại để đón người đưa thư; không cần chờ hồi sức theo đồng hồ.", 9,
      listOf(EnemyArchetype.FIELD_GOBLIN_RAIDER, EnemyArchetype.DIRE_WOLF)),
    RescueJourneyChapter("debrief", "Trở lại trại, bắt đầu hộ tống",
      "Theo lối Nam về trại. Người đưa thư chờ tại đây; xác nhận khởi hành khi đội đã sẵn sàng. Sau đó vẫn phải đối phó trinh sát và Kraag.",
      "Đã khởi hành. Bảo vệ người đưa thư, chặn trinh sát hoặc giữ đội hình; tới điểm giao sẽ gặp toán phục kích đã đánh dấu trên bản đồ.", 13),
    RescueJourneyChapter("crossing", "Phá thế gọng kìm ở điểm giao",
      "Sói đang vòng tuyến sau, xạ thủ giữ đường. Dùng điểm đến đổi hướng tiếp cận và dồn hỏa lực; người đưa thư chờ sau đội hình tới khi đường được mở.",
      "Đường giao đã thông. Tiếp tục bọc lót người đưa thư tới cổng; Kraag còn chặn phía trước.", 14,
      listOf(EnemyArchetype.DIRE_WOLF, EnemyArchetype.FIELD_GOBLIN_ARCHER)),
    RescueJourneyChapter("gate", "Hạ Kraag và đưa người đưa thư tới cổng",
      "Né vùng đỏ 1,5 giây bằng điểm đến, phản công lúc Kraag hồi sức; chiến thắng chỉ tính khi người đưa thư thực sự đến cổng.",
      "Người đưa thư đã đến cổng cùng báo cáo hành trình. Trở về Thánh Địa để nhận kết quả được lưu.", 15),
  )
}
