package com.example.game.story

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.HeroClassType

data class DialogueBeat(
  val speakerName: String,
  val speakerTitle: String,
  val emotionTag: String,
  val heroClassForPortrait: HeroClassType?,
  val lineVi: String,
  @param:DrawableRes val panelArtRes: Int? = null,
)

object ManhwaStoryData {

  const val FLAG_PROLOGUE_SEEN = "PROLOGUE_AWAKENING_SEEN"

  val PROLOGUE_BEATS: List<DialogueBeat> = listOf(
    DialogueBeat(
      speakerName = "Hệ Thống Tháp Vĩnh Hằng",
      speakerTitle = "Khế Ước Không Gian",
      emotionTag = "[Đồng bộ ý thức]",
      heroClassForPortrait = null,
      lineVi = "«Chào mừng tới Tầng Chờ Dị Giới — THE SANCTUARY. Ý thức của bạn đã được triệu hồi với tư cách Master thứ 999.»",
      panelArtRes = R.drawable.img_sanctuary_gate,
    ),
    DialogueBeat(
      speakerName = "Kaelen",
      speakerTitle = "Khiên Thép Tiền Phong",
      emotionTag = "[Điềm tĩnh]",
      heroClassForPortrait = HeroClassType.KNIGHT,
      lineVi = "Thánh Địa im lìm suốt bao tháng ngày cuối cùng cũng sáng đèn... Ngài là vị Master mới của chúng tôi sao?",
      panelArtRes = R.drawable.img_sanctuary_gate,
    ),
    DialogueBeat(
      speakerName = "Lyra",
      speakerTitle = "Mắt Ưng Gió Đêm",
      emotionTag = "[Thực dụng]",
      heroClassForPortrait = HeroClassType.ARCHER,
      lineVi = "Đừng vội mừng, Kaelen. Rất nhiều Master trước đây coi chúng ta như quân cờ thí mạng. Hy vọng ngài biết chỉ huy thực sự.",
      panelArtRes = R.drawable.img_sanctuary_gate,
    ),
    DialogueBeat(
      speakerName = "Vesper",
      speakerTitle = "Học Giả Tinh Vân",
      emotionTag = "[Phân tích]",
      heroClassForPortrait = HeroClassType.MAGE,
      lineVi = "Cánh cổng dẫn lên Tầng 01 — Cánh Đồng Khởi Nguyên đã ổn định năng lượng. Hãy kiểm tra trang bị rồi bước tới Tower Gate ở phía Bắc Sảnh nhé, Master.",
      panelArtRes = R.drawable.img_floor01_field,
    ),
  )

  fun floorIntroBeats(floorNumber: Int): List<DialogueBeat> = when (floorNumber) {
    1 -> listOf(
      DialogueBeat(
        speakerName = "Kaelen",
        speakerTitle = "Khiên Thép Tiền Tuyến",
        emotionTag = "[Cảnh giác]",
        heroClassForPortrait = HeroClassType.KNIGHT,
        lineVi = "Gió thổi qua thảm cỏ cao thế này rất dễ che giấu bước chân thú săn. Tôi sẽ đi đầu mở đường dọc theo lối đất mòn!",
      ),
      DialogueBeat(
        speakerName = "Lyra",
        speakerTitle = "Mắt Ưng Gió Đêm",
        emotionTag = "[Sắc bén]",
        heroClassForPortrait = HeroClassType.ARCHER,
        lineVi = "Không gian thoáng đãng rất hợp với cung tên của tôi. Master cứ ra lệnh Tập Trung Hỏa Lực hoặc Giữ Vị Trí khi cần!",
      ),
    )

    2 -> listOf(
      DialogueBeat(
        speakerName = "Vesper",
        speakerTitle = "Học Giả Tinh Vân",
        emotionTag = "[Thận trọng]",
        heroClassForPortrait = HeroClassType.MAGE,
        lineVi = "Sương lam của Khu Rừng Thì Thầm chứa đầy ma lực cổ xưa. Coi chừng các Oán Linh bắn phép từ sau rặng cây!",
      ),
    )

    else -> listOf(
      DialogueBeat(
        speakerName = "Kaelen",
        speakerTitle = "Khiên Thép Tiền Tuyến",
        emotionTag = "[Quyết tâm]",
        heroClassForPortrait = HeroClassType.KNIGHT,
        lineVi = "Phế tích cổ đại ngay trước mắt. Toàn đội giữ vững cự ly chiến thuật theo hiệu lệnh của Master!",
      ),
    )
  }
}
