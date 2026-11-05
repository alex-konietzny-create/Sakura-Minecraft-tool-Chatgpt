package dev.sakuralive.mixin;
import dev.sakuralive.Hotkeys;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Keyboard.class)
public abstract class KeyboardShortcutMixin {
 @Inject(method="onKey",at=@At("HEAD"),cancellable=true)
 private void sakura$shortcut(long window,int action,net.minecraft.client.input.KeyInput key,CallbackInfo ci){if(Hotkeys.keyboard(key,action))ci.cancel();}
}
