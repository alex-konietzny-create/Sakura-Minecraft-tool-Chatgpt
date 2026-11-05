package dev.sakuralive;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.*;
import org.lwjgl.glfw.GLFW;
import java.io.File;
/** Development-only rendering and input regression; no world or gameplay. */
final class SakuraUiSmoke {
 private static final String[] NAMES={"anchor-grid","crystal-grid","utility-grid","themes-grid","anchor-grid-blue","anchor-details"};
 private static final int[] PAGES={1,0,2,3,1,1};
 private static int stage=-1,wait=60;
 private static boolean tested;
 private static int animationFrame=-1;
 private static WindowsSystemInput probe;
 private static int probeWait;
 private static void check(boolean yes,String why){if(!yes)throw new IllegalStateException("Grid UI: "+why);}
 private static Click pointer(SakuraScreen s,double x,double y,int b){
  double scale=Math.max(.01,Math.min(1.2,Math.min((s.width-20.0)/920,(s.height-20.0)/600)));
  return new Click((s.width-scale*920)/2+x*scale,(s.height-scale*600)/2+y*scale,new MouseInput(b,0));
 }
 private static void click(SakuraScreen s,double x,double y){var p=pointer(s,x,y,0);check(s.mouseClicked(p,false),"click "+x+","+y);s.mouseReleased(p);}
 private static void verifyFirePlacement(){
  final var target=new net.minecraft.util.math.BlockPos(0,1,0);
  final var support=target.down();
  for(var fire:new net.minecraft.block.Block[]{net.minecraft.block.Blocks.FIRE,net.minecraft.block.Blocks.SOUL_FIRE}){
   for(boolean wall:new boolean[]{false,true}){
    net.minecraft.world.BlockView view=new net.minecraft.world.BlockView(){
     public net.minecraft.block.entity.BlockEntity getBlockEntity(net.minecraft.util.math.BlockPos p){return null;}
     public net.minecraft.block.BlockState getBlockState(net.minecraft.util.math.BlockPos p){
      if(p.equals(support)||wall&&p.equals(new net.minecraft.util.math.BlockPos(0,2,1)))return net.minecraft.block.Blocks.OBSIDIAN.getDefaultState();
      return p.equals(target)?fire.getDefaultState():net.minecraft.block.Blocks.AIR.getDefaultState();
     }
     public net.minecraft.fluid.FluidState getFluidState(net.minecraft.util.math.BlockPos p){return net.minecraft.fluid.Fluids.EMPTY.getDefaultState();}
     public int getHeight(){return 384;}
     public int getBottomY(){return -64;}
    };
    var hit=PlacementGeometry.supportHit(view,new net.minecraft.util.math.Vec3d(.5,3,2.5),target,
      net.minecraft.util.math.Direction.UP,net.minecraft.block.ShapeContext.absent());
    check(wall?hit==null:hit!=null&&hit.getBlockPos().equals(support),"fire support ray / solid obstruction: "+fire);
    var click=new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(target),net.minecraft.util.math.Direction.NORTH,target,false);
    check(PlacementGeometry.destination(view,click).equals(target),"fire keeps intended placement cell");
   }
  }
  System.out.println("[Sakura UI smoke] PASS: normal fire and soul fire placement rays, exact target cell, solid obstruction");
 }


 private static void verify(MinecraftClient mc){
  verifyFirePlacement();
  var s=SakuraClient.settings;boolean motion=s.reduceMotion,anchor=s.anchor,blue=s.blueAccent;
  int delay=s.anchorOptions.placeMs,slot=s.anchorOptions.anchorSlot;
  s.reduceMotion=true;
  for(int[] size:new int[][]{{320,210},{460,300},{640,420}}){
   var screen=new SakuraScreen(1);mc.setScreen(screen);screen.resize(size[0],size[1]);
   click(screen,517,145);check(s.anchor!=anchor,"power toggle");click(screen,517,145);check(s.anchor==anchor,"power restore");
   click(screen,455,230);check(screen.getFocused() instanceof TextFieldWidget,"delay field focus");
   ((TextFieldWidget)screen.getFocused()).setText("137");screen.keyPressed(new KeyInput(GLFW.GLFW_KEY_ENTER,0,0));
   check(s.anchorOptions.placeMs==137&&SakuraSettings.load().anchorOptions.placeMs==137,"millisecond persistence");
   click(screen,455,260);check(screen.getFocused() instanceof TextFieldWidget,"slot field focus");
   ((TextFieldWidget)screen.getFocused()).setText("8");screen.keyPressed(new KeyInput(GLFW.GLFW_KEY_ENTER,0,0));
   check(s.anchorOptions.anchorSlot==8,"slot editing");
  }
  var key=SakuraClient.anchorKey;var original=InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
  var screen=new SakuraScreen(1);mc.setScreen(screen);click(screen,475,203);
  var input=new KeyInput(GLFW.GLFW_KEY_F8,0,0);screen.keyPressed(input);check(key.matchesKey(input),"key capture");
  click(screen,475,203);screen.keyPressed(new KeyInput(GLFW.GLFW_KEY_ESCAPE,0,0));check(key.matchesKey(input),"capture cancellation");
  click(screen,475,203);var mouse=pointer(screen,0,0,3);screen.mouseClicked(mouse,false);check(key.matchesMouse(mouse),"mouse capture");
  key.setBoundKey(original);KeyBinding.updateKeysByCode();mc.options.write();
  screen=new SakuraScreen(1);mc.setScreen(screen);click(screen,505,171);
  check(mc.currentScreen instanceof SakuraDetailsScreen,"advanced settings");mc.currentScreen.close();
  check(mc.currentScreen instanceof SakuraScreen,"details back navigation");
  screen=new SakuraScreen(3);mc.setScreen(screen);click(screen,440,175);check(s.blueAccent,"theme switch");
  s.reduceMotion=motion;s.blueAccent=blue;s.anchorOptions.placeMs=delay;s.anchorOptions.anchorSlot=slot;s.save();
  System.out.println("[Sakura UI smoke] PASS: card power toggles, direct ms/slot fields, persistence, keyboard/mouse binding, advanced navigation and theme; three UI sizes");
 }
 static void install(){
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(mc.getOverlay()!=null||wait-->0)return;
   if(stage==-1){for(int i=0;i<6;i++)if(!SakuraClient.enabled(i))SakuraClient.toggle(i);mc.setScreen(new SakuraScreen(1));stage=0;wait=40;return;}
   if(!tested&&com.sun.jna.Platform.isWindows()){
    try{
     if(probe==null)probe=new WindowsSystemInput(mc,()->false);
     if(!probe.available()){check(++probeWait<100,"native hooks initialization: "+probe.status());return;}
     var right=WindowsSystemInput.event(new InputPulse.Code(true,4096,2),true);
     check(right.type.intValue()==0&&right.input.mi.dwFlags.intValue()==8,"native right-down structure");
     var key=WindowsSystemInput.event(new InputPulse.Code(false,49,49),false);
     check(key.type.intValue()==1&&key.input.ki.wVk.intValue()==49&&key.input.ki.dwFlags.intValue()==2,"native key-up structure");
     probe.tick(System.nanoTime(),false);check(!probe.tap(mc.options.useKey,System.nanoTime(),30),"hidden Minecraft refuses clicks");
     probe.close();probe=null;
     System.out.println("[Sakura UI smoke] PASS: native Windows backend starts under Minecraft headless AWT; hooks and SendInput structures; hidden-window clicks refused. No real key-down injected.");
    }catch(Exception e){throw new IllegalStateException("Native input probe",e);}
   }
   if(!tested){verify(mc);tested=true;mc.setScreen(new SakuraScreen(1));wait=40;return;}
   if(stage<NAMES.length){
    ScreenshotRecorder.saveScreenshot(mc.runDirectory,"sakura-"+NAMES[stage]+".png",mc.getFramebuffer(),1,t->System.out.println("[Sakura UI smoke] "+t.getString()));
    stage++;if(stage<NAMES.length){SakuraClient.settings.blueAccent=stage==4;mc.setScreen(stage==5?new SakuraDetailsScreen(0):new SakuraScreen(PAGES[stage]));}wait=40;return;
   }
   for(var name:NAMES){var file=new File(mc.runDirectory,"screenshots/sakura-"+name+".png");check(file.isFile()&&file.length()>0,"screenshot "+name);}
   if(animationFrame==-1)System.out.println("[Sakura UI smoke] Grid pages rendered successfully. No gameplay test.");
   if(animationFrame==-1){SakuraClient.settings.reduceMotion=false;SakuraClient.settings.blueAccent=false;mc.setScreen(new SakuraScreen(1));animationFrame=0;wait=0;return;}
   if(animationFrame<18){
    if(animationFrame==3||animationFrame==10)SakuraClient.toggle(0);
    ScreenshotRecorder.saveScreenshot(mc.runDirectory,String.format("sakura-motion-%02d.png",animationFrame),mc.getFramebuffer(),1,t->{});
    animationFrame++;wait=0;return;
   }
   System.out.println("[Sakura UI smoke] PASS: captured 18 live animation frames.");
   mc.scheduleStop();wait=Integer.MAX_VALUE;
  });
 }
}

