# 0.6.4: Caps Lock hotbar support

GLFW Caps Lock (280) now maps to Windows VK_CAPITAL (0x14). Previously it fell into the unsupported-key case and aborted native slot preparation. The existing macro path reads the actual Minecraft hotbar binding, so a Glowstone slot bound to Caps Lock now uses that binding without changing it to a number key. This also enables Caps Lock for other supported keybinding roles.

UI smoke temporarily binds hotbar slot 2 to Caps Lock, verifies matching, maps it through the actual Windows input converter, and validates native down/up structures. The original binding is restored. No OS key or gameplay action is injected by this check. Build and existing regressions are run separately.

Bind Caps Lock to the intended hotbar slot in Minecraft's controls; select that slot number as Glowstone in Sakura, or leave automatic item lookup enabled. Caps Lock must not simultaneously be an active macro trigger or Use/Attack key, because that is a conflicting binding.
