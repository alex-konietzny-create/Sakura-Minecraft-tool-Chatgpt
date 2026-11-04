# Sakura Pink client verification
Minecraft 1.21.11, Java 21, Fabric API required.
Defaults: Right Shift menu, G anchor run/cancel, H crystal toggle, J fast place toggle.
Edit all hotkeys opens Minecraft key bindings (keyboard/mouse, conflict display and reset); Minecraft persists them in options.txt.
Settings persist in config/sakura-live.json. Anchor step and crystal place/break delays: 1–20 ticks (50–1000 ms at 20 TPS).
Crystal slot setting from older configs is ignored: main hand crystals and physical right mouse hold are required. Vanilla crystal item use is suppressed while the assist owns use, avoiding duplicate placements.
Crystal assist waits for a world entity at its placement position before breaking, checks reach and visibility, and times out after 40 ticks. It never removes entities locally or assumes a server accepted a packet.
Anchor sequence stops on invalid items, changed target, manual slot switch, menu, loss of focus, death, disconnect or world change; timeout 100 ticks. Abort restores starting slot only if the user has not changed it. Successful sequence ends on the configured final slot. This does not estimate explosion damage or guarantee player survival.
Manual gameplay checklist (NOT performed in CI):
- Rebind each action, restart Minecraft and verify persistence/conflicts.
- Exercise crystal hold/release, item switch, blocked/occupied base, latency, out-of-range, denied placement and disconnect.
- Exercise anchor delays, wrong items, charge confirmation, cancellation and final-slot behavior.
- Check UI at different GUI scales, keyboard navigation and hover transitions.
CI compilation does not establish runtime mixin compatibility or gameplay correctness.

## Existing anchors (0.2.1)
Aim directly at an existing respawn anchor and press the anchor action hotkey:
- CHARGES > 0: start at detonation using the configured final slot; no anchor or glowstone item is required.
- CHARGES == 0: start at charging using the configured glowstone slot, wait the configured step delay, then detonate.
- A non-anchor target starts the existing placement sequence.
The exact anchor position remains locked. Moving to a different anchor never retargets the sequence. Normal abort and timeout rules still apply.
Manual regression checks: existing anchors with charges 0, 1 and 4; no anchor item in inventory; no glowstone on charged anchors; missing glowstone on uncharged anchors; target removed or changed before the next phase. These gameplay cases have not been run in CI.
Detonation uses vanilla interaction: dimensions in which anchors set spawn retain vanilla behavior.
