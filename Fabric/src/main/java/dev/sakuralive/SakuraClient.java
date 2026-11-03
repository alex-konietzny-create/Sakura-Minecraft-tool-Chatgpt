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
 private static KeyBinding anchorKey, crystalKey, menuKey;
 private static int anchorPhase, waitTicks, crystalWait;
 private static BlockHitResult anchorTarget;

 @Override public void onInitializeClient() {
  var group=KeyBinding.Category.create(Identifier.of("sakuralive","main"));
  anchorKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.anchor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,group));
  crystalKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.crystal",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_H,group));
  menuKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,group));
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
 }
 private void tick(MinecraftClient mc) {
  while(menuKey.wasPressed())mc.setScreen(new SakuraScreen());
  if(mc.player==null||mc.world==null||mc.interactionManager==null){anchorPhase=0;return;}
  if(mc.currentScreen!=null){anchorPhase=0;return;}
  if(waitTicks>0)waitTicks--;
  if(crystalWait>0)crystalWait--;
  fastPlace(mc);
  while(anchorKey.wasPressed()){
   if(settings.anchor && anchorPhase==0 && mc.crosshairTarget instanceof BlockHitResult hit) {
    anchorPhase=1;anchorTarget=hit;waitTicks=0;
   }
  }
  if(anchorPhase!=0&&waitTicks==0)performAnchor(mc);
  while(crystalKey.wasPressed()){
   if(settings.crystal && crystalWait==0 && mc.crosshairTarget instanceof BlockHitResult hit) {
    var inventory=mc.player.getInventory();
    int slot=settings.crystalSlot-1;
    if(inventory.getStack(slot).isOf(Items.END_CRYSTAL)){
     inventory.setSelectedSlot(slot);
     mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,hit);
     mc.player.swingHand(Hand.MAIN_HAND);
     crystalWait=settings.actionDelay;
    }
   }
  }
 }
 private void performAnchor(MinecraftClient mc){
  // Phase 1 place anchor; phase 2 glowstone; phase 3 detonate with non-glowstone item.
  var inventory=mc.player.getInventory();
  int slot=anchorPhase==1?settings.anchorSlot-1:
           anchorPhase==2?settings.glowSlot-1:settings.totemSlot-1;
  if(slot<0||slot>8||anchorTarget==null){anchorPhase=0;return;}
  if(anchorPhase==1 && !inventory.getStack(slot).isOf(Items.RESPAWN_ANCHOR)
    || anchorPhase==2 && !inventory.getStack(slot).isOf(Items.GLOWSTONE)
    || anchorPhase==3 && (inventory.getStack(slot).isOf(Items.GLOWSTONE)
                          || inventory.getStack(slot).isOf(Items.RESPAWN_ANCHOR))){
   anchorPhase=0;return;
  }
  BlockHitResult target=anchorTarget;
  if(anchorPhase>=2){
   // The position clicked in phase 1 is an adjacent support block.
   // Reacquire the new anchor from the crosshair instead of reusing that click.
   if(!(mc.crosshairTarget instanceof BlockHitResult current)
     || !mc.world.getBlockState(current.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)){
    anchorPhase=0;return;
   }
   target=current;
  }
  inventory.setSelectedSlot(slot);
  mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,target);
  mc.player.swingHand(Hand.MAIN_HAND);
  if(anchorPhase==3){anchorPhase=0;inventory.setSelectedSlot(settings.totemSlot-1);}
  else {anchorPhase++;waitTicks=settings.actionDelay;}
 }
 private void fastPlace(MinecraftClient mc){
  if(!settings.fastPlace||!mc.options.useKey.isPressed())return;
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
