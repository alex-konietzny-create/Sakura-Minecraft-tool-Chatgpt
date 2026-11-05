# Sakura Live 0.5.0

## Real speed controls
The overview Action delay updates placement, charging and cover waits together, and limits slot preparation accordingly. Advanced controls remain separate. Anchor/Crystal Timing offers Turbo, Fast and Quiet presets. Turbo uses zero additional anchor action waits and 4ms input holds; Fast uses 25ms waits and 8ms holds. Confirmed slot changes proceed in the same render pass when preparation is zero. All action delays accept and preserve zero; no hidden 10ms clamp. The release watchdog checks every 5ms. Actual input processing, slot changes, world confirmation, FPS and server timing still bound throughput.

Existing default 150ms/35ms anchor configurations migrate to Fast; customized delays are preserved. Original slower Legit defaults remain. Old default Crystal timings migrate to 25ms. The config schema is now 4. On completion the last sequence duration is available in the menu footer.

Crystal no longer wastes an additional frame after a fresh hold or zero-delay removal. If an attacked crystal remains under the crosshair, a retry can occur after max(50ms, break delay), with the original absolute timeout retained. Release, target checks and input guards remain.

## Safe camera
Safe Anchor > Camera: automatic camera toggle and angular speed 30–3600 degrees/s. A visible, in-reach solid support face for the player-side cover cell is selected. Standard Windows relative mouse movements steer the actual camera to it, then back to the original anchor. Slot switching and camera travel can proceed concurrently. Clicks still require the real vanilla crosshair to match the exact intended action. No direct player rotation assignment, direct interactionManager call or custom outgoing packet is used.

The closed-loop controller reads actual view changes, wraps yaw by the shortest path, accounts for sensitivity/inversion and adjusts for pointer acceleration. A nonresponsive camera cancels after 500ms; focus/menu/world loss, hotkey cancellation and completion stop control. Raw mouse input mode is temporarily disabled for standard Windows relative input and restored after the sequence; the saved user option is not changed. Slow camera speeds extend only the Safe sequence timeout. No totem check is introduced.

## UI and validation
Rounded numeric fields replace harsh rectangular strokes. Glyph atlases drop from 2048² to 1536², about 44% less atlas pixel area, while preserving framebuffer-matched glyph resolution. This is not a measured game-FPS improvement.

Tests: camera simulation at 60/120/240 FPS with inversion and doubled pointer gain, speed effect, shortest-path wrap and stalled response. The pure anchor timing model with 50ms world confirmations (no slot or network delay) measures 350ms with old 150ms pauses versus 150ms with zero/25ms pauses. These are model measurements, not real gameplay benchmark results. UI smoke verifies Turbo zero waits persist, all-phase overview delay, and editable/saved camera speed. Native Windows test verifies relative mouse motion reaches a captured GLFW cursor in an empty Minecraft test screen, along with real F24/right-click delivery and release. Full Safe Anchor gameplay remains unverified.
