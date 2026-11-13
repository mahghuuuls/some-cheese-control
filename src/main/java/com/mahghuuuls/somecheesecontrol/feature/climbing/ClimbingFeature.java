package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps the climbing state current and refuses melee attacks in it.
 *
 * <p>Melee refusal uses the attack event, which RLCombat also posts for both hands (as a subclass)
 * before any damage, durability or cooldown work; Spartan Weaponry's melee runs only after an
 * attack event that nobody cancelled.</p>
 */
public final class ClimbingFeature {

    private final boolean melee;

    public ClimbingFeature(Settings settings) {
        this.melee = settings.climbingMelee();
    }

    /**
     * True when any climbing restriction is on, so the state must be tracked. Every climbing
     * restriction added later must be listed here, or it gets no release delay.
     */
    public static boolean isNeeded(Settings settings) {
        return settings.climbingMelee();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote) {
            ClimbingTracker.update(event.player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ClimbingTracker.forget(event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ClimbingTracker.forget(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ClimbingTracker.forget(event.player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        ClimbingTracker.forget(event.player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEntityEvent event) {
        EntityPlayer attacker = event.getEntityPlayer();
        if (!melee || attacker.world.isRemote || !(attacker instanceof EntityPlayerMP)
                || !PlayerExemptions.applies(attacker, Rule.CLIMBING_MELEE) || !ClimbingTracker.isRestricted(attacker)) {
            return;
        }
        event.setCanceled(true);
        Refusals.refuse((EntityPlayerMP) attacker, Rule.CLIMBING_MELEE, Resync.inventory(), attacker.getHeldItemMainhand(),
                event.getTarget().getPosition());
    }
}
