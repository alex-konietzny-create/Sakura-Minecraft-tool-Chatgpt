package dev.sakuralive;
/** Bounded native click attempts; interaction ownership still decides what Vanilla may execute. */
final class DoubleClickBurst {
 private int clicks;private long next;
 boolean ready(long now){return clicks<20&&now-next>=0;}
 void sent(long now,int pulseMs){clicks++;next=now+Math.max(5,pulseMs+1)*1_000_000L;}
}
