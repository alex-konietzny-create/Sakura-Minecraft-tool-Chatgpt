package dev.sakuralive;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class SakuraSettings {
 public boolean anchor = false;
 public boolean crystal = false;
 public boolean fastPlace = false;
 public boolean excludeAnchors = true;
 public int anchorSlot = 1;
 public int glowSlot = 2;
 public int totemSlot = 9;
 public int crystalSlot = 3;
 public int actionDelay = 3;
 public int placeDelay = 0;
 private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
 private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sakura-live.json");
 public static SakuraSettings load() {
  try {
   if(Files.exists(FILE)) {
    SakuraSettings settings=GSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),SakuraSettings.class);
    if(settings!=null){settings.clamp();return settings;}
   }
  } catch(Exception e){System.err.println("[Sakura] Could not load settings: "+e);}
  return new SakuraSettings();
 }
 public void clamp(){
  anchorSlot=cap(anchorSlot,1,9);glowSlot=cap(glowSlot,1,9);
  totemSlot=cap(totemSlot,1,9);crystalSlot=cap(crystalSlot,1,9);
  actionDelay=cap(actionDelay,1,20);placeDelay=cap(placeDelay,0,4);
 }
 private static int cap(int n,int min,int max){return Math.max(min,Math.min(max,n));}
 public void save(){
  clamp();
  try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,GSON.toJson(this),StandardCharsets.UTF_8);}
  catch(Exception e){System.err.println("[Sakura] Could not save settings: "+e);}
 }
}
