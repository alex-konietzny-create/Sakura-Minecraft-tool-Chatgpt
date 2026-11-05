package dev.sakuralive.mixin;
import dev.sakuralive.Hotkeys;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mouse.class)
public abstract class MouseShortcutMixin {
 @Inject(method="onMouseButton",at=@At("HEAD"),cancellable=true)
 private void sakura$shortcut(long window,net.minecraft.client.input.MouseInput key,int action,CallbackInfo ci){if(Hotkeys.mouse(key,action))ci.cancel();}
}
