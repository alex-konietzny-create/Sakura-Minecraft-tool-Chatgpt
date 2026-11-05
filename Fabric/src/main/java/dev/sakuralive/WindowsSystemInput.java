package dev.sakuralive;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.*;
import java.util.function.BooleanSupplier;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Windows OS inputs only. Hooks observe only watched trigger states while this
 * Minecraft window is foreground; no text is captured or stored. Injected events
 * pass through unchanged and do not overwrite the user's physical hold state.
 */
final class WindowsSystemInput implements AutoCloseable {
 private final long window;
 private final BooleanSupplier gameReady;
 private volatile long heartbeat=System.nanoTime();
 private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Sakura input release watchdog");t.setDaemon(true);return t;});
 private final InputPulse pulse;
 private final PhysicalTriggers physicalKeys=new PhysicalTriggers(),physicalMouse=new PhysicalTriggers();
 private volatile boolean hooksReady,closed;
 private volatile String error="";
 private volatile int threadId;
 private HHOOK mouseHook,keyHook;
 private LowLevelMouseProc mouseProc;
 private LowLevelKeyboardProc keyProc;
 WindowsSystemInput(MinecraftClient mc,BooleanSupplier gameReady) throws Exception {
  this.gameReady=gameReady;
  if(!Platform.isWindows())throw new IllegalStateException("Systemeingaben benötigen Windows");
  window=GLFWNativeWin32.glfwGetWin32Window(mc.getWindow().getHandle());
  
  pulse=new InputPulse(new InputPulse.Transport(){
   public void press(InputPulse.Code code){
    if(!foreground()||!gameReady.getAsBoolean())throw new IllegalStateException("Minecraft hat den Fokus verloren");
    send(code,true);
   }
   public void release(InputPulse.Code code){send(code,false);}
   public long holdStartsAt(long fallback){return System.nanoTime();}
   public void pressTogether(InputPulse.Code first,InputPulse.Code second){
    if(!foreground()||!gameReady.getAsBoolean())throw new IllegalStateException("Minecraft hat den Fokus verloren");
    INPUT[] batch=(INPUT[])new INPUT().toArray(2);fill(batch[0],first,true);fill(batch[1],second,true);
    if(User32.INSTANCE.SendInput(new DWORD(2),batch,batch[0].size()).intValue()!=2)throw new IllegalStateException("Windows hat die kombinierte Eingabe abgelehnt");
   }
  });
  Thread thread=new Thread(this::hooks,"Sakura physical trigger states");thread.setDaemon(true);thread.start();
  watchdog.scheduleAtFixedRate(()->{
   try{boolean safe=foreground()&&gameReady.getAsBoolean()&&System.nanoTime()-heartbeat<250_000_000L;pulse.tick(System.nanoTime(),safe);}
   catch(Throwable e){error="Eingabe-Watchdog: "+e.getMessage();}
  },1,1,TimeUnit.MILLISECONDS);
 }
 boolean foreground(){
  HWND current=User32.INSTANCE.GetForegroundWindow();
  return current!=null&&Pointer.nativeValue(current.getPointer())==window&&!closed;
 }
 boolean available(){return hooksReady&&error.isEmpty()&&!closed;}
 String status(){return error.isEmpty()?"Systemeingaben werden initialisiert":error;}
 void tick(long now,boolean gameReady){
  heartbeat=now;pulse.tick(now,gameReady&&foreground());
  if(!gameReady||!foreground()){physicalKeys.clear();physicalMouse.clear();}
 }
 boolean physicalRight(){return physical(new InputPulse.Code(true,InputEvent.BUTTON3_DOWN_MASK,2));}
 boolean physical(KeyBinding binding){return physical(code(binding));}
 private boolean physical(InputPulse.Code code){
  if(!foreground())return false;
  var state=code.mouse()?physicalMouse:physicalKeys;
  return state.isDown(code.physicalVk(),()->(User32.INSTANCE.GetAsyncKeyState(code.physicalVk())&0x8000)!=0);
 }
 boolean tap(KeyBinding binding,long now,int ms){
  if(!available()||!pulse.idle())return false;
  return pulse.tap(code(binding),now,ms,foreground());
 }
 boolean move(int dx,int dy){
  if(!available()||!foreground()||!gameReady.getAsBoolean())return false;
  if(dx==0&&dy==0)return true;
  INPUT input=new INPUT();input.type=new DWORD(INPUT.INPUT_MOUSE);input.input.setType(MOUSEINPUT.class);
  input.input.mi.dx=new LONG(dx);input.input.mi.dy=new LONG(dy);input.input.mi.dwFlags=new DWORD(1);input.write();
  if(User32.INSTANCE.SendInput(new DWORD(1),new INPUT[]{input},input.size()).intValue()!=1)throw new IllegalStateException("Windows hat die Mausbewegung abgelehnt");return true;
 }
 boolean slotAndUse(KeyBinding slot,KeyBinding use,long now,int ms){
  if(!available())return false;
  return pulse.tapTogether(code(slot),code(use),now,ms,foreground());
 }
 void mute(KeyBinding binding){
  var c=code(binding);physical(c);pulse.release(c);
 }
 void releaseAll(){pulse.releaseAll();}
 private void hooks(){
  threadId=Kernel32.INSTANCE.GetCurrentThreadId();
  try{
   mouseProc=(n,w,data)->{
    if(n>=0){
     int message=w.intValue(),button=-1;boolean down=false;
     switch(message){
      case 0x0201->{button=1;down=true;}case 0x0202->button=1;
      case 0x0204->{button=2;down=true;}case 0x0205->button=2;
      case 0x0207->{button=4;down=true;}case 0x0208->button=4;
      case 0x020B,0x020C->{button=((data.mouseData>>>16)&0xffff)==1?5:6;down=message==0x020B;}
     }
     physicalMouse.event(button,down,(data.flags&1)!=0,foreground());
    }
    return User32.INSTANCE.CallNextHookEx(mouseHook,n,w,new LPARAM(Pointer.nativeValue(data.getPointer())));
   };
   keyProc=(n,w,data)->{
    if(n>=0){
     int m=w.intValue();if(m==0x100||m==0x104)physicalKeys.event(data.vkCode,true,(data.flags&0x10)!=0,foreground());
     else if(m==0x101||m==0x105)physicalKeys.event(data.vkCode,false,(data.flags&0x10)!=0,foreground());
    }
    return User32.INSTANCE.CallNextHookEx(keyHook,n,w,new LPARAM(Pointer.nativeValue(data.getPointer())));
   };
   MSG message=new MSG();User32.INSTANCE.PeekMessage(message,null,0,0,0);
   mouseHook=User32.INSTANCE.SetWindowsHookEx(14,mouseProc,Kernel32.INSTANCE.GetModuleHandle(null),0);
   keyHook=User32.INSTANCE.SetWindowsHookEx(13,keyProc,Kernel32.INSTANCE.GetModuleHandle(null),0);
   if(mouseHook==null||keyHook==null)throw new IllegalStateException("Windows-Eingabe-Hook nicht verfügbar: "+Kernel32.INSTANCE.GetLastError());
   hooksReady=true;
   while(!closed&&User32.INSTANCE.GetMessage(message,null,0,0)>0){User32.INSTANCE.TranslateMessage(message);User32.INSTANCE.DispatchMessage(message);}
  }catch(Throwable e){error="Systemeingaben: "+e.getMessage();}
  finally{
   hooksReady=false;
   if(mouseHook!=null)User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
   if(keyHook!=null)User32.INSTANCE.UnhookWindowsHookEx(keyHook);
  }
 }
 static INPUT event(InputPulse.Code code,boolean down){
  INPUT input=new INPUT();fill(input,code,down);return input;
 }
 private static void fill(INPUT input,InputPulse.Code code,boolean down){
  if(code.mouse()){
   input.type=new DWORD(INPUT.INPUT_MOUSE);input.input.setType(MOUSEINPUT.class);
   int flags=switch(code.physicalVk()){case 1->down?0x0002:0x0004;case 2->down?0x0008:0x0010;case 4->down?0x0020:0x0040;default->down?0x0080:0x0100;};
   input.input.mi.dwFlags=new DWORD(flags);input.input.mi.mouseData=new DWORD(code.physicalVk()==5?1:code.physicalVk()==6?2:0);
  }else{
   input.type=new DWORD(INPUT.INPUT_KEYBOARD);input.input.setType(KEYBDINPUT.class);
   input.input.ki.wVk=new WORD(code.physicalVk());input.input.ki.dwFlags=new DWORD((down?0:2)|((code.physicalVk()>=0x21&&code.physicalVk()<=0x2E||code.physicalVk()==0xA3||code.physicalVk()==0xA5||code.physicalVk()==0x6F)?1:0));
  }
  input.write();
 }
 private static void send(InputPulse.Code code,boolean down){
  INPUT input=event(code,down);
  if(User32.INSTANCE.SendInput(new DWORD(1),new INPUT[]{input},input.size()).intValue()!=1)
   throw new IllegalStateException("Windows hat die simulierte Eingabe abgelehnt: "+Kernel32.INSTANCE.GetLastError());
 }
 static InputPulse.Code code(KeyBinding binding){
  var key=KeyBindingHelper.getBoundKeyOf(binding);int k=key.getCode();
  if(key.getCategory()==InputUtil.Type.MOUSE){
   int button=switch(k){case 0->1;case 1->3;case 2->2;case 3->4;case 4->5;default->throw unsupported(binding);};
   int vk=switch(k){case 0->1;case 1->2;case 2->4;case 3->5;default->6;};
   return new InputPulse.Code(true,InputEvent.getMaskForButton(button),vk);
  }
  if(key.getCategory()!=InputUtil.Type.KEYSYM)throw unsupported(binding);
  if(k>=GLFW.GLFW_KEY_A&&k<=GLFW.GLFW_KEY_Z||k>=GLFW.GLFW_KEY_0&&k<=GLFW.GLFW_KEY_9)return new InputPulse.Code(false,k,k);
  if(k>=GLFW.GLFW_KEY_F1&&k<=GLFW.GLFW_KEY_F24)return new InputPulse.Code(false,KeyEvent.VK_F1+k-GLFW.GLFW_KEY_F1,0x70+k-GLFW.GLFW_KEY_F1);
  if(k>=GLFW.GLFW_KEY_KP_0&&k<=GLFW.GLFW_KEY_KP_9)return new InputPulse.Code(false,0x60+k-GLFW.GLFW_KEY_KP_0,0x60+k-GLFW.GLFW_KEY_KP_0);
  int value=switch(k){
   case GLFW.GLFW_KEY_RIGHT_SHIFT->0xA1;case GLFW.GLFW_KEY_RIGHT_CONTROL->0xA3;case GLFW.GLFW_KEY_RIGHT_ALT->0xA5;
   case GLFW.GLFW_KEY_LEFT_SUPER->0x5B;case GLFW.GLFW_KEY_RIGHT_SUPER->0x5C;
   case GLFW.GLFW_KEY_INSERT->0x2D;case GLFW.GLFW_KEY_HOME->0x24;case GLFW.GLFW_KEY_END->0x23;
   case GLFW.GLFW_KEY_PAGE_UP->0x21;case GLFW.GLFW_KEY_PAGE_DOWN->0x22;
   case GLFW.GLFW_KEY_APOSTROPHE->0xDE;case GLFW.GLFW_KEY_COMMA->0xBC;case GLFW.GLFW_KEY_MINUS->0xBD;case GLFW.GLFW_KEY_PERIOD->0xBE;
   case GLFW.GLFW_KEY_SLASH->0xBF;case GLFW.GLFW_KEY_SEMICOLON->0xBA;case GLFW.GLFW_KEY_EQUAL->0xBB;
   case GLFW.GLFW_KEY_LEFT_BRACKET->0xDB;case GLFW.GLFW_KEY_BACKSLASH->0xDC;case GLFW.GLFW_KEY_RIGHT_BRACKET->0xDD;case GLFW.GLFW_KEY_GRAVE_ACCENT->0xC0;
   case GLFW.GLFW_KEY_KP_ADD->0x6B;case GLFW.GLFW_KEY_KP_SUBTRACT->0x6D;case GLFW.GLFW_KEY_KP_MULTIPLY->0x6A;case GLFW.GLFW_KEY_KP_DIVIDE->0x6F;case GLFW.GLFW_KEY_KP_DECIMAL->0x6E;
   case GLFW.GLFW_KEY_SPACE->KeyEvent.VK_SPACE;case GLFW.GLFW_KEY_ENTER->KeyEvent.VK_ENTER;
   case GLFW.GLFW_KEY_CAPS_LOCK->KeyEvent.VK_CAPS_LOCK;
   case GLFW.GLFW_KEY_TAB->KeyEvent.VK_TAB;case GLFW.GLFW_KEY_ESCAPE->KeyEvent.VK_ESCAPE;
   case GLFW.GLFW_KEY_BACKSPACE->KeyEvent.VK_BACK_SPACE;case GLFW.GLFW_KEY_DELETE->KeyEvent.VK_DELETE;
   case GLFW.GLFW_KEY_LEFT->KeyEvent.VK_LEFT;case GLFW.GLFW_KEY_RIGHT->KeyEvent.VK_RIGHT;
   case GLFW.GLFW_KEY_UP->KeyEvent.VK_UP;case GLFW.GLFW_KEY_DOWN->KeyEvent.VK_DOWN;
   case GLFW.GLFW_KEY_LEFT_SHIFT->KeyEvent.VK_SHIFT;case GLFW.GLFW_KEY_LEFT_CONTROL->KeyEvent.VK_CONTROL;
   case GLFW.GLFW_KEY_LEFT_ALT->KeyEvent.VK_ALT;
   default->throw unsupported(binding);
  };
  int vk=switch(k){case GLFW.GLFW_KEY_LEFT_SHIFT->0xA0;case GLFW.GLFW_KEY_LEFT_CONTROL->0xA2;case GLFW.GLFW_KEY_LEFT_ALT->0xA4;default->value;};
  return new InputPulse.Code(false,value,vk);
 }
 private static IllegalArgumentException unsupported(KeyBinding binding){return new IllegalArgumentException("Nicht unterstützte Systemtaste: "+binding.getBoundKeyLocalizedText().getString());}
 @Override public void close(){
  try{releaseAll();}finally{
   closed=true;watchdog.shutdownNow();
   if(threadId!=0)User32.INSTANCE.PostThreadMessage(threadId,0x12,new WPARAM(0),new LPARAM(0));
   physicalKeys.clear();physicalMouse.clear();
  }
 }
}

