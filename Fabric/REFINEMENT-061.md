# 0.6.1: lighter UI and immediate Anchor restart

Cards use Minecraft's native item renderer instead of letter tiles: End Crystal, Anchor, Glowstone, Totem, double Anchors, Obsidian and Target for Outline. Resource-pack item models are respected. Detail headings also show the module item. The main sidebar displays the session's actual Minecraft username and initials, not a fixed Sakura account.

Body type is reduced from 12 to 11 layout pixels and regular/heading weights from 550/700 to 450/600. Controls use neutral black surfaces, brighter readable text and smaller switches instead of broad blue-grey fills. Card descriptions have more space and shorter German labels. Existing hover, entry and toggle animation remains.

Each enabled Anchor shortcut press releases outstanding simulated inputs, clears the pending repeat, restores camera input mode and starts a new plan from the current crosshair/world state. It does not restore an old hotbar slot or wait behind the previous plan. Existing anchors are charged/activated without placing another anchor. Held-repeat remains configurable; manual repress supersedes it. An already submitted server action cannot be recalled by cancelling the local plan.

Interaction target/item checks, focus checks and world confirmation remain. Missing items, blocked targets and server rejections can still prevent success. No guaranteed 1ms execution or anticheat claim. Gameplay has not been tested.
