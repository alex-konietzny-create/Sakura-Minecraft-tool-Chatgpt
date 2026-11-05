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
  System.out.println("PASS: shared/disabled hotkeys, single accepted use, moved-target and wrong-slot rejection, existing-anchor charge entry and no duplicate place retry");
 }
}
