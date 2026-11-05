package dev.sakuralive.mixin;
import dev.sakuralive.SakuraClient;
import net.minecraft.client.network.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Allows only the requested target/slot while a macro owns Use; never invokes an interaction or sends a packet. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class InteractionReceiptMixin {
 @Inject(method="interactBlock",at=@At("HEAD"),cancellable=true)
 private void sakura$guard(ClientPlayerEntity player,Hand hand,BlockHitResult hit,CallbackInfoReturnable<ActionResult> ci){if(!SakuraClient.allowBlockInteraction(player,hand,hit))ci.setReturnValue(ActionResult.FAIL);}
 @Inject(method="interactBlock",at=@At("RETURN"))
 private void sakura$receipt(ClientPlayerEntity player,Hand hand,BlockHitResult hit,CallbackInfoReturnable<ActionResult> ci){SakuraClient.observeInteraction(player,hand,hit,ci.getReturnValue());}
}
