package de.txtmanager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class TxtTextureManager implements ClientModInitializer {
    @Override public void onInitializeClient() {
        KeyBinding open = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.txt_texture_manager.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.wasPressed()) client.setScreen(new TextureManagerScreen(null));
        });
    }
}
