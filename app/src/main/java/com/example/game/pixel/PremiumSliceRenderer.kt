package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import com.example.game.map.ObliqueCamera
import kotlin.math.roundToInt

/** Bounded pixel-cluster effects for the existing Knight shockwave. No emitters, blur or shaders. */
internal object PremiumSliceRenderer {
  private val arc = Path().apply {
    moveTo(7f,-65f); lineTo(20f,-64f); lineTo(31f,-58f); lineTo(39f,-48f)
    lineTo(42f,-36f); lineTo(40f,-27f); lineTo(35f,-23f)
    lineTo(36f,-35f); lineTo(33f,-46f); lineTo(25f,-55f); lineTo(15f,-61f); close()
  }
  fun DrawScope.drawSkillGround(cx:Float,cy:Float,z:Float,progress:Float,skill:Boolean) {
    if (!skill || progress <= 0f) return
    val t = (1f-progress)*.72f + .00001f
    if (t > .48f) return
    val release = t >= .30f
    val radius = (if (release) 26f+(t-.30f)*170f else 17f+t*18f)*z
    val alpha = if (release) ((.48f-t)/.18f).coerceIn(0f,1f)*.42f else .2f
    // Ground is foreshortened with the same .72 camera; the upright slash isn't.
    drawOval(Color(0xff9bbfc4).copy(alpha=alpha),Offset(cx-radius,cy-radius*.72f),
      Size(radius*2,radius*1.44f),style=Stroke(1.5f*z))
    for (i in 0..3) {
      val x = cx + (if(i%2==0) -1 else 1)*radius*.7f
      val y = cy + (if(i<2) -1 else 1)*radius*.4f
      drawRect(Color(0xffc4d4c5).copy(alpha=alpha),Offset(x,y),Size(3f*z,2f*z))
    }
  }
  fun DrawScope.drawContactTrail(cx:Float,cy:Float,z:Float,right:Boolean,progress:Float,skill:Boolean) {
    if (progress <= 0f) return
    val t = (1f-progress)*(if(skill) .72f else .48f) + .00001f
    val contact = if(skill) .30f else .22f
    if (skill && t < contact) {
      // A tiny charge glint at the raised blade; no full-screen bloom.
      val x = cx+(if(right) 5f else -5f)*z
      val y = cy-85f*z
      val a = (.25f+t/contact*.65f).coerceAtMost(.9f)
      drawRect(Color(0xffdce1ba).copy(alpha=a),Offset(x-z,y-4*z),Size(2*z,8*z))
      drawRect(Color(0xffa4c5c8).copy(alpha=a),Offset(x-4*z,y-z),Size(8*z,2*z))
    }
    val age = t-contact
    if (age < 0f || age > .13f) return
    val fade = (1f-age/.13f).coerceIn(0f,1f)
    withTransform({ translate(cx,cy); scale(if(right) z else -z,z,Offset.Zero) }) {
      drawPath(arc,(if(skill) Color(0xffa6ccd1) else Color(0xffd0cbb2)).copy(alpha=fade*.72f))
      drawLine(Color(0xffe4e9d1).copy(alpha=fade),Offset(39f,-45f),Offset(41f,-36f),2f)
      // Four fixed spark clusters. They exist only during the actual contact window.
      for(i in 0..3) {
        val x = 36f+i*3f+age*(18f+i*8f)
        val y = -39f+(i-1)*5f-age*(if(i%2==0) 40f else -30f)
        drawRect(Color(0xffd7cda2).copy(alpha=fade),Offset(x,y),Size(3f,1f))
      }
    }
  }
  fun DrawScope.drawSmithyVerge(camX:Float,camY:Float,zoom:Float) {
    val x = ObliqueCamera.worldToScreenX(950f,camX,zoom,size.width/2f)
    val y = ObliqueCamera.worldToScreenY(720f,camY,zoom,size.height/2f)
    val w = 256f*zoom; val h = 192f*zoom*ObliqueCamera.OBLIQUE_Y_SCALE
    if (x+w<0f || y+h<0f || x>size.width || y>size.height) return
    val image = SanctuaryArtAssets.asset("premium_slice/smithy_verge.png") ?: return
    drawImage(image,dstOffset=IntOffset(x.roundToInt(),y.roundToInt()),
      dstSize=IntSize(w.roundToInt().coerceAtLeast(1),h.roundToInt().coerceAtLeast(1)),filterQuality=FilterQuality.None)
  }
}
