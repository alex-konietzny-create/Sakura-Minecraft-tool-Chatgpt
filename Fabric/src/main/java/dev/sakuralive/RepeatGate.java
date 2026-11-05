package dev.sakuralive;
/** Coalesces spam into one following run, never a cancellation or an unbounded backlog. */
final class RepeatGate {
 private boolean queued;
 void request(){queued=true;}
 boolean take(){boolean result=queued;queued=false;return result;}
}
