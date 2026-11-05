package dev.sakuralive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.*;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
/** Routes bound shortcuts to Sakura only; ordinary gameplay keys and UI input are untouched. */
public final class Hotkeys {
 private static final int[] pending=new int[8];
 private static KeyBinding binding(int i){return i==7?SakuraClient.menuKey:SakuraClient.keyFor(i);}
 private static boolean gameplay(){var mc=MinecraftClient.getInstance();return mc.world!=null&&mc.currentScreen==null&&mc.isWindowFocused();}
 public static boolean keyboard(KeyInput key,int action){return route(i->binding(i).matchesKey(key),action,gameplay());}
 public static boolean mouse(MouseInput key,int action){return route(i->binding(i).matchesMouse(new net.minecraft.client.gui.Click(0,0,key)),action,gameplay());}
 static boolean route(java.util.function.IntPredicate matches,int action,boolean gameplay){
  boolean consumed=false;
  for(int i=0;i<8;i++){
   var b=binding(i);if(b==null||b.isUnbound()||!matches.test(i))continue;
   if(action==GLFW.GLFW_RELEASE)b.setPressed(false);
   if(!gameplay)continue;
   b.setPressed(action!=GLFW.GLFW_RELEASE);
   if(action==GLFW.GLFW_PRESS)pending[i]=Math.min(4,pending[i]+1);
   consumed=true;
  }
  return consumed;
 }
 static boolean take(int i){if(pending[i]==0)return false;pending[i]--;return true;}
 static void clear(){java.util.Arrays.fill(pending,0);}
}
