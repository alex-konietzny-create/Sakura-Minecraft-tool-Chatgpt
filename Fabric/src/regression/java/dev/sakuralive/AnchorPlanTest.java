package dev.sakuralive;

/** Runs without a game world; drives the production state machine with delayed observations. */
public final class AnchorPlanTest {
 private static final AnchorPlan.Timings T=new AnchorPlan.Timings(80,95,70,125,1500);
 private static final AnchorPlan.Seen AIR=new AnchorPlan.Seen(false,0,true,false);
 private static final AnchorPlan.Seen EMPTY=new AnchorPlan.Seen(true,0,false,false);
 private static final AnchorPlan.Seen CHARGED=new AnchorPlan.Seen(true,1,false,false);
 private static void equal(Object actual,Object expected,String label){
  if(!actual.equals(expected))throw new AssertionError(label+": "+actual+" != "+expected);
 }
 private static long at(int ms){return AnchorPlan.ms(ms);}
 private static void existingAnchors(){
  var charged=new AnchorPlan(true,4,false,1,T,0);
  equal(charged.poll(CHARGED,0),AnchorPlan.Action.DETONATE,"charged skips placing and charging");
  var empty=new AnchorPlan(true,0,false,1,T,0);
  equal(empty.poll(EMPTY,0),AnchorPlan.Action.CHARGE,"empty existing skips placement");
  empty.confirm(AnchorPlan.Action.CHARGE,true,0);
  equal(empty.poll(CHARGED,at(94)),AnchorPlan.Action.NONE,"no early detonation");
  equal(empty.poll(CHARGED,at(95)),AnchorPlan.Action.DETONATE,"millisecond charge deadline");
 }
 private static void confirmations(){
  var p=new AnchorPlan(false,0,false,1,T,0);
  equal(p.poll(AIR,0),AnchorPlan.Action.PLACE,"start");
  p.confirm(AnchorPlan.Action.PLACE,true,0);
  equal(p.poll(AIR,at(80)),AnchorPlan.Action.NONE,"accepted click does not prove placement");
  equal(p.poll(EMPTY,at(81)),AnchorPlan.Action.CHARGE,"actual anchor appears");
  p.confirm(AnchorPlan.Action.CHARGE,true,at(81));
  equal(p.poll(EMPTY,at(300)),AnchorPlan.Action.NONE,"no uncharged detonation");
  equal(p.poll(CHARGED,at(301)),AnchorPlan.Action.DETONATE,"observed charge");
 }
 private static void safeCover(){
  var p=new AnchorPlan(true,1,true,1,T,0);
  equal(p.poll(CHARGED,0),AnchorPlan.Action.COVER,"cover before detonation");
  p.confirm(AnchorPlan.Action.COVER,true,0);
  equal(p.poll(CHARGED,at(100)),AnchorPlan.Action.NONE,"wait for actual cover");
  var covered=new AnchorPlan.Seen(true,1,false,true);
  equal(p.poll(covered,at(101)),AnchorPlan.Action.DETONATE,"cover permits detonation without totem input");
  p.defer(at(101),20);
  equal(p.poll(CHARGED,at(121)),AnchorPlan.Action.NONE,"lost cover prevents detonation");
  equal(p.stage(),AnchorPlan.Stage.CANCELLED,"cover removal cancels");
 }
 private static void doubleAnchor(){
  var p=new AnchorPlan(true,1,false,2,T,0);
  equal(p.poll(CHARGED,0),AnchorPlan.Action.DETONATE,"first");
  p.confirm(AnchorPlan.Action.DETONATE,true,0);
  equal(p.poll(CHARGED,at(500)),AnchorPlan.Action.NONE,"no duplicate place until first removed");
  equal(p.poll(AIR,at(501)),AnchorPlan.Action.NONE,"observe first removal");
  equal(p.poll(AIR,at(625)),AnchorPlan.Action.NONE,"repeat delay cannot fire early");
  equal(p.poll(AIR,at(626)),AnchorPlan.Action.PLACE,"second placement after removal and repeat delay");
  p.confirm(AnchorPlan.Action.PLACE,true,at(626));
  equal(p.poll(EMPTY,at(706)),AnchorPlan.Action.CHARGE,"second charge");
  p.confirm(AnchorPlan.Action.CHARGE,true,at(706));
  equal(p.poll(CHARGED,at(801)),AnchorPlan.Action.DETONATE,"second detonation");
  p.confirm(AnchorPlan.Action.DETONATE,true,at(801));
  p.poll(AIR,at(802));
  equal(p.stage(),AnchorPlan.Stage.FINISHED,"only two cycles");
  equal(p.completed(),2,"cycle count");
  equal(p.poll(AIR,at(1000)),AnchorPlan.Action.NONE,"never a third placement");
 }
 private static void doubleFromAir(){
  var p=new AnchorPlan(false,0,false,2,T,0);
  equal(p.poll(AIR,0),AnchorPlan.Action.PLACE,"fresh double starts with first placement");
  p.confirm(AnchorPlan.Action.PLACE,true,0);
  equal(p.poll(EMPTY,at(80)),AnchorPlan.Action.CHARGE,"fresh first charge");
  p.confirm(AnchorPlan.Action.CHARGE,true,at(80));
  equal(p.poll(CHARGED,at(175)),AnchorPlan.Action.DETONATE,"fresh first detonation");
  p.confirm(AnchorPlan.Action.DETONATE,true,at(175));
  equal(p.poll(AIR,at(176)),AnchorPlan.Action.NONE,"fresh waits for free repeat target");
  equal(p.poll(AIR,at(301)),AnchorPlan.Action.PLACE,"fresh second placement");
  p.confirm(AnchorPlan.Action.PLACE,true,at(301));
  equal(p.poll(EMPTY,at(381)),AnchorPlan.Action.CHARGE,"fresh second charge");
  p.confirm(AnchorPlan.Action.CHARGE,true,at(381));
  equal(p.poll(CHARGED,at(476)),AnchorPlan.Action.DETONATE,"fresh second detonation");
  p.confirm(AnchorPlan.Action.DETONATE,true,at(476));p.poll(AIR,at(477));
  equal(p.completed(),2,"fresh double has exactly two explosions");equal(p.stage(),AnchorPlan.Stage.FINISHED,"fresh double ends");
 }
 private static void doublePredictionRollback(){
  var p=new AnchorPlan(true,1,false,2,T,0);
  p.confirm(AnchorPlan.Action.DETONATE,true,0);
  equal(p.poll(AIR,at(50)),AnchorPlan.Action.NONE,"predicted first removal");
  equal(p.poll(CHARGED,at(100)),AnchorPlan.Action.NONE,"server restores first anchor temporarily");
  equal(p.poll(AIR,at(200)),AnchorPlan.Action.NONE,"confirmed removal restarts free interval");
  equal(p.poll(AIR,at(324)),AnchorPlan.Action.NONE,"no early second anchor after rollback");
  equal(p.poll(AIR,at(325)),AnchorPlan.Action.PLACE,"repeat recovers at exact target instead of cancelling");
  equal(p.completed(),1,"first explosion counted once");
  p.confirm(AnchorPlan.Action.PLACE,true,at(325));
  equal(p.poll(EMPTY,at(405)),AnchorPlan.Action.CHARGE,"second anchor after rollback");
 }
 private static void failureAndTimeout(){
  var denied=new AnchorPlan(false,0,false,1,T,0);
  denied.confirm(AnchorPlan.Action.PLACE,false,0);
  equal(denied.poll(AIR,at(10)),AnchorPlan.Action.NONE,"denied action never advances");
  equal(denied.stage(),AnchorPlan.Stage.CANCELLED,"denied");
  var timeout=new AnchorPlan(true,0,false,1,T,0);
  timeout.poll(EMPTY,at(1500));
  equal(timeout.stage(),AnchorPlan.Stage.CANCELLED,"absolute timeout");
  var blocked=new AnchorPlan(true,1,false,2,T,0);
  blocked.confirm(AnchorPlan.Action.DETONATE,true,0);
  blocked.poll(new AnchorPlan.Seen(false,0,false,false),at(1));
  equal(blocked.stage(),AnchorPlan.Stage.CANCELLED,"blocked repeat position cancels");
 }
 public static void main(String[] args){
  existingAnchors();confirmations();safeCover();doubleAnchor();doubleFromAir();doublePredictionRollback();failureAndTimeout();
  System.out.println("PASS: anchor deadlines, world confirmations, cover loss, double-cycle ordering, denials and timeouts");
 }
}
