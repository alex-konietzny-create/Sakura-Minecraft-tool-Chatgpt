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
  java.util.function.IntPredicate bound=i->{var b=binding(i);return b!=null&&!b.isUnbound()&&matches.test(i);};
  for(int i=0;i<8;i++)if(bound.test(i)&&action==GLFW.GLFW_RELEASE)binding(i).setPressed(false);
  if(!gameplay)return false;
  int selected=ShortcutChoice.choose(bound,Hotkeys::eligible,SakuraClient.actionOwner());
  if(selected==-2){SakuraClient.lastStatus="Hotkey mehrfach aktiv: bitte pro Taste nur ein Modul aktivieren";return true;}
  if(selected<0)return false;
  binding(selected).setPressed(action!=GLFW.GLFW_RELEASE);
  if(action==GLFW.GLFW_PRESS)pending[selected]=Math.min(4,pending[selected]+1);
  return true;
 }
 static boolean competing(KeyBinding key){
  if(key==null||key.isUnbound())return false;
  for(int i=0;i<8;i++)if(binding(i)!=key&&eligible(i)&&binding(i)!=null&&!binding(i).isUnbound()&&binding(i).equals(key))return true;
  return false;
 }
 static boolean eligible(int i){return i>=4||SakuraClient.enabled(i);}
 static boolean take(int i){if(pending[i]==0)return false;pending[i]--;return true;}
 static void clear(){java.util.Arrays.fill(pending,0);}
}
