package dev.sakuralive;
public final class CrystalCycleTest {
 private static final CrystalCycle.Timings T=new CrystalCycle.Timings(80,70,900);
 private static long at(int ms){return AnchorPlan.ms(ms);}
 private static void eq(Object actual,Object expected){if(!actual.equals(expected))throw new AssertionError(actual+" != "+expected);}
 public static void main(String[] args){
  var p=new CrystalCycle();
  eq(p.poll(0,-1,true,false,true,T),CrystalCycle.Action.PLACE);
  p.placed(true,0,T);
  eq(p.poll(at(20),4,false,false,true,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(70),4,false,false,true,T),CrystalCycle.Action.BREAK);
  p.attacked(4,at(70),T);
  eq(p.poll(at(139),4,false,true,true,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(140),4,false,true,true,T),CrystalCycle.Action.BREAK);
  p.attacked(4,at(140),T);
  eq(p.poll(at(200),4,false,true,true,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(201),-1,true,false,true,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(280),-1,true,false,true,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(281),-1,true,false,true,T),CrystalCycle.Action.PLACE);
  p.reset();eq(p.poll(at(300),8,false,false,true,T),CrystalCycle.Action.BREAK);
  p.reset();eq(p.poll(at(301),-1,true,false,true,T),CrystalCycle.Action.PLACE);
  p.placed(true,at(301),T);
  eq(p.poll(at(350),-1,false,false,false,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(351),-1,true,false,true,T),CrystalCycle.Action.PLACE);
  p.placed(false,at(351),T);
  eq(p.poll(at(430),-1,true,false,true,T),CrystalCycle.Action.NONE);
  eq(p.poll(at(431),-1,true,false,true,T),CrystalCycle.Action.PLACE);
  p.placed(true,at(431),T);
  p.poll(at(1400),-1,false,false,true,T);
  eq(p.poll(at(1480),-1,true,false,true,T),CrystalCycle.Action.PLACE);
  System.out.println("PASS: fresh crystal hold, obstructing crystal, release/repress, target change, denial and timeout recovery; no manual first attack");
 }
}
