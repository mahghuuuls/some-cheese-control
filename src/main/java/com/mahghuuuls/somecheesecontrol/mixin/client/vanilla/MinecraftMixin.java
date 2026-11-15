package com.mahghuuuls.somecheesecontrol.mixin.client.vanilla;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mahghuuuls.somecheesecontrol.client.SwingHiding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla left click: it swings the arm after an attack whatever any event said. The swing is
 * skipped when the client refused the attack of this click; a block hit, a swing at the air and an
 * allowed attack still swing.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "clickMouse", at = @At("HEAD"), require = 0)
    private void scc$startClick(CallbackInfo ci) {
        SwingHiding.clearAttack();
    }

    @WrapWithCondition(method = "clickMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingArm(Lnet/minecraft/util/EnumHand;)V"),
            require = 0)
    private boolean scc$swing(EntityPlayerSP player, EnumHand hand) {
        return SwingHiding.showsAttack();
    }
}
