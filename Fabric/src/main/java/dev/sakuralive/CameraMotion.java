package dev.sakuralive;
/** Closed-loop native mouse controller. Never writes player rotation or packets. */
final class CameraMotion {
 record Step(int dx,int dy,boolean stalled){}
 private long last,sentAt;
 private double previousYaw,previousPitch,gainX,gainY;
 private int pendingX,pendingY;
 private long awaitingSince;
 void reset(){last=sentAt=awaitingSince=0;pendingX=pendingY=0;gainX=gainY=0;}
 static double wrap(double angle){angle%=360;if(angle>=180)angle-=360;if(angle< -180)angle+=360;return angle;}
 Step step(double yaw,double pitch,double targetYaw,double targetPitch,double sensitivity,boolean invertX,boolean invertY,int speed,long now){
  double base=Math.pow(sensitivity*.6+.2,3)*1.2;
  if(gainX==0){gainX=base;gainY=base;}
  if(pendingX!=0||pendingY!=0){
   double actualX=wrap(yaw-previousYaw)*(invertX?-1:1),actualY=(pitch-previousPitch)*(invertY?-1:1);
   if(Math.abs(actualX)+Math.abs(actualY)<.001){
    if(now-sentAt<50_000_000L)return new Step(0,0,now-awaitingSince>500_000_000L);
   }else{
    if(Math.abs(pendingX)>1&&actualX*pendingX>0)gainX=Math.max(base*.25,Math.min(base*8,Math.abs(actualX/pendingX)));
    if(Math.abs(pendingY)>1&&actualY*pendingY>0)gainY=Math.max(base*.25,Math.min(base*8,Math.abs(actualY/pendingY)));
    awaitingSince=0;
   }
  }
  if(awaitingSince!=0&&now-awaitingSince>500_000_000L)return new Step(0,0,true);
  double dt=last==0?1.0/120:Math.min(.05,Math.max(.001,(now-last)/1e9));last=now;
  double ex=wrap(targetYaw-yaw),ey=Math.max(-89.5,Math.min(89.5,targetPitch))-pitch;
  double length=Math.hypot(ex,ey),limit=speed*dt;
  double fraction=length>limit?limit/length:1;
  // Approach conservatively; measured view changes compensate pointer acceleration.
  int dx=(int)Math.round(ex*fraction*.7/gainX)*(invertX?-1:1),dy=(int)Math.round(ey*fraction*.7/gainY)*(invertY?-1:1);
  if(dx==0&&Math.abs(ex)>.25)dx=ex*(invertX?-1:1)>0?1:-1;
  if(dy==0&&Math.abs(ey)>.25)dy=ey*(invertY?-1:1)>0?1:-1;
  pendingX=Math.max(-800,Math.min(800,dx));pendingY=Math.max(-800,Math.min(800,dy));previousYaw=yaw;previousPitch=pitch;sentAt=now;
  if((pendingX!=0||pendingY!=0)&&awaitingSince==0)awaitingSince=now;
  return new Step(pendingX,pendingY,false);
 }
}
