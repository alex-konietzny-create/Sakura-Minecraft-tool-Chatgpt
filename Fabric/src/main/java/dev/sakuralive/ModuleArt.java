package dev.sakuralive;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.*;
/** Uses Minecraft's own item renderer, including the active resource pack. */
final class ModuleArt {
 static void draw(DrawContext c,int module,int x,int y,float scale){
  Item item=switch(module){case 1->Items.GLOWSTONE;case 2->Items.TOTEM_OF_UNDYING;case 4->Items.END_CRYSTAL;case 5->Items.OBSIDIAN;case 6->Items.TARGET;default->Items.RESPAWN_ANCHOR;};
  c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(scale,scale);
  c.drawItem(new ItemStack(item),0,0);
  if(module==3){c.getMatrices().translate(7,6);c.drawItem(new ItemStack(Items.RESPAWN_ANCHOR),0,0);}
  c.getMatrices().popMatrix();
 }
}
