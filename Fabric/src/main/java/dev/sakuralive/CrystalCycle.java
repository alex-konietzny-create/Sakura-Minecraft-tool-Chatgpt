package dev.sakuralive;

/** Start/release and timeout logic shared by production and deterministic regression checks. */
final class CrystalCycle {
 enum Stage { READY, WAIT_SPAWN, WAIT_REMOVAL, COOLDOWN }
 enum Action { NONE, PLACE, BREAK }
 record Timings(int placeMs,int breakMs,int timeoutMs) {}
 private Stage stage=Stage.READY;
 private long due,expires;
 private int trackedId=-1;
 Stage stage(){return stage;}
 int trackedId(){return trackedId;}
 void reset(){stage=Stage.READY;due=expires=0;trackedId=-1;}
 Action poll(long now,int targetId,boolean canPlace,boolean trackedAlive,boolean sameTarget,Timings t){
  if(stage==Stage.WAIT_REMOVAL){
   if(trackedAlive&&now-expires<0)return targetId==trackedId&&sameTarget&&now-due>=0?Action.BREAK:Action.NONE;
   trackedId=-1;stage=Stage.COOLDOWN;due=now+AnchorPlan.ms(t.placeMs);if(t.placeMs>0)return Action.NONE;
  }
  if(stage==Stage.WAIT_SPAWN){
   if(!sameTarget){reset();return Action.NONE;}
   if(now-expires>=0){stage=Stage.COOLDOWN;due=now+AnchorPlan.ms(t.placeMs);return Action.NONE;}
   if(targetId>=0&&now-due>=0)return Action.BREAK;
   return Action.NONE;
  }
  if(stage==Stage.COOLDOWN){
   if(now-due<0)return Action.NONE;
   stage=Stage.READY;
  }
  // A fresh hold can place or break immediately; it never requires a previous manual attack.
  if(targetId>=0)return Action.BREAK;
  return canPlace?Action.PLACE:Action.NONE;
 }
 void placed(boolean accepted,long now,Timings t){
  stage=accepted?Stage.WAIT_SPAWN:Stage.COOLDOWN;
  due=now+AnchorPlan.ms(accepted?t.breakMs:t.placeMs);expires=now+AnchorPlan.ms(t.timeoutMs);
 }
 void attacked(int id,long now,Timings t){
  if(stage!=Stage.WAIT_REMOVAL||trackedId!=id)expires=now+AnchorPlan.ms(t.timeoutMs);
  trackedId=id;stage=Stage.WAIT_REMOVAL;due=now+AnchorPlan.ms(Math.max(50,t.breakMs));
 }
}
