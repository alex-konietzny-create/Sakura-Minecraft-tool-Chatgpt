package dev.sakuralive;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Sakura Live in-game control panel. All colours are pink or neutral; no purple. */
public final class SakuraScreen extends Screen {
 private static final int BACK = 0xFF0D0A0C;
 private static final int CARD = 0xFF1C1419;
 private static final int CARD_HOVER = 0xFF2C1923;
 private static final int PINK = 0xFFFF4D9B;
 private static final int PINK_LIGHT = 0xFFFFA4CD;
 private static final int TEXT = 0xFFFFEAF2;
 private static final int MUTED = 0xFFB99DA9;
 private static final int OFF = 0xFF3C2A32;
 private final List<Pill> pills = new ArrayList<>();
 private int left, top;
 private final SakuraSettings s = SakuraClient.settings;
 private record Pill(int x,int y,int w,int h,Supplier<String> label,boolean active) {}
 public SakuraScreen(){super(Text.literal("Sakura Live"));}

 @Override protected void init() {
  pills.clear();
  left=(width-600)/2;
  top=(height-350)/2;
  // Cards: Anchor, Crystal, FastPlace
  toggle(left+192,top+75,79,24,()->s.anchor,()->s.anchor=!s.anchor);
  toggle(left+493,top+75,79,24,()->s.crystal,()->s.crystal=!s.crystal);
  field(left+18,top+122,"",()->s.anchorSlot,n->s.anchorSlot=n,1,9);
  field(left+18,top+152,"",()->s.glowSlot,n->s.glowSlot=n,1,9);
  field(left+18,top+182,"",()->s.totemSlot,n->s.totemSlot=n,1,9);
  field(left+302,top+122,"",()->s.crystalSlot,n->s.crystalSlot=n,1,9);
  field(left+302,top+152,"",()->s.actionDelay,n->s.actionDelay=n,1,20);
  toggle(left+145,top+263,90,24,()->s.fastPlace,()->s.fastPlace=!s.fastPlace);
  toggle(left+370,top+263,105,24,()->s.excludeAnchors,()->s.excludeAnchors=!s.excludeAnchors);
  field(left+302,top+296,"",()->s.placeDelay,n->s.placeDelay=n,0,4);
  add(left+548,top+10,34,29,()->"X",()->close());
 }
 private interface Get{int value();}
 private interface Set{void value(int n);}
 private interface Bool{boolean value();}
 private void field(int x,int y,String unused,Get get,Set set,int min,int max){
  add(x+173,y,25,23,()->"-",()->{set.value(Math.max(min,get.value()-1));s.save();});
  add(x+235,y,25,23,()->"+",()->{set.value(Math.min(max,get.value()+1));s.save();});
 }
 private void toggle(int x,int y,int w,int h,Bool get,Runnable change){
  add(x,y,w,h,()->get.value()?"ON":"OFF",()->{change.run();s.save();});
 }
 private void add(int x,int y,int w,int h,Supplier<String> label,Runnable action){
  addDrawableChild(ButtonWidget.builder(Text.empty(),btn->action.run()).dimensions(x,y,w,h).build());
  pills.add(new Pill(x,y,w,h,label,false));
 }
 private void close(){if(client!=null)client.setScreen(null);}
 private void label(DrawContext c,String str,int x,int y,int color){
  c.drawTextWithShadow(textRenderer,Text.literal(str),x,y,color);
 }
 private void rounded(DrawContext c,int x,int y,int w,int h,int r,int color){
  r=Math.min(r,Math.min(w,h)/2);
  c.fill(x,y+r,x+w,y+h-r,color);
  for(int iy=0;iy<r;iy++){
   int d=r-iy;
   int inset=(int)Math.ceil(r-Math.sqrt(r*r-d*d));
   c.fill(x+inset,y+iy,x+w-inset,y+iy+1,color);
   c.fill(x+inset,y+h-iy-1,x+w-inset,y+h-iy,color);
  }
 }
 private void outline(DrawContext c,int x,int y,int w,int h,int r,int border,int fill){
  rounded(c,x,y,w,h,r,border);
  rounded(c,x+1,y+1,w-2,h-2,r-1,fill);
 }
 @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){
  context.fill(0,0,width,height,0xD6000000);
  super.render(context,mouseX,mouseY,delta);
  // Draw over the functional vanilla ButtonWidget hitboxes for custom fully rounded pills.
  outline(context,left,top,600,350,20,0xFF6A2E48,BACK);
  rounded(context,left+14,top+13,5,28,3,PINK);
  label(context,"SAKURA",left+29,top+21,PINK);
  label(context,"LIVE  /  CLIENT",left+95,top+21,TEXT);
  rounded(context,left+17,top+52,270,161,15,CARD);
  rounded(context,left+296,top+52,287,161,15,CARD);
  rounded(context,left+17,top+225,566,96,15,CARD);
  label(context,"ANCHOR MACRO",left+30,top+79,PINK_LIGHT);
  label(context,"CRYSTAL MACRO",left+311,top+79,PINK_LIGHT);
  label(context,"RESPAWN ANCHOR",left+30,top+110,MUTED);
  label(context,"Anchor slot",left+30,top+131,TEXT);
  label(context,"Glowstone slot",left+30,top+161,TEXT);
  label(context,"Totem final slot",left+30,top+191,TEXT);
  label(context,"Crystal slot",left+311,top+131,TEXT);
  label(context,"Delay (ticks)",left+311,top+161,TEXT);
  label(context,"FAST PLACE",left+30,top+242,PINK_LIGHT);
  label(context,"Enable",left+103,top+270,TEXT);
  label(context,"Exclude anchors",left+279,top+270,TEXT);
  label(context,"Place delay",left+311,top+303,TEXT);
  label(context,"G  Anchor    H  Crystal    RIGHT SHIFT  Menu",left+23,top+331,MUTED);
  label(context,Integer.toString(s.anchorSlot),left+222,top+129,PINK_LIGHT);
  label(context,Integer.toString(s.glowSlot),left+222,top+159,PINK_LIGHT);
  label(context,Integer.toString(s.totemSlot),left+222,top+189,PINK_LIGHT);
  label(context,Integer.toString(s.crystalSlot),left+506,top+129,PINK_LIGHT);
  label(context,Integer.toString(s.actionDelay),left+506,top+159,PINK_LIGHT);
  label(context,Integer.toString(s.placeDelay),left+506,top+303,PINK_LIGHT);
  for(Pill p:pills){
   boolean hover=mouseX>=p.x&&mouseX<p.x+p.w&&mouseY>=p.y&&mouseY<p.y+p.h;
   String value=p.label.get();
   int color=value.equals("ON")?(hover?0xFFFF78B0:PINK):(hover?CARD_HOVER:OFF);
   if(value.equals("X"))color=hover?0xFFFF78B0:PINK;
   rounded(context,p.x,p.y,p.w,p.h,Math.min(10,p.h/2),color);
   int ink=value.equals("ON")||value.equals("X")?BACK:TEXT;
   int txtW=textRenderer.getWidth(value);
   label(context,value,p.x+(p.w-txtW)/2,p.y+(p.h-8)/2,ink);
  }
 }
 @Override public boolean shouldPause(){return false;}
}
