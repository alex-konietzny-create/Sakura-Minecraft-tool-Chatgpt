package dev.sakuralive.mixin;
import dev.sakuralive.SakuraClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class InventoryHandoffMixin {
 @Inject(method="setScreen",at=@At("HEAD"),cancellable=true)
 private void sakura$finalSlotBeforeInventory(Screen screen,CallbackInfo ci){if(SakuraClient.deferInventory(screen))ci.cancel();}
}
