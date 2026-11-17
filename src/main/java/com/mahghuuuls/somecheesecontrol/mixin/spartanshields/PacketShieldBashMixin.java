package com.mahghuuuls.somecheesecontrol.mixin.spartanshields;

import com.mahghuuuls.somecheesecontrol.integration.spartanshields.BashRefusalApplied;
import com.mahghuuuls.somecheesecontrol.integration.spartanshields.ShieldBashRefusal;
import com.oblivioussp.spartanshields.network.PacketShieldBash;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Asks {@link ShieldBashRefusal} before Spartan Shields' server bash handler does anything, and
 * skips the whole handler when the bash is refused: no damage, knockback, durability, sound, swing
 * or cooldown. It targets the concrete handler, not the generic bridge method. With
 * {@code require = 0}, another Spartan Shields version only leaves the bash unrefused, which the
 * integration's applied-check reports. The class implements {@link BashRefusalApplied} so the
 * integration can see that the patch is in place.
 *
 * <p>Priority 999, below the default 1000: when another mod's Mixin at the default priority and
 * injector order also injects at the head of this handler (JASS Integrations charges stamina there),
 * Mixin applies this one first, so it runs first and a refused bash reaches no other check. A head
 * injection with a lower priority or order would still run before it. Without a priority the order would follow MixinBooter's
 * unordered set of late loaders and could change between launches.</p>
 */
@Mixin(value = PacketShieldBash.class, priority = 999, remap = false)
public abstract class PacketShieldBashMixin implements BashRefusalApplied {

    @Shadow
    protected EnumHand hand;

    @Shadow
    protected int entityId;

    @Shadow
    protected boolean attackEntity;

    @Inject(method = "handleServerSide(Lcom/oblivioussp/spartanshields/network/PacketShieldBash;Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void somecheesecontrol$refuseBash(PacketShieldBash message, EntityPlayerMP player, CallbackInfo ci) {
        PacketShieldBashMixin received = (PacketShieldBashMixin) (Object) message;
        if (received != null && ShieldBashRefusal.refuses(player, received.hand, received.entityId, received.attackEntity)) {
            ci.cancel();
        }
    }
}
