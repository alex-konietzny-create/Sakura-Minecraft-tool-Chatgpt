package dev.sakuralive.mixin;
import dev.sakuralive.SakuraClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class CrystalUseMixin {
 @Inject(method="doItemUse",at=@At("HEAD"),cancellable=true)
 private void sakura$controlledCrystalUse(CallbackInfo ci){
  if(SakuraClient.ownsItemUse((MinecraftClient)(Object)this))ci.cancel();
 }
}
