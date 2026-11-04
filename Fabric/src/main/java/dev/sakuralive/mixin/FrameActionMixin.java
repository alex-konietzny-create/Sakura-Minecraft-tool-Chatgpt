package dev.sakuralive.mixin;
import dev.sakuralive.SakuraClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class FrameActionMixin {
 @Inject(method="render",at=@At("TAIL"))
 private void sakura$timedActions(boolean tick,CallbackInfo ci){
  SakuraClient.frame((MinecraftClient)(Object)this);
 }
}
