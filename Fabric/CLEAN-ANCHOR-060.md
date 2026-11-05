# Sakura 0.6.0

Existing anchors skip placement and use the configured Glowstone slot before the final activation slot. Native Windows inputs remain the action source. The interaction-manager mixin only filters normal game interactions against the intended action, item, selected slot and fixed target; it does not invoke interactions or send custom action packets.

An accepted use cannot be sent a second time for the same phase. Placement and cover phases no longer retry blindly while waiting for world confirmation. Refused interactions stop with an explanatory status. This avoids guessing success from a keypress. A missing server confirmation can still time out; success is not guaranteed under lag or protection plugins.

Double Anchor retains one immutable target, waits for the first anchor to disappear, then reacquires a supporting face for the second anchor. Camera speed and automatic reacquisition are configurable in its Camera tab. A destroyed support or blocked view stops the sequence. Repeated Double/Legit shortcut presses no longer cancel an active sequence. Normal/Safe presses coalesce to one queued repeat.

Disabled modules cannot steal a shared shortcut. Active shortcut conflicts are visibly marked and blocked unless an already running module owns the shortcut. On the reported configuration, enable Anchor and disable Legit Anchor or assign separate keys for C.

The UI uses framebuffer-density font rendering with weight 550 body and 700 headings, dark navy surfaces, brighter pink accents, separated card headings and controls, and real entry/toggle/hover motion. Primary text/card contrast is approximately 16.3:1 and body/card 9.6:1 before effects. Reduced-motion remains available.

Validation: Java 21/Fabric 1.21.11 build and deterministic state/input/geometry tests; actual Minecraft UI smoke with guard-mixin loading, binding conflicts, editable controls and 18 changing animation frames. No gameplay world, server, complete anchor latency or anticheat test. Millisecond settings are requested pauses, not guarantees of game/server execution at that rate.
