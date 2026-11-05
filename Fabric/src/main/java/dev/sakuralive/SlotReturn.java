package dev.sakuralive;
/** Only emits hotbar keys in gameplay; menu opening waits for observed slot confirmation. */
final class SlotReturn {
 enum Decision {WAIT,PRESS,FINISH,TIMEOUT}
 private final int target;private long started=-1,lastSend;private long sentTick=-1;private int attempts;
 SlotReturn(int target){this.target=target;}
 int target(){return target;}
 Decision poll(boolean safe,int selected,long tick,long now){
  if(!safe){started=-1;return Decision.WAIT;}
  if(selected==target)return Decision.FINISH;
  if(started<0)started=now;
  if(now-started>=1_500_000_000L)return Decision.TIMEOUT;
  if(attempts<3&&(sentTick<0||tick-sentTick>=2&&now-lastSend>=100_000_000L))return Decision.PRESS;
  return Decision.WAIT;
 }
 void sent(long tick,long now){sentTick=tick;lastSend=now;attempts++;}
}
