package dev.sakuralive;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
/** All outline controls with an immediate, shared-geometry preview. */
public final class SakuraOutlineScreen extends Screen {
 private final SakuraSettings s=SakuraClient.settings;
 private final SakuraSettings.OutlineOptions o=s.outline;
 private final java.util.List<Control> controls=new ArrayList<>();
 private final java.util.List<Number> numbers=new ArrayList<>();
 private final java.util.List<OptionSlider> sliders=new ArrayList<>();
 private SmoothUi ui;
 private float scale,left,top;
 private long changedAt;
 private boolean dirty,capture;
 private record Control(ButtonWidget widget,Supplier<String> text,BooleanSupplier enabled){}
 private record Number(TextFieldWidget widget,IntSupplier get,int min,int max,int divisor){}
 public SakuraOutlineScreen(){super(Text.literal("Block Outline"));}
 private int accent(){return s.blueAccent?0xFF62D7FF:0xFFFF68AC;}
 @Override protected void init(){
  if(ui!=null)ui.close();controls.clear();numbers.clear();sliders.clear();
  scale=Math.max(.01f,Math.min(1.2f,Math.min((width-20f)/920,(height-20f)/600)));left=(width/scale-920)/2;top=(height/scale-600)/2;
  ui=new SmoothUi(scale);ui.scaled(1.8f);
  button(25,74,150,28,()->o.enabled?"Outline AN":"Outline AUS",()->o.enabled,()->{o.enabled=!o.enabled;changed();});
  button(185,74,165,28,()->"Hotkey: "+(SakuraClient.outlineKey.isUnbound()?"Keine":SakuraClient.outlineKey.getBoundKeyLocalizedText().getString()),()->capture,()->capture=true);
  button(794,24,100,28,()->"Zurück",()->false,this::close);
  row(25,160,"Linienbreite",()->o.width,n->o.width=n,10,120,10);
  row(25,226,"Linienlänge %",()->o.length,n->o.length=n,1,100,1);
  row(25,292,"Deckkraft",()->o.opacity,n->o.opacity=n,0,255,1);
  button(42,363,243,34,()->"Ecken: "+(o.corners?"AN":"AUS"),()->o.corners,()->{o.corners=!o.corners;changed();});
  row(327,160,"Rot",()->o.red,n->o.red=n,0,255,1);
  row(327,226,"Grün",()->o.green,n->o.green=n,0,255,1);
  row(327,292,"Blau",()->o.blue,n->o.blue=n,0,255,1);
  button(344,363,243,34,()->"Regenbogen: "+(o.rainbow?"AN":"AUS"),()->o.rainbow,()->{o.rainbow=!o.rainbow;changed();});
  row(327,420,"Farbzyklus (s)",()->o.rainbowSeconds,n->o.rainbowSeconds=n,1,30,1);
  button(646,159,243,34,()->"Glow: "+(o.glow?"AN":"AUS"),()->o.glow,()->{o.glow=!o.glow;changed();});
  row(629,225,"Glow-Radius",()->o.glowRadius,n->o.glowRadius=n,1,24,1);
  row(629,291,"Glow-Stärke %",()->o.glowStrength,n->o.glowStrength=n,0,100,1);
  button(646,363,243,34,()->"Pulsieren: "+(o.pulse?"AN":"AUS"),()->o.pulse,()->{o.pulse=!o.pulse;changed();});
  row(629,420,"Pulszyklus (s)",()->o.pulseSeconds,n->o.pulseSeconds=n,1,10,1);
 }
 private void button(int x,int y,int w,int h,Supplier<String> text,BooleanSupplier enabled,Runnable click){
  var widget=addDrawableChild(ButtonWidget.builder(Text.literal(text.get()),b->click.run()).dimensions(x,y,w,h).build());controls.add(new Control(widget,text,enabled));
 }
 private static String value(int n,int divisor){return divisor==1?Integer.toString(n):String.format(java.util.Locale.ROOT,"%.1f",n/(double)divisor);}
 private void row(int x,int y,String label,IntSupplier get,IntConsumer set,int min,int max,int divisor){
  var slider=addDrawableChild(new OptionSlider(x+17,y,243,label,get,set,min,max));sliders.add(slider);
  var field=addDrawableChild(new TextFieldWidget(textRenderer,x+205,y-3,51,20,Text.literal(label)));
  field.setDrawsBackground(false);field.setMaxLength(5);field.setText(value(get.getAsInt(),divisor));field.setTextPredicate(v->v.matches("[0-9.,]{0,5}"));
  field.setChangedListener(v->{try{int n=(int)Math.round(Double.parseDouble(v.replace(',','.'))*divisor);if(n>=min&&n<=max){set.accept(n);changed();}}catch(NumberFormatException ignored){}});
  numbers.add(new Number(field,get,min,max,divisor));
 }
 private void changed(){dirty=true;changedAt=System.nanoTime();}
 private void flush(){if(dirty){dirty=!s.save();}}
 @Override public void tick(){if(dirty&&System.nanoTime()-changedAt>300_000_000L)flush();}
 private Click local(Click c){return new Click(c.x()/scale-left,c.y()/scale-top,c.buttonInfo());}
 @Override public boolean mouseClicked(Click c,boolean doubled){if(capture){assign(InputUtil.Type.MOUSE.createFromCode(c.button()));return true;}var point=local(c);for(var field:numbers)if(field.widget.isMouseOver(point.x(),point.y())){setFocused(field.widget);return field.widget.mouseClicked(point,doubled);}return super.mouseClicked(point,doubled);}
 @Override public boolean mouseReleased(Click c){boolean result=super.mouseReleased(local(c));flush();return result;}
 @Override public boolean mouseDragged(Click c,double dx,double dy){return super.mouseDragged(local(c),dx/scale,dy/scale);}
 private void assign(InputUtil.Key key){SakuraClient.outlineKey.setBoundKey(key);KeyBinding.updateKeysByCode();client.options.write();capture=false;}
 @Override public boolean keyPressed(KeyInput key){
  if(capture){if(key.key()==GLFW.GLFW_KEY_ESCAPE){capture=false;return true;}assign(key.key()==GLFW.GLFW_KEY_DELETE?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(key));return true;}
  if(key.key()==GLFW.GLFW_KEY_ENTER)flush();return super.keyPressed(key);
 }
 @Override public void close(){flush();client.setScreen(new SakuraScreen(2));}
 @Override public void removed(){flush();if(ui!=null){ui.close();ui=null;}super.removed();}
 @Override public boolean shouldPause(){return false;}
 private void text(DrawContext c,String value,int x,int y,int color){ui.text(c,value,x,y-2,color,false);}
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  int mx=(int)(mouseX/scale-left),my=(int)(mouseY/scale-top);long now=System.nanoTime();
  ui.backdrop(c,width,height,accent(),s.reduceMotion);c.getMatrices().pushMatrix();c.getMatrices().scale(scale,scale);c.getMatrices().translate(left,top);
  ui.glow(c,-45,-35,1010,670,0x29000000|(accent()&0xFFFFFF));ui.round(c,0,0,920,600,14,0xFF303036);ui.round(c,1,1,918,598,13,0xFF09090C);
  ui.glow(c,180,-90,650,390,0x35000000|(accent()&0xFFFFFF));
  c.getMatrices().pushMatrix();c.getMatrices().translate(25,27);c.getMatrices().scale(1.8f,1.8f);ui.scaled(1.8f).glowText(c,"Block Outline",0,0,0xAA000000|(accent()&0xFFFFFF));ui.scaled(1.8f).text(c,"Block Outline",0,0,0xFFF7F5FA,true);c.getMatrices().popMatrix();
  for(int i=0;i<3;i++){int x=25+i*302;ui.round(c,x,122,267,395,10,0xFF303036);ui.round(c,x+1,123,265,393,9,0xFF111825);}
  text(c,"LINIEN",42,137,0xFFC9C7D0);text(c,"FARBE",344,137,0xFFC9C7D0);text(c,"LICHT & BEWEGUNG",646,137,0xFFC9C7D0);
  for(var slider:sliders)slider.renderWidget(c,mx,my,delta);
  for(var field:numbers){
   var w=field.widget;if(!w.isFocused())w.setText(value(field.get.getAsInt(),field.divisor));
   ui.round(c,w.getX()-5,w.getY()-2,61,24,5,0xFF111A29);text(c,w.getText(),w.getX(),w.getY()+3,0xFFF7F5FA);
   if(w.isFocused())c.drawStrokedRectangle(w.getX()-5,w.getY()-2,61,24,accent());
  }
  for(var control:controls){var w=control.widget;boolean over=w.isMouseOver(mx,my)||w.isFocused();int color=control.enabled.getAsBoolean()?0xFF43303F:over?0xFF34343E:0xFF25282E;
   ui.round(c,w.getX(),w.getY(),w.getWidth(),w.getHeight(),6,color);String label=control.text.get();text(c,label,w.getX()+(w.getWidth()-ui.width(label))/2,w.getY()+9,control.enabled.getAsBoolean()?accent():0xFFE7E5ED);w.setMessage(Text.literal(label));
  }
  preview(c,680,37,now);
  text(c,"Vorschau oben · AUS stellt die normale Minecraft-Markierung wieder her.",25,544,0xFFC9C7D0);
  text(c,"Änderungen werden automatisch gespeichert · Sakura 0.6.1",25,570,0xFF7C8190);
  if(capture){ui.round(c,240,230,440,120,12,0xFA16141B);text(c,"Taste oder Maustaste drücken",265,261,0xFFF4EBF1);text(c,"Esc: abbrechen · Entf: entfernen",265,300,0xFFC9C7D0);}
  c.getMatrices().popMatrix();
 }
 private void preview(DrawContext c,int x,int y,long now){
  int color=(Math.round(o.opacity*BlockOutline.pulse(o,now))<<24)|BlockOutline.rgb(o,now);
  double[][] p={{0,0,0},{1,0,0},{0,1,0},{1,1,0},{0,0,1},{1,0,1},{0,1,1},{1,1,1}};
  for(int i=0;i<8;i++)for(int axis=0;axis<3;axis++){int j=i^(1<<axis);if(j<i)continue;double[] a=p[i],b=p[j];
   OutlineGeometry.edge(a[0],a[1],a[2],b[0],b[1],b[2],o.length/100.0,o.corners,(ax,ay,az,bx,by,bz)->{
    float x1=x+(float)((ax-az)*31),y1=y+(float)((ax+az)*12+(1-ay)*31),x2=x+(float)((bx-bz)*31),y2=y+(float)((bx+bz)*12+(1-by)*31);
    double len=Math.hypot(x2-x1,y2-y1);float angle=(float)Math.atan2(y2-y1,x2-x1);
    c.getMatrices().pushMatrix();c.getMatrices().translate(x1,y1);c.getMatrices().rotate(angle);
    if(o.glow)ui.glow(c,-o.glowRadius,-o.glowRadius,(int)len+o.glowRadius*2,o.glowRadius*2,((o.glowStrength*2)<<24)|(color&0xFFFFFF));
    ui.round(c,0,0,Math.max(1,(int)len),Math.max(1,Math.round(o.width/10f)),1,color);c.getMatrices().popMatrix();
   });
  }
 }
 private final class OptionSlider extends SliderWidget {
  final String label;final IntSupplier get;final IntConsumer set;final int min,max;
  OptionSlider(int x,int y,int w,String label,IntSupplier get,IntConsumer set,int min,int max){super(x,y,w,49,Text.literal(label),(get.getAsInt()-min)/(double)(max-min));this.label=label;this.get=get;this.set=set;this.min=min;this.max=max;}
  @Override protected void updateMessage(){if(get!=null)setMessage(Text.literal(label+": "+get.getAsInt()));}
  @Override protected void applyValue(){if(set!=null){set.accept(min+(int)Math.round(value*(max-min)));changed();}}
  @Override public void renderWidget(DrawContext c,int mx,int my,float delta){value=(get.getAsInt()-min)/(double)(max-min);text(c,label,getX(),getY(),0xFFEEF3FC);ui.round(c,getX(),getY()+31,getWidth(),3,1,0xFF383541);int fill=(int)Math.round(getWidth()*value);ui.round(c,getX(),getY()+31,Math.max(1,fill),3,1,accent());ui.round(c,getX()+fill-4,getY()+28,9,9,4,0xFFF2ECF3);}
 }
}
