package com.example.game.lobby

import androidx.compose.ui.geometry.Rect
import com.example.data.model.HeroAssignment
import com.example.game.map.WorldObstacle

/** Runtime only: never serialized as an assignment or a save field. */
enum class LobbyLocomotion { WANDERING, IDLE, CHATTING, COWERING, FORMING_PARTY, BEING_PICKED_UP }
data class LobbyHeroDrop(val heroId: String, val assignment: HeroAssignment)
internal data class LobbyDragSession(val heroId: String, val originX: Float, val originY: Float)

data class LobbyDropZone(val assignment: HeroAssignment, val bounds: Rect) {
  companion object {
    fun fromObstacle(obstacle: WorldObstacle, assignment: HeroAssignment) = LobbyDropZone(assignment,
      Rect(obstacle.x - obstacle.footprintHalfW, obstacle.y - obstacle.footprintDepthNorth,
        obstacle.x + obstacle.footprintHalfW, obstacle.y + 90f))
  }
}
