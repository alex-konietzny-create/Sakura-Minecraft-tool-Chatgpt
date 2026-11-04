package dev.sakuralive;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

/** Reference-led card grid, with original Sakura identity and working module controls. */
public final class SakuraScreen extends Screen {
 static final int W=920,H=600;
 private static final int INK=0xFFE8EBF2,MUTED=0xFF9199AA;
 private final SakuraSettings s=SakuraClient.settings;
 private final List<Control> controls=new ArrayList<>();
 private final List<Field> fields=new ArrayList<>();
 private final List<Card> cards=new ArrayList<>();
 private int page;
 private float scale,left,top,dt;
 private long opened=System.nanoTime(),last=opened,dirtyAt,noticeUntil;
 private boolean dirty;
 private String notice="";
 private KeyBinding capture;
 private enum Kind {NAV,POWER,KEY,TITLE,BUTTON}
 private record Control(ButtonWidget widget,Kind kind,Supplier<String> text,Runnable action,BooleanSupplier selected,String hint,float[] hover){}
 private record Field(TextFieldWidget widget,IntSupplier get,int min,int max,String hint){}
 private record Card(int x,int y,int module,String title,String initials,String description){}
 public SakuraScreen(){this(1);}
 SakuraScreen(int page){super(Text.literal("Sakura Live"));this.page=page;}
 private int accent(){return s.blueAccent?0xFF35B5D8:0xFFEF72AB;}
 private Text ui(String value){return s.smoothFont?Text.literal(value).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sakuralive","grid")))):Text.literal(value);}
 private int tw(String value){return textRenderer.getWidth(ui(value));}
 private String fit(String value,int max){return tw(value)<=max?value:textRenderer.trimToWidth(ui(value),Math.max(0,max-tw("…"))).getString()+"…";}
 @Override protected void init(){
  controls.clear();fields.clear();cards.clear();labels.clear();capture=null;
  scale=Math.max(.01f,Math.min(1.2f,Math.min((width-20f)/W,(height-20f)/H)));
  left=(width/scale-W)/2;top=(height/scale-H)/2;
  String[] nav={"Crystal","Anchor","Utility","Themes"};
  for(int i=0;i<4;i++){final int n=i;button(7,i==3?242:83+i*43,182,35,Kind.NAV,()->nav[n],()->select(n),()->page==n,"Bereich öffnen");}
  button(882,5,25,23,Kind.BUTTON,()->"×",this::close,()->false,"Menü schließen");
  if(page==1){
   card(222,119,0,"Anchor","AN","Place, charge and detonate.");
   card(571,119,1,"Legit Anchor","LA","Hold your key and keep your aim.");
   card(222,347,2,"Safe Anchor","SA","Glowstone cover before detonation.");
   card(571,347,3,"Double Anchor","DA","Two anchors. One fixed position.");
  }else if(page==0){
   card(222,119,4,"Crystal Assist","CR","Hold crystals and right mouse.");
  }else if(page==2){
   card(222,119,5,"Fast Place","FP","Block placement with your timing.");
  }else{
   button(240,160,131,35,Kind.BUTTON,()->"Sakura Pink",()->{s.blueAccent=false;changed();},()->!s.blueAccent,"Pinker Sakura-Akzent");
   button(385,160,131,35,Kind.BUTTON,()->"Ocean Blue",()->{s.blueAccent=true;changed();},()->s.blueAccent,"Blaue Farbwelt der Referenz");
   button(240,235,276,35,Kind.BUTTON,()->"Animationen: "+(!s.reduceMotion?"AN":"AUS"),()->{s.reduceMotion=!s.reduceMotion;changed();},()->!s.reduceMotion,"Reduzierte Bewegung umschalten");
   button(240,286,276,35,Kind.BUTTON,()->"Klare Schrift: "+(s.smoothFont?"AN":"AUS"),()->{s.smoothFont=!s.smoothFont;changed();},()->s.smoothFont,"Schrift umschalten");
   button(240,368,276,30,Kind.KEY,()->"Menü: "+keyName(SakuraClient.menuKey),()->capture=SakuraClient.menuKey,()->false,"Menü-Hotkey ändern");
  }
 }
 private void select(int next){flush();page=next;clearAndInit();}
 private void card(int x,int y,int module,String title,String initials,String description){
  cards.add(new Card(x,y,module,title,initials,description));
  button(x+62,y+15,198,35,Kind.TITLE,()->title,()->details(module),()->false,"Alle Einstellungen für "+title+" öffnen");
  button(x+285,y+16,21,21,Kind.POWER,()->"⏻",()->{SakuraClient.toggle(module);changed();},()->SakuraClient.enabled(module),"Modul ein-/ausschalten");
  button(x+263,y+44,44,16,Kind.BUTTON,()->"Mehr",()->details(module),()->false,"Weitere Einstellungen öffnen");
  keyRow(x,y+75,module);
  if(module<4){
   var o=SakuraClient.optionsFor(module);
   number(x,y+104,module==2?"Charge delay":"Delay",()->module==2?o.chargeMs:o.placeMs,n->{if(module==2)o.chargeMs=n;else o.placeMs=n;},10,2000,"ms");
   if(module==3){
    number(x,y+133,"Repeat delay",()->o.repeatMs,n->o.repeatMs=n,10,2000,"ms");
    number(x,y+162,"Anchor slot",()->o.anchorSlot,n->o.anchorSlot=n,1,9,"");
    number(x,y+191,"Glowstone slot",()->o.glowSlot,n->o.glowSlot=n,1,9,"");
   }else{
    number(x,y+133,"Anchor slot",()->o.anchorSlot,n->o.anchorSlot=n,1,9,"");
    number(x,y+162,"Glowstone slot",()->o.glowSlot,n->o.glowSlot=n,1,9,"");
    number(x,y+191,"Final slot",()->o.finalSlot,n->o.finalSlot=n,1,9,"");
   }
  }else if(module==4){
   number(x,y+104,"Place delay",()->s.crystalPlaceMs,n->s.crystalPlaceMs=n,10,2000,"ms");
   number(x,y+133,"Break delay",()->s.crystalBreakMs,n->s.crystalBreakMs=n,10,2000,"ms");
   number(x,y+162,"Timeout",()->s.crystalTimeoutMs,n->s.crystalTimeoutMs=n,250,10000,"ms");
  }else{
   number(x,y+104,"Delay",()->s.placeDelayMs,n->s.placeDelayMs=n,0,200,"ms");
   button(x+18,y+146,285,28,Kind.BUTTON,()->"Exclude anchors: "+(s.excludeAnchors?"ON":"OFF"),()->{s.excludeAnchors=!s.excludeAnchors;changed();},()->s.excludeAnchors,"Anchor / Glowstone ausnehmen");
  }
 }
 private void details(int module){flush();client.setScreen(new SakuraDetailsScreen(module));}
 private void keyRow(int x,int y,int module){
  button(x+210,y-1,86,22,Kind.KEY,()->keyName(SakuraClient.keyFor(module)),()->capture=SakuraClient.keyFor(module),()->false,"Hotkey ändern · "+conflict(SakuraClient.keyFor(module)));
 }
 private String keyName(KeyBinding key){return key.isUnbound()?"None":key.getBoundKeyLocalizedText().getString();}
 private String conflict(KeyBinding key){
  if(key.isUnbound())return "nicht belegt";
  for(var other:client.options.allKeys)if(other!=key&&!other.isUnbound()&&key.equals(other))return "Konflikt: "+Text.translatable(other.getId()).getString();
  return "Esc bricht ab";
 }
 private final Map<Integer,String> labels=new HashMap<>();
 private void number(int x,int y,String label,IntSupplier get,IntConsumer set,int min,int max,String unit){
  var widget=addDrawableChild(new TextFieldWidget(textRenderer,x+215,y+2,unit.isEmpty()?77:51,18,Text.literal(label)));
  widget.setMaxLength(5);widget.setDrawsBackground(false);widget.setEditableColor(INK);widget.setText(Integer.toString(get.getAsInt()));
  widget.setTextPredicate(v->v.matches("[0-9]{0,5}"));
  widget.setChangedListener(v->{if(!v.isEmpty()){int n=Integer.parseInt(v);if(n>=min&&n<=max&&n!=get.getAsInt()){set.accept(n);dirty=true;dirtyAt=System.nanoTime();}}});
  fields.add(new Field(widget,get,min,max,label+" · "+min+"–"+max+(unit.isEmpty()?"":" "+unit)));
  labels.put(y*1000+x,label+"|"+unit);
 }
 private void button(int x,int y,int w,int h,Kind kind,Supplier<String> text,Runnable action,BooleanSupplier on,String hint){
  var widget=addDrawableChild(ButtonWidget.builder(Text.literal(text.get()),b->action.run()).dimensions(x,y,w,h).build());
  controls.add(new Control(widget,kind,text,action,on,hint,new float[]{0}));
 }
 private void changed(){dirty=true;dirtyAt=System.nanoTime();flush();}
 private void flush(){if(!dirty)return;boolean ok=s.save();dirty=!ok;notice=ok?"Saved":"Save failed";noticeUntil=System.nanoTime()+1_500_000_000L;}
 @Override public void tick(){if(dirty&&System.nanoTime()-dirtyAt>300_000_000L)flush();}
 private void assign(InputUtil.Key key){capture.setBoundKey(key);KeyBinding.updateKeysByCode();client.options.write();capture=null;notice="Keybind saved";noticeUntil=System.nanoTime()+1_500_000_000L;}
 @Override public boolean keyPressed(KeyInput input){
  if(capture!=null){if(input.key()==GLFW.GLFW_KEY_ESCAPE){capture=null;return true;}assign(input.key()==GLFW.GLFW_KEY_DELETE||input.key()==GLFW.GLFW_KEY_BACKSPACE?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(input));return true;}
  if(input.key()==GLFW.GLFW_KEY_ENTER)flush();return super.keyPressed(input);
 }
 private float offset(){float t=s.reduceMotion?1:Math.min(1,(System.nanoTime()-opened)/220_000_000f);return 9*(1-t)*(1-t);}
 private Click local(Click c){return new Click(c.x()/scale-left,c.y()/scale-top-offset(),c.buttonInfo());}
 @Override public boolean mouseClicked(Click c,boolean doubled){if(capture!=null){assign(InputUtil.Type.MOUSE.createFromCode(c.button()));return true;}return super.mouseClicked(local(c),doubled);}
 @Override public boolean mouseReleased(Click c){boolean done=super.mouseReleased(local(c));flush();return done;}
 @Override public boolean mouseDragged(Click c,double x,double y){return capture!=null||super.mouseDragged(local(c),x/scale,y/scale);}
 @Override public void close(){capture=null;flush();super.close();}
 @Override public void removed(){flush();super.removed();}
 @Override public boolean shouldPause(){return false;}
 private void text(DrawContext c,String v,int x,int y,int color){c.drawText(textRenderer,ui(v),x,y,color,false);}
 private void bold(DrawContext c,String v,int x,int y,int color){
  Text value=s.smoothFont?Text.literal(v).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sakuralive","grid_bold")))):ui(v).copy().formatted(net.minecraft.util.Formatting.BOLD);
  c.drawText(textRenderer,value,x,y,color,false);
 }
 private void round(DrawContext c,int x,int y,int w,int h,int r,int color){
  c.fill(x,y+r,x+w,y+h-r,color);
  for(int i=0;i<r;i++){int d=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));c.fill(x+d,y+i,x+w-d,y+i+1,color);c.fill(x+d,y+h-i-1,x+w-d,y+h-i,color);}
 }
 private void line(DrawContext c,int x,int y,int x2,int y2,int color){
  int dx=Math.abs(x2-x),sx=x<x2?1:-1,dy=-Math.abs(y2-y),sy=y<y2?1:-1,e=dx+dy;
  while(true){c.fill(x,y,x+1,y+1,color);if(x==x2&&y==y2)break;int twice=2*e;if(twice>=dy){e+=dy;x+=sx;}if(twice<=dx){e+=dx;y+=sy;}}
 }
 private void icon(DrawContext c,int x,int y,String type,int color){
  if(type.equals("Crystal")){line(c,x+6,y,x+12,y+6,color);line(c,x+12,y+6,x+6,y+12,color);line(c,x+6,y+12,x,y+6,color);line(c,x,y+6,x+6,y,color);line(c,x+6,y+2,x+6,y+10,color);}
  else if(type.equals("Anchor")){c.drawStrokedRectangle(x,y+1,13,11,color);c.drawStrokedRectangle(x+4,y+4,5,5,color);c.fill(x+2,y+12,x+11,y+14,color);}
  else if(type.equals("Utility")){line(c,x+1,y+12,x+12,y+1,color);line(c,x,y+2,x+11,y+13,color);line(c,x+8,y+1,x+12,y+1,color);line(c,x+12,y+1,x+12,y+5,color);}
  else{round(c,x,y,14,14,7,color);round(c,x+2,y+2,10,10,5,0xFF0B0E18);c.fill(x+4,y+3,x+6,y+5,color);c.fill(x+8,y+4,x+10,y+6,color);c.fill(x+3,y+8,x+5,y+10,color);}
 }
 private void panel(DrawContext c,int x,int y,int w,int h){round(c,x,y,w,h,10,0xFF202637);round(c,x+1,y+1,w-2,h-2,9,0xFF101622);}
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  long now=System.nanoTime();dt=Math.min(.1f,(now-last)/1_000_000_000f);last=now;
  int mx=(int)(mouseX/scale-left),my=(int)(mouseY/scale-top-offset());
  c.fill(0,0,width,height,0xB0000000);c.getMatrices().pushMatrix();c.getMatrices().scale(scale,scale);c.getMatrices().translate(left,top+offset());
  round(c,0,0,W,H,9,0xFF1A2030);round(c,1,1,W-2,H-2,8,0xFF090C15);
  c.fill(1,29,197,H-1,0xFF0B0E18);c.fill(197,29,198,H-1,0xFF1A2030);c.fill(1,28,W-1,29,0xFF1C2230);
  bold(c,"SAKURA",14,8,accent());text(c,"LIVE",66,8,INK);c.fill(102,6,103,21,0xFF293043);text(c,"Pink Client",115,8,MUTED);
  text(c,"MACROS",17,57,0xFF677086);text(c,"OTHER",17,215,0xFF677086);
  String title=page==1?"Anchor Macros":page==0?"Crystal Macros":page==2?"Utility Macros":"Themes";
  c.getMatrices().pushMatrix();c.getMatrices().translate(222,62);c.getMatrices().scale(1.65f,1.65f);bold(c,title,0,0,INK);c.getMatrices().popMatrix();
  text(c,page==3?"Choose your accent and interface options.":"Configure your "+(page==1?"anchor":page==0?"crystal":"utility")+" macros",222,89,MUTED);
  if(page==3)panel(c,222,119,318,321);
  for(var card:cards){
   int x=card.x,y=card.y;panel(c,x,y,318,218);
   round(c,x+17,y+17,37,37,7,0xFF1B2333);c.drawStrokedRectangle(x+19,y+19,33,33,0xFF29354B);
   bold(c,card.initials,x+25,y+29,accent());
   text(c,fit(card.description,228),x+63,y+38,MUTED);
   text(c,"Keybind",x+18,y+79,MUTED);
  }
  for(var field:fields){
   var w=field.widget;int row=w.getY()-2,x=w.getX()-215;
   String[] description=labels.get(row*1000+x).split("\\|",-1);
   text(c,description[0],x+18,row+4,MUTED);
   int bw=description[1].isEmpty()?86:60;
   round(c,x+210,row-1,bw,23,5,0xFF1A2231);c.drawStrokedRectangle(x+211,row, bw-2,21,0xFF252F41);
   if(!description[1].isEmpty())text(c,description[1],x+277,row+4,0xFF626D83);
   if(!w.isFocused()&&!w.getText().equals(Integer.toString(field.get.getAsInt())))w.setText(Integer.toString(field.get.getAsInt()));
   w.render(c,mx,my,delta);
  }
  String hint="";
  for(var p:controls){
   var w=p.widget;int x=w.getX(),y=w.getY(),bw=w.getWidth(),bh=w.getHeight();boolean over=w.isMouseOver(mx,my)||w.isFocused(),on=p.selected.getAsBoolean();
   p.hover[0]+=( (over?1:0)-p.hover[0])*(s.reduceMotion?1:1-(float)Math.exp(-dt*15));
   String value=p.text.get();w.setMessage(Text.literal(value+". "+p.hint));if(over)hint=p.hint;
   if(p.kind==Kind.NAV){
    if(on||over)round(c,x,y,bw,bh,4,on?(s.blueAccent?0xFF0D2836:0xFF2B1A29):0xFF141B29);
    if(on)c.fill(x,y+8,x+2,y+bh-8,accent());
    icon(c,x+14,y+10,value,on?accent():0xFF737D91);
    text(c,value,x+36,y+11,on?accent():MUTED);
   }else if(p.kind==Kind.POWER){
    round(c,x,y,bw,bh,10,on?0xFF313348:0xFF222A3C);
    c.fill(x+10,y+3,x+12,y+10,on?accent():0xFF7A8497);
    // Open ring constructed from short strokes, independent of missing font symbols.
    c.fill(x+5,y+7,x+7,y+14,on?accent():0xFF7A8497);c.fill(x+15,y+7,x+17,y+14,on?accent():0xFF7A8497);c.fill(x+7,y+14,x+15,y+16,on?accent():0xFF7A8497);
   }else if(p.kind==Kind.TITLE)bold(c,value,x,y+3,INK);
   else{
    round(c,x,y,bw,bh,5,over||on?0xFF292B43:0xFF1A2231);
    String shown=fit(value,bw-10);text(c,shown,x+(bw-tw(shown))/2,y+(bh-10)/2,p.kind==Kind.KEY?INK:over||on?accent():MUTED);
   }
   if(w.isFocused())c.drawStrokedRectangle(x-1,y-1,bw+2,bh+2,accent());
  }
  c.fill(9,530,187,531,0xFF1E2635);round(c,13,547,33,33,16,0xFF263044);bold(c,"SL",22,558,accent());round(c,39,572,7,7,3,0xFF45C6A0);
  text(c,"Sakura Live",55,551,INK);text(c,"LOCAL CONFIG",55,569,0xFF647088);
  text(c,"Sakura 0.3.2",222,581,MUTED);
  String status=now<noticeUntil?notice:hint.isEmpty()?"Settings saved locally · macros pause while this menu is open":hint;
  text(c,fit(status,555),W-18-Math.min(555,tw(status)),581,now<noticeUntil?accent():0xFF707B91);
  if(capture!=null){
   round(c,208,111,698,458,10,0xF0090C15);panel(c,285,222,534,150);
   bold(c,"Set keybind",307,244,INK);text(c,Text.translatable(capture.getId()).getString(),307,275,MUTED);
   text(c,"Press a key or mouse button.",307,306,INK);text(c,"Esc: cancel   ·   Delete: unbind",307,337,MUTED);
  }
  c.getMatrices().popMatrix();
 }
}
