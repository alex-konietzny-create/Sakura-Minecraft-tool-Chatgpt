# Sakura 0.6.2: observed gameplay and limits

## Behavior

- Anchor steps observe incoming server block updates and reconciled prediction states instead of treating local prediction as confirmation. These are read-only observations; the mod does not construct action packets.
- At measured latency of 50 ms or greater, slot preparation uses separate native hotbar input followed by confirmation of the actual selected slot. Below that threshold, the existing optional paired slot/use input remains available.
- Automatic hotbar lookup prefers the configured slot and otherwise searches the nine actual stacks. It can be disabled in each Anchor module's Hotbar settings. Missing items produce a specific status. Final slot remains configurable and does not require a Totem.
- Double Anchor explicitly triggers its first blast with an Anchor in the main hand, then places/charges another at the same position and uses the configured final hotbar slot (e.g. Totem) for the second blast. No offhand swap is performed.
- Crosshair drift before a click is accepted suppresses the wrong-target interaction and reacquires the requested action instead of immediately cancelling. Safe camera feedback filters large disturbances and eases near its target. When the target is obscured temporarily, it waits within the configured timeout.
- Obstructing fire can be extinguished with native attack input. Use waits while Minecraft is still processing block breaking. Destructed supports, unreachable targets and missing items still stop the macro.
- Outline adds optional depth-independent rendering of the selected voxel shape's outer edges. There are no interior diagonals. Length, color, width and glow still apply.

## Actual local gameplay test

Java 21, Fabric Minecraft 1.21.11, own integrated creative overworld. A task-only development fixture supplied an obsidian platform, Anchor/Glowstone stacks, and a Totem in hotbar slot 9; offhand was empty. Flying was enabled in the fixture to isolate the sequence from blast knockback. Fixture code and delayed-observation hooks are excluded from the release.

Before the correction, a second Anchor was placed but charging hit intervening fire and aborted. A separate run also demonstrated that explosion knockback can move the player beyond reach; the mod correctly stopped instead of placing remotely.

After correction, with configured Anchor/Glowstone slots deliberately wrong (8/7, actual stacks in 1/2), separate native slot preparation and incoming confirmation observations delayed by an extra 60 ms:

1. Double: server observed Anchor 0 charge -> 1 charge -> removed -> Anchor 0 charge -> 1 charge -> removed at the same block position. Native interaction receipts showed the first blast holding Respawn Anchor and the second holding Totem of Undying, empty offhand. Completed status: 1167 ms.
2. Safe: server observed Anchor placed and charged, then Glowstone cover on the player side before the blast. Native receipt showed Totem for activation. Completed status: 817 ms.
3. In-world outline rendered all outer cube edges through the selected block without diagonal center lines.

These are bounded local test results, not remote-server benchmarks or guarantees. The artificial delay affects confirmation observations, not all network traffic; it is not a real 60 ms RTT connection. Server protection, tick lag, destroyed supports, player movement and loss of focus can still prevent completion. Millisecond settings specify requested pauses, not guaranteed action rates.

Deterministic checks additionally cover confirmations delayed by 60/120/250/600 ms, two-cycle ordering, wrong/missing hotbar slots, recovery after a moved ray, and camera convergence after simulated disturbances. UI smoke checks key capture, persisted Minecraft options, changed-key routing, mouse bindings, outline toggle persistence and controls at three sizes.
