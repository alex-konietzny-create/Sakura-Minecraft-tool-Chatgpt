package dev.sakuralive;

import dev.sakuralive.mixin.ClientCooldownAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

/** Input arrives on ticks; timed actions run on the render thread, at most one per frame. */
public final class SakuraClient implements ClientModInitializer {
 public static final SakuraSettings settings=SakuraSettings.load();
 public static KeyBinding anchorKey,legitKey,safeKey,doubleKey,crystalKey,fastKey,menuKey;
 public static String lastStatus="Bereit";
 private static SakuraClient instance;
 private Run run;
 private Object world,player;
 private BlockPos crystalBase;
 private final CrystalCycle crystalCycle=new CrystalCycle();
 private int crystalSlot=-1;
 private boolean crystalHeld;
 private static final class Run {
  int mode,original,expected;
  BlockPos pos,cover;
  SakuraSettings.AnchorOptions options;
  AnchorPlan plan;
  KeyBinding key;
 }
 @Override public void onInitializeClient(){
  instance=this;
  var category=KeyBinding.Category.create(Identifier.of("sakuralive","main"));
  anchorKey=bind("anchor",GLFW.GLFW_KEY_G,category);legitKey=bind("legit",GLFW.GLFW_KEY_UNKNOWN,category);
  safeKey=bind("safe",GLFW.GLFW_KEY_UNKNOWN,category);doubleKey=bind("double",GLFW.GLFW_KEY_UNKNOWN,category);
  crystalKey=bind("crystal",GLFW.GLFW_KEY_H,category);fastKey=bind("fastplace",GLFW.GLFW_KEY_J,category);
  menuKey=bind("menu",GLFW.GLFW_KEY_RIGHT_SHIFT,category);
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()&&Boolean.getBoolean("sakura.uiSmoke"))SakuraUiSmoke.install();
 }
 private KeyBinding bind(String id,int code,KeyBinding.Category category){
  return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive."+id,InputUtil.Type.KEYSYM,code,category));
 }
 public static KeyBinding keyFor(int mode){
  return switch(mode){case 0->anchorKey;case 1->legitKey;case 2->safeKey;case 3->doubleKey;case 4->crystalKey;case 5->fastKey;default->menuKey;};
 }
 public static SakuraSettings.AnchorOptions optionsFor(int mode){
  return switch(mode){case 1->settings.legitOptions;case 2->settings.safeOptions;case 3->settings.doubleOptions;default->settings.anchorOptions;};
 }
 public static boolean enabled(int mode){
  return switch(mode){case 0->settings.anchor;case 1->settings.legitAnchor;case 2->settings.safeAnchor;case 3->settings.doubleAnchor;case 4->settings.crystal;case 5->settings.fastPlace;default->true;};
 }
 public static void toggle(int mode){
  switch(mode){case 0->settings.anchor=!settings.anchor;case 1->settings.legitAnchor=!settings.legitAnchor;case 2->settings.safeAnchor=!settings.safeAnchor;case 3->settings.doubleAnchor=!settings.doubleAnchor;case 4->settings.crystal=!settings.crystal;case 5->settings.fastPlace=!settings.fastPlace;}
 }
 private static boolean ready(MinecraftClient mc){
  return mc.player!=null&&mc.world!=null&&mc.interactionManager!=null&&mc.currentScreen==null
    &&mc.isWindowFocused()&&!mc.isPaused()&&mc.player.isAlive()&&!mc.player.isSpectator();
 }
 private void tick(MinecraftClient mc){
  while(menuKey.wasPressed())mc.setScreen(new SakuraScreen());
  if(!ready(mc)){cancel(mc,"Menü / Spiel pausiert");drain();return;}
  while(crystalKey.wasPressed()){toggle(4);settings.save();}
  while(fastKey.wasPressed()){toggle(5);settings.save();}
  for(int mode=0;mode<4;mode++)while(keyFor(mode).wasPressed()){
   if(run!=null){cancel(mc,"Per Hotkey abgebrochen");continue;}
   if(enabled(mode))start(mc,mode);
  }
  fastPlace(mc);
 }
 private void drain(){for(int i=0;i<6;i++)while(keyFor(i).wasPressed()){}}
 private void start(MinecraftClient mc,int mode){
  if(!(mc.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK
     ||!mc.player.canInteractWithBlockAt(hit.getBlockPos(),0)){lastStatus="Kein erreichbarer Block anvisiert";return;}
  var state=mc.world.getBlockState(hit.getBlockPos());boolean existing=state.isOf(Blocks.RESPAWN_ANCHOR);
  var r=new Run();r.mode=mode;r.options=optionsFor(mode);r.key=keyFor(mode);
  r.original=r.expected=mc.player.getInventory().getSelectedSlot();
  r.pos=existing||state.isReplaceable()?hit.getBlockPos().toImmutable():hit.getBlockPos().offset(hit.getSide());
  r.cover=coverPosition(mc,r.pos);
  var o=r.options;
  r.plan=new AnchorPlan(existing,existing?state.get(RespawnAnchorBlock.CHARGES):0,mode==2,mode==3?2:1,
    new AnchorPlan.Timings(o.placeMs,o.chargeMs,o.coverMs,o.repeatMs,o.timeoutMs),System.nanoTime());
  run=r;world=mc.world;player=mc.player;clearCrystal();lastStatus="Anchor gestartet";
 }
 public static void frame(MinecraftClient mc){if(instance!=null)instance.advance(mc);}
 private void advance(MinecraftClient mc){
  if(!ready(mc)){cancel(mc,"Unterbrochen");clearCrystal();return;}
  if(world!=mc.world||player!=mc.player){
   if(run!=null){run=null;lastStatus="Welt / Spieler gewechselt";}
   clearCrystal();world=mc.world;player=mc.player;
  }
  if(run!=null){advanceAnchor(mc,System.nanoTime());return;}
  crystal(mc,System.nanoTime());
 }
 private void advanceAnchor(MinecraftClient mc,long now){
  var r=run;var o=r.options;
  if(!enabled(r.mode)||mc.player.getInventory().getSelectedSlot()!=r.expected
     ||(o.holdToRun&&!r.key.isPressed())){cancel(mc,"Losgelassen / Slot geändert");return;}
  if(!mc.player.canInteractWithBlockAt(r.pos,0)){cancel(mc,"Anchor außer Reichweite");return;}
  var state=mc.world.getBlockState(r.pos);boolean present=state.isOf(Blocks.RESPAWN_ANCHOR);
  var action=r.plan.poll(new AnchorPlan.Seen(present,present?state.get(RespawnAnchorBlock.CHARGES):0,
    state.isReplaceable(),mc.world.getBlockState(r.cover).isOf(Blocks.GLOWSTONE)),now);
  if(r.plan.done()){
   if(r.plan.stage()==AnchorPlan.Stage.FINISHED){lastStatus="Anchor abgeschlossen";run=null;}
   else cancel(mc,r.plan.reason());
   return;
  }
  lastStatus=(r.mode==3?"Double "+(r.plan.completed()+1)+"/2 · ":"")+r.plan.stage().name();
  if(action==AnchorPlan.Action.NONE)return;
  BlockHitResult hit;
  if(action==AnchorPlan.Action.PLACE){
   hit=placementHit(mc,r.pos);
   if(hit==null){cancel(mc,"Keine sichtbare Platzierfläche an der festen Position");return;}
   if(o.requireAim&&!aimsAtPlacement(mc,r.pos)){cancel(mc,"Platzierziel nicht mehr anvisiert");return;}
  }else if(action==AnchorPlan.Action.COVER){
   if(!r.cover.equals(coverPosition(mc,r.pos))){cancel(mc,"Spielerseite verändert");return;}
   if(!mc.world.getBlockState(r.cover).isReplaceable()
     ||!mc.world.getOtherEntities(null,new Box(r.cover)).isEmpty()){cancel(mc,"Deckungsposition belegt");return;}
   hit=placementHit(mc,r.cover);
   if(hit==null){cancel(mc,"Glowstone-Deckung nicht platzierbar");return;}
   if(o.requireAim&&!aimsAtAnchor(mc,r.pos)){cancel(mc,"Anchor nicht anvisiert");return;}
  }else{
   hit=r.mode==2&&action==AnchorPlan.Action.DETONATE?safeAnchorHit(mc,r.pos,r.cover,o.requireAim):anchorHit(mc,r.pos,o.requireAim);
   if(hit==null){cancel(mc,"Exaktes Anchor-Ziel verloren");return;}
  }
  int slot=switch(action){case PLACE->o.anchorSlot-1;case CHARGE,COVER->o.glowSlot-1;default->o.finalSlot-1;};
  var stack=mc.player.getInventory().getStack(slot);
  if(action==AnchorPlan.Action.PLACE&&!stack.isOf(Items.RESPAWN_ANCHOR)
    ||(action==AnchorPlan.Action.CHARGE||action==AnchorPlan.Action.COVER)&&!stack.isOf(Items.GLOWSTONE)){
   cancel(mc,"Benötigtes Item im konfigurierten Slot fehlt");return;
  }
  if(action==AnchorPlan.Action.DETONATE){
   // A normal final item (including an empty hand) is allowed; no totem requirement.
   if(stack.isOf(Items.GLOWSTONE)||stack.isOf(Items.RESPAWN_ANCHOR)||mc.player.getOffHandStack().isOf(Items.GLOWSTONE)){
    cancel(mc,"Glowstone / Anchor verhindert zuverlässiges Auslösen");return;
   }
   if(r.mode==2&&(!r.cover.equals(coverPosition(mc,r.pos))||!mc.world.getBlockState(r.cover).isOf(Blocks.GLOWSTONE))){
    cancel(mc,"Glowstone-Deckung fehlt auf deiner Seite");return;
   }
   if(mc.player.isSneaking()&&stack.getItem() instanceof BlockItem){cancel(mc,"Schleichen mit Block im finalen Slot");return;}
  }
  if(r.expected!=slot){
   mc.player.getInventory().setSelectedSlot(slot);r.expected=slot;
   r.plan.defer(now,o.switchMs);return;
  }
  boolean accepted=mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,hit).isAccepted();
  if(accepted)mc.player.swingHand(Hand.MAIN_HAND);
  r.plan.confirm(action,accepted,now);
 }
 private static BlockPos coverPosition(MinecraftClient mc,BlockPos anchor){
  double dx=mc.player.getX()-(anchor.getX()+0.5),dz=mc.player.getZ()-(anchor.getZ()+0.5);
  Direction side=Math.abs(dx)>=Math.abs(dz)?(dx>=0?Direction.EAST:Direction.WEST):(dz>=0?Direction.SOUTH:Direction.NORTH);
  return anchor.offset(side);
 }
 private static boolean aimsAtAnchor(MinecraftClient mc,BlockPos pos){
  return mc.crosshairTarget instanceof BlockHitResult h&&h.getType()==HitResult.Type.BLOCK&&h.getBlockPos().equals(pos);
 }
 private static boolean aimsAtPlacement(MinecraftClient mc,BlockPos pos){
  if(!(mc.crosshairTarget instanceof BlockHitResult h)||h.getType()!=HitResult.Type.BLOCK)return false;
  var clicked=h.getBlockPos();
  return (mc.world.getBlockState(clicked).isReplaceable()?clicked:clicked.offset(h.getSide())).equals(pos);
 }
 private static BlockHitResult trace(MinecraftClient mc,BlockPos pos,Vec3d end){
  var hit=mc.world.raycast(new RaycastContext(mc.player.getEyePos(),end,RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,mc.player));
  return hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos)&&mc.player.canInteractWithBlockAt(pos,0)?hit:null;
 }
 private static BlockHitResult anchorHit(MinecraftClient mc,BlockPos pos,boolean requireAim){
  if(aimsAtAnchor(mc,pos))return (BlockHitResult)mc.crosshairTarget;
  return requireAim?null:trace(mc,pos,Vec3d.ofCenter(pos));
 }
 private static BlockHitResult safeAnchorHit(MinecraftClient mc,BlockPos pos,BlockPos cover,boolean requireAim){
  if(requireAim&&!aimsAtAnchor(mc,pos)&&!aimsAtAnchor(mc,cover))return null;
  var direct=anchorHit(mc,pos,false);if(direct!=null)return direct;
  // Cover may hide the anchor's center. Use a genuinely visible top face, never click through cover.
  return trace(mc,pos,Vec3d.ofCenter(pos).add(0,0.4999,0));
 }
 private static BlockHitResult placementHit(MinecraftClient mc,BlockPos pos){
  for(Direction dir:new Direction[]{Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,Direction.UP}){
   BlockPos support=pos.offset(dir);var state=mc.world.getBlockState(support);
   if(state.isReplaceable()||state.isOf(Blocks.RESPAWN_ANCHOR)||!state.isFullCube(mc.world,support))continue;
   Direction face=dir.getOpposite();
   Vec3d point=Vec3d.ofCenter(support).add(face.getOffsetX()*0.4999,face.getOffsetY()*0.4999,face.getOffsetZ()*0.4999);
   var hit=trace(mc,support,point);
   if(hit!=null&&hit.getSide()==face)return hit;
  }
  return null;
 }
 private void cancel(MinecraftClient mc,String why){
  if(run!=null){
   if(mc.player==player&&mc.world==world&&mc.player!=null&&mc.player.getInventory().getSelectedSlot()==run.expected)
    mc.player.getInventory().setSelectedSlot(run.original);
   run=null;lastStatus=why;
  }
 }
 public static boolean ownsItemUse(MinecraftClient mc){return ready(mc)&&(crystalActive(mc)||(instance!=null&&instance.run!=null));}
 public static boolean crystalActive(MinecraftClient mc){
  return settings.crystal&&ready(mc)&&(instance==null||instance.run==null)
    &&mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)
    &&GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS;
 }
 private void clearCrystal(){crystalBase=null;crystalSlot=-1;crystalHeld=false;crystalCycle.reset();}
 private static BlockHitResult crystalBaseHit(MinecraftClient mc){
  HitResult result=mc.player.raycast(mc.player.getBlockInteractionRange(),1.0f,false);
  if(result instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK){
   var state=mc.world.getBlockState(hit.getBlockPos());
   if(state.isOf(Blocks.OBSIDIAN)||state.isOf(Blocks.BEDROCK))return hit;
  }
  return null;
 }
 private static boolean hittable(MinecraftClient mc,EndCrystalEntity entity){
  return entity.isAlive()&&mc.player.canInteractWithEntity(entity,0)&&mc.player.canSee(entity);
 }
 private void crystal(MinecraftClient mc,long now){
  if(!crystalActive(mc)){clearCrystal();return;}
  int slot=mc.player.getInventory().getSelectedSlot();
  if(!crystalHeld||crystalSlot!=slot){clearCrystal();crystalHeld=true;crystalSlot=slot;}
  // A shield/item-use state started before assist activation must not lock startup.
  if(mc.player.isUsingItem()){mc.interactionManager.stopUsingItem(mc.player);return;}
  var timing=new CrystalCycle.Timings(settings.crystalPlaceMs,settings.crystalBreakMs,settings.crystalTimeoutMs);
  var baseHit=crystalBaseHit(mc);
  BlockPos aimedBase=baseHit==null?null:baseHit.getBlockPos();
  EndCrystalEntity aimed=null;
  if(mc.crosshairTarget instanceof EntityHitResult hit&&hit.getEntity() instanceof EndCrystalEntity entity&&hittable(mc,entity))aimed=entity;
  boolean waiting=crystalCycle.stage()==CrystalCycle.Stage.WAIT_SPAWN;
  BlockPos search=waiting?crystalBase:aimedBase;
  EndCrystalEntity target=waiting?null:aimed;
  // The entity can block placement while vanilla's crosshair still reports its base.
  if(target==null&&search!=null){
   for(var entity:mc.world.getEntitiesByClass(EndCrystalEntity.class,new Box(search.up()).expand(0.25,1,0.25),
     e->e.getBlockPos().equals(search.up())&&hittable(mc,e))){target=entity;break;}
  }
  boolean same=!waiting||(aimedBase!=null&&aimedBase.equals(crystalBase))
    ||(aimed!=null&&aimed.getBlockPos().equals(crystalBase.up()));
  var tracked=mc.world.getEntityById(crystalCycle.trackedId());
  boolean trackedAlive=tracked!=null&&tracked.isAlive();
  boolean canPlace=baseHit!=null&&mc.player.canInteractWithBlockAt(aimedBase,0)&&mc.world.isAir(aimedBase.up())
    &&mc.world.getOtherEntities(null,new Box(aimedBase.up()).stretch(0,1,0)).isEmpty();
  var action=crystalCycle.poll(now,target==null?-1:target.getId(),canPlace,trackedAlive,same,timing);
  if(action==CrystalCycle.Action.BREAK&&target!=null){
   mc.interactionManager.attackEntity(mc.player,target);mc.player.swingHand(Hand.MAIN_HAND);
   crystalCycle.attacked(target.getId(),now,timing);crystalBase=null;return;
  }
  if(action==CrystalCycle.Action.PLACE&&baseHit!=null){
   boolean accepted=mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,baseHit).isAccepted();
   crystalCycle.placed(accepted,now,timing);
   if(accepted){mc.player.swingHand(Hand.MAIN_HAND);crystalBase=aimedBase.toImmutable();}
  }
 }
 private void fastPlace(MinecraftClient mc){
  if(run!=null||!settings.fastPlace||!mc.options.useKey.isPressed())return;
  var main=mc.player.getMainHandStack();var off=mc.player.getOffHandStack();
  boolean mb=main.getItem() instanceof BlockItem,ob=off.getItem() instanceof BlockItem;
  if(!mb&&!ob)return;
  var stack=mb?main:off;
  if(settings.excludeAnchors&&(stack.isOf(Items.RESPAWN_ANCHOR)||stack.isOf(Items.GLOWSTONE)))return;
  int ticks=(settings.placeDelayMs+49)/50;
  var cooldown=(ClientCooldownAccessor)mc;
  if(cooldown.sakura$getItemUseCooldown()>ticks)cooldown.sakura$setItemUseCooldown(ticks);
 }
}
