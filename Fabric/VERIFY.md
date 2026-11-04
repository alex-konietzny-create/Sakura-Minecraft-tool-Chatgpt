# Sakura Pink 0.3.0

Seven settings pages: Anchor, Legit Anchor, Safe Anchor, Double Anchor, Crystal, Fast Place and Design.
Each actionable module has its own enable switch, hotkey and settings; anchor modes have independent hotbar slots, delays and targeting options.

## Timing and precision
Anchor and crystal actions use monotonic millisecond deadlines on the client render thread, with no background world access and at most one action per rendered frame. FPS, game scheduling, predicted block states and server latency affect actual timing; the configured values are minimum waits, not a real-time guarantee. Fast Place still uses Minecraft's vanilla use cooldown: milliseconds round up to 50 ms ticks, shown explicitly in its settings.
Old tick configurations migrate once into schema 3 millisecond values. Settings use atomic replacement where supported.

## Anchor modes
- Anchor detects existing anchors: charged skips placement/charging, empty starts at charging.
- Legit Anchor defaults to slower waits and requires holding its own hotkey; no anti-detection claim.
- Safe Anchor places actual glowstone cover in the cardinal position adjacent to the anchor on the player's side. It requires a free, visible, reachable placement face and checks the cover again before detonation. If cover cannot be placed or the player changes sides, it aborts. It does not require or check a totem and does not calculate explosion damage or guarantee survival.
- Double Anchor locks one BlockPos, waits for the first anchor to disappear in world state, applies its repeat delay, then places/charges/detonates a second anchor at the same position. If explosion removes all supports or the position is obstructed, it aborts rather than placing elsewhere.

All modes validate range, items and the exact target, cancel on menus, focus loss, death, player/world changes or manual slot changes, and time out rather than replay stale actions.
The final slot may be empty or contain a normal item. Glowstone/anchor items that would interfere with activation remain invalid. No totem is required.

## Crystal startup
A fresh right-mouse hold with main-hand crystals starts immediately, with no prior manual attack. Startup clears stale state, stops an already active item-use action, resolves crystals above an aimed base even when the crosshair reports the block, waits for spawn/removal and recovers from denied placement, target changes and timeouts. Vanilla item use is suppressed only while the assist owns it.

## Design
Original pink/black workspace with embedded Abel font (SIL OFL license bundled), calmer panels, individual Timing/Hotbar/Behavior sections, direct numeric millisecond fields and sliders, keyboard/mouse key capture, animated controls and reduced-motion / default-font options.

## Checks and limits
The build runs deterministic production-state-machine regressions for anchor deadlines, existing anchors, cover loss, double-cycle ordering, crystal cold startup, release/repress, denial and timeout recovery.
The UI workflow launches Minecraft without a world, tests input/fields/persistence at three UI sizes, and captures all seven pages plus Hotbar and Behavior.
These checks do not establish PvP gameplay correctness, cover damage reduction, server compatibility or guaranteed survival. Manual gameplay validation is still needed.
