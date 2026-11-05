package dev.sakuralive;
public final class CameraMotionTest {
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 static double simulate(int fps,int speed,boolean inverted,double acceleration){
  var c=new CameraMotion();double yaw=0,pitch=0,gain=.15*acceleration;long now=1_000_000;
  for(int i=0;i<fps*2;i++){
   var s=c.step(yaw,pitch,90,35,.5,inverted,inverted,speed,now);check(!s.stalled(),"responsive camera");
   yaw+=s.dx()*gain*(inverted?-1:1);pitch+=s.dy()*gain*(inverted?-1:1);now+=1_000_000_000L/fps;
  }
  return Math.hypot(CameraMotion.wrap(90-yaw),35-pitch);
 }
 public static void main(String[] args){
  for(int fps:new int[]{60,120,240})for(boolean inverted:new boolean[]{false,true})check(simulate(fps,900,inverted,2)<1,"converges across FPS / inversion / pointer gain");
  check(simulate(120,30,false,1)>10,"camera speed setting changes actual travel");
  var c=new CameraMotion();var s=c.step(179,0,-179,0,.5,false,false,900,1);check(s.dx()>0&&s.dx()<20,"shortest yaw wrap");
  boolean stalled=false;for(int i=0;i<80;i++)stalled|=c.step(0,0,90,0,.5,false,false,900,20_000_000L+i*10_000_000L).stalled();check(stalled,"unresponsive OS movement stops");
  System.out.println("PASS: camera convergence at 60/120/240 FPS, inversion, acceleration feedback, speed, wrap and no-response stop");
 }
}
