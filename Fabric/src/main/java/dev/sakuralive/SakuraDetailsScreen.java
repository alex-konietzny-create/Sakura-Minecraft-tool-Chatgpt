package dev.sakuralive;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

/** Original settings workspace. One inverse transform serves all pointer events. */
public final class SakuraDetailsScreen extends Screen {
 static final int W=920,H=580;
 private static int PINK=0xFFFF78AB,INK=0xFFF1EDF3,MUTED=0xFFAAA3B0,BORDER=0xFF302B35;
 private static final String[] NAMES={"Anchor","Legit Anchor","Safe Anchor","Double Anchor","Crystal","Fast Place","Design"};
 private static final String[] SHORT={"Standard-Sequenz","Bewusst halten","Glowstone-Deckung","Zwei Durchläufe","Halten & Ausführen","Schneller platzieren","Darstellung"};
 private final SakuraSettings s=SakuraClient.settings;
 private final List<Control> controls=new ArrayList<>();
 private final List<MsSlider> sliders=new ArrayList<>();
 private final List<NumberField> fields=new ArrayList<>();
 private int tab,section;
 private float scale,left,top,dt,rail;
 private long opened=System.nanoTime(),pageOpened=opened,lastFrame=opened,changedAt,noticeUntil;
 private boolean dirty;
 private String notice="";
 private KeyBinding capture;
 private enum Skin { NAV,TAB,SWITCH,HERO,KEY,SLOT,BUTTON }
 private record Control(ButtonWidget widget,Skin skin,Supplier<String> text,Supplier<String> detail,BooleanSupplier selected,int index,float[] tween){}
 private record NumberField(TextFieldWidget widget,IntSupplier get,IntConsumer set,int min,int max){}
 public SakuraDetailsScreen(){this(0);}
 SakuraDetailsScreen(int initial){this(initial,0);}
 SakuraDetailsScreen(int initial,int initialSection){super(Text.literal("Sakura Live"));tab=initial;section=initialSection;rail=104+tab*43;}
 @Override protected void init(){
  controls.clear();sliders.clear();fields.clear();capture=null;
  scale=Math.max(0.01f,Math.min(1.2f,Math.min((width-20f)/W,(height-20f)/H)));
  left=(width/scale-W)/2;top=(height/scale-H)/2;
  for(int i=0;i<7;i++){
   final int n=i;
   button(24,104+i*43,148,36,Skin.NAV,()->NAMES[n],()->SHORT[n],()->tab==n,i,()->select(n,0));
  }
  button(872,21,24,24,Skin.BUTTON,()->"×",()->"Schließen",()->false,0,this::close);
  button(28,470,140,28,Skin.KEY,()->keyName(SakuraClient.menuKey),()->conflict(SakuraClient.menuKey),()->false,0,()->capture=SakuraClient.menuKey);
  button(28,510,140,28,Skin.BUTTON,()->"Alle Hotkeys",()->"Minecraft-Tastenbelegung",()->false,0,
    ()->client.setScreen(new KeybindsScreen(this,client.options)));
  if(tab==6){
   toggle(230,250,378,"Weiche Animationen",()->!s.reduceMotion,()->s.reduceMotion=!s.reduceMotion);
   toggle(230,320,378,"Klare Schrift",()->s.smoothFont,()->s.smoothFont=!s.smoothFont);
   button(230,404,378,38,Skin.KEY,()->"Menü · "+keyName(SakuraClient.menuKey),()->"Hotkey direkt ändern",()->false,0,()->capture=SakuraClient.menuKey);
   return;
  }
  button(210,96,686,56,Skin.HERO,()->NAMES[tab],this::hint,()->SakuraClient.enabled(tab),0,()->{SakuraClient.toggle(tab);changed();});
  String[] labels=tab<4?new String[]{"Timing","Hotbar","Verhalten"}:new String[]{"Timing","Verhalten"};
  for(int i=0;i<labels.length;i++){
   final int n=i;
   button(210+i*126,169,118,29,Skin.TAB,()->labels[n],()->"Einstellungen: "+labels[n],()->section==n,0,()->select(tab,n));
  }
  var o=SakuraClient.optionsFor(tab);
  if(section==0){
   if(tab<4){
    slider(230,253,378,"Nach Platzierung",()->o.placeMs,n->o.placeMs=n,10,2000);
    slider(230,317,378,"Nach dem Laden",()->o.chargeMs,n->o.chargeMs=n,10,2000);
    slider(230,381,378,"Slot-Vorbereitung",()->o.switchMs,n->o.switchMs=n,0,500);
    if(tab==2)slider(230,445,378,"Nach Glowstone-Deckung",()->o.coverMs,n->o.coverMs=n,10,2000);
    if(tab==3)slider(230,445,378,"Zwischen beiden Durchläufen",()->o.repeatMs,n->o.repeatMs=n,10,2000);
   }else if(tab==4){
    slider(230,253,378,"Vor nächster Platzierung",()->s.crystalPlaceMs,n->s.crystalPlaceMs=n,10,2000);
    slider(230,337,378,"Vor dem Abbau",()->s.crystalBreakMs,n->s.crystalBreakMs=n,10,2000);
    slider(230,421,378,"Bestätigung: Zeitlimit",()->s.crystalTimeoutMs,n->s.crystalTimeoutMs=n,250,10000);
   }else{
    slider(230,253,378,"Platzierpause",()->s.placeDelayMs,n->s.placeDelayMs=n,0,200);
    toggle(230,350,378,"Anchor / Glowstone ausnehmen",()->s.excludeAnchors,()->s.excludeAnchors=!s.excludeAnchors);
   }
  }else if(tab<4&&section==1){
   slots(276,()->o.anchorSlot,n->o.anchorSlot=n,"Anchor-Slot");
   slots(348,()->o.glowSlot,n->o.glowSlot=n,"Glowstone-Slot");
   slots(420,()->o.finalSlot,n->o.finalSlot=n,"Finaler Slot");
  }else{
   button(230,255,378,36,Skin.KEY,()->keyName(SakuraClient.keyFor(tab)),()->"Klicken und Taste oder Maustaste drücken",()->false,0,()->capture=SakuraClient.keyFor(tab));
   if(tab<4){
    toggle(230,333,378,"Exaktes Anvisieren",()->o.requireAim,()->o.requireAim=!o.requireAim);
    toggle(230,391,378,"Hotkey gedrückt halten",()->o.holdToRun,()->o.holdToRun=!o.holdToRun);
    slider(230,451,378,"Bestätigung: Zeitlimit",()->o.timeoutMs,n->o.timeoutMs=n,250,10000);
   }
  }
 }
 private void select(int next,int nextSection){flush();tab=next;section=nextSection;pageOpened=System.nanoTime();clearAndInit();}
 private void button(int x,int y,int w,int h,Skin skin,Supplier<String> text,Supplier<String> detail,BooleanSupplier selected,int index,Runnable action){
  var widget=addDrawableChild(ButtonWidget.builder(Text.literal(text.get()),b->action.run()).dimensions(x,y,w,h).build());
  controls.add(new Control(widget,skin,text,detail,selected,index,new float[]{0,selected.getAsBoolean()?1:0}));
 }
 private void toggle(int x,int y,int w,String name,BooleanSupplier get,Runnable change){
  button(x,y,w,42,Skin.SWITCH,()->name,()->get.getAsBoolean()?"Aktiviert":"Deaktiviert",get,0,()->{change.run();changed();});
 }
 private void slots(int y,IntSupplier get,IntConsumer set,String title){
  for(int i=1;i<=9;i++){
   final int slot=i;
   button(230+(i-1)*42,y,36,36,Skin.SLOT,()->Integer.toString(slot),()->title+": "+slot,()->get.getAsInt()==slot,slot,()->{set.accept(slot);changed();});
  }
 }
 private void slider(int x,int y,int w,String name,IntSupplier get,IntConsumer set,int min,int max){
  sliders.add(addDrawableChild(new MsSlider(x,y,w,name,get,set,min,max)));
  var field=addDrawableChild(new TextFieldWidget(textRenderer,x+w-78,y-3,58,22,Text.literal(name+" in Millisekunden")));
  field.setDrawsBackground(false);field.setEditableColor(PINK);field.setMaxLength(5);
  field.setText(Integer.toString(get.getAsInt()));field.setTextPredicate(v->v.matches("[0-9]{0,5}"));
  field.setChangedListener(v->{
   if(!v.isEmpty()){
    int n=Integer.parseInt(v);
    if(n>=min&&n<=max&&n!=get.getAsInt()){set.accept(n);dirty=true;changedAt=System.nanoTime();}
   }
  });
  fields.add(new NumberField(field,get,set,min,max));
 }
 private String keyName(KeyBinding key){return key.getBoundKeyLocalizedText().getString();}
 private String conflict(KeyBinding key){
  if(key.isUnbound())return "Nicht belegt · klicken zum Zuweisen";
  for(var other:client.options.allKeys)if(other!=key&&!other.isUnbound()&&key.equals(other))
   return "Konflikt: "+Text.translatable(other.getId()).getString();
  return "Taste oder Maus · Esc bricht ab";
 }
 private String hint(){
  if(!SakuraClient.enabled(tab))return "Deaktiviert · hier einschalten";
  return switch(tab){
   case 1->"Langsamere Abläufe · Haltemodus separat einstellbar";
   case 2->"Glowstone vor dem Auslösen als Deckung · ohne Totem-Prüfung";
   case 3->"Zwei Anchor auf exakt derselben Blockposition";
   case 4->"Kristalle in der Haupthand + rechte Maustaste halten";
   case 5->"Block in der Hand + Benutzen-Taste halten";
   default->"Vorhandenen Anchor erkennen oder neuen platzieren";
  };
 }
 private void changed(){dirty=true;changedAt=System.nanoTime();flush();}
 private void flush(){
  if(!dirty)return;
  boolean success=s.save();dirty=!success;
  notice=success?"Gespeichert":"Speichern fehlgeschlagen";noticeUntil=System.nanoTime()+1_500_000_000L;
 }
 @Override public void tick(){if(dirty&&System.nanoTime()-changedAt>350_000_000L)flush();}
 private void assign(InputUtil.Key key){
  capture.setBoundKey(key);KeyBinding.updateKeysByCode();client.options.write();capture=null;
  notice="Hotkey gespeichert";noticeUntil=System.nanoTime()+1_500_000_000L;
 }
 @Override public boolean keyPressed(KeyInput input){
  if(capture!=null){
   if(input.key()==GLFW.GLFW_KEY_ESCAPE){capture=null;return true;}
   assign(input.key()==GLFW.GLFW_KEY_DELETE||input.key()==GLFW.GLFW_KEY_BACKSPACE?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(input));return true;
  }
  if(input.key()==GLFW.GLFW_KEY_ENTER){flush();}
  return super.keyPressed(input);
 }
 private float offset(){
  if(s.reduceMotion)return 0;
  float t=Math.min(1,(System.nanoTime()-opened)/240_000_000f);return 14*(1-t)*(1-t)*(1-t);
 }
 private Click local(Click click){return new Click(click.x()/scale-left,click.y()/scale-top-offset(),click.buttonInfo());}
 @Override public boolean mouseClicked(Click click,boolean doubled){
  if(capture!=null){assign(InputUtil.Type.MOUSE.createFromCode(click.button()));return true;}
  return super.mouseClicked(local(click),doubled);
 }
 @Override public boolean mouseDragged(Click click,double dx,double dy){return capture!=null||super.mouseDragged(local(click),dx/scale,dy/scale);}
 @Override public boolean mouseReleased(Click click){boolean done=super.mouseReleased(local(click));flush();return done;}
 @Override public void removed(){capture=null;flush();super.removed();}
 @Override public void close(){capture=null;flush();if(client!=null)client.setScreen(new SakuraScreen(tab<4?1:tab==4?0:tab==5?2:3));}
 @Override public boolean shouldPause(){return false;}
 private float rate(){return s.reduceMotion?1:1-(float)Math.exp(-dt*17);}
 private float ease(float a,float b){return a+(b-a)*rate();}
 private int mix(int a,int b,float t){
  int out=0;for(int shift=0;shift<=24;shift+=8)out|=(int)(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;return out;
 }
 private Text ui(String value){
  var text=Text.literal(value);
  return s.smoothFont?text.setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sakuralive","grid")))):text;
 }
 private int tw(String value){return textRenderer.getWidth(ui(value));}
 private String fit(String value,int max){
  if(tw(value)<=max)return value;
  return textRenderer.trimToWidth(ui(value),Math.max(0,max-tw("…"))).getString()+"…";
 }
 private void label(DrawContext c,String value,int x,int y,int color){c.drawText(textRenderer,ui(value),x,y,color,false);}
 private void small(DrawContext c,String value,int x,int y,int color,int max){label(c,fit(value,max),x,y,color);}
 private void title(DrawContext c,String value,int x,int y,float size){
  c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(size,size);label(c,value,0,0,INK);c.getMatrices().popMatrix();
 }
 private void round(DrawContext c,int x,int y,int w,int h,int radius,int color){
  if(w<=0||h<=0)return;int r=Math.min(radius,Math.min(w,h)/2);c.fill(x,y+r,x+w,y+h-r,color);
  for(int i=0;i<r;i++){
   int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));
   c.fill(x+inset,y+i,x+w-inset,y+i+1,color);c.fill(x+inset,y+h-i-1,x+w-inset,y+h-i,color);
  }
 }
 private void card(DrawContext c,int x,int y,int w,int h){
  round(c,x,y,w,h,12,BORDER);round(c,x+1,y+1,w-2,h-2,11,0xFF19171E);
 }
 private void switchAt(DrawContext c,int x,int y,float on){
  round(c,x,y,34,18,9,mix(0xFF44404B,0xFFDD5B92,on));round(c,x+3+Math.round(on*16),y+3,12,12,6,INK);
 }
 private void flower(DrawContext c){
  for(int[] p:new int[][]{{0,-7},{7,-2},{4,6},{-4,6},{-7,-2}})round(c,42+p[0]-4,41+p[1]-4,8,8,4,PINK);
  round(c,40,39,4,4,2,0xFFFFD8E8);
 }
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  PINK=s.blueAccent?0xFF35B5D8:0xFFFF78AB;
  long now=System.nanoTime();dt=Math.min(.1f,(now-lastFrame)/1_000_000_000f);lastFrame=now;
  float off=offset();int mx=(int)(mouseX/scale-left),my=(int)(mouseY/scale-top-off);
  c.fill(0,0,width,height,0xB0000000);
  c.getMatrices().pushMatrix();c.getMatrices().scale(scale,scale);c.getMatrices().translate(left,top+off);
  for(int i=6;i>=1;i--)round(c,-i,2-i,W+2*i,H+2*i,20+i,0x06000000);
  round(c,0,0,W,H,20,0xFF42323D);round(c,1,1,W-2,H-2,19,0xFF100F14);
  round(c,12,12,174,556,13,0xFF16131B);
  flower(c);title(c,"SAKURA",63,29,1.3f);label(c,"LIVE  /  PINK",64,48,MUTED);
  label(c,"MODULE",29,82,0xFF7C7485);
  rail=ease(rail,104+tab*43);round(c,18,Math.round(rail+9),2,18,1,PINK);
  int active=0;for(int i=0;i<6;i++)if(SakuraClient.enabled(i))active++;
  label(c,active+" / 6 aktiv",29,429,MUTED);
  label(c,"MENÜ-HOTKEY",29,452,0xFF7C7485);
  label(c,"0.3.2 / FABRIC 1.21.11",29,551,0xFF7C7485);
  label(c,"SAKURA / "+(tab<4?"ANCHOR SUITE":tab==6?"PERSONALISIEREN":"UTILITY"),212,22,MUTED);
  title(c,NAMES[tab],210,39,2.25f);
  label(c,SHORT[tab]+" · eigene Einstellungen",212,73,MUTED);
  if(tab==6){
   round(c,210,96,686,56,11,0xFF241B28);
   label(c,"DEIN SAKURA",226,107,PINK);
   label(c,"Ruhige Flächen. Eigene Schrift. Bewegung nach Wunsch.",226,130,MUTED);
   label(c,"LOOK & FEEL",212,178,MUTED);
  }
  if(tab!=6){card(c,210,214,418,304);card(c,642,214,254,304);}
  else{
   card(c,210,214,418,304);card(c,642,214,254,304);
   label(c,"DEIN LOOK",230,232,MUTED);
   label(c,"Schrift, Bewegung und Bedienung.",230,461,MUTED);
  }
  if(tab!=6){
   label(c,section==0?"ZEITEN IN MILLISEKUNDEN":tab<4&&section==1?"DEINE HOTBAR":"HOTKEY & VERHALTEN",230,232,MUTED);
   if(tab<4&&section==1){
    label(c,"Anchor",230,257,INK);label(c,"Glowstone",230,329,INK);label(c,"Finaler Slot · kein Totem erforderlich",230,401,INK);
   }else if(section>0){
    String note=conflict(SakuraClient.keyFor(tab));
    small(c,note,230,305,note.startsWith("Konflikt")?0xFFFFB490:MUTED,378);
    if(tab==4){
     label(c,"Startet direkt beim Halten.",230,354,INK);
     label(c,"Kein manueller erster Crystal-Schlag nötig.",230,381,MUTED);
     label(c,"Loslassen oder Item-Wechsel setzt zurück.",230,408,MUTED);
    }
    if(tab==5)label(c,"Wirkt nur auf Block-Items.",230,354,MUTED);
   }
   if(tab==5&&section==0){
    label(c,"Minecraft verarbeitet Fast Place in Spiel-Ticks.",230,420,MUTED);
    label(c,"Effektiv: "+((s.placeDelayMs+49)/50)*50+" ms bei 20 TPS",230,447,INK);
   }
  }
  label(c,"ABLAUF",660,233,MUTED);
  String[][] steps={
   {"Ziel festhalten","Anchor setzen / erkennen","Laden bestätigen","Auslösen"},
   {"Hotkey bewusst halten","Exaktes Ziel beibehalten","Eigene langsamere Pausen","Loslassen bricht ab"},
   {"Anchor setzen / erkennen","Mit Glowstone laden","Glowstone auf deiner Seite","Mit Deckung auslösen"},
   {"Ersten Anchor auslösen","Entfernung abwarten","Gleiche Position neu setzen","Zweiten Anchor auslösen"},
   {"Kristalle in die Haupthand","Rechte Maustaste halten","Platzieren oder Crystal finden","Abbauen & erneut platzieren"},
   {"Block in die Hand nehmen","Benutzen-Taste halten","Anchor bei Bedarf ausnehmen","Eigene Platzierpause"},
   {"Klare Schrift wählen","Bewegung nach Wunsch","Menü-Hotkey zuweisen","Änderungen speichern"}
  };
  c.fill(668,274,669,399,0xFF45303E);
  for(int i=0;i<4;i++){
   round(c,664,267+i*40,9,9,4,i==0?PINK:0xFF605064);
   small(c,steps[tab][i],684,265+i*40,INK,194);
  }
  c.fill(660,431,878,432,BORDER);
  label(c,tab==2?"DECKUNG, KEIN SCHADENSSCHILD":"TIMING-HINWEIS",660,448,MUTED);
  small(c,tab==2?"Deckung ist keine Überlebensgarantie.":"Millisekunden sind Mindestpausen.",660,469,MUTED,218);
  small(c,tab==2?"Platz muss frei und erreichbar sein.":"FPS & Server beeinflussen den Ablauf.",660,490,MUTED,218);
  String hover="";
  for(Control p:controls){
   var w=p.widget;int x=w.getX(),y=w.getY(),bw=w.getWidth(),bh=w.getHeight();
   boolean over=w.isMouseOver(mx,my)||w.isFocused();
   p.tween[0]=ease(p.tween[0],over?1:0);p.tween[1]=ease(p.tween[1],p.selected.getAsBoolean()?1:0);
   float h=p.tween[0],on=p.tween[1];String text=p.text.get(),detail=p.detail.get();
   w.setMessage(Text.literal(text+". "+detail));if(over)hover=detail;
   if(p.skin==Skin.NAV){
    round(c,x,y,bw,bh,7,mix(mix(0xFF16131B,0xFF211C27,h),0xFF30202D,on));
    small(c,text,x+13,y+7,mix(MUTED,INK,on),bw-32);
    if(p.index<6){round(c,x+bw-17,y+16,4,4,2,SakuraClient.enabled(p.index)?PINK:0xFF65596B);}
   }else if(p.skin==Skin.TAB){
    round(c,x,y,bw,bh,7,mix(0xFF151219,0xFF30202D,on));
    label(c,text,x+(bw-tw(text))/2,y+7,on>.5?PINK:MUTED);
   }else if(p.skin==Skin.HERO){
    round(c,x,y,bw,bh,11,mix(0xFF201B25,0xFF29202B,on));
    round(c,x+1,y+12,2,bh-24,1,mix(0xFF645A6A,PINK,on));
    label(c,text,x+16,y+11,INK);small(c,detail,x+16,y+33,MUTED,bw-97);
    switchAt(c,x+bw-52,y+18,on);
   }else if(p.skin==Skin.SWITCH){
    round(c,x,y,bw,bh,8,mix(0xFF24202A,0xFF332837,h));
    small(c,text,x+12,y+13,INK,bw-72);switchAt(c,x+bw-47,y+12,on);
   }else if(p.skin==Skin.SLOT){
    round(c,x,y,bw,bh,7,mix(mix(0xFF27212E,0xFF443247,h),0xFF9F3F6B,on));
    label(c,text,x+5,y+3,INK);
    if(client.player!=null){
     var stack=client.player.getInventory().getStack(p.index-1);
     if(!stack.isEmpty()){c.getMatrices().pushMatrix();c.getMatrices().translate(x+16,y+16);c.getMatrices().scale(.8f,.8f);c.drawItem(stack,0,0);c.getMatrices().popMatrix();}
    }
   }else{
    round(c,x,y,bw,bh,7,mix(0xFF2A2231,0xFF46304B,h));
    String shown=bw<40?text:fit(text,bw-18);
    label(c,shown,x+(bw-tw(shown))/2,y+(bh-11)/2,p.skin==Skin.KEY?PINK:INK);
   }
   if(w.isFocused())c.drawStrokedRectangle(x-2,y-2,bw+4,bh+4,0xFFFFB4D0);
  }
  for(var slider:sliders){
   slider.render(c,mx,my,delta);
   if(slider.isMouseOver(mx,my)||slider.isFocused())hover="Ziehen · Pfeiltasten: 1 ms · Shift + Pfeil: 10 ms · Zahl direkt eingeben";
  }
  for(var f:fields){
   var w=f.widget;
   if(!w.isFocused()&&!w.getText().equals(Integer.toString(f.get.getAsInt())))w.setText(Integer.toString(f.get.getAsInt()));
   boolean valid=!w.getText().isEmpty()&&Integer.parseInt(w.getText())>=f.min&&Integer.parseInt(w.getText())<=f.max;
   round(c,w.getX()-7,w.getY()-3,85,26,6,valid?0xFF342535:0xFF58303E);
   label(c,"ms",w.getX()+59,w.getY()+4,MUTED);w.render(c,mx,my,delta);
   if(w.isFocused())hover="Erlaubt: "+f.min+" bis "+f.max+" ms. Ungültige Werte werden nicht übernommen.";
  }
  float reveal=s.reduceMotion?1:Math.min(1,(now-pageOpened)/150_000_000f);
  if(reveal<1)round(c,208,212,690,308,12,((int)(180*(1-reveal))<<24)|0x00100F14);
  round(c,210,533,686,25,7,0xFF1E1822);
  round(c,221,542,5,5,2,0xFFE9AD7B);
  label(c,"Im Menü pausiert",234,538,MUTED);
  String status=now<noticeUntil?notice:"Einstellungen automatisch gespeichert";
  small(c,status,894-tw(status),538,now<noticeUntil?PINK:MUTED,420);
  small(c,hover.isEmpty()?SakuraClient.lastStatus:hover,212,563,MUTED,684);
  if(capture!=null){
   round(c,199,91,707,432,14,0xF8100F14);card(c,279,229,526,150);
   title(c,"Hotkey zuweisen",301,248,1.7f);
   small(c,Text.translatable(capture.getId()).getString(),301,284,MUTED,480);
   label(c,"Taste oder Maustaste drücken.",301,318,INK);
   label(c,"Esc: abbrechen     Entf / Backspace: entfernen",301,349,MUTED);
  }
  c.getMatrices().popMatrix();
 }
 private final class MsSlider extends SliderWidget {
  private final String name;private final IntSupplier get;private final IntConsumer set;private final int min,max;private float visual;
  MsSlider(int x,int y,int w,String name,IntSupplier get,IntConsumer set,int min,int max){
   super(x,y+22,w,33,Text.literal(name),(get.getAsInt()-min)/(double)(max-min));
   this.name=name;this.get=get;this.set=set;this.min=min;this.max=max;visual=(float)value;updateMessage();
  }
  @Override protected void updateMessage(){if(get!=null)setMessage(Text.literal(name+": "+get.getAsInt()+" Millisekunden"));}
  @Override protected void applyValue(){
   int n=min+(int)Math.round(value*(max-min));if(n!=get.getAsInt()){set.accept(n);dirty=true;changedAt=System.nanoTime();}updateMessage();
  }
  @Override public void onRelease(Click click){super.onRelease(click);flush();}
  @Override public boolean keyPressed(KeyInput input){
   if(input.key()==GLFW.GLFW_KEY_LEFT||input.key()==GLFW.GLFW_KEY_RIGHT){
    int n=SakuraSettings.cap(get.getAsInt()+(input.key()==GLFW.GLFW_KEY_RIGHT?1:-1)*(input.hasShift()?10:1),min,max);
    set.accept(n);value=(n-min)/(double)(max-min);dirty=true;updateMessage();flush();return true;
   }
   return super.keyPressed(input);
  }
  @Override public void renderWidget(DrawContext c,int mx,int my,float delta){
   int x=getX(),y=getY()-22,w=getWidth();value=(get.getAsInt()-min)/(double)(max-min);
   visual=ease(visual,(float)value);small(c,name,x,y,INK,w-99);
   round(c,x+4,y+30,w-8,3,1,0xFF49374B);round(c,x+4,y+30,Math.max(1,Math.round((w-8)*visual)),3,1,PINK);
   int knob=x+4+Math.round((w-8)*visual);round(c,knob-4,y+27,8,9,4,INK);
   label(c,Integer.toString(min),x,y+43,MUTED);String end=Integer.toString(max);label(c,end,x+w-tw(end),y+43,MUTED);
   if(isFocused())c.drawStrokedRectangle(x-3,y-4,w+6,60,0xFFFFB4D0);
  }
 }
}
