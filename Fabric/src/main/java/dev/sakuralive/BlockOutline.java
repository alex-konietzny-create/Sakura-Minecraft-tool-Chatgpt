package dev.sakuralive;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
/** Replaces only the normal selected-block outline, using its actual voxel shape and depth-tested lines. */
public final class BlockOutline {
 public static int rgb(SakuraSettings.OutlineOptions o,long now){
  if(!o.rainbow)return (o.red<<16)|(o.green<<8)|o.blue;
  return java.awt.Color.HSBtoRGB((float)((now/1_000_000_000.0/o.rainbowSeconds)%1),.8f,1)&0xFFFFFF;
 }
 public static float pulse(SakuraSettings.OutlineOptions o,long now){return o.pulse?(float)(.8+.2*Math.sin(now/1_000_000_000.0*Math.PI*2/o.pulseSeconds)):1;}
 public static boolean render(VertexConsumerProvider.Immediate consumers,MatrixStack matrices,boolean translucent,WorldRenderState world){
  var o=SakuraClient.settings.outline;if(!o.enabled)return false;
  var state=world.outlineRenderState;if(state==null||state.isTranslucent()!=translucent)return true;
  long now=System.nanoTime();int rgb=rgb(o,now),alpha=Math.round(o.opacity*pulse(o,now));
  var camera=world.cameraRenderState.pos;var pos=state.pos();var vertex=consumers.getBuffer(RenderLayers.linesTranslucent());
  var entry=matrices.peek();double ox=pos.getX()-camera.x,oy=pos.getY()-camera.y,oz=pos.getZ()-camera.z;
  if(o.glow)for(int i=3;i>=1;i--){
   int halo=Math.round(alpha*o.glowStrength/100f*.14f*(4-i)/3f);
   draw(state.shape(),entry,vertex,ox,oy,oz,o,(halo<<24)|rgb,o.width/10f+o.glowRadius*i/3f);
  }
  draw(state.shape(),entry,vertex,ox,oy,oz,o,(alpha<<24)|rgb,o.width/10f);
  consumers.drawCurrentLayer();return true;
 }
 private static void draw(net.minecraft.util.shape.VoxelShape shape,MatrixStack.Entry entry,VertexConsumer v,double ox,double oy,double oz,SakuraSettings.OutlineOptions o,int color,float width){
  shape.forEachEdge((ax,ay,az,bx,by,bz)->OutlineGeometry.edge(ax,ay,az,bx,by,bz,o.length/100.0,o.corners,(x1,y1,z1,x2,y2,z2)->{
   double dx=x2-x1,dy=y2-y1,dz=z2-z1,len=Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<1e-8)return;
   v.vertex(entry,(float)(x1+ox),(float)(y1+oy),(float)(z1+oz)).color(color).normal(entry,(float)(dx/len),(float)(dy/len),(float)(dz/len)).lineWidth(width);
   v.vertex(entry,(float)(x2+ox),(float)(y2+oy),(float)(z2+oz)).color(color).normal(entry,(float)(dx/len),(float)(dy/len),(float)(dz/len)).lineWidth(width);
  }));
 }
}
