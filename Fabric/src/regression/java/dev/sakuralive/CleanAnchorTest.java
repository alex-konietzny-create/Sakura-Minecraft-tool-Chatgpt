package dev.sakuralive;
public final class CleanAnchorTest {
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 public static void main(String[] args){
  check(ShortcutChoice.choose(i->i==0||i==1,i->i==1,-1)==1,"disabled Anchor sharing C cannot steal enabled Legit press");
  check(ShortcutChoice.choose(i->i==0||i==1,i->true,-1)==-2,"ambiguous active modules cannot both start");
  check(ShortcutChoice.choose(i->i==0||i==1,i->true,0)==0,"running mode owns its shortcut");
  check(ShortcutChoice.choose(i->i==0||i==7,i->true,0)==7,"menu remains an escape path");
  var gate=new InteractionGate();gate.arm();
  check(gate.evaluate(true,false,true,true,true,true)==InteractionGate.Decision.ALLOW,"one correct native use allowed");
  gate.result(true);for(int i=0;i<100;i++)check(gate.evaluate(true,false,true,true,true,true)==InteractionGate.Decision.SUPPRESS,"accepted place cannot be emitted twice");
  gate.arm();check(gate.evaluate(true,false,true,true,false,true)==InteractionGate.Decision.REJECT,"moved crosshair cannot place on another cell");
  check(gate.evaluate(true,false,true,false,true,true)==InteractionGate.Decision.REJECT,"wrong selected slot cannot perform action");
  gate.arm();check(gate.evaluate(true,true,true,true,true,true)==InteractionGate.Decision.SUPPRESS&&gate.accepted(),"world confirmation consumes stale queued click");
  var timing=new AnchorPlan.Timings(0,0,0,0,1000);var existing=new AnchorPlan(true,0,false,1,timing,0);
  check(existing.poll(new AnchorPlan.Seen(true,0,false,false),0)==AnchorPlan.Action.CHARGE,"existing anchor enters glowstone step, regardless of held totem or item");
  var placed=new AnchorPlan(false,0,false,1,timing,0);placed.confirm(AnchorPlan.Action.PLACE,true,0);
  check(placed.retryAfterProcessing(new AnchorPlan.Seen(false,0,true,false),100000000)==AnchorPlan.Action.NONE,"no blind duplicate placement while awaiting confirmation");
  check(HotbarChoice.choose(0,i->i==7)==7,"find glowstone despite wrong configured slot");
  check(HotbarChoice.choose(7,i->i==2||i==7)==7,"prefer configured matching stack");
  check(HotbarChoice.choose(0,i->false)==-1,"missing stack does not invent a slot");
  for(int delay:new int[]{60,120,250,600}){
   var plan=new AnchorPlan(false,0,false,2,new AnchorPlan.Timings(0,0,0,0,5000),0);
   long t=0;var air=new AnchorPlan.Seen(false,0,true,false);var uncharged=new AnchorPlan.Seen(true,0,false,false);var charged=new AnchorPlan.Seen(true,1,false,false);
   for(int cycle=0;cycle<2;cycle++){
    check(plan.poll(air,t)==AnchorPlan.Action.PLACE,"place cycle "+cycle);plan.confirm(AnchorPlan.Action.PLACE,true,t);
    check(plan.poll(air,t+AnchorPlan.ms(delay)-1)==AnchorPlan.Action.NONE,"wait for delayed placement");t+=AnchorPlan.ms(delay);
    check(plan.poll(uncharged,t)==AnchorPlan.Action.CHARGE,"charge after confirmation");plan.confirm(AnchorPlan.Action.CHARGE,true,t);
    plan.reacquire(AnchorPlan.Action.CHARGE);check(plan.poll(uncharged,t)==AnchorPlan.Action.CHARGE,"moved ray can reacquire without cancel");plan.confirm(AnchorPlan.Action.CHARGE,true,t);
    check(plan.poll(uncharged,t+AnchorPlan.ms(delay)-1)==AnchorPlan.Action.NONE,"wait for delayed charge");t+=AnchorPlan.ms(delay);
    check(plan.poll(charged,t)==AnchorPlan.Action.DETONATE,"detonate after confirmed charge");plan.confirm(AnchorPlan.Action.DETONATE,true,t);
    check(plan.poll(charged,t+AnchorPlan.ms(delay)-1)==AnchorPlan.Action.NONE,"wait for delayed removal");t+=AnchorPlan.ms(delay);
    plan.poll(air,t);
   }
   check(plan.done()&&plan.completed()==2,"both cycles complete with "+delay+"ms confirmations");
  }
  var burst=new DoubleClickBurst();long time=0;for(int i=0;i<20;i++){check(burst.ready(time),"burst has next native click");burst.sent(time,8);check(!burst.ready(time),"no overlapping burst pulse");time+=9_000_000;}
  check(!burst.ready(time+1_000_000_000),"burst hard limit is 20, never infinite spam");
  System.out.println("PASS: shared/disabled hotkeys, single accepted use, moved-target and wrong-slot rejection, existing-anchor charge entry and no duplicate place retry");
 }
}
