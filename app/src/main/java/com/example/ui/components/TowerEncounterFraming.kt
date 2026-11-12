package com.example.ui.components

import com.example.game.combat.TowerCombatEngine
import kotlin.math.hypot
import kotlin.math.max

/** Framing contains living heroes and nearby revealed combatants; never reveals future encounters. */
internal object TowerEncounterFraming {
  data class Bounds(val left:Float,val top:Float,val right:Float,val bottom:Float) {
    val width get()=right-left
    val height get()=bottom-top
    val centerX get()=(left+right)*.5f
    val centerY get()=(top+bottom)*.5f
  }
  fun bounds(engine:TowerCombatEngine,includeThreats:Boolean=true):Bounds? {
    var left=Float.POSITIVE_INFINITY;var top=Float.POSITIVE_INFINITY
    var right=Float.NEGATIVE_INFINITY;var bottom=Float.NEGATIVE_INFINITY
    for(hero in engine.heroes) if(!hero.knockedOut) {
      left=minOf(left,hero.x);right=maxOf(right,hero.x)
      top=minOf(top,hero.y);bottom=maxOf(bottom,hero.y)
    }
    if(!left.isFinite())return null
    if(includeThreats)for(enemy in engine.enemies) {
      if(enemy.dead || !engine.isEnemyRevealed(enemy))continue
      val relevant=engine.heroes.any { hero ->
        !hero.knockedOut && (hero.currentTargetId==enemy.id || enemy.aggroed) &&
          hypot(hero.x-enemy.x,hero.y-enemy.y)<=max(240f,enemy.archetype.attackRange+120f)
      }
      if(relevant) {
        left=minOf(left,enemy.x);right=maxOf(right,enemy.x)
        top=minOf(top,enemy.y);bottom=maxOf(bottom,enemy.y)
      }
    }
    return Bounds(left,top,right,bottom)
  }
}

