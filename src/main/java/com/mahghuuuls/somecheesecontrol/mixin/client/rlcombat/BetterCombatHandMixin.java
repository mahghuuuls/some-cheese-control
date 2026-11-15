package com.mahghuuuls.somecheesecontrol.mixin.client.rlcombat;

import com.mahghuuuls.somecheesecontrol.client.SwingHiding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RLCombat's hand animation. It is skipped at the head of the method instead of at its call, because
 * the call's receiver is an RLCombat type this mod does not compile against. The record it reads is
 * set only by a refused attack and cleared at the next click, so other animations still play.
 */
@Pseudo
@Mixin(targets = "bettercombat.mod.client.animation.util.BetterCombatHand", remap = false)
public abstract class BetterCombatHandMixin {

    @Inject(method = "initiateAnimation()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void scc$startAnimation(CallbackInfo ci) {
        if (!SwingHiding.showsAttack()) {
            ci.cancel();
        }
    }
}
