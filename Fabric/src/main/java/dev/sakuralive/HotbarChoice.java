package dev.sakuralive;
import java.util.function.IntPredicate;
/** Prefer the configured slot; optionally find an actual matching stack in the hotbar. */
final class HotbarChoice {
 static int choose(int preferred,IntPredicate acceptable){
  if(preferred>=0&&preferred<9&&acceptable.test(preferred))return preferred;
  for(int i=0;i<9;i++)if(acceptable.test(i))return i;
  return -1;
 }
}
