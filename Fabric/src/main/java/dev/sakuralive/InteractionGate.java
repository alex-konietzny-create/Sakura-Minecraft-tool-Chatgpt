package dev.sakuralive;
/** One accepted Minecraft block-use per issued macro phase. */
final class InteractionGate {
 enum Decision {ALLOW,SUPPRESS,REJECT}
 private boolean armed,accepted;
 void arm(){armed=true;accepted=false;}
 boolean accepted(){return accepted;}
 void result(boolean ok){if(ok)accepted=true;}
 Decision evaluate(boolean waiting,boolean complete,boolean mainHand,boolean slot,boolean target,boolean item){
  if(!armed||!waiting||accepted)return Decision.SUPPRESS;
  if(complete){accepted=true;return Decision.SUPPRESS;}
  return mainHand&&slot&&target&&item?Decision.ALLOW:Decision.REJECT;
 }
}
