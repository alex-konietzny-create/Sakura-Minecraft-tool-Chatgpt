package dev.sakuralive;
import dev.sakuralive.mixin.*;
import net.minecraft.client.render.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
/** Only the selected block, including its normally hidden rear edges, is drawn without depth occlusion. */
final class InteriorLines {
 private static RenderLayer layer;
 static RenderLayer layer(){
  if(layer==null){var pipeline=RenderPipeline.builder(LinePipelineAccess.sakura$lines()).withLocation(net.minecraft.util.Identifier.of("sakuralive","pipeline/interior_lines")).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withBlend(BlendFunction.TRANSLUCENT).build();layer=RenderLayerAccess.sakura$create("sakura_interior_lines",RenderSetup.builder(pipeline).build());}
  return layer;
 }
}

