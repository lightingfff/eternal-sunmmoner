package com.example.game.pixel

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.game.map.GroundView

/** Carries the projection through nested draw passes without changing the global camera. */
private class ProjectedDrawScope(scope: DrawScope, val view: GroundView) : DrawScope by scope

internal fun DrawScope.withGroundView(view: GroundView, draw: DrawScope.() -> Unit) {
  ProjectedDrawScope(this, view).draw()
}

internal val DrawScope.groundView: GroundView
  get() = (this as? ProjectedDrawScope)?.view ?: GroundView.STANDARD
