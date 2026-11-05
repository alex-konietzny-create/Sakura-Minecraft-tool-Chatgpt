package dev.sakuralive;

/** Thread-confined deterministic sequence. Time is monotonic nanoseconds. No Minecraft calls. */
final class AnchorPlan {
 enum Stage { PLACE, WAIT_ANCHOR, CHARGE, WAIT_CHARGE, COVER, WAIT_COVER, DETONATE, WAIT_REMOVED, FINISHED, CANCELLED }
 enum Action { NONE, PLACE, CHARGE, COVER, DETONATE }
 record Timings(int placeMs,int chargeMs,int coverMs,int repeatMs,int timeoutMs) {}
 record Seen(boolean anchor,int charges,boolean replaceable,boolean cover) {}
 private final Timings timing;
 private final boolean coverRequired;
 private final int total;
 private Stage stage;
 private long due,expires,removedSince=Long.MIN_VALUE;
 private int completed;
 private String reason="";
 AnchorPlan(boolean existing,int charges,boolean coverRequired,int total,Timings timing,long now){
  this.timing=timing;this.coverRequired=coverRequired;this.total=total;
  stage=existing?(charges>0?(coverRequired?Stage.COVER:Stage.DETONATE):Stage.CHARGE):Stage.PLACE;
  due=now;expires=now+ms(timing.timeoutMs);
 }
 static long ms(int value){return value*1_000_000L;}
 Stage stage(){return stage;}
 int completed(){return completed;}
 String reason(){return reason;}
 boolean done(){return stage==Stage.FINISHED||stage==Stage.CANCELLED;}
 void cancel(String why){stage=Stage.CANCELLED;reason=why;}
 void defer(long now,int delayMs){due=now+ms(delayMs);}
 private void transition(Stage next,long now,int delayMs){
  stage=next;due=now+ms(delayMs);expires=now+ms(timing.timeoutMs);
 }
 Action poll(Seen seen,long now){
  if(done())return Action.NONE;
  if(now-expires>=0){cancel("Zeitlimit: "+stage);return Action.NONE;}
  if(now-due<0)return Action.NONE;
  // Only observations advance waiting states. An accepted click is not world confirmation.
  for(int i=0;i<5;i++){
   switch(stage){
    case PLACE -> {
     if(seen.anchor||!seen.replaceable){cancel("Ziel für Platzierung belegt");return Action.NONE;}
     return Action.PLACE;
    }
    case WAIT_ANCHOR -> {
     if(!seen.anchor){
      if(!seen.replaceable)cancel("Ziel durch anderen Block belegt");
      return Action.NONE;
     }
     transition(seen.charges>0?(coverRequired?Stage.COVER:Stage.DETONATE):Stage.CHARGE,now,0);
    }
    case CHARGE -> {
     if(!seen.anchor){cancel("Anchor fehlt");return Action.NONE;}
     if(seen.charges>0){transition(coverRequired?Stage.COVER:Stage.DETONATE,now,0);continue;}
     return Action.CHARGE;
    }
    case WAIT_CHARGE -> {
     if(!seen.anchor){cancel("Anchor entfernt");return Action.NONE;}
     if(seen.charges<=0)return Action.NONE;
     transition(coverRequired?Stage.COVER:Stage.DETONATE,now,0);
    }
    case COVER -> {
     if(!seen.anchor||seen.charges<=0){cancel("Geladener Anchor fehlt");return Action.NONE;}
     if(seen.cover){transition(Stage.DETONATE,now,0);continue;}
     return Action.COVER;
    }
    case WAIT_COVER -> {
     if(!seen.anchor||seen.charges<=0){cancel("Anchor entfernt");return Action.NONE;}
     if(!seen.cover)return Action.NONE;
     transition(Stage.DETONATE,now,0);
    }
    case DETONATE -> {
     if(!seen.anchor||seen.charges<=0){cancel("Anchor nicht geladen");return Action.NONE;}
     if(coverRequired&&!seen.cover){cancel("Glowstone-Deckung fehlt");return Action.NONE;}
     return Action.DETONATE;
    }
    case WAIT_REMOVED -> {
     // Client prediction can briefly show air before the server restores the first anchor.
     // Keep the fixed target and require a continuous free interval before the second click.
     if(seen.anchor){removedSince=Long.MIN_VALUE;return Action.NONE;}
     if(completed+1>=total){completed++;stage=Stage.FINISHED;return Action.NONE;}
     if(!seen.replaceable){cancel("Position nach Explosion belegt");return Action.NONE;}
     if(removedSince==Long.MIN_VALUE)removedSince=now;
     if(now-removedSince<ms(timing.repeatMs))return Action.NONE;
     completed++;removedSince=Long.MIN_VALUE;
     transition(Stage.PLACE,now,0);continue;
    }
    default -> {return Action.NONE;}
   }
  }
  return Action.NONE;
 }
 Action retryAfterProcessing(Seen seen,long now){
  if(done()||now-due<0||now-expires>=0)return Action.NONE;
  return switch(stage){
   case WAIT_ANCHOR->!seen.anchor&&seen.replaceable?Action.PLACE:Action.NONE;
   case WAIT_CHARGE->seen.anchor&&seen.charges==0?Action.CHARGE:Action.NONE;
   case WAIT_COVER->seen.anchor&&seen.charges>0&&!seen.cover?Action.COVER:Action.NONE;
   case WAIT_REMOVED->seen.anchor&&seen.charges>0&&(!coverRequired||seen.cover)?Action.DETONATE:Action.NONE;
   default->Action.NONE;
  };
 }
 void confirm(Action action,boolean accepted,long now){
  if(done())return;
  if(!accepted){cancel("Interaktion abgelehnt");return;}
  boolean retry=action==Action.PLACE&&stage==Stage.WAIT_ANCHOR||action==Action.CHARGE&&stage==Stage.WAIT_CHARGE||action==Action.COVER&&stage==Stage.WAIT_COVER||action==Action.DETONATE&&stage==Stage.WAIT_REMOVED;
  if(retry){due=now+ms(switch(action){case PLACE->timing.placeMs;case CHARGE->timing.chargeMs;case COVER->timing.coverMs;default->0;});return;}
  switch(action){
   case PLACE -> transition(Stage.WAIT_ANCHOR,now,timing.placeMs);
   case CHARGE -> transition(Stage.WAIT_CHARGE,now,timing.chargeMs);
   case COVER -> transition(Stage.WAIT_COVER,now,timing.coverMs);
   case DETONATE -> transition(Stage.WAIT_REMOVED,now,0);
   default -> {}
  }
 }
}
