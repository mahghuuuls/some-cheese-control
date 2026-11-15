package com.mahghuuuls.somecheesecontrol.mixin.client.vanilla;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mahghuuuls.somecheesecontrol.client.SwingHiding;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The vanilla attack: after sending it, the client resets its own attack bar whatever any event
 * said. The reset is skipped when the client refused this attack, so the bar stays as it was; the
 * server, which refused it too, resets nothing either.
 */
@Mixin(PlayerControllerMP.class)
public abstract class PlayerControllerMPMixin {

    @WrapWithCondition(method = "attackEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;resetCooldown()V"),
            require = 0)
    private boolean scc$resetAttackBar(EntityPlayer player) {
        return SwingHiding.showsAttack();
    }
}
