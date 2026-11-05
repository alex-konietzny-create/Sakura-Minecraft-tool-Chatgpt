package dev.sakuralive;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Only watched down/up states, never typed text; no state is retained outside focus. */
final class PhysicalTriggers {
 private final Set<Integer> watched=new HashSet<>();
 private final Map<Integer,Boolean> down=new HashMap<>();
 synchronized boolean isDown(int key,BooleanSupplier initial){
  watched.add(key);
  if(!down.containsKey(key))down.put(key,initial.getAsBoolean());
  return down.get(key);
 }
 synchronized void event(int key,boolean pressed,boolean injected,boolean foreground){
  if(!foreground){down.clear();return;}
  if(!injected&&watched.contains(key))down.put(key,pressed);
 }
 synchronized void clear(){down.clear();}
}

