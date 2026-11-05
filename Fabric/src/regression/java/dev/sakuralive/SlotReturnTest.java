package dev.sakuralive;
public final class SlotReturnTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  var s=new SlotReturn(8);check(s.poll(true,0,1,0)==SlotReturn.Decision.PRESS,"request final slot before inventory opens");s.sent(1,0);
  check(s.poll(true,0,1,50_000_000)==SlotReturn.Decision.WAIT,"no duplicate in same input tick");
  check(s.poll(true,8,2,60_000_000)==SlotReturn.Decision.FINISH,"only observed final slot permits inventory");
  s=new SlotReturn(8);check(s.poll(false,0,3,5_000_000_000L)==SlotReturn.Decision.WAIT,"never send a hotbar key in inventory / without focus");
  check(s.poll(true,0,4,20_000_000_000L)==SlotReturn.Decision.PRESS,"resume after long menu without stale timeout");
  s.sent(4,20_000_000_000L);check(s.poll(true,0,6,20_100_000_000L)==SlotReturn.Decision.PRESS,"bounded retry after actual ticks");
  check(s.poll(true,0,50,22_000_000_000L)==SlotReturn.Decision.TIMEOUT,"failed key cannot block inventory forever");
  System.out.println("PASS: inventory handoff waits for actual final slot, never injects into menus, resumes safely and times out");
 }
}
