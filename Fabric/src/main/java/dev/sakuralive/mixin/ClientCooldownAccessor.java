package dev.sakuralive.mixin;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MinecraftClient.class)
public interface ClientCooldownAccessor {
 @Accessor("itemUseCooldown") int sakura$getItemUseCooldown();
 @Accessor("itemUseCooldown") void sakura$setItemUseCooldown(int ticks);
}