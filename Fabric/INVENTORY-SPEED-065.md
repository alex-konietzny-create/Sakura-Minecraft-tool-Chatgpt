# 0.6.5: final-slot handoff, camera damping and zero added default waits

Opening Inventory during an Anchor sequence now stops the sequence, releases native action inputs, resolves the Totem/final hotbar slot and waits for an observed native slot change before opening InventoryScreen. The configured final slot is preferred when it contains a Totem; otherwise an actual Totem in the nine hotbar slots is preferred. Without a Totem, an appropriate final item remains allowed.

No native hotbar key is injected into an open screen, where it could swap a stack under the pointer. Other interruptions defer the final-slot return until gameplay resumes. Slot return checks the original world/player, uses bounded native retries and cannot indefinitely block a requested inventory when input preparation fails. Pending block uses are suppressed during the handoff. Slot restoration no longer returns to an original Anchor slot.

Safe camera movement now has damped angular velocity, a bounded speed, fractional native mouse-count accumulation and braking near the target. It continues to use actual native mouse input and observed camera feedback, not direct player rotations or action packets.

New normal/Safe/Double defaults use zero additional action/cover/repeat waits and a 2 ms input pulse instead of 8 ms. Schema 5 migrates only the exact previous 25/25/25 ms, zero-switch, 75 ms repeat profile; different custom timing values remain. The Legit profile remains deliberately slower. Server confirmations and actual hotbar changes still gate progression; zero configured waits are not zero total execution time.

The main menu has quieter card contours, reduced inactive text glow and shorter copy. Runtime phases are labelled in German.

## Verification

- Deterministic tests: slot-return confirmation before inventory opening; no input into menus/lost focus; resuming after a long pause; bounded timeout/retry. Existing input, Anchor, delayed-confirmation, hotbar and camera tests also run. Camera tests cover 60/120/240 FPS, pointer gain/inversion and disturbances.
- Own local creative integrated Minecraft 1.21.11 world: fixture requested InventoryScreen when an actual Anchor existed and slot 1 held Anchor. Native input selected slot 9/Totem, the status reported confirmation, and the creative inventory UI was visually observed with the Totem held and Anchor/Glowstone still in their slots. The creative redirect was visually verified; the fixture's final automatic check was specific to InventoryScreen. Fixture code is not included in the release.
- After closing inventory, the existing Anchor was loaded and detonated on a fresh native hotkey press; completion status was 178 ms. This is a bounded local, already-placed-Anchor result, not a placement-through-explosion benchmark or remote-server guarantee.
- The redesigned Safe camera has deterministic verification in this release; its further live-window test was stopped with Escape. Previous successful Safe gameplay tests are documented in ANCHOR-062-VERIFICATION.md and must not be treated as a fresh test of the new controller.

Air placement without a valid surface remains unsupported with native vanilla clicks. No Prestige Client code was copied.
