package dev.sakuralive.mixin;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(RenderPipelines.class)
public interface LinePipelineAccess {
 @Accessor("RENDERTYPE_LINES_SNIPPET") static RenderPipeline.Snippet sakura$lines(){throw new AssertionError();}
}
