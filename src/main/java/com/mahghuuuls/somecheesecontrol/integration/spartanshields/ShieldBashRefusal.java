package com.mahghuuuls.somecheesecontrol.integration.spartanshields;

import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingTracker;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import com.oblivioussp.spartanshields.item.ItemShieldBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * The one question the shield bash Mixin asks: is this bash refused? Spartan Shields handles the
 * bash packet on the server thread and spends nothing before the Mixin asks, so a refused bash has
 * no effect at all. The client has already swung the arm; that swing stays (an accepted exception
 * to the early client refusal).
 *
 * <p>It answers "no" until {@link SpartanShieldsIntegration} activates it, and again after an error,
 * which is reported to {@link IntegrationGate} once.</p>
 */
public final class ShieldBashRefusal {

    private static volatile boolean active;

    private ShieldBashRefusal() {
    }

    static void activate() {
        active = true;
    }

    /**
     * True when the bash must not run; the refusal's message and line are already sent.
     *
     * @param victimId the entity id the bash aims at, when {@code attacksEntity}
     */
    public static boolean refuses(EntityPlayerMP player, @Nullable EnumHand hand, int victimId, boolean attacksEntity) {
        if (!active || player == null || hand == null) {
            return false;
        }
        try {
            ItemStack shield = player.getHeldItem(hand);
            boolean spartanShield = !shield.isEmpty() && shield.getItem() instanceof ItemShieldBase;
            boolean blocking = player.isActiveItemStackBlocking();
            boolean coolingDown = spartanShield && player.getCooldownTracker().hasCooldown(shield.getItem());
            if (!ShieldBashRules.refuses(blocking, spartanShield, coolingDown, PlayerExemptions.applies(player, Rule.CLIMBING_MELEE),
                    ClimbingTracker.isRestricted(player))) {
                return false;
            }
            Refusals.refuse(player, Rule.CLIMBING_MELEE, Resync.none(), shield, target(player, victimId, attacksEntity), "attack", "shieldBash");
            return true;
        } catch (RuntimeException | LinkageError e) {
            active = false;
            IntegrationGate.fail(SpartanShieldsIntegration.MOD_ID, e);
            return false;
        }
    }

    @Nullable
    private static BlockPos target(EntityPlayerMP player, int victimId, boolean attacksEntity) {
        Entity victim = attacksEntity ? player.world.getEntityByID(victimId) : null;
        return victim != null ? victim.getPosition() : null;
    }
}
