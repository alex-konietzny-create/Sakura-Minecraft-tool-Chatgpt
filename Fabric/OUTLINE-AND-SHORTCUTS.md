# Sakura Live 0.4.2

Adds Block Outline under Utility. Settings: toggle, own configurable keyboard/mouse hotkey, line width 1.0–12.0px, edge length 1–100%, centered segments or corner stubs, opacity 0–255, individual RGB channels, rainbow toggle and 1–30s cycle, glow toggle/radius/strength, pulse toggle and 1–10s cycle. All settings persist through the existing atomic config save. Settings include a live cube preview using the same segment generator as the world renderer.

Rendering targets Minecraft 1.21.11 WorldRenderer.renderTargetBlockOutline with the actual OutlineRenderState voxel shape and camera position. Uses depth-tested translucent lines and nested low-alpha wider line passes for the glow halo. It replaces only the normal selected-block outline; turning the module off restores vanilla. No arbitrary through-wall block scan or extended interaction reach is added. The preview does not claim to be an in-world gameplay test.

Gameplay macro/menu shortcuts are consumed before normal Keyboard/Mouse handling, routed to Sakura once per press, with release handling and repeat suppression. Ordinary keys and all chat/inventory/settings inputs pass through. This prevents an overlapping vanilla keybind from also firing; it does not conceal the resulting game actions or guarantee any anti-cheat outcome. Minecraft sends normal action packets generated from standard simulated OS input. No injected flags are concealed.

Opening animation starts after glyph resources have loaded, with headings preloaded. Navigation reuses fonts at unchanged GUI scale. Live animation frames are now captured once per rendered frame at 50ms intervals, avoiding tick catch-up producing duplicate images. Main and detailed settings share ambient background and glow.

Validation: Java/Gradle build, anchor/crystal/pulse and edge geometry regressions; actual Minecraft UI checks include shortcut isolation from a shared vanilla binding, UI passthrough, RGB editing/persistence, key/mouse bindings and multiple UI scales. Native OS-key/click loopback was verified in a focused empty Minecraft test screen. World gameplay is not verified.
