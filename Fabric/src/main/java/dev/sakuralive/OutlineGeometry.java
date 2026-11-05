package dev.sakuralive;
/** Shared line-length geometry for the world renderer, live preview and regressions. */
final class OutlineGeometry {
 interface Segment {void draw(double ax,double ay,double az,double bx,double by,double bz);}
 static void edge(double ax,double ay,double az,double bx,double by,double bz,double fraction,boolean corners,Segment out){
  double f=Math.max(0,Math.min(1,fraction));if(f<=0)return;
  if(corners&&f<1){part(ax,ay,az,bx,by,bz,0,f/2,out);part(ax,ay,az,bx,by,bz,1-f/2,1,out);}
  else part(ax,ay,az,bx,by,bz,(1-f)/2,(1+f)/2,out);
 }
 private static void part(double ax,double ay,double az,double bx,double by,double bz,double from,double to,Segment out){
  double dx=bx-ax,dy=by-ay,dz=bz-az;out.draw(ax+dx*from,ay+dy*from,az+dz*from,ax+dx*to,ay+dy*to,az+dz*to);
 }
}
