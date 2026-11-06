package dev.sakuralive;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

/** Sakura 198-inspired compact client panel, with current module settings.
 * All controls are drawn and hit-tested in one consistent scaled coordinate space.
 */
public final class SakuraScreen extends Screen {
 private static final int W=660,H=440;
 private static final int BG=0xFF0D0A0C,CARD=0xFF1C1419,EDGE=0xFF6A2E48;
 private static final int PINK=0xFFFF4D9B,PALE=0xFFFFA4CD,WHITE=0xFFFFEAF2;
 private static final int MUTED=0xFFB99DA9,BUTTON=0xFF33222C,HOVER=0xFF4B2B3D;
 private static final String[] MODES={"Anchor","Legit","Safe","Double","Crystal","FastPlace","Design"};
 private final SakuraSettings s=SakuraClient.settings;
 private final List<Control> controls=new ArrayList<>();
 private record Control(int x,int y,int w,int h,Supplier<String> text,Runnable action,BooleanSupplier selected){}
 private int tab;
 private float scale,left,top,hoverProgress;
 private int previousScale;
 private boolean dragging;
 private double dragX,dragY;
 private int dragOffsetX,dragOffsetY;
 private long opened=System.nanoTime();
 private KeyBinding recording;
 private String toast="";
 private long toastExpiry;
 public SakuraScreen(){this(0);}
 public SakuraScreen(int initial){super(Text.literal("Sakura Live"));tab=initial;}
 SakuraScreen(int initial,int section){this(initial);}
 @Override protected void init(){
  recording=null;dragging=false;controls.clear();
  previousScale=s.uiScalePercent;
  scale=Math.max(.01f,Math.min(s.uiScalePercent/100f,Math.min((width-16f)/W,(height-16f)/H)));
  float maxX=Math.max(0,(width/scale-W)/2),maxY=Math.max(0,(height/scale-H)/2);
  s.windowOffsetX=(int)Math.max(-maxX,Math.min(maxX,s.windowOffsetX));
  s.windowOffsetY=(int)Math.max(-maxY,Math.min(maxY,s.windowOffsetY));
  left=(width/scale-W)/2+s.windowOffsetX;top=(height/scale-H)/2+s.windowOffsetY;
  btn(617,13,27,25,()->"X",()->close(),()->false);
  for(int i=0;i<MODES.length;i++){
   final int n=i;btn(19+i*90,61,83,29,()->MODES[n],()->{tab=n;init();},()->tab==n);
  }
  if(tab<=3)anchorControls();
  else if(tab==4)crystalControls();
  else if(tab==5)fastControls();
  else designControls();
 }
 private void btn(int x,int y,int w,int h,Supplier<String> text,Runnable action,BooleanSupplier selected){
  controls.add(new Control(x,y,w,h,text,action,selected));
 }
 private void toggle(int x,int y,int w,String title,BooleanSupplier on,Runnable action){
  btn(x,y,w,28,()->title+"  "+(on.getAsBoolean()?"ON":"OFF"),()->{action.run();save();},on);
 }
 private void value(int x,int y,String title,IntSupplier get,IntConsumer set,int min,int max){
  btn(x+176,y,24,24,()->"-",()->{set.accept(Math.max(min,get.getAsInt()-1));save();},()->false);
  btn(x+246,y,24,24,()->"+",()->{set.accept(Math.min(max,get.getAsInt()+1));save();},()->false);
 }
 private void keyControl(int x,int y,KeyBinding key){
  btn(x,y,267,29,()->"HOTKEY  "+key.getBoundKeyLocalizedText().getString(),()->recording=key,()->false);
 }
 private void anchorControls(){
  var o=SakuraClient.optionsFor(tab);
  toggle(35,137,255,"Enabled",()->SakuraClient.enabled(tab),()->SakuraClient.toggle(tab));
  value(35,180,"Place delay",()->o.placeMs,n->o.placeMs=n,10,2000);
  value(35,214,"Charge delay",()->o.chargeMs,n->o.chargeMs=n,10,2000);
  value(35,248,"Switch delay",()->o.switchMs,n->o.switchMs=n,0,500);
  if(tab==3)value(35,282,"Second anchor delay",()->o.repeatMs,n->o.repeatMs=n,10,2000);
  else if(tab==2)value(35,282,"Cover delay",()->o.coverMs,n->o.coverMs=n,10,2000);
  else toggle(35,282,255,"Hold hotkey",()->o.holdToRun,()->o.holdToRun=!o.holdToRun);
  value(342,157,"Anchor slot",()->o.anchorSlot,n->o.anchorSlot=n,1,9);
  value(342,191,"Glowstone slot",()->o.glowSlot,n->o.glowSlot=n,1,9);
  if(tab==3){
   value(342,225,"First trigger slot",()->o.firstFinalSlot,n->o.firstFinalSlot=n,1,9);
   value(342,259,"Second trigger slot",()->o.finalSlot,n->o.finalSlot=n,1,9);
  }else value(342,225,"Final slot",()->o.finalSlot,n->o.finalSlot=n,1,9);
  keyControl(342,309,SakuraClient.keyFor(tab));
 }
 private void crystalControls(){
  toggle(35,137,255,"Enabled",()->s.crystal,()->s.crystal=!s.crystal);
  value(35,184,"Place delay",()->s.crystalPlaceMs,n->s.crystalPlaceMs=n,10,2000);
  value(35,226,"Break delay",()->s.crystalBreakMs,n->s.crystalBreakMs=n,10,2000);
  value(35,268,"Timeout",()->s.crystalTimeoutMs,n->s.crystalTimeoutMs=n,250,10000);
  keyControl(342,184,SakuraClient.crystalKey);
 }
 private void fastControls(){
  toggle(35,137,255,"Enabled",()->s.fastPlace,()->s.fastPlace=!s.fastPlace);
  value(35,184,"Place delay",()->s.placeDelayMs,n->s.placeDelayMs=n,0,200);
  toggle(35,255,255,"Exclude anchors",()->s.excludeAnchors,()->s.excludeAnchors=!s.excludeAnchors);
  keyControl(342,184,SakuraClient.fastKey);
 }
 private void designControls(){
  value(35,166,"UI scale %",()->s.uiScalePercent,n->s.uiScalePercent=n,60,130);
  value(35,215,"Background dim",()->s.overlayDim,n->s.overlayDim=n,0,180);
  toggle(35,268,255,"Animations",()->!s.reduceMotion,()->s.reduceMotion=!s.reduceMotion);
  btn(342,166,267,30,()->"Reset position",()->{s.windowOffsetX=0;s.windowOffsetY=0;save();init();},()->false);
  keyControl(342,215,SakuraClient.menuKey);
  btn(342,268,267,30,()->"All Minecraft keybinds",()->client.setScreen(new KeybindsScreen(this,client.options)),()->false);
 }
 private void save(){
  toast=s.save()?"Saved":"Save failed";toastExpiry=System.nanoTime()+1_500_000_000L;
 }
 private void close(){save();if(client!=null)client.setScreen(null);}
 @Override public void removed(){save();super.removed();}
 @Override public boolean shouldPause(){return false;}
 @Override public boolean keyPressed(KeyInput input){
  if(recording!=null){
   if(input.key()==GLFW.GLFW_KEY_ESCAPE){recording=null;return true;}
   InputUtil.Key key=input.key()==GLFW.GLFW_KEY_BACKSPACE||input.key()==GLFW.GLFW_KEY_DELETE
       ?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(input);
   assign(key);return true;
  }
  return super.keyPressed(input);
 }
 private void assign(InputUtil.Key key){
  recording.setBoundKey(key);
  KeyBinding.updateKeysByCode();client.options.write();
  recording=null;toast="Hotkey saved";toastExpiry=System.nanoTime()+1_500_000_000L;
 }
 private float lx(double x){return (float)(x/scale-left);}
 private float ly(double y){return (float)(y/scale-top);}
 @Override public boolean mouseClicked(Click click,boolean doubleClick){
  if(recording!=null){assign(InputUtil.Type.MOUSE.createFromCode(click.button()));return true;}
  float x=lx(click.x()),y=ly(click.y());
  if(click.button()==GLFW.GLFW_MOUSE_BUTTON_LEFT){
   for(int i=controls.size()-1;i>=0;i--){
    var b=controls.get(i);
    if(x>=b.x&&x<b.x+b.w&&y>=b.y&&y<b.y+b.h){b.action.run();return true;}
   }
   if(x>=16&&x<606&&y>=6&&y<49){
    dragging=true;dragX=click.x();dragY=click.y();dragOffsetX=s.windowOffsetX;dragOffsetY=s.windowOffsetY;return true;
   }
  }
  return super.mouseClicked(click,doubleClick);
 }
 @Override public boolean mouseDragged(Click click,double dx,double dy){
  if(dragging){
   float maxX=Math.max(0,(width/scale-W)/2),maxY=Math.max(0,(height/scale-H)/2);
   s.windowOffsetX=(int)Math.round(Math.max(-maxX,Math.min(maxX,dragOffsetX+(click.x()-dragX)/scale)));
   s.windowOffsetY=(int)Math.round(Math.max(-maxY,Math.min(maxY,dragOffsetY+(click.y()-dragY)/scale)));
   left=(width/scale-W)/2+s.windowOffsetX;top=(height/scale-H)/2+s.windowOffsetY;
   return true;
  }
  return super.mouseDragged(click,dx,dy);
 }
 @Override public boolean mouseReleased(Click click){
  if(dragging){dragging=false;save();return true;}
  return super.mouseReleased(click);
 }
 private void text(DrawContext c,String str,int x,int y,int color){
  c.drawTextWithShadow(textRenderer,Text.literal(str),x,y,color);
 }
 private void round(DrawContext c,int x,int y,int w,int h,int r,int color){
  if(w<=0||h<=0)return;
  r=Math.min(r,Math.min(w,h)/2);
  c.fill(x,y+r,x+w,y+h-r,color);
  for(int i=0;i<r;i++){
   double d=r-i-.5;
   int inset=(int)Math.ceil(r-Math.sqrt(r*r-d*d));
   c.fill(x+inset,y+i,x+w-inset,y+i+1,color);
   c.fill(x+inset,y+h-i-1,x+w-inset,y+h-i,color);
  }
 }
 private void drawValue(DrawContext c,int x,int y,String label,int value,String suffix){
  text(c,label,x,y+8,WHITE);text(c,value+suffix,x+213,y+8,PALE);
 }
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  c.fill(0,0,width,height,s.overlayDim<<24);
  c.getMatrices().pushMatrix();
  c.getMatrices().scale(scale,scale);c.getMatrices().translate(left,top);
  if(!s.reduceMotion){
   long n=System.nanoTime();float t=Math.min(1f,(n-opened)/220_000_000f);
   hoverProgress+=((1-t)-hoverProgress)*.15f;
  }
  round(c,-2,-2,W+4,H+4,21,0x553C1929);
  round(c,0,0,W,H,20,EDGE);round(c,1,1,W-2,H-2,19,BG);
  round(c,17,12,5,27,3,PINK);text(c,"SAKURA",31,21,PINK);
  text(c,"LIVE / CLIENT",93,21,WHITE);
  text(c,"PINK  •  FABRIC 1.21.11",411,22,MUTED);
  round(c,17,53,626,46,12,CARD);
  round(c,17,111,297,254,15,CARD);
  round(c,326,111,317,254,15,CARD);
  round(c,17,377,626,46,12,CARD);
  float x=lx(mouseX),y=ly(mouseY);
  for(var b:controls){
   boolean over=x>=b.x&&x<b.x+b.w&&y>=b.y&&y<b.y+b.h;
   boolean selected=b.selected.getAsBoolean();
   int base=selected?PINK:over?HOVER:BUTTON;
   round(c,b.x,b.y,b.w,b.h,Math.min(11,b.h/2),base);
   String label=b.text.get();
   int textW=textRenderer.getWidth(label);
   text(c,label,b.x+(b.w-textW)/2,b.y+(b.h-9)/2,selected?BG:WHITE);
  }
  if(tab<=3){
   var o=SakuraClient.optionsFor(tab);
   text(c,"ANCHOR / TIMING",34,119,PALE);text(c,"HOTBAR / CONTROLS",342,119,PALE);
   drawValue(c,35,180,"Place",o.placeMs," ms");
   drawValue(c,35,214,"Charge",o.chargeMs," ms");
   drawValue(c,35,248,"Switch",o.switchMs," ms");
   if(tab==3)drawValue(c,35,282,"Repeat",o.repeatMs," ms");
   else if(tab==2)drawValue(c,35,282,"Cover",o.coverMs," ms");
   drawValue(c,342,157,"Anchor",o.anchorSlot,"");
   drawValue(c,342,191,"Glowstone",o.glowSlot,"");
   if(tab==3){
    drawValue(c,342,225,"First trigger",o.firstFinalSlot,"");
    drawValue(c,342,259,"Second trigger",o.finalSlot,"");
   }else drawValue(c,342,225,"Final slot",o.finalSlot,"");
  }else if(tab==4){
   text(c,"CRYSTAL / TIMING",34,119,PALE);text(c,"CONTROLS",342,119,PALE);
   drawValue(c,35,184,"Place",s.crystalPlaceMs," ms");
   drawValue(c,35,226,"Break",s.crystalBreakMs," ms");
   drawValue(c,35,268,"Timeout",s.crystalTimeoutMs," ms");
   text(c,"Hold right mouse + crystal in hand",342,151,MUTED);
  }else if(tab==5){
   text(c,"FAST PLACE",34,119,PALE);text(c,"CONTROLS",342,119,PALE);
   drawValue(c,35,184,"Delay",s.placeDelayMs," ms");
  }else{
   text(c,"DESIGN / PERSONALIZE",34,119,PALE);text(c,"PREFERENCES",342,119,PALE);
   drawValue(c,35,166,"Scale",s.uiScalePercent,"%");
   drawValue(c,35,215,"Dim",s.overlayDim,"");
   text(c,"Drag the top bar to move this window",342,323,MUTED);
  }
  text(c,"G / H / RIGHT SHIFT can be changed per module",31,393,MUTED);
  String status=System.nanoTime()<toastExpiry?toast:SakuraClient.lastStatus;
  if(status.length()>38)status=status.substring(0,35)+"...";
  text(c,status,350,393,PALE);
  if(recording!=null){
   round(c,105,119,449,233,16,0xF20C090C);round(c,120,136,420,198,14,CARD);
   text(c,"SET HOTKEY",147,169,PINK);
   text(c,"Press a keyboard key or mouse button",147,207,WHITE);
   text(c,"Esc cancels  /  Backspace clears",147,238,MUTED);
  }
  c.getMatrices().popMatrix();
 }
}
