package dev.sakuralive;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.*;
import net.minecraft.util.Identifier;

/** Rasterizes at the actual framebuffer density: alpha-covered edges survive GUI scaling. */
final class SmoothUi implements AutoCloseable {
 private static long nextId;
 private static final int ATLAS=2048,CELL=96,COLS=ATLAS/CELL;
 private final MinecraftClient mc=MinecraftClient.getInstance();
 private final float density;
 private final Map<Float,SmoothUi> headings=new HashMap<>();
 private final Identifier[] atlas=new Identifier[2];
 private Identifier glowAtlas;
 private final Map<Integer,float[]> glyphs=new HashMap<>();
 private final Map<String,Identifier> shapes=new HashMap<>();
 private final ArrayList<Identifier> owned=new ArrayList<>();
 SmoothUi(float scale){
  density=Math.max(.5f,Math.min(4f,scale*(float)mc.getWindow().getScaleFactor()));
  for(int weight=0;weight<2;weight++){
   var image=new BufferedImage(ATLAS,ATLAS,BufferedImage.TYPE_INT_ARGB);
   var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
   g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
   try(var stream=mc.getResourceManager().getResource(Identifier.of("sakuralive","font/sakura-grid"+(weight==1?"-bold":"")+".ttf")).orElseThrow().getInputStream()){
    var font=Font.createFont(Font.TRUETYPE_FONT,stream).deriveFont(12f*density);g.setFont(font);g.setColor(Color.WHITE);
    int index=0;
    for(int cp=32;cp<=383;cp++){
     paintGlyph(g,font,cp,index++,weight);
    }
    for(int cp:new int[]{0x2026,0x2013,0x2014,0x2190,0x2192,0xFFFD})paintGlyph(g,font,cp,index++,weight);
   }catch(Exception e){throw new IllegalStateException("Smooth UI font could not load",e);}finally{g.dispose();}
   atlas[weight]=upload(image);
   if(weight==1){
    var half=new BufferedImage(ATLAS/2,ATLAS/2,BufferedImage.TYPE_INT_ARGB);var down=half.createGraphics();
    down.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);down.drawImage(image,0,0,ATLAS/2,ATLAS/2,null);down.dispose();
    float[] kernel=new float[25];java.util.Arrays.fill(kernel,1f/25);
    var blur=new java.awt.image.ConvolveOp(new java.awt.image.Kernel(5,5,kernel),java.awt.image.ConvolveOp.EDGE_NO_OP,null);
    glowAtlas=upload(blur.filter(half,null));
   }
  }
 }
 private void paintGlyph(Graphics2D g,Font font,int cp,int index,int weight){
  String text=new String(Character.toChars(cp));int x=(index%COLS)*CELL,y=(index/COLS)*CELL;
  var gv=font.createGlyphVector(g.getFontRenderContext(),text);
  float advance=(float)gv.getGlyphMetrics(0).getAdvanceX();
  // Baseline + ascent align the top of UI text; generous cells protect accents and descenders.
  g.drawString(text,x+4,y+4+font.getLineMetrics(text,g.getFontRenderContext()).getAscent());
  float[] data=glyphs.computeIfAbsent(cp,k->new float[6]);data[weight*3]=x;data[weight*3+1]=y;data[weight*3+2]=advance;
 }
 private Identifier upload(BufferedImage image){
  var nativeImage=new NativeImage(image.getWidth(),image.getHeight(),false);
  for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)nativeImage.setColorArgb(x,y,image.getRGB(x,y));
  var id=Identifier.of("sakuralive","ui/runtime_"+(nextId++));
  var texture=new NativeImageBackedTexture(()->"Sakura smooth UI",nativeImage);
  mc.getTextureManager().registerTexture(id,texture);texture.upload();owned.add(id);return id;
 }
 SmoothUi scaled(float factor){return headings.computeIfAbsent(factor,k->new SmoothUi(density/(float)mc.getWindow().getScaleFactor()*factor));}
 int width(String text){float n=0;for(int cp:text.codePoints().toArray())n+=glyphs.getOrDefault(cp,glyphs.get(0xFFFD))[2]/density;return Math.round(n);}
 void text(DrawContext c,String text,float x,float y,int color,boolean bold){
  int weight=bold?1:0;float pen=0;
  c.getMatrices().pushMatrix();c.getMatrices().translate(x-4/density,y-4/density);c.getMatrices().scale(1/density,1/density);
  for(int cp:text.codePoints().toArray()){
   var glyph=glyphs.getOrDefault(cp,glyphs.get(0xFFFD));
   c.drawTexture(RenderPipelines.GUI_TEXTURED,atlas[weight],Math.round(pen),0,glyph[weight*3],glyph[weight*3+1],CELL,CELL,ATLAS,ATLAS,color);
   pen+=glyph[weight*3+2];
  }
  c.getMatrices().popMatrix();
 }
 void glowText(DrawContext c,String text,float x,float y,int color){
  float pen=0;c.getMatrices().pushMatrix();c.getMatrices().translate(x-4/density,y-4/density);c.getMatrices().scale(1/density,1/density);
  for(int cp:text.codePoints().toArray()){
   var glyph=glyphs.getOrDefault(cp,glyphs.get(0xFFFD));
   c.drawTexture(RenderPipelines.GUI_TEXTURED,glowAtlas,Math.round(pen),0,glyph[3]/2,glyph[4]/2,CELL,CELL,CELL/2,CELL/2,ATLAS/2,ATLAS/2,color);
   pen+=glyph[5];
  }
  c.getMatrices().popMatrix();
 }
 void glow(DrawContext c,int x,int y,int w,int h,int color){
  String key="ambient";var id=shapes.get(key);
  if(id==null){
   var image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
   for(int py=0;py<128;py++)for(int px=0;px<128;px++){
    double dx=(px-63.5)/63.5,dy=(py-63.5)/63.5,d=dx*dx+dy*dy;
    int alpha=(int)(255*Math.max(0,(Math.exp(-d*4)-Math.exp(-4))/(1-Math.exp(-4))));
    image.setRGB(px,py,(alpha<<24)|0xFFFFFF);
   }
   id=upload(image);shapes.put(key,id);
  }
  c.drawTexture(RenderPipelines.GUI_TEXTURED,id,x,y,0,0,w,h,128,128,128,128,color);
 }
 void backdrop(DrawContext c,int w,int h,int accent,boolean still){
  double time=still?0:System.nanoTime()/1_000_000_000.0;
  c.fillGradient(0,0,w,h,0xFF0D0C12,0xFF17121B);
  glow(c,(int)(-.15*w+Math.sin(time*.22)*w*.06),-h/3,w, h*4/3,0x4D000000|(accent&0xFFFFFF));
  glow(c,(int)(w*.36+Math.cos(time*.17)*w*.05),h/4,w,h,0x33000000|(accent&0xFFFFFF));
 }
 void icon(DrawContext c,int x,int y,String type,int color){
  String key="icon:"+type;Identifier id=shapes.get(key);int size=Math.max(16,Math.round(16*density));
  if(id==null){
   var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();g.scale(density,density);
   g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(1.25f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
   if(type.equals("Crystal")){var path=new java.awt.geom.Path2D.Float();path.moveTo(8,1);path.lineTo(14,8);path.lineTo(8,15);path.lineTo(2,8);path.closePath();g.draw(path);g.drawLine(8,2,8,14);}
   else if(type.equals("Anchor")){g.draw(new RoundRectangle2D.Float(2,2,12,11,3,3));g.draw(new RoundRectangle2D.Float(6,5,4,5,1,1));g.drawLine(5,15,11,15);}
   else if(type.equals("Utility")){g.drawLine(3,13,13,3);g.drawLine(3,3,13,13);g.drawLine(10,3,13,3);g.drawLine(13,3,13,6);}
   else{g.draw(new java.awt.geom.Ellipse2D.Float(1,1,14,14));g.fill(new java.awt.geom.Ellipse2D.Float(4,4,3,3));g.fill(new java.awt.geom.Ellipse2D.Float(9,5,3,3));g.fill(new java.awt.geom.Ellipse2D.Float(4,9,3,3));}
   g.dispose();id=upload(image);shapes.put(key,id);
  }
  c.drawTexture(RenderPipelines.GUI_TEXTURED,id,x,y,0,0,16,16,size,size,size,size,color);
 }
 void round(DrawContext c,int x,int y,int w,int h,int radius,int color){
  if(w<=0||h<=0)return;
  String key=w+":"+h+":"+radius;
  var id=shapes.get(key);
  int pw=Math.max(1,Math.round(w*density)),ph=Math.max(1,Math.round(h*density));
  if(id==null){
   var image=new BufferedImage(pw,ph,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
   g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Color.WHITE);
   g.fill(new RoundRectangle2D.Float(0,0,pw,ph,2*radius*density,2*radius*density));g.dispose();id=upload(image);shapes.put(key,id);
  }
  c.drawTexture(RenderPipelines.GUI_TEXTURED,id,x,y,0,0,w,h,pw,ph,pw,ph,color);
 }
 @Override public void close(){for(var font:headings.values())font.close();headings.clear();for(var id:owned)mc.getTextureManager().destroyTexture(id);owned.clear();shapes.clear();glyphs.clear();}
}
