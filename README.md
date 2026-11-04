# Sakura Live — Pink Fabric client

Minecraft Java **1.21.11**, Fabric Loader and Fabric API, Java 21.

Download the artifact **SakuraLive-Pink-Fabric-1.21.11** from the latest successful [GitHub Actions build](https://github.com/alex-konietzny-create/Sakura-Minecraft-tool-Chatgpt/actions).
Extract the ZIP, put the contained JAR in your Minecraft instance's mods folder, and remove older Sakura JARs.

## Controls
- Right Shift: open the rounded pink/black workspace, with Anchor, Crystal and Utility tabs.
- G: run / cancel anchor sequence (enable the module first).
- H: toggle crystal assist.
- J: toggle fast place.

Use **Edit all hotkeys** in the workspace, or Minecraft Options → Controls → Key Binds → Sakura Live. Keyboard and mouse bindings, conflict indicators and reset use Minecraft's native controls. Bindings persist in options.txt; module settings persist in config/sakura-live.json.

## Crystal
Requires crystals in the **main hand** and the physical **right mouse button held**.
Aim at obsidian/bedrock to place; the assist waits for the crystal entity before breaking it. Existing crystals can be broken by aiming at them. Placement and break timings are separately adjustable, 1–20 ticks. No inventory autoswitch.
Release the button, change the held item, open a menu, lose window focus, die or leave the world to stop. Reach, visibility, occupancy and timeout checks limit stale actions. Vanilla item use is suppressed while the assist controls crystals.

## Anchor
Enable the module, configure anchor, glowstone and final hotbar slots, and set the step delay (1–20 ticks).
Aim at the support, press the action hotkey, then keep aiming at the newly placed anchor for charge/detonation. Press the hotkey again to cancel.
Manual slot changes and invalid targets cancel the sequence. Vanilla item use is suppressed during the sequence. Glowstone in the offhand cancels detonation. A completed sequence selects the configured final slot.

## Verification
CI builds and remaps the Fabric JAR with Java 21. Gameplay, appearance in a running client and compatibility with other mods require manual verification. No gameplay test is claimed.
See [Fabric/VERIFY.md](Fabric/VERIFY.md) for the manual checklist and state-handling limits.
