package dev.sakuralive;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
 public static KeyBinding anchorKey,legitKey,safeKey,doubleKey,crystalKey,fastKey,menuKey,outlineKey;
 public static String lastStatus="Bereit";
 private static SakuraClient instance;
 private Run run;
 private long inputTick;
 private PendingRepeat pendingRepeat;
 private static final class PendingRepeat {Run source;boolean queued;long freeSince,deadline;}
 private WindowsSystemInput inputs;
 private String inputError="";
 private boolean fastHeld;
 private long nextFast;
 private Object world,player;
 private BlockPos crystalBase;
 private final CrystalCycle crystalCycle=new CrystalCycle();
 private int crystalSlot=-1;
 private boolean crystalHeld;
 private static final class Run {
  int mode,original,expected,pendingSlot=-1;
  long slotDeadline,started,lastUseTick=-1;
  BlockPos pos,cover;
  SakuraSettings.AnchorOptions options;
  AnchorPlan plan;
  KeyBinding key;
  final CameraMotion camera=new CameraMotion();
  Vec3d cameraTarget;
  AnchorPlan.Action cameraAction=AnchorPlan.Action.NONE;
  int previousRaw=-1;
  final RepeatGate repeats=new RepeatGate();
  final InteractionGate useGate=new InteractionGate();
  boolean repeating,batched,interactionRefused,aimDrift;
  net.minecraft.block.BlockState confirmedAnchor,confirmedCover;
  AnchorPlan.Action sentAction=AnchorPlan.Action.NONE;
  int sentSlot=-1;
 }
 @Override public void onInitializeClient(){
  instance=this;
  var category=KeyBinding.Category.create(Identifier.of("sakuralive","main"));
  anchorKey=bind("anchor",GLFW.GLFW_KEY_G,category);legitKey=bind("legit",GLFW.GLFW_KEY_UNKNOWN,category);
  safeKey=bind("safe",GLFW.GLFW_KEY_UNKNOWN,category);doubleKey=bind("double",GLFW.GLFW_KEY_UNKNOWN,category);
  crystalKey=bind("crystal",GLFW.GLFW_KEY_H,category);fastKey=bind("fastplace",GLFW.GLFW_KEY_J,category);
  outlineKey=bind("outline",GLFW.GLFW_KEY_UNKNOWN,category);
  menuKey=bind("menu",GLFW.GLFW_KEY_RIGHT_SHIFT,category);
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{if(run!=null)restoreCamera(mc,run);if(inputs!=null)inputs.close();});
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()&&Boolean.getBoolean("sakura.uiSmoke"))SakuraUiSmoke.install();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()&&Boolean.getBoolean("sakura.nativeSmoke"))SakuraNativeSmoke.install();
 }
 private KeyBinding bind(String id,int code,KeyBinding.Category category){
  return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive."+id,InputUtil.Type.KEYSYM,code,category));
 }
 public static int actionOwner(){return instance==null?-1:instance.run!=null?instance.run.mode:instance.pendingRepeat!=null?instance.pendingRepeat.source.mode:-1;}
 public static KeyBinding keyFor(int mode){
  return switch(mode){case 0->anchorKey;case 1->legitKey;case 2->safeKey;case 3->doubleKey;case 4->crystalKey;case 5->fastKey;case 6->outlineKey;default->menuKey;};
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
    &&mc.getOverlay()==null&&mc.mouse.isCursorLocked()&&mc.isWindowFocused()&&!mc.isPaused()&&mc.player.isAlive()&&!mc.player.isSpectator();
 }
 private void tick(MinecraftClient mc){
  inputTick++;
  while(Hotkeys.take(7)||menuKey.wasPressed())mc.setScreen(new SakuraScreen());
  while(Hotkeys.take(6)||outlineKey.wasPressed()){settings.outline.enabled=!settings.outline.enabled;settings.save();}
  if(!ready(mc)){cancel(mc,"Menü / Spiel pausiert");drain();return;}
  while(Hotkeys.take(4)||crystalKey.wasPressed()){toggle(4);settings.save();}
  while(Hotkeys.take(5)||fastKey.wasPressed()){toggle(5);settings.save();}
  for(int mode=0;mode<4;mode++)while(Hotkeys.take(mode)||keyFor(mode).wasPressed()){
   if(!enabled(mode))continue;
   // A fresh physical press supersedes the old target; never queue behind it.
   pendingRepeat=null;
   if(inputs!=null)inputs.releaseAll();
   if(run!=null){restoreCamera(mc,run);run=null;}
   start(mc,mode);
  }
 }
 private void drain(){Hotkeys.clear();for(int i=0;i<6;i++)while(keyFor(i).wasPressed()){}}
 private void start(MinecraftClient mc,int mode){
  if(!(mc.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK
     ||!mc.player.canInteractWithBlockAt(hit.getBlockPos(),0)){lastStatus="Kein erreichbarer Block anvisiert";return;}
  var state=mc.world.getBlockState(hit.getBlockPos());boolean existing=state.isOf(Blocks.RESPAWN_ANCHOR);
  var r=new Run();r.started=System.nanoTime();r.mode=mode;r.options=optionsFor(mode);r.key=keyFor(mode);
  r.original=r.expected=mc.player.getInventory().getSelectedSlot();
  r.pos=existing?hit.getBlockPos().toImmutable():PlacementGeometry.destination(mc.world,hit).toImmutable();
  r.cover=coverPosition(mc,r.pos);r.confirmedAnchor=mc.world.getBlockState(r.pos);r.confirmedCover=mc.world.getBlockState(r.cover);
  var o=r.options;
  r.plan=new AnchorPlan(existing,existing?state.get(RespawnAnchorBlock.CHARGES):0,mode==2,mode==3?2:1,
    new AnchorPlan.Timings(o.placeMs,o.chargeMs,o.coverMs,o.repeatMs,mode==2&&o.autoCamera?Math.max(o.timeoutMs,180000/o.cameraSpeed+1000):o.timeoutMs),System.nanoTime());
  run=r;world=mc.world;player=mc.player;clearCrystal();lastStatus="Anchor gestartet";
 }
 public static void frame(MinecraftClient mc){
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()&&Boolean.getBoolean("sakura.uiSmoke"))SakuraUiSmoke.frame(mc);
  if(instance==null)return;
  try{instance.advance(mc);}catch(RuntimeException|LinkageError e){
   if(instance.run!=null)restoreCamera(mc,instance.run);instance.run=null;instance.pendingRepeat=null;lastStatus="Systemeingabe angehalten: "+e.getMessage();
   if(instance.inputs!=null)try{instance.inputs.releaseAll();}catch(RuntimeException ignored){}
  }
 }
 private boolean ensureInputs(MinecraftClient mc){
  if(!inputError.isEmpty()){lastStatus=inputError;return false;}
  try{if(inputs==null)inputs=new WindowsSystemInput(mc,()->ready(mc));}
  catch(Exception|LinkageError e){inputError="Systemeingaben nicht verfügbar: "+e.getMessage();lastStatus=inputError;return false;}
  if(!inputs.available()){lastStatus=inputs.status();return false;}
  for(int i=0;i<8;i++)if(Hotkeys.eligible(i)&&!keyFor(i).isUnbound()&&(keyFor(i).equals(mc.options.useKey)||keyFor(i).equals(mc.options.attackKey))){lastStatus="Makro-Hotkey kollidiert mit Benutzen / Angriff";return false;}
  return inputs.foreground();
 }
 private KeyBinding slotBinding(MinecraftClient mc,int slot){
  var key=mc.options.hotbarKeys[slot];
  if(key.equals(mc.options.useKey)||key.equals(mc.options.attackKey))throw new IllegalArgumentException("Hotbar-Taste kollidiert mit Benutzen / Angriff");
  for(int i=0;i<8;i++)if(Hotkeys.eligible(i)&&!keyFor(i).isUnbound()&&key.equals(keyFor(i)))throw new IllegalArgumentException("Hotbar-Taste kollidiert mit einem Makro");
  return key;
 }
 private static int latency(MinecraftClient mc){var entry=mc.getNetworkHandler()==null?null:mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());return entry==null?0:entry.getLatency();}
 public static void confirmedBlock(net.minecraft.util.math.BlockPos pos,net.minecraft.block.BlockState state){if(instance==null||instance.run==null)return;var r=instance.run;if(pos.equals(r.pos))r.confirmedAnchor=state;if(pos.equals(r.cover))r.confirmedCover=state;}
 private boolean slotInput(MinecraftClient mc,int slot,long now){return inputs.tap(slotBinding(mc,slot),now,settings.systemPulseMs);}
 private void advance(MinecraftClient mc){
  long now=System.nanoTime();
  if(inputs!=null)inputs.tick(now,ready(mc));
  if(!ready(mc)){cancel(mc,"Unterbrochen");clearCrystal();fastHeld=false;return;}
  if(!ensureInputs(mc))return;
  try{advanceInputs(mc,now);}
  catch(RuntimeException e){cancel(mc,"Eingabe abgebrochen: "+e.getMessage());clearCrystal();inputs.releaseAll();lastStatus="Eingabe abgebrochen: "+e.getMessage();}
 }
 private void advanceInputs(MinecraftClient mc,long now){
  if(world!=mc.world||player!=mc.player){
   inputs.releaseAll();pendingRepeat=null;
   if(run!=null){restoreCamera(mc,run);run=null;lastStatus="Welt / Spieler gewechselt";}
   clearCrystal();world=mc.world;player=mc.player;
  }
  if(pendingRepeat!=null){
   fastHeld=false;advanceRepeat(mc,now);
   if(run!=null)advanceAnchor(mc,now);
   return;
  }
  if(run!=null){fastHeld=false;advanceAnchor(mc,now);return;}
  if(crystalActive(mc)){fastHeld=false;crystal(mc,now);}
  else {clearCrystal();fastPlace(mc,now);}
 }
 private void advanceAnchor(MinecraftClient mc,long now){
  var r=run;var o=r.options;
  int selected=mc.player.getInventory().getSelectedSlot();
  if(!enabled(r.mode)||(o.holdToRun&&!inputs.physical(r.key))){cancel(mc,"Losgelassen / deaktiviert");return;}
  if(r.pendingSlot>=0){
   if(selected==r.pendingSlot){r.expected=selected;r.pendingSlot=-1;if(!r.batched)r.plan.defer(now,o.switchMs);r.batched=false;if(o.switchMs>0)return;}
   else {if(selected!=r.expected||now-r.slotDeadline>=0)cancel(mc,"Slotwechsel nicht bestätigt");else if(automaticAim(r)&&r.cameraTarget!=null)moveCamera(mc,r,now);return;}
  }
  if(selected!=r.expected){cancel(mc,"Slot manuell geändert");return;}
  if(!mc.player.canInteractWithBlockAt(r.pos,0)){cancel(mc,"Anchor außer Reichweite");return;}
  var state=r.confirmedAnchor==null?mc.world.getBlockState(r.pos):r.confirmedAnchor;boolean present=state.isOf(Blocks.RESPAWN_ANCHOR);
  var seen=new AnchorPlan.Seen(present,present?state.get(RespawnAnchorBlock.CHARGES):0,
    PlacementGeometry.replaceable(state),(r.confirmedCover==null?mc.world.getBlockState(r.cover):r.confirmedCover).isOf(Blocks.GLOWSTONE));
  if(r.aimDrift){r.aimDrift=false;if(!r.useGate.accepted()){r.plan.reacquire(r.sentAction);r.lastUseTick=-1;r.cameraTarget=null;r.camera.reset();}}
  var action=r.plan.poll(seen,now);
  if(r.interactionRefused){r.interactionRefused=false;if(r.plan.waitingFor(r.sentAction)){cancel(mc,"Minecraft hat den Klick abgelehnt · Ziel/Slot prüfen, erneut drücken");return;}}
  if(action==AnchorPlan.Action.NONE&&!r.useGate.accepted()&&r.lastUseTick>=0&&inputTick-r.lastUseTick>=2)action=r.plan.retryAfterProcessing(seen,now);
  if(r.plan.done()){
   if(r.plan.stage()==AnchorPlan.Stage.FINISHED){lastStatus="Anchor abgeschlossen · "+((now-r.started)/1_000_000)+" ms";restoreCamera(mc,r);run=null;queueRepeat(mc,r,now);}
   else cancel(mc,r.plan.reason());
   return;
  }
  lastStatus=(r.mode==3?"Double "+(r.plan.completed()+1)+"/2 · ":"")+r.plan.stage().name();
  if(action==AnchorPlan.Action.NONE)return;
  // Fire is replaceable for block placement but can steal the following charge ray.
  if(mc.crosshairTarget instanceof BlockHitResult fire && (mc.world.getBlockState(fire.getBlockPos()).isOf(Blocks.FIRE)||mc.world.getBlockState(fire.getBlockPos()).isOf(Blocks.SOUL_FIRE))){
   if(mc.player.canInteractWithBlockAt(fire.getBlockPos(),0)&&fire.getBlockPos().getSquaredDistance(r.pos)<=9){inputs.tap(mc.options.attackKey,now,settings.systemPulseMs);lastStatus="Störendes Feuer entfernen";return;}
  }
  if(mc.interactionManager.isBreakingBlock()){lastStatus="Feuer-Klick abschließen";return;}
  // Native clicks can act only on the actual vanilla crosshair target.
  boolean aimed=switch(action){
   case PLACE->aimsAtPlacement(mc,r.pos);
   case COVER->aimsAtPlacement(mc,r.cover)
     &&mc.crosshairTarget instanceof BlockHitResult h&&!mc.world.getBlockState(h.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
   default->aimsAtAnchor(mc,r.pos);
  };

  if(action==AnchorPlan.Action.COVER&&(!r.cover.equals(coverPosition(mc,r.pos))
     ||!PlacementGeometry.replaceable(mc.world.getBlockState(r.cover))
     ||!mc.world.getOtherEntities(null,new Box(r.cover)).isEmpty())){
   cancel(mc,"Deckungsposition nicht frei");return;
  }
  boolean firstDoubleBlast=r.mode==3&&r.plan.completed()==0;
  int slot=switch(action){case PLACE->o.anchorSlot-1;case CHARGE,COVER->o.glowSlot-1;default->firstDoubleBlast?o.anchorSlot-1:o.finalSlot-1;};
  final var desired=action;
  if(o.autoSlots)slot=HotbarChoice.choose(slot,i->{var item=mc.player.getInventory().getStack(i);return switch(desired){case PLACE->item.isOf(Items.RESPAWN_ANCHOR);case CHARGE,COVER->item.isOf(Items.GLOWSTONE);default->firstDoubleBlast?item.isOf(Items.RESPAWN_ANCHOR):!item.isOf(Items.GLOWSTONE);};});
  if(slot<0){cancel(mc,action==AnchorPlan.Action.PLACE?"Kein Anchor in der Hotbar":action==AnchorPlan.Action.CHARGE||action==AnchorPlan.Action.COVER?"Kein Glowstone in der Hotbar":"Kein passender Auslöse-Slot");return;}
  var stack=mc.player.getInventory().getStack(slot);
  if(action==AnchorPlan.Action.PLACE&&!stack.isOf(Items.RESPAWN_ANCHOR)
    ||(action==AnchorPlan.Action.CHARGE||action==AnchorPlan.Action.COVER)&&!stack.isOf(Items.GLOWSTONE)){
   cancel(mc,"Benötigtes Item im konfigurierten Slot fehlt");return;
  }
  if(action==AnchorPlan.Action.DETONATE){
   // Vanilla accepts a held Anchor for the default block action when not sneaking; offhand stays untouched.
   if(stack.isOf(Items.GLOWSTONE)||mc.player.getOffHandStack().isOf(Items.GLOWSTONE)){
    cancel(mc,"Glowstone verhindert zuverlässiges Auslösen");return;
   }
   if(r.mode==2&&(!r.cover.equals(coverPosition(mc,r.pos))||!mc.world.getBlockState(r.cover).isOf(Blocks.GLOWSTONE))){
    cancel(mc,"Glowstone-Deckung fehlt auf deiner Seite");return;
   }
   if(mc.player.isSneaking()&&stack.getItem() instanceof BlockItem){cancel(mc,"Schleichen mit Block im finalen Slot");return;}
  }
  if(action==AnchorPlan.Action.DETONATE&&r.plan.stage()==AnchorPlan.Stage.WAIT_REMOVED&&stack.getItem() instanceof BlockItem){lastStatus="Warte auf Explosion; kein zusätzlicher Block-Klick";return;}
  if(r.expected!=slot){
   if(o.bundledInput&&o.switchMs==0&&latency(mc)<50){
    if(!aimed){if(automaticAim(r))aimSafe(mc,r,action,now);return;}
    if(mc.player.isUsingItem()){inputs.mute(mc.options.useKey);return;}
    if(inputs.slotAndUse(slotBinding(mc,slot),mc.options.useKey,now,settings.systemPulseMs)){
     r.pendingSlot=slot;r.batched=true;r.slotDeadline=now+AnchorPlan.ms(Math.max(750,o.timeoutMs));r.lastUseTick=inputTick;r.sentAction=action;r.sentSlot=slot;r.useGate.arm();r.interactionRefused=false;r.cameraTarget=null;r.camera.reset();r.plan.confirm(action,true,now);
    }
    return;
   }
   if(slotInput(mc,slot,now)){r.pendingSlot=slot;r.slotDeadline=now+AnchorPlan.ms(Math.max(750,o.timeoutMs));}
   if(!aimed&&automaticAim(r))aimSafe(mc,r,action,now);
   return;
  }
  if(!aimed){
   if(automaticAim(r))aimSafe(mc,r,action,now);
   else lastStatus=action==AnchorPlan.Action.COVER?"Deckungsfläche selbst anvisieren":"Feste Anchor-Position selbst anvisieren";
   return;
  }
  r.cameraTarget=null;r.camera.reset();
  if(mc.player.isUsingItem()){inputs.mute(mc.options.useKey);lastStatus="Benutzen-Taste freigeben, dann Anchor fortsetzen";return;}
  if(inputs.tap(mc.options.useKey,now,settings.systemPulseMs)){
   r.lastUseTick=inputTick;r.sentAction=action;r.sentSlot=slot;r.useGate.arm();r.interactionRefused=false;r.cameraTarget=null;r.camera.reset();r.plan.confirm(action,true,now);
  }

 }
 public static boolean allowBlockInteraction(net.minecraft.client.network.ClientPlayerEntity player,Hand hand,BlockHitResult hit){
  if(instance==null||instance.run==null)return true;
  var mc=MinecraftClient.getInstance();var r=instance.run;
  if(mc.world==null||mc.world!=instance.world||player!=mc.player||mc.currentScreen!=null)return false;
  var target=r.sentAction==AnchorPlan.Action.COVER?r.cover:r.pos;
  var state=mc.world.getBlockState(target);boolean anchor=state.isOf(Blocks.RESPAWN_ANCHOR);
  boolean complete=switch(r.sentAction){case PLACE->anchor;case CHARGE->anchor&&state.get(RespawnAnchorBlock.CHARGES)>0;case COVER->state.isOf(Blocks.GLOWSTONE);case DETONATE->!anchor;default->false;};
  boolean aimed=switch(r.sentAction){case PLACE->PlacementGeometry.replaceable(state)&&PlacementGeometry.destination(mc.world,hit).equals(target);case COVER->PlacementGeometry.replaceable(state)&&!mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)&&PlacementGeometry.destination(mc.world,hit).equals(target);case CHARGE,DETONATE->anchor&&hit.getBlockPos().equals(target);default->false;};
  var stack=player.getMainHandStack();boolean item=switch(r.sentAction){case PLACE->stack.isOf(Items.RESPAWN_ANCHOR);case CHARGE,COVER->stack.isOf(Items.GLOWSTONE);case DETONATE->!stack.isOf(Items.GLOWSTONE)&&!player.getOffHandStack().isOf(Items.GLOWSTONE);default->false;};
  var decision=r.useGate.evaluate(r.plan.waitingFor(r.sentAction),complete,hand==Hand.MAIN_HAND,player.getInventory().getSelectedSlot()==r.sentSlot,aimed,item);
  if(decision==InteractionGate.Decision.REJECT){
   if(hand==Hand.MAIN_HAND&&!aimed&&item&&player.getInventory().getSelectedSlot()==r.sentSlot)r.aimDrift=true;
   else if(hand==Hand.MAIN_HAND)r.interactionRefused=true;
  }
  return decision==InteractionGate.Decision.ALLOW;
 }
 public static void observeInteraction(net.minecraft.client.network.ClientPlayerEntity player,Hand hand,BlockHitResult hit,ActionResult result){
  if(instance==null||instance.run==null||hand!=Hand.MAIN_HAND)return;
  var r=instance.run;if(r.sentAction==AnchorPlan.Action.NONE||player.getInventory().getSelectedSlot()!=r.sentSlot)return;
  var target=r.sentAction==AnchorPlan.Action.COVER?r.cover:r.pos;
  boolean placement=r.sentAction==AnchorPlan.Action.PLACE||r.sentAction==AnchorPlan.Action.COVER;
  boolean matches=hit.getBlockPos().equals(target)||placement&&hit.getBlockPos().offset(hit.getSide()).equals(target);
  if(!matches||result==null||r.useGate.accepted()||r.aimDrift)return;
  r.useGate.result(result.isAccepted());if(!result.isAccepted())r.interactionRefused=true;
 }
 private static boolean automaticAim(Run r){return r.options.autoCamera&&(r.mode==2||r.mode==3&&r.plan.completed()>0);}
 private void queueRepeat(MinecraftClient mc,Run source,long now){
  if(source.mode!=0&&source.mode!=2)return;
  boolean queued=source.repeats.take(),held=source.options.repeatWhileHeld&&inputs.physical(source.key);
  if(!queued&&!held)return;
  var next=new PendingRepeat();next.source=source;next.queued=queued;next.freeSince=now;
  next.deadline=now+AnchorPlan.ms(Math.max(source.options.timeoutMs,source.options.repeatMs+500));pendingRepeat=next;
 }
 private void advanceRepeat(MinecraftClient mc,long now){
  var next=pendingRepeat;var previous=next.source;var o=previous.options;
  if(!enabled(previous.mode)||!next.queued&&(!o.repeatWhileHeld||!inputs.physical(previous.key))){pendingRepeat=null;return;}
  if(mc.player.getInventory().getSelectedSlot()!=previous.expected){cancel(mc,"Spam gestoppt: Slot manuell geändert");return;}
  if(!mc.player.canInteractWithBlockAt(previous.pos,0)||now-next.deadline>=0){cancel(mc,"Spam-Ziel nicht mehr erreichbar / bestätigt");return;}
  var state=mc.world.getBlockState(previous.pos);
  if(state.isOf(Blocks.RESPAWN_ANCHOR)){next.freeSince=now;return;}
  if(!PlacementGeometry.replaceable(state)){cancel(mc,"Spam-Ziel durch anderen Block belegt");return;}
  if(now-next.freeSince<AnchorPlan.ms(o.repeatMs))return;
  if(!mc.player.getInventory().getStack(o.anchorSlot-1).isOf(Items.RESPAWN_ANCHOR)||!mc.player.getInventory().getStack(o.glowSlot-1).isOf(Items.GLOWSTONE)){cancel(mc,"Spam gestoppt: Anchor / Glowstone fehlt");return;}
  var r=new Run();r.mode=previous.mode;r.options=o;r.key=previous.key;r.pos=previous.pos;r.cover=previous.cover;
  r.confirmedAnchor=mc.world.getBlockState(r.pos);r.confirmedCover=mc.world.getBlockState(r.cover);r.started=now;r.original=r.expected=mc.player.getInventory().getSelectedSlot();r.repeating=true;
  r.plan=new AnchorPlan(false,0,r.mode==2,1,new AnchorPlan.Timings(o.placeMs,o.chargeMs,o.coverMs,o.repeatMs,r.mode==2&&o.autoCamera?Math.max(o.timeoutMs,180000/o.cameraSpeed+1000):o.timeoutMs),now);
  pendingRepeat=null;run=r;clearCrystal();lastStatus="Anchor-Wiederholung gestartet";
 }
 private void aimSafe(MinecraftClient mc,Run r,AnchorPlan.Action action,long now){
  if(action==AnchorPlan.Action.PLACE&&r.mode!=2&&!r.repeating&&!(r.mode==3&&r.plan.completed()>0))return;
  if(!mc.mouse.isCursorLocked()){cancel(mc,"Safe-Kamera benötigt gefangenen Mauszeiger");return;}
  if(r.cameraTarget==null||r.cameraAction!=action){
   r.cameraAction=action;r.camera.reset();
   if(action==AnchorPlan.Action.COVER||action==AnchorPlan.Action.PLACE){
    BlockPos target=action==AnchorPlan.Action.COVER?r.cover:r.pos;
    BlockHitResult best=null;double distance=Double.MAX_VALUE;
    for(Direction face:Direction.values()){
     var hit=PlacementGeometry.supportHit(mc.world,mc.player.getEyePos(),target,face,ShapeContext.of(mc.player));
     if(hit!=null&&mc.player.canInteractWithBlockAt(hit.getBlockPos(),0)){
      double d=mc.player.getEyePos().squaredDistanceTo(hit.getPos());if(d<distance){distance=d;best=hit;}
     }
    }
    if(best==null){lastStatus="Warte auf sichtbare Stützfläche am festen Ziel";return;}
    r.cameraTarget=best.getPos();
   }else{
    var point=PlacementGeometry.visibleAnchor(mc.world,mc.player.getEyePos(),r.pos,ShapeContext.of(mc.player));
    if(point==null){lastStatus="Warte auf freie Sicht zum Anchor";return;}
    r.cameraTarget=point;
   }
  }
  moveCamera(mc,r,now);
 }
 private void moveCamera(MinecraftClient mc,Run r,long now){
  if(r.cameraTarget==null||run!=r)return;
  long window=mc.getWindow().getHandle();
  if(org.lwjgl.glfw.GLFW.glfwRawMouseMotionSupported()&&r.previousRaw<0){r.previousRaw=GLFW.glfwGetInputMode(window,GLFW.GLFW_RAW_MOUSE_MOTION);GLFW.glfwSetInputMode(window,GLFW.GLFW_RAW_MOUSE_MOTION,GLFW.GLFW_FALSE);}
  var delta=r.cameraTarget.subtract(mc.player.getEyePos());
  double yaw=Math.toDegrees(Math.atan2(delta.z,delta.x))-90,pitch=-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
  var step=r.camera.step(mc.player.getYaw(),mc.player.getPitch(),yaw,pitch,mc.options.getMouseSensitivity().getValue(),mc.options.getInvertMouseX().getValue(),mc.options.getInvertMouseY().getValue(),r.options.cameraSpeed,now);
  if(step.stalled()){cancel(mc,"Keine Kamerareaktion auf simulierte Mausbewegung");return;}
  inputs.move(step.dx(),step.dy());lastStatus=r.cameraAction==AnchorPlan.Action.COVER?"Safe: Kamera zur Glowstone-Deckung":"Safe: Kamera zurück zum Anchor";
 }
 private static void restoreCamera(MinecraftClient mc,Run r){if(r.previousRaw>=0){if(mc.mouse.isCursorLocked())GLFW.glfwSetInputMode(mc.getWindow().getHandle(),GLFW.GLFW_RAW_MOUSE_MOTION,r.previousRaw);r.previousRaw=-1;}r.cameraTarget=null;r.camera.reset();}
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
  return PlacementGeometry.destination(mc.world,h).equals(pos);
 }
 private void cancel(MinecraftClient mc,String why){
  pendingRepeat=null;
  if(inputs!=null)inputs.releaseAll();
  if(run!=null){
   restoreCamera(mc,run);
   if(inputs!=null&&inputs.available()&&ready(mc)&&inputs.foreground()&&mc.player==player&&mc.world==world
     &&mc.player.getInventory().getSelectedSlot()==run.expected&&run.original!=run.expected){
    try{slotInput(mc,run.original,System.nanoTime());}catch(RuntimeException ignored){}
   }
   run=null;lastStatus=why;
  }
 }
 public static boolean crystalActive(MinecraftClient mc){
  return settings.crystal&&ready(mc)&&instance!=null&&instance.run==null&&instance.inputs!=null
    &&instance.inputs.available()&&mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)&&instance.inputs.physicalRight();
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
  if(!crystalHeld||crystalSlot!=slot){clearCrystal();crystalHeld=true;crystalSlot=slot;inputs.mute(mc.options.useKey);}
  // A shield/item-use state started before assist activation must not lock startup.
  if(mc.player.isUsingItem()){lastStatus="Warte auf Loslassen der Benutzen-Taste";return;}
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
   if(!(mc.crosshairTarget instanceof EntityHitResult aimedHit)||aimedHit.getEntity().getId()!=target.getId()){
    lastStatus="Crystal zum Abbauen selbst anvisieren";return;
   }
   if(inputs.tap(mc.options.attackKey,now,settings.systemPulseMs)){
    crystalCycle.attacked(target.getId(),now,timing);crystalBase=null;
   }
   return;
  }
  if(action==CrystalCycle.Action.PLACE&&baseHit!=null){
   if(!(mc.crosshairTarget instanceof BlockHitResult actual)||actual.getType()!=HitResult.Type.BLOCK||!actual.getBlockPos().equals(aimedBase))return;
   if(inputs.tap(mc.options.useKey,now,settings.systemPulseMs)){
    crystalCycle.placed(true,now,timing);crystalBase=aimedBase.toImmutable();
   }
  }
 }
 private void fastPlace(MinecraftClient mc,long now){
  if(!settings.fastPlace||!inputs.physical(mc.options.useKey)){fastHeld=false;nextFast=0;return;}
  var main=mc.player.getMainHandStack();var off=mc.player.getOffHandStack();
  boolean mb=main.getItem() instanceof BlockItem,ob=off.getItem() instanceof BlockItem;
  if(!mb&&!ob){fastHeld=false;return;}
  var stack=mb?main:off;
  if(settings.excludeAnchors&&(stack.isOf(Items.RESPAWN_ANCHOR)||stack.isOf(Items.GLOWSTONE))){fastHeld=false;return;}
  if(!fastHeld){fastHeld=true;inputs.mute(mc.options.useKey);nextFast=now;}
  if(now-nextFast>=0&&inputs.tap(mc.options.useKey,now,settings.systemPulseMs))
   nextFast=now+AnchorPlan.ms(Math.max(settings.placeDelayMs,settings.systemPulseMs));
 }
}

