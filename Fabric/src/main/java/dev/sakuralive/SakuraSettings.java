package dev.sakuralive;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Schema 3 migrates old tick delays to milliseconds once, preserving existing controls. */
public final class SakuraSettings {
 public int schemaVersion=3;
 public boolean smoothFont=true,reduceMotion=false,anchor=false,legitAnchor=false,safeAnchor=false,doubleAnchor=false,crystal=false,fastPlace=false,excludeAnchors=true;
 public AnchorOptions anchorOptions=new AnchorOptions();
 public AnchorOptions legitOptions=AnchorOptions.legit();
 public AnchorOptions safeOptions=new AnchorOptions();
 public AnchorOptions doubleOptions=new AnchorOptions();
 public int systemPulseMs=25;
 public int crystalPlaceMs=150,crystalBreakMs=150,crystalTimeoutMs=2000,placeDelayMs=0;
 public static final class AnchorOptions {
  public int anchorSlot=1,glowSlot=2,finalSlot=9;
  public int placeMs=150,chargeMs=150,switchMs=35,coverMs=120,repeatMs=200,timeoutMs=2000;
  public boolean holdToRun=false;
  static AnchorOptions legit(){
   var o=new AnchorOptions();o.placeMs=280;o.chargeMs=280;o.switchMs=80;o.holdToRun=true;return o;
  }
  void clamp(){
   anchorSlot=cap(anchorSlot,1,9);glowSlot=cap(glowSlot,1,9);finalSlot=cap(finalSlot,1,9);
   placeMs=cap(placeMs,10,2000);chargeMs=cap(chargeMs,10,2000);switchMs=cap(switchMs,0,500);
   coverMs=cap(coverMs,10,2000);repeatMs=cap(repeatMs,10,2000);
   timeoutMs=cap(timeoutMs,250,10000);
   // Confirmation timeout must outlive every configured wait plus slot preparation.
   timeoutMs=Math.max(timeoutMs,Math.max(Math.max(placeMs,chargeMs),Math.max(coverMs,repeatMs))+switchMs+500);
  }
 }
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("sakura-live.json");
 static int cap(int n,int min,int max){return Math.max(min,Math.min(max,n));}
 public static SakuraSettings load(){
  try{
   if(Files.exists(FILE)){
    var raw=JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonObject();
    var s=GSON.fromJson(raw,SakuraSettings.class);
    if(!raw.has("schemaVersion")||raw.get("schemaVersion").getAsInt()<3){
     s.anchorOptions.anchorSlot=old(raw,"anchorSlot",1);s.anchorOptions.glowSlot=old(raw,"glowSlot",2);s.anchorOptions.finalSlot=old(raw,"totemSlot",9);
     s.anchorOptions.placeMs=s.anchorOptions.chargeMs=old(raw,"actionDelay",3)*50;
     s.crystalPlaceMs=old(raw,"crystalPlaceDelay",3)*50;s.crystalBreakMs=old(raw,"crystalBreakDelay",3)*50;
     s.placeDelayMs=old(raw,"placeDelay",0)*50;s.schemaVersion=3;
    }
    s.clamp();return s;
   }
  }catch(Exception e){System.err.println("[Sakura] Settings load: "+e);}
  return new SakuraSettings();
 }
 private static int old(JsonObject raw,String key,int fallback){return raw.has(key)?raw.get(key).getAsInt():fallback;}
 public void clamp(){
  systemPulseMs=cap(systemPulseMs,10,100);
  if(anchorOptions==null)anchorOptions=new AnchorOptions();if(legitOptions==null)legitOptions=AnchorOptions.legit();
  if(safeOptions==null)safeOptions=new AnchorOptions();if(doubleOptions==null)doubleOptions=new AnchorOptions();
  anchorOptions.clamp();legitOptions.clamp();safeOptions.clamp();doubleOptions.clamp();
  crystalPlaceMs=cap(crystalPlaceMs,10,2000);crystalBreakMs=cap(crystalBreakMs,10,2000);
  crystalTimeoutMs=cap(crystalTimeoutMs,250,10000);placeDelayMs=cap(placeDelayMs,0,200);
 }
 public boolean save(){
  clamp();
  try{
   Files.createDirectories(FILE.getParent());
   Path temporary=FILE.resolveSibling("sakura-live.json.tmp");
   Files.writeString(temporary,GSON.toJson(this),StandardCharsets.UTF_8);
   try{Files.move(temporary,FILE,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException e){Files.move(temporary,FILE,StandardCopyOption.REPLACE_EXISTING);}
   return true;
  }catch(Exception e){System.err.println("[Sakura] Settings save: "+e);return false;}
 }
}
