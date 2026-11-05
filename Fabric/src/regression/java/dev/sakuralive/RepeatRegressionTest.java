package dev.sakuralive;
public final class RepeatRegressionTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[] args){
  var queue=new RepeatGate();for(int i=0;i<1000;i++)queue.request();check(queue.take(),"spam retains one following sequence");check(!queue.take(),"no runaway backlog after 1000 presses");queue.request();check(queue.take()&&!queue.take(),"a later press can queue the next cycle");
  var timing=new AnchorPlan.Timings(1,1,1,1,1000);
  var plan=new AnchorPlan(false,0,false,1,timing,0);
  plan.confirm(AnchorPlan.Action.PLACE,true,0);
  var uncharged=new AnchorPlan.Seen(true,0,false,false);
  check(plan.poll(uncharged,999999)==AnchorPlan.Action.NONE,"one millisecond never rounded down");
  check(plan.poll(uncharged,1000000)==AnchorPlan.Action.CHARGE,"one millisecond deadline accepted exactly");
  plan.confirm(AnchorPlan.Action.CHARGE,true,1000000);
  var charged=new AnchorPlan.Seen(true,1,false,false);
  check(plan.poll(charged,1999999)==AnchorPlan.Action.NONE,"charge pause keeps nanosecond deadline");
  check(plan.poll(charged,2000000)==AnchorPlan.Action.DETONATE,"charge pause is not clamped to 10ms");
  plan.confirm(AnchorPlan.Action.DETONATE,true,2000000);
  check(plan.poll(charged,9000000)==AnchorPlan.Action.NONE&&!plan.done(),"no new place while old anchor still exists");
  var safe=new AnchorPlan(true,1,true,1,timing,0);
  check(safe.poll(charged,0)==AnchorPlan.Action.COVER,"safe repeat still requires cover");
  safe.confirm(AnchorPlan.Action.COVER,true,0);check(safe.poll(charged,1000000)==AnchorPlan.Action.NONE,"no detonation without observed cover");
  var lostClick=new AnchorPlan(false,0,false,1,timing,0);
  lostClick.confirm(AnchorPlan.Action.PLACE,true,0);
  var air=new AnchorPlan.Seen(false,0,true,false);
  check(lostClick.retryAfterProcessing(air,999999)==AnchorPlan.Action.NONE,"retry respects configured 1ms deadline");
  check(lostClick.retryAfterProcessing(air,1000000)==AnchorPlan.Action.NONE,"unconfirmed placement is never blindly sent twice");
  check(lostClick.retryAfterProcessing(uncharged,1000000)==AnchorPlan.Action.NONE,"late successful place prevents duplicate placement");
  lostClick.confirm(AnchorPlan.Action.PLACE,true,1000000);
  check(lostClick.poll(uncharged,2000000)==AnchorPlan.Action.CHARGE,"retry recovers the existing anchor sequence");
  lostClick.confirm(AnchorPlan.Action.CHARGE,true,2000000);
  check(lostClick.retryAfterProcessing(uncharged,3000000)==AnchorPlan.Action.CHARGE,"missed charge retries");
  check(lostClick.retryAfterProcessing(charged,3000000)==AnchorPlan.Action.NONE,"confirmed charge prevents repeat charge");
  var blocked=new AnchorPlan(false,0,false,1,timing,0);blocked.confirm(AnchorPlan.Action.PLACE,true,0);
  for(int ms=1;ms<1000;ms++)blocked.confirm(AnchorPlan.Action.PLACE,true,AnchorPlan.ms(ms));
  blocked.poll(air,AnchorPlan.ms(1000));check(blocked.stage()==AnchorPlan.Stage.CANCELLED,"retries never extend absolute timeout forever");
  var events=new java.util.ArrayList<String>();var key=new InputPulse.Code(false,49,49);var mouse=new InputPulse.Code(true,4096,2);
  var batch=new InputPulse(new InputPulse.Transport(){public void press(InputPulse.Code code){events.add("down:"+code.value());}public void release(InputPulse.Code code){events.add("up:"+code.value());}});
  check(batch.tapTogether(key,mouse,0,1,true),"slot and use can share one input processing step");
  check(events.equals(java.util.List.of("up:49","up:4096","down:49","down:4096")),"slot precedes use in grouped input");
  check(!batch.tapTogether(key,mouse,1,1,true),"grouped input cannot overlap itself");batch.tick(1000000,true);check(batch.idle(),"both grouped keys release");
  var failedRelease=new java.util.HashSet<Integer>();
  var partial=new InputPulse(new InputPulse.Transport(){
   public void press(InputPulse.Code code){if(code.mouse()){failedRelease.add(49);throw new IllegalStateException("second input denied");}}
   public void release(InputPulse.Code code){if(failedRelease.remove(code.value()))throw new IllegalStateException("release retry");}
  });
  try{partial.tapTogether(key,mouse,0,1,true);throw new AssertionError("partial pair must fail");}catch(IllegalStateException expected){}
  check(!partial.idle(),"failed key-up remains tracked after partial pair failure");partial.releaseAll();check(partial.idle(),"partial pair cleanup can retry safely");
  var clocked=new InputPulse(new InputPulse.Transport(){public void press(InputPulse.Code code){}public void release(InputPulse.Code code){}public long holdStartsAt(long fallback){return 37000000;}});
  clocked.tap(key,0,1,true);clocked.tick(37999999,true);check(!clocked.idle(),"native hold begins after sending, not at stale render timestamp");clocked.tick(38000000,true);check(clocked.idle(),"requested hold ends one full millisecond after send");
  System.out.println("PASS: spam coalesces without backlog; exact 1ms deadlines; removal and safe-cover confirmations retained (deterministic tests, not OS timing)");
 }
}
