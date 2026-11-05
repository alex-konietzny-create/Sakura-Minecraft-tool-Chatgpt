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
 private static final int INK=0xFFF7F5FA,MUTED=0xFFC9C7D0;
 private final SakuraSettings s=SakuraClient.settings;
 private final List<Control> controls=new ArrayList<>();
 private final List<Field> fields=new ArrayList<>();
 private final List<Card> cards=new ArrayList<>();
 private int page;
 private SmoothUi smooth;
 private boolean animationReady,hasRendered;
 private float smoothScale;
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
 private int accent(){return s.blueAccent?0xFF62D7FF:0xFFFF68AC;}
 private Text ui(String value){return s.smoothFont?Text.literal(value).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sakuralive","grid")))):Text.literal(value);}
 private int tw(String value){return smooth!=null?smooth.width(value):textRenderer.getWidth(ui(value));}
 private String fit(String value,int max){if(tw(value)<=max)return value;while(!value.isEmpty()&&tw(value+"…")>max)value=value.substring(0,value.offsetByCodePoints(value.length(),-1));return value+"…";}
 @Override protected void init(){
  animationReady=false;
  controls.clear();fields.clear();cards.clear();labels.clear();capture=null;
  scale=Math.max(.01f,Math.min(1.2f,Math.min((width-20f)/W,(height-20f)/H)));
  left=(width/scale-W)/2;top=(height/scale-H)/2;
  if(smooth!=null&&(!s.smoothFont||smoothScale!=scale)){smooth.close();smooth=null;}
  if(s.smoothFont&&smooth==null){smooth=new SmoothUi(scale);smoothScale=scale;}
  String[] nav={"Crystal","Anchor","Utility","Themes"};
  for(int i=0;i<4;i++){final int n=i;button(7,i==3?242:83+i*43,182,35,Kind.NAV,()->nav[n],()->select(n),()->page==n,"Bereich öffnen");}
  button(882,5,25,23,Kind.BUTTON,()->"×",this::close,()->false,"Menü schließen");
  if(page==1){
   card(222,119,0,"Anchor","AN","Platzieren & auslösen");
   card(571,119,1,"Legit Anchor","LA","Kontrolliert halten");
   card(222,347,2,"Safe Anchor","SA","Deckung & Kamerafahrt");
   card(571,347,3,"Double Anchor","DA","Zweifache Sequenz");
  }else if(page==0){
   card(222,119,4,"Crystal Assist","CR","Kristall halten · Rechtsklick halten");
  }else if(page==2){
   card(222,119,5,"Fast Place","FP","Blöcke in deinem Rhythmus");
   button(571,119,318,80,Kind.BUTTON,()->"Block Outline · Einstellungen",()->{flush();client.setScreen(new SakuraOutlineScreen());},()->s.outline.enabled,"RGB, Glow, Breite und Kantenlänge einstellen");
  }else{
   button(240,160,131,35,Kind.BUTTON,()->"Sakura Pink",()->{s.blueAccent=false;changed();},()->!s.blueAccent,"Pinker Sakura-Akzent");
   button(385,160,131,35,Kind.BUTTON,()->"Ocean Blue",()->{s.blueAccent=true;changed();},()->s.blueAccent,"Blaue Farbwelt der Referenz");
   button(240,235,276,35,Kind.BUTTON,()->"Animationen: "+(!s.reduceMotion?"AN":"AUS"),()->{s.reduceMotion=!s.reduceMotion;changed();},()->!s.reduceMotion,"Reduzierte Bewegung umschalten");
   button(240,286,276,35,Kind.BUTTON,()->"Klare Schrift: "+(s.smoothFont?"AN":"AUS"),()->{s.smoothFont=!s.smoothFont;changed();clearAndInit();},()->s.smoothFont,"Schrift umschalten");
   number(222,425,"Input press",()->s.systemPulseMs,n->s.systemPulseMs=n,1,100,"ms");
   button(240,368,276,30,Kind.KEY,()->"Menü: "+keyName(SakuraClient.menuKey),()->capture=SakuraClient.menuKey,()->false,"Menü-Hotkey ändern");
  }
 }
 private void select(int next){flush();page=next;pageOpened=System.nanoTime();cardMotion.clear();clearAndInit();}
 private void card(int x,int y,int module,String title,String initials,String description){
  cards.add(new Card(x,y,module,title,initials,description));
  button(x+62,y+15,198,35,Kind.TITLE,()->title,()->details(module),()->false,"Alle Einstellungen für "+title+" öffnen");
  button(x+285,y+16,21,21,Kind.POWER,()->"⏻",()->{SakuraClient.toggle(module);changed();},()->SakuraClient.enabled(module),"Modul ein-/ausschalten");
  button(x+244,y+45,59,18,Kind.BUTTON,()->"Öffnen →",()->details(module),()->false,"Weitere Einstellungen öffnen");
  keyRow(x,y+75,module);
  if(module<4){
   var o=SakuraClient.optionsFor(module);
   number(x,y+104,"Aktionspause",()->o.placeMs,n->{o.actionDelay(n);s.systemPulseMs=Math.min(s.systemPulseMs,Math.max(1,n));},0,2000,"ms");
   if(module==3){
    number(x,y+133,"Wiederholpause",()->o.repeatMs,n->o.repeatMs=n,0,2000,"ms");
    number(x,y+162,"Anchor-Slot",()->o.anchorSlot,n->o.anchorSlot=n,1,9,"");
    number(x,y+191,"Glowstone-Slot",()->o.glowSlot,n->o.glowSlot=n,1,9,"");
   }else{
    number(x,y+133,"Anchor-Slot",()->o.anchorSlot,n->o.anchorSlot=n,1,9,"");
    number(x,y+162,"Glowstone-Slot",()->o.glowSlot,n->o.glowSlot=n,1,9,"");
    if(module==2)number(x,y+191,"Kamera °/s",()->o.cameraSpeed,n->o.cameraSpeed=n,30,3600,"");
    else number(x,y+191,"Auslöse-Slot",()->o.finalSlot,n->o.finalSlot=n,1,9,"");
   }
  }else if(module==4){
   number(x,y+104,"Platzierpause",()->s.crystalPlaceMs,n->s.crystalPlaceMs=n,0,2000,"ms");
   number(x,y+133,"Abbaupause",()->s.crystalBreakMs,n->s.crystalBreakMs=n,0,2000,"ms");
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
 private String keyName(KeyBinding key){return key.isUnbound()?"Keine":key.getBoundKeyLocalizedText().getString()+(Hotkeys.competing(key)?" !":"");}
 private String conflict(KeyBinding key){
  if(key.isUnbound())return "nicht belegt";
  if(Hotkeys.competing(key))return "Taste ist mehrfach aktiv belegt; bitte trennen";
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
 @Override public void removed(){flush();if(smooth!=null){smooth.close();smooth=null;}super.removed();}
 @Override public boolean shouldPause(){return false;}
 private void text(DrawContext c,String v,int x,int y,int color){if(smooth!=null){smooth.text(c,v,x,y-2,color,false);return;}c.drawText(textRenderer,ui(v),x,y,color,false);}
 private void bold(DrawContext c,String v,int x,int y,int color){
  if(smooth!=null){smooth.text(c,v,x,y-2,color,true);return;}
  Text value=s.smoothFont?Text.literal(v).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sakuralive","grid_bold")))):ui(v).copy().formatted(net.minecraft.util.Formatting.BOLD);
  c.drawText(textRenderer,value,x,y,color,false);
 }
 private void round(DrawContext c,int x,int y,int w,int h,int r,int color){
  if(smooth!=null){smooth.round(c,x,y,w,h,r,color);return;}
  c.fill(x,y+r,x+w,y+h-r,color);
  for(int i=0;i<r;i++){int d=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));c.fill(x+d,y+i,x+w-d,y+i+1,color);c.fill(x+d,y+h-i-1,x+w-d,y+h-i,color);}
 }
 private void line(DrawContext c,int x,int y,int x2,int y2,int color){
  int dx=Math.abs(x2-x),sx=x<x2?1:-1,dy=-Math.abs(y2-y),sy=y<y2?1:-1,e=dx+dy;
  while(true){c.fill(x,y,x+1,y+1,color);if(x==x2&&y==y2)break;int twice=2*e;if(twice>=dy){e+=dy;x+=sx;}if(twice<=dx){e+=dx;y+=sy;}}
 }
 private void icon(DrawContext c,int x,int y,String type,int color){
  if(smooth!=null){smooth.icon(c,x,y,type,color);return;}
  if(type.equals("Crystal")){line(c,x+6,y,x+12,y+6,color);line(c,x+12,y+6,x+6,y+12,color);line(c,x+6,y+12,x,y+6,color);line(c,x,y+6,x+6,y,color);line(c,x+6,y+2,x+6,y+10,color);}
  else if(type.equals("Anchor")){c.drawStrokedRectangle(x,y+1,13,11,color);c.drawStrokedRectangle(x+4,y+4,5,5,color);c.fill(x+2,y+12,x+11,y+14,color);}
  else if(type.equals("Utility")){line(c,x+1,y+12,x+12,y+1,color);line(c,x,y+2,x+11,y+13,color);line(c,x+8,y+1,x+12,y+1,color);line(c,x+12,y+1,x+12,y+5,color);}
  else{round(c,x,y,14,14,7,color);round(c,x+2,y+2,10,10,5,0xFF101014);c.fill(x+4,y+3,x+6,y+5,color);c.fill(x+8,y+4,x+10,y+6,color);c.fill(x+3,y+8,x+5,y+10,color);}
 }
 private void panel(DrawContext c,int x,int y,int w,int h){round(c,x,y,w,h,10,0xFF303036);round(c,x+1,y+1,w-2,h-2,9,0xFF15151B);}
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  if(!animationReady){if(smooth!=null)smooth.scaled(1.5f);long start=System.nanoTime();if(!hasRendered)opened=start;pageOpened=last=start;animationReady=true;hasRendered=true;}
  long now=System.nanoTime();dt=Math.min(.1f,(now-last)/1_000_000_000f);last=now;
  int mx=(int)(mouseX/scale-left),my=(int)(mouseY/scale-top-offset());
  if(smooth!=null)smooth.backdrop(c,width,height,accent(),s.reduceMotion);else c.fill(0,0,width,height,0xE00D0C12);c.getMatrices().pushMatrix();c.getMatrices().scale(scale,scale);c.getMatrices().translate(left,top+offset());
  if(smooth!=null)smooth.glow(c,-45,-35,W+90,H+70,0x29000000|(accent()&0xFFFFFF));
  round(c,0,0,W,H,16,0xFF303036);round(c,1,1,W-2,H-2,15,0xFF09090C);
  if(smooth!=null){
   double phase=s.reduceMotion?0:now/1_000_000_000.0;
   c.enableScissor(1,29,W-1,H-1);
   smooth.glow(c,200+(int)(Math.sin(phase*.3)*35),-80,610,400,0x26000000|(accent()&0xFFFFFF));
   smooth.glow(c,570,300+(int)(Math.cos(phase*.22)*30),440,380,0x18000000|(accent()&0xFFFFFF));
   c.disableScissor();
  }
  navY=approach(navY,page==3?242:83+page*43,16);
  c.fill(1,29,197,H-1,0xFF101014);c.fill(197,29,198,H-1,0xFF303036);c.fill(1,28,W-1,29,0xFF1C2230);
  if(smooth!=null)smooth.glowText(c,"SAKURA",14,6,0x99000000|(accent()&0xFFFFFF));
  bold(c,"SAKURA",14,8,accent());text(c,"LIVE",66,8,INK);c.fill(102,6,103,21,0xFF293043);text(c,"FABRIC 1.21.11",115,8,MUTED);
  text(c,"MODULE",17,57,0xFF91A1BC);text(c,"DARSTELLUNG",17,215,0xFF91A1BC);
  round(c,7,Math.round(navY),182,35,5,s.blueAccent?0xFF102A38:0xFF35162A);
  round(c,7,Math.round(navY)+8,3,19,1,accent());
  String title=page==1?"Anchor-Makros":page==0?"Crystal-Makros":page==2?"Werkzeuge":"Themes";
  c.getMatrices().pushMatrix();c.getMatrices().translate(222,62);c.getMatrices().scale(1.5f,1.5f);if(smooth!=null){smooth.scaled(1.5f).glowText(c,title,0,-2,0xA0000000|(accent()&0xFFFFFF));smooth.scaled(1.5f).text(c,title,0,-2,INK,true);}else bold(c,title,0,0,INK);c.getMatrices().popMatrix();
  text(c,page==3?"Farbe, Bewegung und Schrift nach deinem Geschmack.":(page==1?"Präzise Abläufe. Deine Hotkeys. Deine Kontrolle.":page==0?"Crystal-Abläufe und Timing.":"Platzierung und Block-Markierung."),222,89,MUTED);
  if(page==1){int active=0;for(int i=0;i<4;i++)if(SakuraClient.enabled(i))active++;round(c,747,64,142,27,13,0xFF182438);round(c,759,75,5,5,2,accent());text(c,active+" / 4 AKTIV",773,73,INK);}
  if(page==3)panel(c,222,119,318,360);
  for(var card:cards){
   int x=card.x,y=card.y;
   c.getMatrices().pushMatrix();c.getMatrices().translate(0,cardOffset(card.module));
   var motion=cardMotion.computeIfAbsent(card.module,k->new float[]{0,0});
   boolean over=mx>=x&&mx<x+318&&my>=y&&my<y+218;
   motion[0]=approach(motion[0],over?1:0,12);
   motion[1]=approach(motion[1],SakuraClient.enabled(card.module)?1:0,10);
   if(smooth!=null&&motion[0]>.01f)smooth.glow(c,x-25,y-22,368,260,((int)(motion[0]*60)<<24)|(accent()&0xFFFFFF));
   round(c,x,y,318,218,10,mix(0xFF303036,mix(0xFF344055,accent(),.24f),motion[0]));
   round(c,x+1,y+1,316,216,9,mix(0xFF15151B,0xFF202026,motion[0]));
   ModuleArt.draw(c,card.module,x+20,y+20,1.8f);
   text(c,fit(card.description,187),x+63,y+35,MUTED);
   c.fill(x+17,y+67,x+301,y+68,0xFF2C3A51);
   text(c,"Hotkey",x+18,y+79,MUTED);
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
   round(c,x+210,row-1,bw,23,5,0xFF303038);round(c,x+211,row,bw-2,21,4,0xFF0C0C10);
   if(!description[1].isEmpty())text(c,description[1],x+277,row+4,0xFF99A8C2);
   if(!w.isFocused()&&!w.getText().equals(Integer.toString(field.get.getAsInt())))w.setText(Integer.toString(field.get.getAsInt()));
   String value=w.getText();
   boolean valid=!value.isEmpty()&&Integer.parseInt(value)>=field.min&&Integer.parseInt(value)<=field.max;
   if(w.isFocused()){round(c,x+210,row-1,bw,23,5,valid?accent():0xFFFF8C91);round(c,x+211,row,bw-2,21,4,0xFF0C0C10);}
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
    if(!on&&hover>.01f)round(c,x,y,bw,bh,4,mix(0xFF101014,0xFF172030,hover));
    icon(c,x+14,y+10,value,mix(0xFFA3B2C9,accent(),active));
    if(smooth!=null)smooth.glowText(c,value,x+36,y+9,((int)(150+70*Math.max(active,hover))<<24)|(accent()&0xFFFFFF));
    bold(c,value,x+36,y+11,mix(MUTED,accent(),active));
   }else if(p.kind==Kind.POWER){
    // A real tween drives track color and thumb position on every frame.
    round(c,x-5,y+5,25,13,6,mix(0xFF43506A,accent(),active));
    if(smooth!=null)smooth.glow(c,x-23,y-14,62,50,((int)(active*80+hover*30)<<24)|(accent()&0xFFFFFF));
    int thumb=Math.round(x-3+active*11);
    round(c,thumb,y+7,9,9,4,mix(0xFFBEC9DA,0xFFFFFFFF,active));
   }else if(p.kind==Kind.TITLE){
    if(smooth!=null)smooth.glowText(c,value,x,y+1,((int)(185+55*hover)<<24)|(accent()&0xFFFFFF));
    bold(c,value,x,y+3,INK);
   }
   else{
    round(c,x,y,bw,bh,5,mix(0xFF202026,0xFF303038,Math.max(hover,active*.6f)));
    String shown=fit(value,bw-10);
    if(value.startsWith("Block Outline"))ModuleArt.draw(c,6,x+14,y+28,1.5f);
    if(value.startsWith("Block Outline")){
     if(smooth!=null)smooth.glowText(c,shown,x+(bw-tw(shown))/2,y+(bh-10)/2-2,0xC0000000|(accent()&0xFFFFFF));
     bold(c,shown,x+(bw-tw(shown))/2,y+(bh-10)/2,INK);
    }else text(c,shown,x+(bw-tw(shown))/2,y+(bh-10)/2,p.kind==Kind.KEY?INK:mix(MUTED,accent(),Math.max(hover,active)));
   }
   if(p.kind==Kind.KEY&&value.endsWith(" !")){round(c,x,y,bw,bh,5,0xFF4C3120);String shown=fit(value,bw-10);text(c,shown,x+(bw-tw(shown))/2,y+(bh-10)/2,0xFFFFCA83);}
   if(w.isFocused())c.drawStrokedRectangle(x-1,y-1,bw+2,bh+2,accent());
   if(x>=222&&page!=3)c.getMatrices().popMatrix();
  }
  c.fill(9,530,187,531,0xFF1E2635);round(c,13,547,33,33,16,0xFF27364F);bold(c,client.getSession().getUsername().substring(0,Math.min(2,client.getSession().getUsername().length())).toUpperCase(java.util.Locale.ROOT),20,558,accent());round(c,39,572,7,7,3,0xFF45C6A0);
  text(c,fit(client.getSession().getUsername(),128),55,551,INK);text(c,"Minecraft-Profil",55,569,MUTED);
  text(c,"Sakura 0.6.4",222,581,MUTED);
  String status=now<noticeUntil?notice:hint.isEmpty()?("Bereit".equals(SakuraClient.lastStatus)?"Settings saved locally · macros pause while this menu is open":"Letzter Ablauf: "+SakuraClient.lastStatus):hint;
  text(c,fit(status,555),W-18-Math.min(555,tw(status)),581,now<noticeUntil?accent():0xFF707B91);
  if(capture!=null){
   round(c,208,111,698,458,10,0xF0090C15);panel(c,285,222,534,150);
   bold(c,"Set keybind",307,244,INK);text(c,Text.translatable(capture.getId()).getString(),307,275,MUTED);
   text(c,"Press a key or mouse button.",307,306,INK);text(c,"Esc: cancel   ·   Delete: unbind",307,337,MUTED);
  }
  c.getMatrices().popMatrix();
 }
}

