# Sakura Live 0.4.1

## Input
All macro placement, breaking and hotbar changes use standard Windows SendInput. There are no direct interactionManager attacks/placements, outgoing packet sends, or selected-slot assignments. Minecraft still sends its ordinary network packets. No input flags are hidden or modified to bypass detection; automation is not represented as manual input.

Inputs require the exact Minecraft window in foreground and a live unpaused game with no open screen. A 10ms watchdog releases owned inputs on focus/menu loss or a 250ms render heartbeat timeout. Slot changes wait for vanilla inventory selection confirmation. No overlapping synthetic pulses. Configured press duration 10–100ms in Themes. Physical trigger hooks retain only watched down/up state; injected event flags are used solely to avoid corrupting physical hold detection, never cleared or concealed.

Native input supports Windows only. All clicks use the real crosshair. Safe Anchor requires aiming at the cover placement surface and then the anchor; Crystal breaking requires the crystal under the crosshair. No camera rotation or off-crosshair placement is performed. Delays are minimum waits, bounded by Minecraft input processing, FPS and server updates. Existing totem-independent behavior remains.

## Double Anchor
The immutable initial BlockPos is reused for both cycles. First anchor must disappear; the target must remain replaceable continuously for the repeat delay before the second placement. A temporary server restoration resets that interval instead of cancelling the second cycle. Two anchors and charging materials plus a surviving support surface are needed. Regression covers prediction rollback, delayed removal, both detonations and no third placement.

## UI
Neutral charcoal surfaces and Sakura pink/blue accents. Antialiased font, rounded masks and icons rasterize at framebuffer pixel density. Larger headings have their own density-matched glyph atlases. Native text fields retain editing behavior. Shared animated ambient background and soft title/switch/hover glow across grid and detail views. Opening/card stagger, sliding navigation, hover and switches use frame-time easing; Reduced Motion snaps transitions.

## Verified locally
- Java build; anchor/crystal state regressions, pulse/release/focus regression with fake transport.
- Real Minecraft UI smoke: direct fields, saved settings, bindings, navigation, themes at three sizes; actual animation frames and detail screen screenshots.
- Windows native backend initializes despite Minecraft AWT headless mode.
- Actual Windows SendInput F24 and right click reached Minecraft GLFW callbacks in an empty focused test screen; both released. No game world or multiplayer test was performed.

The native smoke harness is development-only and enabled solely with sakura.nativeSmoke=true. The UI smoke uses sakura.uiSmoke=true. No hidden-window local helper is included in production.
