package dev.sakuralive;
public final class SpeedRegressionTest {
 static long run(int delay){
  var p=new AnchorPlan(false,0,false,1,new AnchorPlan.Timings(delay,delay,delay,0,2000),0);
  int state=0,next=-1;long confirmAt=-1;
  for(int ms=0;ms<3000;ms++){
   if(ms==confirmAt){state=next;confirmAt=-1;}
   var action=p.poll(new AnchorPlan.Seen(state>0,state==2?1:0,state==0,false),AnchorPlan.ms(ms));
   if(p.done())return ms;
   if(action!=AnchorPlan.Action.NONE){next=switch(action){case PLACE->1;case CHARGE->2;default->0;};confirmAt=(ms/50+1)*50;p.confirm(action,true,AnchorPlan.ms(ms));}
  }
  throw new AssertionError("sequence timed out");
 }
 public static void main(String[] args){long turbo=run(0),fast=run(25),slow=run(150);if(!(turbo<slow&&fast<slow))throw new AssertionError("sliders must affect timing");System.out.println("PASS: 50ms confirmation model: Turbo="+turbo+"ms, Fast="+fast+"ms, 150ms pauses="+slow+"ms (model, not gameplay)");}
}
