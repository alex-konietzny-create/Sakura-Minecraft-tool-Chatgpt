package dev.sakuralive.mixin;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(RenderLayer.class)
public interface RenderLayerAccess {
 @Invoker("of") static RenderLayer sakura$create(String name,RenderSetup setup){throw new AssertionError();}
}
