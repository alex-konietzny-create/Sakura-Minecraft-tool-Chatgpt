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
 private float scale,left,top,dt,navY;
 private long pageOpened=System.nanoTime();
 private final Map<Integer,float[]> cardMotion=new HashMap<>();
 private long opened=System.nanoTime(),last=opened,dirtyAt,noticeUntil;
 private boolean dirty;
 private String notice="";
 private KeyBinding capture;
 private enum Kind {NAV,POWER,KEY,TITLE,BUTTON}
 private record Control(ButtonWidget widget,Kind kind,Supplier<String> text,Runnable action,BooleanSupplier selected,String hint,float[] hover){}
 private record Field(TextFieldWidget widget,IntSupplier get,int min,int max,String hint){}
 private record Card(int x,int y,int module,String title,String initials,String description){}
 public SakuraScreen(){this(1);}
 SakuraScreen(int page){super(Text.literal("Sakura Live"));this.page=page;navY=page==3?242:83+page*43;}
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
 private void select(int next){flush();page=next;pageOpened=System.nanoTime();cardMotion.clear();clearAndInit();}
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
  controls.add(new Control(widget,kind,text,action,on,hint,new float[]{0,on.getAsBoolean()?1:0,0}));
 }
 private void changed(){dirty=true;dirtyAt=System.nanoTime();flush();}
 private void flush(){if(!dirty)return;boolean ok=s.save();dirty=!ok;notice=ok?"Saved":"Save failed";noticeUntil=System.nanoTime()+1_500_000_000L;}
 @Override public void tick(){if(dirty&&System.nanoTime()-dirtyAt>300_000_000L)flush();}
 private void assign(InputUtil.Key key){capture.setBoundKey(key);KeyBinding.updateKeysByCode();client.options.write();capture=null;notice="Keybind saved";noticeUntil=System.nanoTime()+1_500_000_000L;}
 @Override public boolean keyPressed(KeyInput input){
  if(capture!=null){if(input.key()==GLFW.GLFW_KEY_ESCAPE){capture=null;return true;}assign(input.key()==GLFW.GLFW_KEY_DELETE||input.key()==GLFW.GLFW_KEY_BACKSPACE?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(input));return true;}
  if(input.key()==GLFW.GLFW_KEY_ENTER)flush();return super.keyPressed(input);
 }
 private float offset(){return 18*(1-ease(progress(opened,320)));}
 private float progress(long start,int ms){return s.reduceMotion?1:Math.max(0,Math.min(1,(System.nanoTime()-start)/(ms*1_000_000f)));}
 private static float ease(float t){return 1-(float)Math.pow(1-t,3);}
 private float approach(float from,float to,float speed){return s.reduceMotion?to:from+(to-from)*(1-(float)Math.exp(-dt*speed));}
 private static int mix(int a,int b,float t){t=Math.max(0,Math.min(1,t));int out=0;for(int shift=0;shift<=24;shift+=8)out|=Math.round(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;return out;}
 private float cardOffset(int module){return 12*(1-ease(progress(pageOpened+(module%4)*35_000_000L,300)));}
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
  navY=approach(navY,page==3?242:83+page*43,16);
  c.fill(1,29,197,H-1,0xFF0B0E18);c.fill(197,29,198,H-1,0xFF1A2030);c.fill(1,28,W-1,29,0xFF1C2230);
  bold(c,"SAKURA",14,8,accent());text(c,"LIVE",66,8,INK);c.fill(102,6,103,21,0xFF293043);text(c,"Pink Client",115,8,MUTED);
  text(c,"MACROS",17,57,0xFF677086);text(c,"OTHER",17,215,0xFF677086);
  round(c,7,Math.round(navY),182,35,5,s.blueAccent?0xFF102A38:0xFF2B1A29);
  round(c,7,Math.round(navY)+8,3,19,1,accent());
  String title=page==1?"Anchor Macros":page==0?"Crystal Macros":page==2?"Utility Macros":"Themes";
  c.getMatrices().pushMatrix();c.getMatrices().translate(222,62);c.getMatrices().scale(1.65f,1.65f);bold(c,title,0,0,INK);c.getMatrices().popMatrix();
  text(c,page==3?"Choose your accent and interface options.":"Configure your "+(page==1?"anchor":page==0?"crystal":"utility")+" macros",222,89,MUTED);
  if(page==3)panel(c,222,119,318,321);
  for(var card:cards){
   int x=card.x,y=card.y;
   c.getMatrices().pushMatrix();c.getMatrices().translate(0,cardOffset(card.module));
   var motion=cardMotion.computeIfAbsent(card.module,k->new float[]{0,0});
   boolean over=mx>=x&&mx<x+318&&my>=y&&my<y+218;
   motion[0]=approach(motion[0],over?1:0,12);
   motion[1]=approach(motion[1],SakuraClient.enabled(card.module)?1:0,10);
   round(c,x,y,318,218,10,mix(0xFF202637,mix(0xFF344055,accent(),.24f),motion[0]));
   round(c,x+1,y+1,316,216,9,mix(0xFF101622,0xFF151D2C,motion[0]));
   round(c,x+16,y+17,3,37,1,mix(0xFF263044,accent(),motion[1]));
   round(c,x+17,y+17,37,37,7,0xFF1B2333);c.drawStrokedRectangle(x+19,y+19,33,33,0xFF29354B);
   bold(c,card.initials,x+25,y+29,accent());
   text(c,fit(card.description,228),x+63,y+38,MUTED);
   text(c,"Keybind",x+18,y+79,MUTED);
   c.getMatrices().popMatrix();
  }
  for(var field:fields){
   var w=field.widget;
   int fieldModule=cards.stream().filter(card->w.getX()>=card.x&&w.getX()<card.x+318&&w.getY()>=card.y&&w.getY()<card.y+218).mapToInt(Card::module).findFirst().orElse(0);
   c.getMatrices().pushMatrix();c.getMatrices().translate(0,cardOffset(fieldModule));
   int row=w.getY()-2,x=w.getX()-215;
   String[] description=labels.get(row*1000+x).split("\\|",-1);
   text(c,description[0],x+18,row+4,MUTED);
   int bw=description[1].isEmpty()?86:60;
   round(c,x+210,row-1,bw,23,5,0xFF1A2231);c.drawStrokedRectangle(x+211,row, bw-2,21,0xFF252F41);
   if(!description[1].isEmpty())text(c,description[1],x+277,row+4,0xFF626D83);
   if(!w.isFocused()&&!w.getText().equals(Integer.toString(field.get.getAsInt())))w.setText(Integer.toString(field.get.getAsInt()));
   String value=w.getText();
   boolean valid=!value.isEmpty()&&Integer.parseInt(value)>=field.min&&Integer.parseInt(value)<=field.max;
   if(w.isFocused())c.drawStrokedRectangle(x+210,row-1,bw,23,valid?accent():0xFFFF8C91);
   text(c,value,w.getX()+1,w.getY()+2,valid?INK:0xFFFF8C91);
   if(w.isFocused()&&(now/500_000_000L)%2==0){
    int cursor=Math.max(0,Math.min(value.length(),w.getCursor()));
    int caret=w.getX()+1+tw(value.substring(0,cursor));c.fill(caret,w.getY()+1,caret+1,w.getY()+14,INK);
   }
   c.getMatrices().popMatrix();
  }
  String hint="";
  for(var p:controls){
   var w=p.widget;int x=w.getX(),y=w.getY(),bw=w.getWidth(),bh=w.getHeight();boolean over=w.isMouseOver(mx,my)||w.isFocused(),on=p.selected.getAsBoolean();
   p.hover[0]=approach(p.hover[0],over?1:0,16);
   p.hover[1]=approach(p.hover[1],on?1:0,14);
   if(x>=222&&page!=3){
    int module=cards.stream().filter(card->x>=card.x&&x<card.x+318&&y>=card.y&&y<card.y+218).mapToInt(Card::module).findFirst().orElse(0);
    c.getMatrices().pushMatrix();c.getMatrices().translate(0,cardOffset(module));
   }
   float hover=p.hover[0],active=p.hover[1];
   String value=p.text.get();w.setMessage(Text.literal(value+". "+p.hint));if(over)hint=p.hint;
   if(p.kind==Kind.NAV){
    if(!on&&hover>.01f)round(c,x,y,bw,bh,4,mix(0xFF0B0E18,0xFF172030,hover));
    icon(c,x+14,y+10,value,mix(0xFF737D91,accent(),active));
    text(c,value,x+36,y+11,mix(MUTED,accent(),active));
   }else if(p.kind==Kind.POWER){
    // A real tween drives track color and thumb position on every frame.
    round(c,x-7,y+3,29,16,8,mix(0xFF30394C,accent(),active));
    int thumb=Math.round(x-4+active*12);
    round(c,thumb,y+6,10,10,5,mix(0xFF8B95A8,0xFFFFFFFF,active));
   }else if(p.kind==Kind.TITLE)bold(c,value,x,y+3,INK);
   else{
    round(c,x,y,bw,bh,5,mix(0xFF1A2231,0xFF30354A,Math.max(hover,active*.6f)));
    String shown=fit(value,bw-10);text(c,shown,x+(bw-tw(shown))/2,y+(bh-10)/2,p.kind==Kind.KEY?INK:mix(MUTED,accent(),Math.max(hover,active)));
   }
   if(w.isFocused())c.drawStrokedRectangle(x-1,y-1,bw+2,bh+2,accent());
   if(x>=222&&page!=3)c.getMatrices().popMatrix();
  }
  c.fill(9,530,187,531,0xFF1E2635);round(c,13,547,33,33,16,0xFF263044);bold(c,"SL",22,558,accent());round(c,39,572,7,7,3,0xFF45C6A0);
  text(c,"Sakura Live",55,551,INK);text(c,"LOCAL CONFIG",55,569,0xFF647088);
  text(c,"Sakura 0.3.3",222,581,MUTED);
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

