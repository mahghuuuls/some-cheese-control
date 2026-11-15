package com.mahghuuuls.somecheesecontrol.mixin.client.rlcombat;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mahghuuuls.somecheesecontrol.client.SwingHiding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.MouseEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RLCombat's client attack, which replaces the vanilla left click and adds an offhand attack: after
 * sending an attack it swings the arm, resets the attack bar and its own animation timer. These are
 * skipped when the client refused the attack of this click. A swing at the air uses the same calls
 * without an attack, so it is never skipped.
 *
 * <p>The target belongs to RLCombat and is named by string ({@code Pseudo}: it may be absent), so
 * RLCombat is not needed to build. Methods and calls are written in full, with the names the game
 * uses at run time, because RLCombat is never present in the development game; they were read from
 * RLCombat 2.2.4's classes and match 2.2.6's source.</p>
 */
@Pseudo
@Mixin(targets = "bettercombat.mod.client.handler.EventHandlersClient", remap = false)
public abstract class EventHandlersClientMixin {

    private static final String SWING = "Lnet/minecraft/entity/player/EntityPlayer;func_184609_a(Lnet/minecraft/util/EnumHand;)V";
    private static final String RESET_ATTACK_BAR = "Lnet/minecraft/entity/player/EntityPlayer;func_184821_cY()V";
    private static final String LEFT_CLICK = "onMouseLeftClick(Lnet/minecraftforge/client/event/MouseEvent;)V";
    private static final String RIGHT_CLICK = "onMouseRightClick()V";
    private static final String SEND_TO_SERVER =
            "Lnet/minecraftforge/fml/common/network/simpleimpl/SimpleNetworkWrapper;sendToServer(Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;)V";

    @Inject(method = LEFT_CLICK, at = @At("HEAD"), require = 0)
    private static void scc$startMainhandClick(MouseEvent event, CallbackInfo ci) {
        SwingHiding.clearAttack();
    }

    @Inject(method = RIGHT_CLICK, at = @At("HEAD"), require = 0)
    private static void scc$startOffhandClick(CallbackInfo ci) {
        SwingHiding.clearAttack();
    }

    /** The offhand attack posts no client event; the climbing rule is asked when its packet has gone. */
    @Inject(method = RIGHT_CLICK, at = @At(value = "INVOKE", target = SEND_TO_SERVER, shift = At.Shift.AFTER), require = 0)
    private static void scc$offhandAttackSent(CallbackInfo ci) {
        SwingHiding.onOffhandAttackSent();
    }

    @WrapWithCondition(method = {LEFT_CLICK, RIGHT_CLICK}, at = @At(value = "INVOKE", target = SWING), require = 0)
    private static boolean scc$swing(EntityPlayer player, EnumHand hand) {
        return SwingHiding.showsAttack();
    }

    @WrapWithCondition(method = LEFT_CLICK, at = @At(value = "INVOKE", target = RESET_ATTACK_BAR), require = 0)
    private static boolean scc$resetAttackBar(EntityPlayer player) {
        return SwingHiding.showsAttack();
    }

    @WrapWithCondition(method = LEFT_CLICK, at = @At(value = "INVOKE",
            target = "Lbettercombat/mod/client/handler/EventHandlersClient;resetMainhandAnimationCooldown(F)V"), require = 0)
    private static boolean scc$resetMainhandAnimation(float cooldown) {
        return SwingHiding.showsAttack();
    }

    @WrapWithCondition(method = RIGHT_CLICK, at = @At(value = "INVOKE",
            target = "Lbettercombat/mod/client/handler/EventHandlersClient;resetOffhandAnimationCooldown(F)V"), require = 0)
    private static boolean scc$resetOffhandAnimation(float cooldown) {
        return SwingHiding.showsAttack();
    }
}
