package dev.sakuralive;
import net.minecraft.block.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

/** Placement rays ignore non-colliding fire, while still respecting solid obstructions. */
final class PlacementGeometry {
 static boolean replaceable(BlockState state){
  return state.isReplaceable()||state.isOf(Blocks.FIRE)||state.isOf(Blocks.SOUL_FIRE);
 }
 static BlockPos destination(BlockView view,BlockHitResult hit){
  return replaceable(view.getBlockState(hit.getBlockPos()))?hit.getBlockPos():hit.getBlockPos().offset(hit.getSide());
 }
 static BlockHitResult supportHit(BlockView view,Vec3d eye,BlockPos target,Direction face,ShapeContext context){
  if(!replaceable(view.getBlockState(target)))return null;
  BlockPos support=target.offset(face.getOpposite());
  var state=view.getBlockState(support);
  if(replaceable(state)||state.isOf(Blocks.RESPAWN_ANCHOR)||!state.isFullCube(view,support))return null;
  Vec3d point=Vec3d.ofCenter(support).add(face.getOffsetX()*.4999,face.getOffsetY()*.4999,face.getOffsetZ()*.4999);
  var hit=view.raycast(new RaycastContext(eye,point,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,context));
  return hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(support)&&hit.getSide()==face?hit:null;
 }
}
