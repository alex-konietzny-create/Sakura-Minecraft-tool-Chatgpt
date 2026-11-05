package dev.sakuralive;
import java.util.function.IntPredicate;
/** A physical press has one owner; inactive modules cannot steal or cancel an active shortcut. */
final class ShortcutChoice {
 static int choose(IntPredicate matches,IntPredicate eligible,int owner){
  if(matches.test(7)&&eligible.test(7))return 7;
  if(owner>=0&&owner<4&&matches.test(owner)&&eligible.test(owner))return owner;
  int selected=-1;
  for(int i=0;i<7;i++)if(matches.test(i)&&eligible.test(i)){if(selected>=0)return -2;selected=i;}
  return selected;
 }
}
