package dev.sakuralive.mixin;
import dev.sakuralive.BlockOutline;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorldRenderer.class)
public abstract class BlockOutlineMixin {
 @Inject(method="renderTargetBlockOutline",at=@At("HEAD"),cancellable=true)
 private void sakura$outline(VertexConsumerProvider.Immediate consumers,MatrixStack matrices,boolean translucent,WorldRenderState state,CallbackInfo ci){
  if(BlockOutline.render(consumers,matrices,translucent,state))ci.cancel();
 }
}
