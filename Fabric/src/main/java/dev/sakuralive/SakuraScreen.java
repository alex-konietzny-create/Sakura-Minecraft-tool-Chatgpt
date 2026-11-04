package dev.sakuralive;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public final class SakuraScreen extends Screen {
 private static final int PINK=0xFFFF5098, INK=0xFFFFEAF2, MUTED=0xFFAB939E;
 private final SakuraSettings s=SakuraClient.settings;
 private final List<Control> controls=new ArrayList<>();
 private int left,top,tab;
 private float scale=1;
 private long opened=System.nanoTime(),lastFrame=opened;
 private record Control(int x,int y,int w,int h,Supplier<String> label,BooleanSupplier on,float[] hover,ButtonWidget widget){}
 public SakuraScreen(){super(Text.literal("Sakura Live"));}
 @Override protected void init(){
  controls.clear();
  scale=Math.max(0.1f,Math.min(1f,Math.min((width-16)/620f,(height-16)/380f)));
  left=(int)((width/scale-620)/2);top=(int)((height/scale-380)/2);
  add(22,105,133,32,()->"01  Anchor",()->tab==0,()->select(0));
  add(22,146,133,32,()->"02  Crystal",()->tab==1,()->select(1));
  add(22,187,133,32,()->"03  Utility",()->tab==2,()->select(2));
  add(22,296,133,30,()->"Edit all hotkeys",()->false,()->client.setScreen(new KeybindsScreen(this,client.options)));
  add(570,18,30,26,()->"X",()->false,this::close);
  if(tab==0){
   toggle(194,107,"Anchor macro",()->s.anchor,()->s.anchor=!s.anchor);
   number(194,154,"Anchor slot",()->s.anchorSlot,n->s.anchorSlot=n,1,9);
   number(194,191,"Glowstone slot",()->s.glowSlot,n->s.glowSlot=n,1,9);
   number(194,228,"Final slot",()->s.totemSlot,n->s.totemSlot=n,1,9);
   number(194,265,"Step delay / ticks",()->s.actionDelay,n->s.actionDelay=n,1,20);
  }else if(tab==1){
   toggle(194,107,"Crystal assist",()->s.crystal,()->s.crystal=!s.crystal);
   number(194,161,"Place interval / ticks",()->s.crystalPlaceDelay,n->s.crystalPlaceDelay=n,1,20);
   number(194,202,"Break delay / ticks",()->s.crystalBreakDelay,n->s.crystalBreakDelay=n,1,20);
  }else{
   toggle(194,107,"Fast place",()->s.fastPlace,()->s.fastPlace=!s.fastPlace);
   toggle(194,154,"Exclude anchors",()->s.excludeAnchors,()->s.excludeAnchors=!s.excludeAnchors);
   number(194,201,"Place delay / ticks",()->s.placeDelay,n->s.placeDelay=n,0,4);
  }
 }
 private void select(int value){tab=value;clearChildren();init();}
 private void toggle(int x,int y,String label,BooleanSupplier get,Runnable set){
  add(x,y,390,30,()->label+"   /   "+(get.getAsBoolean()?"ON":"OFF"),get,()->{set.run();s.save();});
 }
 private void number(int x,int y,String name,IntSupplier get,IntConsumer set,int min,int max){
  add(x,y,312,28,()->name+"   "+get.getAsInt(),()->false,()->{});
  add(x+320,y,31,28,()->"-",()->false,()->{set.accept(Math.max(min,get.getAsInt()-1));s.save();});
  add(x+359,y,31,28,()->"+",()->false,()->{set.accept(Math.min(max,get.getAsInt()+1));s.save();});
 }
 private void add(int x,int y,int w,int h,Supplier<String> label,BooleanSupplier on,Runnable action){
  int ax=left+x,ay=top+y;
  var widget=addDrawableChild(ButtonWidget.builder(Text.literal(label.get()),b->action.run())
   .dimensions(Math.round(ax*scale),Math.round(ay*scale),Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale))).build());
  controls.add(new Control(ax,ay,w,h,label,on,new float[]{0},widget));
 }
 private void text(DrawContext c,String str,int x,int y,int color){c.drawTextWithShadow(textRenderer,Text.literal(str),left+x,top+y,color);}
 private void round(DrawContext c,int x,int y,int w,int h,int r,int color){
  c.fill(x,y+r,x+w,y+h-r,color);
  for(int i=0;i<r;i++){
   int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-0.5)*(r-i-0.5)));
   c.fill(x+inset,y+i,x+w-inset,y+i+1,color);
   c.fill(x+inset,y+h-i-1,x+w-inset,y+h-i,color);
  }
 }
 private void panel(DrawContext c,int x,int y,int w,int h,int color){round(c,left+x,top+y,w,h,14,color);}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  long now=System.nanoTime();
  float dt=Math.min(0.1f,(now-lastFrame)/1_000_000_000f);lastFrame=now;
  float reveal=Math.min(1,(now-opened)/350_000_000f);
  c.fill(0,0,width,height,0xB8000000);
  c.getMatrices().pushMatrix();c.getMatrices().scale(scale,scale);
  panel(c,-5,4,630,383,0x50000000);
  panel(c,0,0,620,380,0xFF100D10);
  panel(c,12,65,155,278,0xFF191418);
  panel(c,179,65,429,278,0xFF1D161B);
  round(c,left+23,top+22,(int)(121*(1-Math.pow(1-reveal,3))),3,1,PINK);
  text(c,"SAKURA",23,35,PINK);text(c,"LIVE / PINK EDITION",185,29,INK);
  text(c,"YOUR RHYTHM. YOUR CONTROLS.",185,44,MUTED);
  text(c,"WORKSPACE",24,82,MUTED);
  text(c,tab==0?"Anchor sequence":tab==1?"Crystal assist":"Utility",194,82,INK);
  if(tab==1){
   text(c,"Hold crystals in MAIN HAND + hold RIGHT MOUSE.",194,251,MUTED);
   text(c,"Aim at obsidian / bedrock or an existing crystal.",194,269,MUTED);
   text(c,"Release, open a menu or switch item to stop.",194,287,MUTED);
  }else if(tab==0){
   text(c,"Aim at the placed anchor for the next steps.",194,309,MUTED);
  }else{
   text(c,"20 ticks = 1 second. Settings save automatically.",194,254,MUTED);
  }
  String binding=(tab==0?SakuraClient.anchorKey:tab==1?SakuraClient.crystalKey:SakuraClient.fastKey).getBoundKeyLocalizedText().getString();
  text(c,"HOTKEY  "+binding,194,326,PINK);
  text(c,"1.21.11 / FABRIC",23,355,MUTED);
  text(c,"Menu: "+SakuraClient.menuKey.getBoundKeyLocalizedText().getString(),340,355,MUTED);
  for(Control p:controls){
   boolean hover=(mx/scale>=p.x&&mx/scale<p.x+p.w&&my/scale>=p.y&&my/scale<p.y+p.h)||p.widget.isFocused();
   p.hover[0]+=( (hover?1:0)-p.hover[0])*(1-(float)Math.exp(-dt*14));
   int shade=(int)(22*p.hover[0]);
   int color=p.on.getAsBoolean()?0xFF9C305E:0xFF30222B;
   int red=Math.min(255,((color>>16)&255)+shade),green=Math.min(255,((color>>8)&255)+shade),blue=Math.min(255,(color&255)+shade);
   round(c,p.x,p.y,p.w,p.h,9,0xFF000000|(red<<16)|(green<<8)|blue);
   if(hover)round(c,p.x+6,p.y+p.h-3,(int)((p.w-12)*p.hover[0]),2,1,PINK);
   String label=p.label.get();p.widget.setMessage(Text.literal(label));
   c.drawTextWithShadow(textRenderer,Text.literal(label),p.x+(p.w-textRenderer.getWidth(label))/2,p.y+(p.h-8)/2,INK);
  }
  c.getMatrices().popMatrix();
 }
 @Override public void close(){s.save();super.close();}
 @Override public boolean shouldPause(){return false;}
}
