package dev.sakuralive;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.ScreenshotRecorder;
import java.io.File;

/** Opt-in development-only rendering check; never enabled in a production instance. */
final class SakuraUiSmoke {
 private static int stage=-1,wait=60;
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
