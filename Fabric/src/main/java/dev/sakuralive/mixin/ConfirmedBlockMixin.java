package dev.sakuralive.mixin;
import dev.sakuralive.SakuraClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe incoming authoritative block changes; never send an action packet. */
@Mixin(ClientWorld.class)
public abstract class ConfirmedBlockMixin {
 @Inject(method="handleBlockUpdate",at=@At("TAIL"))
 private void sakura$confirmed(BlockPos pos,BlockState state,int flags,CallbackInfo ci){SakuraClient.confirmedBlock(pos,state);}
 @Inject(method="processPendingUpdate",at=@At("TAIL"))
 private void sakura$reconciled(BlockPos pos,BlockState state,net.minecraft.util.math.Vec3d playerPos,CallbackInfo ci){SakuraClient.confirmedBlock(pos,state);}
}

