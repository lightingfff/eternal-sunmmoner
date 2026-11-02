Native Android Canvas production renderer helpers, Floor 1's unchanged map/props, world-Y sorted heroes/enemies/courier, normal zoom=1, 1280x720.
One real TowerCombatEngine playthrough at 0.05s simulation steps. Commands and phase/HP/fear values are in courier_playthrough.csv.
Explicit test party: Kaelen knight level1 HP480/ATK42/DEF32, Lyra archer level4 HP350/ATK44/DEF20, Mira priest level4 HP330/ATK30/DEF20. No equipment.
No hero/enemy/courier position, HP, damage, outcome, or encounter phase is assigned by the world-capture fixture. Intro dismissal matches the runtime button.
World images isolate production rendering without fabricating a mission HUD. courier_hud_* separately captures the real Compose component and real order callbacks.
These are native desktop test captures, NOT DEVICE TESTED; they establish neither phone FPS nor thermal behavior.

tower_portrait_400dp.png and tower_landscape_720dp.png: the actual TowerBattleScreen with the real new-guest roster/equipment, opening dismissed, scene paused for capture.
tower_scout_gameplay_400dp.png: actual TowerBattleScreen after 20.5 seconds of its controlled gameplay frame clock, with moving escort, scout countdown, fear feedback and active Intercept button. No injected encounter state.
starter_party_playthrough.csv: separate complete engine run with the actual new-guest Knight/Archer/Mage, starter gear, no training/stat edits; 60Hz logical simulation, taught orders.
