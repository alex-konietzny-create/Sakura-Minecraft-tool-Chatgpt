package dev.sakuralive;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public final class SakuraClient implements ClientModInitializer {
 private static volatile Config cfg=new Config();
 private static KeyBinding anchorKey,crystalKey;
 private static int step=0,timer=0,crystalTimer=0;
 private record Config(boolean anchor,boolean crystal,int anchorSlot,int glowSlot,int totemSlot,int crystalSlot,int delay) {}
 @Override public void onInitializeClient() {
  anchorKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.anchor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,"category.sakuralive"));
  crystalKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sakuralive.crystal",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_H,"category.sakuralive"));
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  startIpc();
 }
 private void tick(MinecraftClient mc) {
  if(mc.player==null || mc.world==null || mc.interactionManager==null){step=0;return;}
  if(timer>0)timer--;if(crystalTimer>0)crystalTimer--;
  while(anchorKey.wasPressed())if(cfg.anchor && step==0){step=1;timer=0;}
  if(step!=0 && timer==0) {
   int slot=step==2?cfg.glowSlot:cfg.anchorSlot;
   if(slot<1||slot>9||!(mc.crosshairTarget instanceof BlockHitResult hit)){step=0;return;}
   var inv=mc.player.getInventory();
   if(step==1&&!inv.getStack(slot-1).isOf(Items.RESPAWN_ANCHOR) || step==2&&!inv.getStack(slot-1).isOf(Items.GLOWSTONE)){step=0;return;}
   inv.setSelectedSlot(slot-1);
   mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,hit);
   mc.player.swingHand(Hand.MAIN_HAND);
   if(++step>3){step=0;inv.setSelectedSlot(cfg.totemSlot-1);} else timer=cfg.delay;
  }
  while(crystalKey.wasPressed())if(cfg.crystal && crystalTimer==0 && mc.crosshairTarget instanceof BlockHitResult hit) {
   var inv=mc.player.getInventory(); int slot=cfg.crystalSlot-1;
   if(inv.getStack(slot).isOf(Items.END_CRYSTAL)) {
    inv.setSelectedSlot(slot); mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,hit);
    mc.player.swingHand(Hand.MAIN_HAND);crystalTimer=cfg.delay;
   }
  }
 }
 private void startIpc(){
  try{
   HttpServer srv=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
   long pid=ProcessHandle.current().pid();
   srv.createContext("/hello",e->{byte[] data=("SAKURA:"+pid).getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,data.length);try(var out=e.getResponseBody()){out.write(data);}});
   srv.createContext("/config",e->{
    if(!"POST".equals(e.getRequestMethod())){e.sendResponseHeaders(405,-1);e.close();return;}
    String json=new String(e.getRequestBody().readNBytes(4096),StandardCharsets.UTF_8);
    cfg=new Config(bool(json,"anchor"),bool(json,"crystal"),num(json,"anchorSlot",1,1,9),num(json,"glowSlot",2,1,9),num(json,"totemSlot",9,1,9),num(json,"crystalSlot",3,1,9),num(json,"delay",3,1,20));
    byte[] ok="OK".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,ok.length);try(var out=e.getResponseBody()){out.write(ok);}
   });
   srv.setExecutor(Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Sakura IPC");t.setDaemon(true);return t;}));srv.start();
   Path dir=Path.of(System.getProperty("java.io.tmpdir"),"SakuraLive");Files.createDirectories(dir);
   Path file=dir.resolve(pid+".port");Files.writeString(file,Integer.toString(srv.getAddress().getPort()));
   Runtime.getRuntime().addShutdownHook(new Thread(()->{try{Files.deleteIfExists(file);}catch(IOException ignored){}}));
  }catch(IOException ex){System.err.println("[Sakura] IPC error: "+ex);}
 }
 private static boolean bool(String s,String k){return Pattern.compile("\\\""+k+"\\\"\\s*:\\s*true").matcher(s).find();}
 private static int num(String s,String k,int fallback,int lo,int hi){
  Matcher m=Pattern.compile("\\\""+k+"\\\"\\s*:\\s*(\\d+)").matcher(s);
  if(!m.find())return fallback;try{return Math.max(lo,Math.min(hi,Integer.parseInt(m.group(1))));}catch(NumberFormatException ex){return fallback;}
 }
}
