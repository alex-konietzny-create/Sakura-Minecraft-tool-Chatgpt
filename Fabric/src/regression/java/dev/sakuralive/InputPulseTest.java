package dev.sakuralive;
import java.util.*;
public final class InputPulseTest {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args){
  var events=new ArrayList<String>();
  var key=new InputPulse.Code(false,49,49);
  var mouse=new InputPulse.Code(true,4096,2);
  var queue=new InputPulse(new InputPulse.Transport(){
   public void press(InputPulse.Code c){events.add("down:"+c.value());}
   public void release(InputPulse.Code c){events.add("up:"+c.value());}
  });
  check(!queue.tap(key,0,25,false)&&events.isEmpty(),"no key-down outside foreground");
  check(queue.tap(key,0,25,true),"slot pulse");
  check(events.equals(List.of("up:49","down:49")),"new edge instead of relying on held state");
  check(!queue.tap(key,1,25,true),"no repeated down while owned");
  queue.tick(24_999_999,true);check(!queue.idle(),"minimum hold duration");
  queue.tick(25_000_000,true);check(queue.idle()&&events.get(2).equals("up:49"),"release at deadline");
  queue.tap(key,30_000_000,25,true);queue.tap(mouse,31_000_000,25,true);
  long before=events.stream().filter(e->e.startsWith("down:")).count();
  check(!queue.tap(mouse,32_000_000,25,false),"lost focus refuses new input");
  check(queue.idle(),"focus loss releases all owned buttons");
  check(events.stream().filter(e->e.startsWith("down:")).count()==before,"no down after focus loss");
  var failures=new HashSet<Integer>();
  var cleanup=new InputPulse(new InputPulse.Transport(){
   public void press(InputPulse.Code c){}
   public void release(InputPulse.Code c){if(failures.remove(c.value()))throw new IllegalStateException("retry");}
  });
  cleanup.tap(key,0,25,true);cleanup.tap(mouse,0,25,true);failures.add(key.value());
  try{cleanup.releaseAll();}catch(IllegalStateException expected){}
  cleanup.releaseAll();check(cleanup.idle(),"release failures do not strand other buttons; failed releases retry");
  var trigger=new PhysicalTriggers();
  check(trigger.isDown(2,()->true),"initial physical hold");
  trigger.event(2,false,true,true);check(trigger.isDown(2,()->false),"synthetic release cannot end physical hold");
  trigger.event(2,false,false,true);check(!trigger.isDown(2,()->true),"real release ends hold");
  trigger.event(2,true,false,true);check(trigger.isDown(2,()->false),"real repress");
  trigger.event(2,true,false,false);check(!trigger.isDown(2,()->false),"focus loss clears physical states");
  System.out.println("PASS: system pulses, minimum hold, no overlap, foreground guard, release-all and cleanup retry; fake transport only");
 }
}
