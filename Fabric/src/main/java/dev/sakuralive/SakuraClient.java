package dev.sakuralive;

import dev.sakuralive.mixin.ClientCooldownAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;

/** Self-contained Fabric mod: no desktop application or IPC needed. */
public final class SakuraClient implements ClientModInitializer {
 public static final SakuraSettings settings=SakuraSettings.load();
 public static KeyBinding anchorKey, crystalKey, menuKey, fastKey;
 private static net.minecraft.client.world.ClientWorld lastWorld;
 private static net.minecraft.util.math.BlockPos crystalBase;
 private static int pendingAge;
 private static int originalSlot=-1, expectedSlot=-1;
 private static net.minecraft.util.math.BlockPos placedAnchor;
 private static int anchorAge;
 private static int anchorPhase, waitTicks, crystalWait;
 private static BlockHitResult anchorTarget;

 @Override public void onInitializeClient() {
  var group=KeyBinding.Category.create(Identifier.of("sakuralive","main"));
  anchorKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.anchor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,group));
  crystalKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.crystal",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_H,group));
  menuKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,group));
  fastKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.fastplace",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_J,group));
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("sakura.uiSmoke"))SakuraUiSmoke.install();
 }
 private void tick(MinecraftClient mc) {
  while(menuKey.wasPressed())mc.setScreen(new SakuraScreen());
  if(mc.world!=lastWorld){originalSlot=-1;reset(mc);lastWorld=mc.world;}
  if(mc.player==null||mc.world==null||mc.interactionManager==null||mc.currentScreen!=null
     ||!mc.isWindowFocused()||!mc.player.isAlive()||mc.player.isSpectator()){
   reset(mc); drain(); return;
  }
  while(crystalKey.wasPressed()){settings.crystal=!settings.crystal;settings.save();}
  while(fastKey.wasPressed()){settings.fastPlace=!settings.fastPlace;settings.save();}
  if(waitTicks>0)waitTicks--;
  if(crystalWait>0)crystalWait--;
  while(anchorKey.wasPressed()){
   if(anchorPhase!=0){reset(mc);continue;}
   if(settings.anchor && mc.crosshairTarget instanceof BlockHitResult hit
       && hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK
       && mc.player.canInteractWithBlockAt(hit.getBlockPos(),0)){
    var state=mc.world.getBlockState(hit.getBlockPos());
    boolean existing=state.isOf(Blocks.RESPAWN_ANCHOR);
    // Never enter the placement phase when the crosshair already targets an anchor.
    anchorPhase=existing
      ?(state.get(net.minecraft.block.RespawnAnchorBlock.CHARGES)>0?3:2)
      :1;
    anchorTarget=hit;waitTicks=0;anchorAge=0;
    originalSlot=mc.player.getInventory().getSelectedSlot();expectedSlot=originalSlot;
    placedAnchor=existing||state.isReplaceable()?hit.getBlockPos().toImmutable():hit.getBlockPos().offset(hit.getSide());
   }
  }
  if(anchorPhase!=0){
   crystalBase=null;
   if(!settings.anchor||++anchorAge>100||mc.player.getInventory().getSelectedSlot()!=expectedSlot){reset(mc);return;}
   if(waitTicks==0)performAnchor(mc);
   return;
  }
  fastPlace(mc);
  crystal(mc);
 }
 private void drain(){while(anchorKey.wasPressed()){} while(crystalKey.wasPressed()){} while(fastKey.wasPressed()){}}
 private static void reset(MinecraftClient mc){
  if(mc.player!=null&&originalSlot>=0&&mc.player.getInventory().getSelectedSlot()==expectedSlot)
   mc.player.getInventory().setSelectedSlot(originalSlot);
  originalSlot=expectedSlot=-1;anchorPhase=waitTicks=crystalWait=pendingAge=0;
  anchorTarget=null;placedAnchor=null;crystalBase=null;
 }
 public static boolean ownsItemUse(MinecraftClient mc){
  return crystalActive(mc)||(anchorPhase!=0&&mc.player!=null&&mc.world==lastWorld&&mc.currentScreen==null&&mc.isWindowFocused());
 }
 public static boolean crystalActive(MinecraftClient mc){
  return settings.crystal&&mc.player!=null&&mc.world!=null&&mc.currentScreen==null&&mc.isWindowFocused()
    &&mc.player.isAlive()&&!mc.player.isSpectator()&&!mc.player.isUsingItem()&&anchorPhase==0
    &&mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)
    &&GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS;
 }
 private void crystal(MinecraftClient mc){
  if(!crystalActive(mc)){crystalBase=null;pendingAge=0;crystalWait=0;return;}
  if(crystalBase!=null){
   if(++pendingAge>40){crystalBase=null;crystalWait=settings.crystalPlaceDelay;return;}
   if(crystalWait>0)return;
   var crystals=mc.world.getEntitiesByClass(net.minecraft.entity.decoration.EndCrystalEntity.class,
     new net.minecraft.util.math.Box(crystalBase.up()).expand(0.25,1,0.25),
     e->e.isAlive()&&e.getBlockPos().equals(crystalBase.up()));
   for(var entity:crystals){
    if(mc.player.canInteractWithEntity(entity,0)&&mc.player.canSee(entity)){
     mc.interactionManager.attackEntity(mc.player,entity);mc.player.swingHand(Hand.MAIN_HAND);
     crystalBase=null;crystalWait=settings.crystalPlaceDelay;return;
    }
   }
   return;
  }
  if(crystalWait>0)return;
  if(mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit
     &&hit.getEntity() instanceof net.minecraft.entity.decoration.EndCrystalEntity entity
     &&entity.isAlive()&&mc.player.canInteractWithEntity(entity,0)&&mc.player.canSee(entity)){
   mc.interactionManager.attackEntity(mc.player,entity);mc.player.swingHand(Hand.MAIN_HAND);
   crystalWait=settings.crystalBreakDelay;return;
  }
  if(!(mc.crosshairTarget instanceof BlockHitResult hit))return;
  var pos=hit.getBlockPos();var state=mc.world.getBlockState(pos);
  if(!(state.isOf(Blocks.OBSIDIAN)||state.isOf(Blocks.BEDROCK))
     ||!mc.player.canInteractWithBlockAt(pos,0)||!mc.world.isAir(pos.up())
     ||!mc.world.getOtherEntities(null,new net.minecraft.util.math.Box(pos.up()).stretch(0,1,0)).isEmpty())return;
  var result=mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,hit);
  crystalWait=settings.crystalPlaceDelay;
  if(result.isAccepted()){mc.player.swingHand(Hand.MAIN_HAND);crystalBase=pos.toImmutable();pendingAge=0;crystalWait=settings.crystalBreakDelay;}
 }

 private void performAnchor(MinecraftClient mc){
  // Phase 1 place anchor; phase 2 glowstone; phase 3 detonate with non-glowstone item.
  var inventory=mc.player.getInventory();
  int slot=anchorPhase==1?settings.anchorSlot-1:
           anchorPhase==2?settings.glowSlot-1:settings.totemSlot-1;
  if(slot<0||slot>8||anchorTarget==null){reset(mc);return;}
  if(anchorPhase==1 && !inventory.getStack(slot).isOf(Items.RESPAWN_ANCHOR)
    || anchorPhase==2 && !inventory.getStack(slot).isOf(Items.GLOWSTONE)
    || anchorPhase==3 && (inventory.getStack(slot).isOf(Items.GLOWSTONE)
                          || inventory.getStack(slot).isOf(Items.RESPAWN_ANCHOR))){
   reset(mc);return;
  }
  BlockHitResult target=anchorTarget;
  if(anchorPhase>=2){
   // The position clicked in phase 1 is an adjacent support block.
   // For placed and pre-existing anchors, only interact with that exact position.
   if(!(mc.crosshairTarget instanceof BlockHitResult current)
     || !current.getBlockPos().equals(placedAnchor)
     || !mc.player.canInteractWithBlockAt(current.getBlockPos(),0)
     || !mc.world.getBlockState(current.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)){
    if(mc.world.getBlockState(placedAnchor).isAir())return;
    reset(mc);return;
   }
   target=current;
  }
  if(anchorPhase==3 && mc.player.getOffHandStack().isOf(Items.GLOWSTONE)){reset(mc);return;}
  if(anchorPhase==3 && mc.world.getBlockState(target.getBlockPos()).get(net.minecraft.block.RespawnAnchorBlock.CHARGES)==0)return;
  inventory.setSelectedSlot(slot);expectedSlot=slot;
  if(!mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,target).isAccepted()){reset(mc);return;}
  mc.player.swingHand(Hand.MAIN_HAND);
  if(anchorPhase==3){anchorPhase=0;originalSlot=expectedSlot=-1;inventory.setSelectedSlot(settings.totemSlot-1);}
  else {anchorPhase++;waitTicks=settings.actionDelay;}
 }
 private void fastPlace(MinecraftClient mc){
  if(anchorPhase!=0||!settings.fastPlace||!mc.options.useKey.isPressed())return;
  var main=mc.player.getMainHandStack();
  var off=mc.player.getOffHandStack();
  boolean mainBlock=main.getItem() instanceof BlockItem;
  boolean offBlock=off.getItem() instanceof BlockItem;
  if(!mainBlock&&!offBlock)return;
  if(settings.excludeAnchors && ((mainBlock&&(main.isOf(Items.RESPAWN_ANCHOR)||main.isOf(Items.GLOWSTONE)))
          ||(!mainBlock&&(off.isOf(Items.RESPAWN_ANCHOR)||off.isOf(Items.GLOWSTONE)))))return;
  ClientCooldownAccessor cooldown=(ClientCooldownAccessor)mc;
  if(cooldown.sakura$getItemUseCooldown()>settings.placeDelay)
   cooldown.sakura$setItemUseCooldown(settings.placeDelay);
 }
}
