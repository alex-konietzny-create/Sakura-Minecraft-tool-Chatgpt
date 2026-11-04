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

/** Opt-in dev-only UI check. No game world is joined and no gameplay is claimed. */
final class SakuraUiSmoke {
 private static final String[] NAMES={"anchor","legit","safe","double","crystal","fastplace","design","anchor-hotbar","anchor-controls"};
 private static int stage=-1,wait=60;
 private static boolean tested;
 private static void check(boolean value,String why){if(!value)throw new IllegalStateException("UI assertion: "+why);}
 private static Click pointer(SakuraScreen screen,double x,double y,int button){
  double scale=Math.max(.01,Math.min(1.2,Math.min((screen.width-20.0)/920,(screen.height-20.0)/580)));
  return new Click((screen.width-scale*920)/2+x*scale,(screen.height-scale*580)/2+y*scale,new MouseInput(button,0));
 }
 private static void click(SakuraScreen screen,double x,double y){
  var p=pointer(screen,x,y,0);check(screen.mouseClicked(p,false),"click "+x+","+y);screen.mouseReleased(p);
 }
 private static SakuraScreen page(MinecraftClient mc,int tab,int section){var screen=new SakuraScreen(tab,section);mc.setScreen(screen);return screen;}
 private static void verifyInputs(MinecraftClient mc){
  var s=SakuraClient.settings;boolean motion=s.reduceMotion,anchor=s.anchor;
  int oldMs=s.anchorOptions.placeMs,oldSlot=s.anchorOptions.anchorSlot;
  s.reduceMotion=true;
  for(int[] size:new int[][]{{320,200},{460,290},{640,400}}){
   var screen=page(mc,0,0);screen.resize(size[0],size[1]);
   click(screen,250,120);check(s.anchor!=anchor,"switch inverse scale");
   click(screen,250,120);check(s.anchor==anchor,"switch restoration");
   var from=pointer(screen,604,284,0);var to=pointer(screen,234,284,0);
   check(screen.mouseClicked(from,false),"slider press");check(s.anchorOptions.placeMs==2000,"slider maximum");
   screen.mouseDragged(to,to.x()-from.x(),0);check(s.anchorOptions.placeMs==10,"slider drag minimum");screen.mouseReleased(to);
   screen.keyPressed(new KeyInput(GLFW.GLFW_KEY_RIGHT,0,0));check(s.anchorOptions.placeMs==11,"1 ms keyboard step");
   click(screen,540,258);check(screen.getFocused() instanceof TextFieldWidget,"numeric field receives pointer, not slider");
   ((TextFieldWidget)screen.getFocused()).setText("137");
   screen.keyPressed(new KeyInput(GLFW.GLFW_KEY_ENTER,0,0));
   check(s.anchorOptions.placeMs==137&&SakuraSettings.load().anchorOptions.placeMs==137,"exact millisecond input persists");
  }
  var screen=page(mc,0,1);click(screen,584,294);check(s.anchorOptions.anchorSlot==9,"slot grid");
  var key=SakuraClient.anchorKey;var old=InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
  screen=page(mc,0,2);click(screen,250,270);
  var input=new KeyInput(GLFW.GLFW_KEY_F8,0,0);screen.keyPressed(input);check(key.matchesKey(input),"inline key");
  click(screen,250,270);screen.keyPressed(new KeyInput(GLFW.GLFW_KEY_ESCAPE,0,0));check(key.matchesKey(input),"capture cancel");
  click(screen,250,270);var mouse=pointer(screen,0,0,3);screen.mouseClicked(mouse,false);check(key.matchesMouse(mouse),"inline mouse");
  key.setBoundKey(old);KeyBinding.updateKeysByCode();mc.options.write();
  s.anchorOptions.placeMs=oldMs;s.anchorOptions.anchorSlot=oldSlot;s.reduceMotion=motion;s.save();
  System.out.println("[Sakura UI smoke] PASS: scaled input, numeric ms fields, slider drag, 1 ms steps, persistence, hotbar and keyboard/mouse binding");
 }
 private static SakuraScreen screenshotPage(int index){
  return index<7?new SakuraScreen(index):new SakuraScreen(0,index==7?1:2);
 }
 static void install(){
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(mc.getOverlay()!=null||wait-->0)return;
   if(stage==-1){
    for(int i=0;i<6;i++)if(!SakuraClient.enabled(i))SakuraClient.toggle(i);
    mc.setScreen(screenshotPage(0));stage=0;wait=40;return;
   }
   if(!tested){
    verifyInputs(mc);tested=true;mc.setScreen(screenshotPage(0));wait=40;return;
   }
   if(stage<NAMES.length){
    ScreenshotRecorder.saveScreenshot(mc.runDirectory,"sakura-"+NAMES[stage]+".png",mc.getFramebuffer(),1,
      text->System.out.println("[Sakura UI smoke] "+text.getString()));
    stage++;if(stage<NAMES.length)mc.setScreen(screenshotPage(stage));wait=40;return;
   }
   for(String name:NAMES){
    File file=new File(mc.runDirectory,"screenshots/sakura-"+name+".png");
    check(file.isFile()&&file.length()>0,"missing screenshot "+name);
   }
   System.out.println("[Sakura UI smoke] Rendered seven module pages and two settings sections. No gameplay test.");
   mc.scheduleStop();wait=Integer.MAX_VALUE;
  });
 }
}
