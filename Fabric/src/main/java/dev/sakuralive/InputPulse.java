package dev.sakuralive;

import java.util.*;

/** Native-event scheduling with an injectable transport; tests never press real keys. */
final class InputPulse {
 record Code(boolean mouse,int value,int physicalVk) {}
 interface Transport {void press(Code code);void release(Code code);}
 private final Transport transport;
 private final Map<Code,Long> held=new HashMap<>();
 InputPulse(Transport transport){this.transport=transport;}
 synchronized boolean tap(Code code,long now,int durationMs,boolean focused){
  if(!focused){releaseAll();return false;}
  if(held.containsKey(code))return false;
  // Release first so a physically held use button still produces a new native down event.
  transport.release(code);
  try{transport.press(code);held.put(code,now+durationMs*1_000_000L);return true;}
  catch(RuntimeException e){transport.release(code);throw e;}
 }
 synchronized void tick(long now,boolean focused){
  if(!focused){releaseAll();return;}
  var it=held.entrySet().iterator();
  while(it.hasNext()){var entry=it.next();if(now-entry.getValue()>=0){transport.release(entry.getKey());it.remove();}}
 }
 synchronized void release(Code code){held.remove(code);transport.release(code);}
 synchronized void releaseAll(){
  RuntimeException failure=null;
  for(var code:new ArrayList<>(held.keySet())){
   try{transport.release(code);held.remove(code);}catch(RuntimeException e){failure=e;}
  }
  if(failure!=null)throw failure;
 }
 synchronized boolean idle(){return held.isEmpty();}
}

