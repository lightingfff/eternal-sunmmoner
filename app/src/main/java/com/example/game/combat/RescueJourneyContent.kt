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

/** Fixed, inspectable field itinerary. Distances are traversed at the existing courier speed. */
object RescueJourneyContent {
  val routePoints: List<Pair<Float, Float>> = listOf(
    600f to 1049f, 900f to 1040f, 960f to 1085f, 1340f to 640f, 1740f to 630f,
    2120f to 1030f, 2780f to 1040f, 2780f to 720f, 3100f to 700f, 3400f to 630f,
    4000f to 630f, 3900f to 1500f, 3500f to 2200f, 2800f to 2200f, 2300f to 2275f,
    1800f to 2350f, 600f to 2100f, 680f to 1800f, 680f to 1400f,
    680f to 1040f, 600f to 1049f,
  )
  val chapters = listOf(
    RescueJourneyChapter("archives", "Tìm dấu tuyến tiếp vận ở thư khố Bắc",
      "Người đưa thư: Cổng đang khóa bằng ấn thất lạc. Bản đồ ở thư khố sẽ chỉ đường tới toán giữ ấn. Hai xạ thủ canh hai phía; áp sát rồi dồn hỏa lực.",
      "Bản đồ ghi: toán giữ ấn mắc kẹt trên gờ Đông. Đội chọn đi tìm họ trước khi đưa thư về cổng.", 4,
      listOf(EnemyArchetype.FIELD_GOBLIN_ARCHER, EnemyArchetype.FIELD_GOBLIN_ARCHER)),
    RescueJourneyChapter("recon", "Điều tra trạm gác và dấu xe bị phục kích",
      "Vết bánh xe và một mảnh khiên dẫn tới trạm gác bỏ hoang. Khảo sát hiện trường để tìm dấu đường của toán giữ ấn trước khi tiến lên gờ Đông.",
      "Các vết cào cho thấy bầy sói vòng sau toán gác; mảnh khiên còn dùng được tăng 30 khiên cho anh hùng tiền tuyến.", 6),
    RescueJourneyChapter("wardens", "Giải vây toán gác ở gờ Đông",
      "Dấu chân sói vòng ra sau. Tập hợp quanh người đưa thư để giữ cung thủ và pháp sư trong đội hình; đừng đuổi từng con riêng lẻ.",
      "Toán gác được cứu, băng bó đội hình và chỉ đài tín hiệu. Họ cảnh báo: một trinh sát bạn còn mắc lại ở đó.", 9,
      listOf(EnemyArchetype.DIRE_WOLF, EnemyArchetype.DIRE_WOLF)),
    RescueJourneyChapter("signal", "Giành quyền kiểm soát đài tín hiệu",
      "Một lính canh giữ đài. Khi dọn xong, Master phải chọn phá tù và để giảm quân ở cầu hoặc giải cứu trinh sát để nhận trợ giúp y tế.",
      "Quyết định tại đài sẽ được ghi trong báo cáo chuyến đi; đội chuyển tới cầu vượt khe phía Nam.", 10,
      listOf(EnemyArchetype.FIELD_GOBLIN_RAIDER)),
    RescueJourneyChapter("crossing", "Mở lối qua cầu gãy",
      "Bộ binh chặn đầu cầu trong khi xạ thủ bắn từ sau. Chọn mục tiêu và áp sát xạ thủ; lệnh di chuyển có thể hủy đòn đang vung để tránh bị giữ chân.",
      "Đã thông lối qua khe. Người đưa thư nhận ra dấu của trạm cứu thương ở phía Nam; đội có thể lấy thuốc trước đoạn cuối.", 11,
      listOf(EnemyArchetype.FIELD_GOBLIN_RAIDER, EnemyArchetype.FIELD_GOBLIN_ARCHER)),
    RescueJourneyChapter("medicine", "Thu hồi thuốc ở trạm cứu thương",
      "Trạm còn thuốc và sổ ghi chép. Đưa đội tới kiểm tra, băng bó người còn đứng vững; thuốc không hồi sinh người đã ngã.",
      "Thuốc đã phân cho đội. Sổ ghi chép chỉ điểm cất ấn ở kho phía Tây: đem ấn về mới mở được đường rút cho cả đoàn.", 13),
    RescueJourneyChapter("seal", "Lấy ấn mở đường ở kho Tây",
      "Sói ép tuyến sau trong khi xạ thủ giữ kho. Dọn mối đe dọa rồi tập hợp ít nhất hai người còn đứng vững để kiểm kê ấn; nếu chỉ còn một người, vẫn có thể mang ấn về.",
      "Đã tìm thấy ấn nguyên vẹn và danh sách người mất tích. Người đưa thư mang chứng cứ về trại, hoàn tất vòng cứu viện.", 15,
      listOf(EnemyArchetype.DIRE_WOLF, EnemyArchetype.FIELD_GOBLIN_ARCHER)),
    RescueJourneyChapter("relay", "Báo tin qua trạm tiếp sức phía Tây",
      "Đường trại ở phía trước. Chuyển danh sách người mất tích qua trạm tiếp sức để đồng đội chuẩn bị cứu viện; người gác trạm còn thuốc băng bó cho đội.",
      "Tin đã tới trại, đội được tiếp sức. Hãy mang ấn về mở cổng cho người đưa thư.", 16),
    RescueJourneyChapter("debrief", "Bàn giao ấn tại trại và mở tuyến hộ tống",
      "Mang ấn và báo cáo về trại. Kiểm tra đủ đội hình rồi chọn ưu tiên cứu người hoặc tiếp vận trên chặng hộ tống qua trinh sát và Kraag.",
      "Trại đã nhận ấn và mở lối về cổng. Những người được cứu ở gờ Đông sẽ tiếp nhận thư; chặng hộ tống cuối bắt đầu.", 20),
  )
}
