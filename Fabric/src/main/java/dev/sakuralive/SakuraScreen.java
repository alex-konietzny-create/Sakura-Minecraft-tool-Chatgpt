package dev.sakuralive;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

/**
 * Logical 760 x 480 canvas. Rendering and every pointer event share the same
 * transform, including the opening motion. Native widgets retain keyboard focus,
 * click sounds, narration and slider dragging; their skins are drawn here.
 */
public final class SakuraScreen extends Screen {
 private static final int W=760,H=480;
 private static final int PINK=0xFFFF659F, WHITE=0xFFFFEDF4, MUTED=0xFFB49EA9;
 private static final int CARD=0xFF1C171C, BORDER=0xFF382630;
 private final SakuraSettings s=SakuraClient.settings;
 private final List<Control> controls=new ArrayList<>();
 private final List<DelaySlider> sliders=new ArrayList<>();
 private float scale=1,left,top,dt,railY=112;
 private int tab;
 private long opened=System.nanoTime(),pageOpened=opened,lastFrame=opened,noticeUntil;
 private KeyBinding capture;
 private String notice="";
 private boolean dirty;
 private enum Skin { NAV, HERO, SWITCH, KEY, SLOT, BUTTON }
 private record Control(ButtonWidget widget,Skin skin,Supplier<String> title,
   Supplier<String> detail,BooleanSupplier selected,int index,float[] tween) {}
 public SakuraScreen(){super(Text.literal("Sakura Live · Pink"));}
 @Override protected void init(){
  controls.clear();sliders.clear();capture=null;
  scale=Math.max(0.01f,Math.min(1.15f,Math.min((width-20f)/W,(height-20f)/H)));
  left=(width/scale-W)/2;top=(height/scale-H)/2;
  String[] names={"Anchor","Crystal","Utility"};
  String[] desc={"Sequenz & Hotbar","Platzieren & Abbauen","Fast Place & Bewegung"};
  for(int i=0;i<3;i++){
   final int n=i;
   button(27,112+i*54,126,44,Skin.NAV,()->names[n],()->desc[n],()->tab==n,n,()->select(n));
  }
  button(720,21,22,24,Skin.BUTTON,()->"×",()->"Menü schließen",()->false,0,this::close);
  button(29,354,122,30,Skin.KEY,()->"Menü: "+keyName(SakuraClient.menuKey),
    ()->conflict(SakuraClient.menuKey),()->capture==SakuraClient.menuKey,0,()->capture=SakuraClient.menuKey);
  button(29,402,122,28,Skin.BUTTON,()->"Alle Hotkeys",()->"Minecraft-Tastenbelegung öffnen",()->false,0,
    ()->client.setScreen(new KeybindsScreen(this,client.options)));
  button(190,101,546,58,Skin.HERO,()->tab==0?"Anchor-Makro":tab==1?"Crystal-Assistent":"Fast Place",
    this::moduleHint,this::enabled,0,()->{
     if(tab==0)s.anchor=!s.anchor;else if(tab==1)s.crystal=!s.crystal;else s.fastPlace=!s.fastPlace;
     changed();
    });
  if(tab==0){
   slots(246,()->s.anchorSlot,n->s.anchorSlot=n,"Anchor");
   slots(300,()->s.glowSlot,n->s.glowSlot=n,"Glowstone");
   slots(354,()->s.totemSlot,n->s.totemSlot=n,"Finaler Slot");
   delay(486,224,234,"Schritt-Delay",()->s.actionDelay,n->s.actionDelay=n,1,20);
   bind(486,338,234);
  }else if(tab==1){
   delay(206,226,234,"Platzierpause",()->s.crystalPlaceDelay,n->s.crystalPlaceDelay=n,1,20);
   delay(206,314,234,"Abbaupause",()->s.crystalBreakDelay,n->s.crystalBreakDelay=n,1,20);
   bind(486,225,234);
  }else{
   delay(206,226,234,"Platzierpause",()->s.placeDelay,n->s.placeDelay=n,0,4);
   button(206,320,234,54,Skin.SWITCH,()->"Anchor ausschließen",()->"Anchor & Glowstone ausnehmen",
     ()->s.excludeAnchors,0,()->{s.excludeAnchors=!s.excludeAnchors;changed();});
   bind(486,225,234);
   button(486,320,234,54,Skin.SWITCH,()->"Animationen",()->s.reduceMotion?"Bewegung reduziert":"Weiche Übergänge",
     ()->!s.reduceMotion,0,()->{s.reduceMotion=!s.reduceMotion;changed();});
  }
 }
 private void select(int next){
  if(next==tab)return;
  flush();tab=next;pageOpened=System.nanoTime();clearAndInit();
 }
 private void button(int x,int y,int w,int h,Skin skin,Supplier<String> title,Supplier<String> detail,
                     BooleanSupplier selected,int index,Runnable action){
  var widget=addDrawableChild(ButtonWidget.builder(Text.literal(title.get()),b->action.run()).dimensions(x,y,w,h).build());
  controls.add(new Control(widget,skin,title,detail,selected,index,new float[]{0,selected.getAsBoolean()?1:0}));
 }
 private void slots(int y,IntSupplier get,IntConsumer set,String label){
  for(int i=1;i<=9;i++){
   final int slot=i;
   button(206+(i-1)*26,y,24,30,Skin.SLOT,()->Integer.toString(slot),
     ()->label+" auf Hotbar-Slot "+slot+" setzen",()->get.getAsInt()==slot,slot,
     ()->{set.accept(slot);changed();});
  }
 }
 private void bind(int x,int y,int w){
  button(x,y,w,32,Skin.KEY,()->keyName(moduleKey()),()->"Klicken und Taste oder Maustaste drücken",
    ()->capture==moduleKey(),0,()->capture=moduleKey());
 }
 private void delay(int x,int y,int w,String name,IntSupplier get,IntConsumer set,int min,int max){
  sliders.add(addDrawableChild(new DelaySlider(x,y,w,name,get,set,min,max)));
 }
 private KeyBinding moduleKey(){return tab==0?SakuraClient.anchorKey:tab==1?SakuraClient.crystalKey:SakuraClient.fastKey;}
 private boolean enabled(){return tab==0?s.anchor:tab==1?s.crystal:s.fastPlace;}
 private String keyName(KeyBinding key){return key.getBoundKeyLocalizedText().getString();}
 private String conflict(KeyBinding key){
  if(key.isUnbound())return "Nicht belegt";
  for(var other:client.options.allKeys)
   if(other!=key&&!other.isUnbound()&&key.equals(other))return "Konflikt: "+Text.translatable(other.getTranslationKey()).getString();
  return "Klicken zum Ändern · Esc bricht ab";
 }
 private String moduleHint(){
  if(!enabled())return "Ausgeschaltet · hier aktivieren";
  if(client.player==null)return "Aktiviert · keine Welt geöffnet";
  if(tab==1&&!client.player.getMainHandStack().isOf(Items.END_CRYSTAL))return "Aktiviert · Kristalle in die Haupthand nehmen";
  if(tab==0&&!client.player.getInventory().getStack(s.anchorSlot-1).isOf(Items.RESPAWN_ANCHOR))return "Aktiviert · im Anchor-Slot fehlt ein Anchor";
  if(tab==0&&!client.player.getInventory().getStack(s.glowSlot-1).isOf(Items.GLOWSTONE))return "Aktiviert · im Glowstone-Slot fehlt Glowstone";
  return tab==0?"Aktiviert · Starten / Abbrechen mit "+keyName(moduleKey()):
         tab==1?"Aktiviert · rechte Maustaste zum Ausführen halten":"Aktiviert · beim Halten der Benutzen-Taste";
 }
 private void changed(){dirty=true;flush();}
 private void flush(){
  if(!dirty)return;
  boolean ok=s.save();dirty=!ok;
  notifyUser(ok?"Einstellungen gespeichert":"Speichern fehlgeschlagen · siehe Log");
 }
 private void notifyUser(String message){notice=message;noticeUntil=System.nanoTime()+2_000_000_000L;}
 private void assign(InputUtil.Key key){
  capture.setBoundKey(key);KeyBinding.updateKeysByCode();client.options.write();
  notifyUser("Hotkey gesetzt: "+keyName(capture));capture=null;
 }
 @Override public boolean keyPressed(KeyInput input){
  if(capture!=null){
   if(input.key()==GLFW.GLFW_KEY_ESCAPE){capture=null;return true;}
   assign(input.key()==GLFW.GLFW_KEY_DELETE||input.key()==GLFW.GLFW_KEY_BACKSPACE
     ?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(input));return true;
  }
  return super.keyPressed(input);
 }
 private float introOffset(){
  if(s.reduceMotion)return 0;
  float t=Math.min(1,(System.nanoTime()-opened)/260_000_000f);
  return 16*(1-t)*(1-t)*(1-t);
 }
 private Click local(Click click){return new Click(click.x()/scale-left,click.y()/scale-top-introOffset(),click.buttonInfo());}
 @Override public boolean mouseClicked(Click click,boolean doubled){
  if(capture!=null){assign(InputUtil.Type.MOUSE.createFromCode(click.button()));return true;}
  return super.mouseClicked(local(click),doubled);
 }
 @Override public boolean mouseReleased(Click click){
  boolean result=super.mouseReleased(local(click));flush();return result;
 }
 @Override public boolean mouseDragged(Click click,double dx,double dy){
  if(capture!=null)return true;
  return super.mouseDragged(local(click),dx/scale,dy/scale);
 }
 @Override public void removed(){capture=null;flush();super.removed();}
 @Override public void close(){capture=null;flush();super.close();}
 @Override public boolean shouldPause(){return false;}

