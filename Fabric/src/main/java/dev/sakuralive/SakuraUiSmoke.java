package dev.sakuralive;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.ScreenshotRecorder;
import java.io.File;

/** Opt-in development-only rendering check; never enabled in a production instance. */
final class SakuraUiSmoke {
 private static int stage=-1,wait=60;
 private static boolean tested;
 private static void check(boolean value,String message){if(!value)throw new IllegalStateException("UI assertion: "+message);}
 private static net.minecraft.client.gui.Click pointer(SakuraScreen screen,double x,double y,int button){
  double scale=Math.max(0.01,Math.min(1.15,Math.min((screen.width-20.0)/760,(screen.height-20.0)/480)));
  return new net.minecraft.client.gui.Click((screen.width-scale*760)/2+x*scale,
    (screen.height-scale*480)/2+y*scale,new net.minecraft.client.input.MouseInput(button,0));
 }
 private static void click(SakuraScreen screen,double x,double y){
  var p=pointer(screen,x,y,0);check(screen.mouseClicked(p,false),"click at "+x+","+y);screen.mouseReleased(p);
 }
 private static void verifyInputs(SakuraScreen screen){
  var s=SakuraClient.settings;
  int w=screen.width,h=screen.height,oldDelay=s.actionDelay,oldSlot=s.anchorSlot;
  boolean motion=s.reduceMotion,anchor=s.anchor;
  s.reduceMotion=true;
  for(int[] size:new int[][]{{320,200},{432,270},{w,h}}){
   screen.resize(size[0],size[1]);
   click(screen,220,125);check(s.anchor!=anchor,"scaled switch");
   click(screen,220,125);check(s.anchor==anchor,"switch restore");
   click(screen,426,258);check(s.anchorSlot==9,"slot grid");
   var from=pointer(screen,716,251,0);
   var to=pointer(screen,490,251,0);
   check(screen.mouseClicked(from,false),"slider click");
   check(s.actionDelay==20,"slider maximum");
   screen.mouseDragged(to,to.x()-from.x(),0);
   check(s.actionDelay==1,"slider drag minimum");screen.mouseReleased(to);
   screen.keyPressed(new net.minecraft.client.input.KeyInput(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT,0,0));
   check(s.actionDelay==2,"slider keyboard step");
   check(SakuraSettings.load().actionDelay==2,"slider persisted");
  }
  var key=SakuraClient.anchorKey;
  var old=net.minecraft.client.util.InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
  click(screen,500,350);
  var input=new net.minecraft.client.input.KeyInput(org.lwjgl.glfw.GLFW.GLFW_KEY_F8,0,0);
  check(screen.keyPressed(input)&&key.matchesKey(input),"inline keyboard binding");
  click(screen,500,350);
  screen.keyPressed(new net.minecraft.client.input.KeyInput(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0));
  check(key.matchesKey(input),"Escape cancels capture");
  click(screen,500,350);
  var mouse=pointer(screen,0,0,3);
  screen.mouseClicked(mouse,false);check(key.matchesMouse(mouse),"inline mouse binding");
  key.setBoundKey(old);net.minecraft.client.option.KeyBinding.updateKeysByCode();
  net.minecraft.client.MinecraftClient.getInstance().options.write();
  s.actionDelay=oldDelay;s.anchorSlot=oldSlot;s.reduceMotion=motion;s.save();
  System.out.println("[Sakura UI smoke] PASS: scaled switches, slots, slider drag, keyboard steps, persistence, keyboard/mouse binding and capture cancellation.");
 }
 static void install(){
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(mc.getOverlay()!=null)return;
   if(wait-->0)return;
   if(stage==-1){
    SakuraClient.settings.anchor=true;
    SakuraClient.settings.crystal=true;
    SakuraClient.settings.fastPlace=true;
    mc.setScreen(new SakuraScreen(0));stage=0;wait=40;return;
   }
   if(stage==0&&!tested){
    verifyInputs((SakuraScreen)mc.currentScreen);
    tested=true;mc.setScreen(new SakuraScreen(0));
    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getHandle(),1,1);
    wait=40;return;
   }
   if(stage<3){
    String name=new String[]{"anchor","crystal","utility"}[stage];
    ScreenshotRecorder.saveScreenshot(mc.runDirectory,"sakura-"+name+".png",mc.getFramebuffer(),1,
      text->System.out.println("[Sakura UI smoke] "+text.getString()));
    stage++;
    if(stage<3)mc.setScreen(new SakuraScreen(stage));
    wait=40;return;
   }
   for(String name:new String[]{"anchor","crystal","utility"}){
    File file=new File(mc.runDirectory,"screenshots/sakura-"+name+".png");
    if(!file.isFile()||file.length()==0)throw new IllegalStateException("Missing UI screenshot: "+name);
   }
   System.out.println("[Sakura UI smoke] Rendered all three tabs successfully. No gameplay test.");
   mc.scheduleStop();wait=Integer.MAX_VALUE;
  });
 }
}
