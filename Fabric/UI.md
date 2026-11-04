# Pink 0.2 interface
- Logical 760 × 480 workspace, original blossom mark, warm black surfaces, pink accents.
- Slide-in entry, eased navigation rail, tab reveal, animated switches and slider handles.
- Motion can be reduced in Utility; the setting persists.
- Hotbar slot grids include current inventory item thumbnails when a player is present.
- Delay sliders display ticks and nominal milliseconds (50 ms per tick).
- Direct key capture per module and for the menu; keyboard or mouse; Escape cancels, Delete/Backspace clears.
- Conflicts check all registered Minecraft bindings. Native keybind controls remain accessible.
- Pointer hit testing uses the same inverse scale/translation as rendering.
- Keyboard tab navigation, focus outlines, slider arrows and native widget narration.
- Settings save when controls change or a slider is released; failed settings writes are displayed.
- Menu status explicitly reports that macros are paused.

The Render Sakura UI workflow launches a development Minecraft client with software OpenGL and captures the three tabs. Its opt-in harness is gated by both development mode and sakura.uiSmoke. This check renders menus without joining a world and is not gameplay validation.
Manual checks still needed: actual mouse/keyboard interaction, long translated key labels, installed resource packs, GUI scales and behavior with other mods.
