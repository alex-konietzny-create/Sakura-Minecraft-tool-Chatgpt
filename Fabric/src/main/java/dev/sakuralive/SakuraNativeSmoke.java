package dev.sakuralive;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
/** Explicit development-only native-input loopback. Never joins a world or sends input elsewhere. */
final class SakuraNativeSmoke {
 private static int wait=80,stage,ticks;
 private static WindowsSystemInput input;
 private static Target target;
 private static KeyBinding testKey;
 private static double baseX,baseY;
 private static void fail(String why){if(input!=null)input.close();MinecraftClient.getInstance().scheduleStop();throw new IllegalStateException("Native loopback: "+why);}
 static void install(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.getOverlay()!=null||wait-->0)return;
  if(++ticks>2400){fail("timeout / foreground not granted at stage "+stage+" keys="+(target==null?0:target.keys)+" clicks="+(target==null?0:target.clicks));return;}
  try{
   if(stage==0){
    target=new Target();mc.setScreen(target);
    var category=KeyBinding.Category.create(net.minecraft.util.Identifier.of("sakuralive","native_test"));
    testKey=new KeyBinding("sakura.native_test",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_F24,category);
    input=new WindowsSystemInput(mc,()->mc.currentScreen==target&&mc.isWindowFocused());
    GLFW.glfwFocusWindow(mc.getWindow().getHandle());stage=1;wait=5;return;
   }
   boolean safe=mc.currentScreen==target&&mc.isWindowFocused()&&input.foreground();
   input.tick(System.nanoTime(),safe);
   if(!safe||!input.available())return;
   if(stage==1){
    var mouse=new KeyBinding("sakura.native_pair_mouse",InputUtil.Type.MOUSE,1,KeyBinding.Category.create(net.minecraft.util.Identifier.of("sakuralive","native_pair")));
    GLFW.glfwSetCursorPos(mc.getWindow().getHandle(),mc.getWindow().getWidth()/2.0,mc.getWindow().getHeight()/2.0);
    if(input.slotAndUse(testKey,mouse,System.nanoTime(),1)){stage=3;wait=5;}return;
   }
   if(stage==2){
    if(target.keys<1)return;
    GLFW.glfwSetCursorPos(mc.getWindow().getHandle(),mc.getWindow().getWidth()/2.0,mc.getWindow().getHeight()/2.0);
    // Only a right click over this empty screen, with no actionable widgets.
    var mouse=new KeyBinding("sakura.native_mouse",InputUtil.Type.MOUSE,1,KeyBinding.Category.create(net.minecraft.util.Identifier.of("sakuralive","native_mouse")));
    if(input.tap(mouse,System.nanoTime(),1)){stage=3;wait=5;}return;
   }
   if(stage==3){
    if(target.clicks<1||target.keys<1)return;
    if(GLFW.glfwGetKey(mc.getWindow().getHandle(),GLFW.GLFW_KEY_F24)!=GLFW.GLFW_RELEASE||GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),1)!=GLFW.GLFW_RELEASE)return;
    GLFW.glfwSetInputMode(mc.getWindow().getHandle(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_DISABLED);
    if(GLFW.glfwRawMouseMotionSupported())GLFW.glfwSetInputMode(mc.getWindow().getHandle(),GLFW.GLFW_RAW_MOUSE_MOTION,GLFW.GLFW_FALSE);
    stage=4;wait=4;return;
   }
   if(stage==4){baseX=mc.mouse.getX();baseY=mc.mouse.getY();input.move(24,8);stage=5;wait=4;return;}
   if(stage==5){
    if(Math.hypot(mc.mouse.getX()-baseX,mc.mouse.getY()-baseY)<1){fail("captured cursor did not receive native movement");return;}
    GLFW.glfwSetInputMode(mc.getWindow().getHandle(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_NORMAL);
    input.close();input=null;
    System.out.println("[Sakura native smoke] PASS: relative SendInput movement received with captured cursor and raw mode temporarily disabled.");
    System.out.println("[Sakura native smoke] PASS: nominal 1ms Windows SendInput F24 and right-click received through Minecraft GLFW callbacks; both released. Empty test screen, no gameplay.");
    mc.scheduleStop();wait=Integer.MAX_VALUE;
   }
  }catch(Exception e){fail(e.toString());}
 });}
 private static final class Target extends Screen {
  int keys,clicks;
  Target(){super(Text.literal("Sakura input verification"));}
  @Override public boolean keyPressed(KeyInput event){if(event.key()==GLFW.GLFW_KEY_F24){keys++;return true;}return true;}
  @Override public boolean mouseClicked(Click event,boolean doubled){if(event.button()==1)clicks++;return true;}
  @Override public void render(DrawContext c,int x,int y,float delta){c.fill(0,0,width,height,0xFF121416);c.drawCenteredTextWithShadow(textRenderer,"Sakura: testing native input in this empty screen",width/2,height/2,0xFFFFFFFF);}
  @Override public boolean shouldPause(){return false;}
 }
}