 private static float ease(float from,float to,float rate){return from+(to-from)*rate;}
 private float rate(){return s.reduceMotion?1:1-(float)Math.exp(-dt*16);}
 private static int blend(int a,int b,float t){
  t=Math.max(0,Math.min(1,t));int result=0;
  for(int shift=0;shift<=24;shift+=8)result|=((int)(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t))<<shift;
  return result;
 }
 private void round(DrawContext c,int x,int y,int w,int h,int radius,int color){
  if(w<=0||h<=0)return;
  int r=Math.min(radius,Math.min(w,h)/2);
  c.fill(x,y+r,x+w,y+h-r,color);
  for(int i=0;i<r;i++){
   int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-0.5)*(r-i-0.5)));
   c.fill(x+inset,y+i,x+w-inset,y+i+1,color);
   c.fill(x+inset,y+h-i-1,x+w-inset,y+h-i,color);
  }
 }
 private void panel(DrawContext c,int x,int y,int w,int h){
  round(c,x,y,w,h,14,BORDER);round(c,x+1,y+1,w-2,h-2,13,CARD);
 }
 private void label(DrawContext c,String text,int x,int y,int color){c.drawText(textRenderer,text,x,y,color,false);}
 private String fit(String text,int w){return textRenderer.getWidth(text)<=w?text:textRenderer.trimToWidth(text,Math.max(0,w-12))+"…";}
 private void small(DrawContext c,String text,int x,int y,int color,int w){label(c,fit(text,w),x,y,color);}
 private void title(DrawContext c,String text,int x,int y,float size,int color){
  c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(size,size);
  label(c,text,0,0,color);c.getMatrices().popMatrix();
 }
 private void switchTrack(DrawContext c,int x,int y,float value){
  round(c,x-2,y-2,48,24,12,blend(0x00382A33,0x334E283A,value));
  round(c,x,y,44,20,10,blend(0xFF42333D,0xFFEA508C,value));
  round(c,x+3+Math.round(24*value),y+3,14,14,7,WHITE);
 }
 private void blossom(DrawContext c,int x,int y){
  // Five geometric petals, original mark rendered without external textures.
  int[][] petals={{0,-7},{7,-2},{4,6},{-4,6},{-7,-2}};
  for(int[] p:petals)round(c,x+p[0]-4,y+p[1]-4,8,8,4,PINK);
  round(c,x-3,y-3,6,6,3,0xFFFFCBDF);
 }
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  long now=System.nanoTime();dt=Math.min(0.1f,(now-lastFrame)/1_000_000_000f);lastFrame=now;
  float offset=introOffset();
  int mx=(int)(mouseX/scale-left),my=(int)(mouseY/scale-top-offset);
  c.fill(0,0,width,height,0xB5000000);
  c.getMatrices().pushMatrix();c.getMatrices().scale(scale,scale);c.getMatrices().translate(left,top+offset);
  for(int i=8;i>=1;i--)round(c,-i,2-i,W+i*2,H+i*2,23+i,0x05000000);
  round(c,0,0,W,H,22,0xFF4C303E);round(c,1,1,W-2,H-2,21,0xFF110F13);
  round(c,13,13,154,454,14,0xFF19151B);
  blossom(c,43,45);title(c,"SAKURA",64,36,1.12f,WHITE);
  label(c,"LIVE / PINK",64,53,MUTED);
  label(c,"MODULE",29,91,0xFF897581);
  railY=ease(railY,112+tab*54,rate());
  round(c,19,Math.round(railY+10),3,24,1,PINK);
  int active=(s.anchor?1:0)+(s.crystal?1:0)+(s.fastPlace?1:0);
  round(c,29,294,122,28,8,0xFF251B24);
  round(c,39,304,7,7,3,active>0?PINK:0xFF75636F);
  label(c,active+" / 3 aktiviert",53,304,MUTED);
  label(c,"MENÜ-HOTKEY",29,337,0xFF897581);
  label(c,"TASTENBELEGUNG",29,389,0xFF897581);
  label(c,"v0.2  /  1.21.11",29,448,0xFF897581);
  label(c,"DEIN SETUP / "+(tab==2?"UTILITY":"COMBAT"),192,27,MUTED);
  title(c,tab==0?"Anchor":tab==1?"Crystal":"Utility",190,45,2.1f,WHITE);
  label(c,tab==0?"Drei Schritte. Dein Timing.":tab==1?"Halten. Platzieren. Abbauen.":"Weniger Klicks. Mehr Kontrolle.",192,74,MUTED);
  round(c,640,48,94,22,7,0xFF2A1A25);label(c,"PINK EDITION",650,55,PINK);
  c.fill(190,88,736,89,0xFF30232D);
  if(tab==0){
   panel(c,190,175,266,226);label(c,"01 / HOTBAR",206,193,MUTED);
   label(c,"Wähle die Slots deiner Sequenz.",206,211,MUTED);
   label(c,"Anchor",206,232,WHITE);label(c,"Glowstone",206,286,WHITE);label(c,"Finaler Slot",206,340,WHITE);
   panel(c,470,175,266,110);label(c,"02 / TIMING",486,193,MUTED);
   panel(c,470,297,266,104);label(c,"03 / STARTEN & ABBRECHEN",486,315,MUTED);
  }else{
   panel(c,190,175,266,226);label(c,tab==1?"01 / TIMING":"01 / PLATZIEREN",206,193,MUTED);
   label(c,tab==1?"Getrennte Pausen für beide Schritte.":"Abstand zwischen Benutzungen.",206,211,MUTED);
   panel(c,470,175,266,226);label(c,"02 / HOTKEY & VERHALTEN",486,193,MUTED);
   if(tab==1){
    label(c,"SO FUNKTIONIERT'S",486,296,MUTED);
    label(c,"1  Kristalle in die Haupthand",486,319,WHITE);
    label(c,"2  Auf Obsidian / Bedrock zielen",486,339,WHITE);
    label(c,"3  Rechte Maustaste halten",486,359,WHITE);
    label(c,"Loslassen stoppt den Assistenten.",486,381,MUTED);
   }
  }
  String hoverHint="";
  for(Control p:controls){
   var w=p.widget;int x=w.getX(),y=w.getY(),bw=w.getWidth(),bh=w.getHeight();
   boolean over=w.isMouseOver(mx,my)||w.isFocused();
   p.tween[0]=ease(p.tween[0],over?1:0,rate());p.tween[1]=ease(p.tween[1],p.selected.getAsBoolean()?1:0,rate());
   float hover=p.tween[0],on=p.tween[1];
   String text=p.title.get(),detail=p.detail.get();w.setMessage(Text.literal(text+". "+detail));
   if(over)hoverHint=detail;
   if(p.skin==Skin.HERO){
    round(c,x,y,bw,bh,13,blend(BORDER,0xFFAF456E,on));
    round(c,x+1,y+1,bw-2,bh-2,12,blend(0xFF201920,0xFF34202C,on));
    label(c,text,x+16,y+14,WHITE);small(c,detail,x+16,y+34,MUTED,420);
    label(c,p.selected.getAsBoolean()?"AN":"AUS",x+bw-101,y+25,p.selected.getAsBoolean()?PINK:MUTED);
    switchTrack(c,x+bw-60,y+19,on);
   }else if(p.skin==Skin.NAV){
    round(c,x,y,bw,bh,9,blend(blend(0xFF19151B,0xFF2A2029,hover),0xFF3A2030,on));
    label(c,"0"+(p.index+1),x+9,y+10,blend(MUTED,PINK,on));
    label(c,text,x+31,y+9,blend(MUTED,WHITE,on));
    small(c,detail,x+31,y+26,MUTED,bw-36);
   }else if(p.skin==Skin.SWITCH){
    round(c,x,y,bw,bh,9,blend(0xFF261E26,0xFF342431,hover));
    small(c,text,x+10,y+11,WHITE,bw-68);small(c,detail,x+10,y+34,MUTED,bw-18);
    switchTrack(c,x+bw-54,y+8,on);
   }else if(p.skin==Skin.SLOT){
    round(c,x,y,bw,bh,6,blend(blend(0xFF2A212B,0xFF463140,hover),0xFFA83D69,on));
    label(c,text,x+3,y+3,WHITE);
    if(client.player!=null){
     var item=client.player.getInventory().getStack(p.index-1);
     if(!item.isEmpty()){
      c.getMatrices().pushMatrix();c.getMatrices().translate(x+7,y+13);c.getMatrices().scale(0.7f,0.7f);
      c.drawItem(item,0,0);c.getMatrices().popMatrix();
     }
    }
   }else{
    round(c,x,y,bw,bh,8,blend(0xFF35232F,0xFF583248,hover));
    String fitted=fit(text,bw-18);
    label(c,fitted,x+(bw-textRenderer.getWidth(fitted))/2,y+(bh-8)/2,p.skin==Skin.KEY?PINK:WHITE);
   }
   if(w.isFocused())c.drawStrokedRectangle(x-2,y-2,bw+4,bh+4,0xFFFFB7D3);
  }
  for(var slider:sliders){
   slider.render(c,mx,my,delta);
   if(slider.isMouseOver(mx,my)||slider.isFocused())hoverHint="Ziehen oder mit Tab auswählen und Pfeiltasten benutzen.";
  }
  int keyY=tab==0?380:270;
  small(c,conflict(moduleKey()),486,keyY,moduleKey().isUnbound()?0xFFE8AE72:conflict(moduleKey()).startsWith("Konflikt")?0xFFFFAA8B:MUTED,234);
  if(tab==0&&s.anchorSlot==s.glowSlot)small(c,"Anchor und Glowstone brauchen eigene Slots.",206,385,0xFFFFAA8B,234);
  float page=s.reduceMotion?1:Math.min(1,(now-pageOpened)/180_000_000f);
  if(page<1)round(c,188,173,550,230,14,((int)(180*(1-page))<<24)|0x00110F13);
  round(c,190,417,546,42,11,0xFF211A22);
  round(c,203,430,6,6,3,now<noticeUntil?PINK:0xFFE8AE72);
  small(c,now<noticeUntil?notice:"Menü geöffnet · Makros pausieren",217,426,WHITE,501);
  small(c,hoverHint.isEmpty()?"Esc schließt das Menü · Änderungen werden gespeichert.":hoverHint,217,443,MUTED,501);
  if(capture!=null){
   round(c,181,92,564,316,15,0xF5110F13);
   panel(c,238,183,450,158);blossom(c,269,213);
   title(c,"Hotkey zuweisen",291,205,1.3f,WHITE);
   small(c,Text.translatable(capture.getTranslationKey()).getString(),260,239,MUTED,405);
   label(c,"Jetzt eine Taste oder Maustaste drücken.",260,265,WHITE);
   label(c,"Esc: abbrechen   /   Entf: Belegung entfernen",260,296,MUTED);
   label(c,"Menü und Makros bleiben währenddessen pausiert.",260,317,MUTED);
  }
  c.getMatrices().popMatrix();
 }
 private final class DelaySlider extends SliderWidget {
  private final String name;
  private final IntSupplier get;
  private final IntConsumer set;
  private final int min,max;
  private float visual;
  DelaySlider(int x,int y,int w,String name,IntSupplier get,IntConsumer set,int min,int max){
   super(x,y,w,61,Text.literal(name),(get.getAsInt()-min)/(double)(max-min));
   this.name=name;this.get=get;this.set=set;this.min=min;this.max=max;visual=(float)value;updateMessage();
  }
  @Override protected void updateMessage(){
   if(get!=null)setMessage(Text.literal(name+": "+get.getAsInt()+" Ticks, "+get.getAsInt()*50+" Millisekunden"));
  }
  @Override protected void applyValue(){
   int next=min+(int)Math.round(value*(max-min));
   if(get.getAsInt()!=next){set.accept(next);dirty=true;}
   updateMessage();
  }
  @Override public void onRelease(Click click){super.onRelease(click);flush();}
  @Override public boolean keyPressed(KeyInput input){
   if(input.key()==GLFW.GLFW_KEY_LEFT||input.key()==GLFW.GLFW_KEY_RIGHT){
    int next=Math.max(min,Math.min(max,get.getAsInt()+(input.key()==GLFW.GLFW_KEY_RIGHT?1:-1)));
    if(next!=get.getAsInt()){set.accept(next);value=(next-min)/(double)(max-min);dirty=true;updateMessage();flush();}
    return true;
   }
   return super.keyPressed(input);
  }
  @Override public void renderWidget(DrawContext c,int mx,int my,float delta){
   int x=getX(),y=getY(),w=getWidth(),n=get.getAsInt();
   visual=ease(visual,(n-min)/(float)(max-min),rate());
   label(c,name,x,y,WHITE);
   String valueText=n+" t  /  "+n*50+" ms";
   label(c,valueText,x+w-textRenderer.getWidth(valueText),y,PINK);
   round(c,x+4,y+25,w-8,5,2,0xFF43303F);
   round(c,x+4,y+25,Math.max(1,Math.round((w-8)*visual)),5,2,PINK);
   for(int i=0;i<=max-min;i++){
    int px=x+4+Math.round((w-8)*i/(float)(max-min));
    round(c,px,y+36,1,2,0,0xFF6C4B5B);
   }
   int knob=x+4+Math.round((w-8)*visual);
   round(c,knob-8,y+19,16,17,8,0x335C2840);
   round(c,knob-5,y+22,10,11,5,WHITE);
   label(c,Integer.toString(min),x,y+48,MUTED);label(c,Integer.toString(max),x+w-textRenderer.getWidth(Integer.toString(max)),y+48,MUTED);
   if(isFocused())c.drawStrokedRectangle(x-3,y-4,w+6,66,0xFFFFB7D3);
  }
 }
}
