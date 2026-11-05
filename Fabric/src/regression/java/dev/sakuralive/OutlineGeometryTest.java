package dev.sakuralive;
import java.util.*;
public final class OutlineGeometryTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[] args){
  var segments=new ArrayList<double[]>();OutlineGeometry.Segment sink=(a,b,c,d,e,f)->segments.add(new double[]{a,b,c,d,e,f});
  OutlineGeometry.edge(0,0,0,1,0,0,.4,false,sink);check(segments.size()==1&&Math.abs(segments.get(0)[0]-.3)<1e-8&&Math.abs(segments.get(0)[3]-.7)<1e-8,"centered shortened edge");
  segments.clear();OutlineGeometry.edge(0,0,0,1,0,0,.4,true,sink);check(segments.size()==2&&Math.abs(segments.get(0)[3]-.2)<1e-8&&Math.abs(segments.get(1)[0]-.8)<1e-8,"two corner stubs share total length");
  segments.clear();OutlineGeometry.edge(0,0,0,0,2,0,1,true,sink);check(segments.size()==1&&segments.get(0)[4]==2,"full vertical edge");
  segments.clear();OutlineGeometry.edge(0,0,0,1,1,1,0,true,sink);check(segments.isEmpty(),"zero length has no segments");
  System.out.println("PASS: outline lengths, corner mode, full edges and zero-length handling");
 }
}
