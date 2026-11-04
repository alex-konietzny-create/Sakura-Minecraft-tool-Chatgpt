# Sakura 0.4.0 — Windows system input only

All macro actions emit native OS keyboard and mouse events through Java Robot. The mod no longer calls interactBlock, attackEntity, stopUsingItem or setSelectedSlot, modifies item-use cooldowns or constructs network packets. Vanilla Minecraft processes the inputs and sends its own normal multiplayer packets.

The active mixin only drives the per-frame scheduler. The earlier item-use cancellation and cooldown accessor mixins have been removed.

## Requirements and behavior
- Windows, Java 21 with java.desktop, and Minecraft in the foreground.
- Only watched trigger down/up states are tracked while that exact Minecraft HWND is foreground. No typed text is collected or logged. Injected events pass through unchanged and do not cancel physical hold state.
- Hotbar slots are selected using their real Minecraft keybindings and confirmed from the observed selected slot before a click.
- Common letter, number, F1–F12, mouse and navigation bindings are supported. Unsupported scan codes / keys abort rather than falling back to direct game actions.
- The use/attack binding is pressed and released with a configurable 10–100 ms hold duration. Focus loss, pause, death or world change releases owned inputs.
- Native vanilla targeting and cooldowns apply. There is no auto-aim. Crystal attacks wait until the crystal is actually under the crosshair. Safe Anchor waits for the user to aim at a non-anchor support face for the glowstone cover and then aim at the anchor again.
- The fixed target and double-anchor removal/repeat checks remain. No totem is required.

## Verification
The local build runs anchor/crystal state-machine and input-pulse regressions. Input tests use a fake transport to verify focus blocking, timed releases, cleanup and physical-trigger handling without pressing any real system keys.
The dev UI smoke checks Windows hook initialization and refusal to send background input only when its hidden window is not foreground; it never sends a test key to another app.
Live OS-driven gameplay, exact timing on the user's PC, arbitrary key layouts and server compatibility still need manual validation. This is native programmatic input, not a claim of indistinguishable hardware input or packet-free multiplayer.
