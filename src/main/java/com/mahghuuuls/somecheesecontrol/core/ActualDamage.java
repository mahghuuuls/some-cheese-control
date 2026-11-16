package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The only owner of "actual damage": damage applied to an entity after armor, potions and
 * absorption, also when it only removes absorption hearts, and also when it kills. The
 * countermeasures ask {@link #isActual} from their own damage listeners.
 *
 * <p>The game takes a hit's absorption part before it posts the damage event, so a hit that only
 * removes absorption hearts arrives there with no damage left. The absorption is therefore noted
 * when the hit starts (the hurt event) and compared at the damage event. The note listens last and
 * the countermeasures listen last, all without canceled events, so a hit another mod cancels never
 * counts; a hit blocked by a shield, or fully absorbed by the invulnerability time, never reaches the
 * damage event. A stronger hit during the invulnerability time does reach it, with the difference as
 * damage, and is not counted: a hit during that time is not actual damage.</p>
 *
 * <p>Registered when any countermeasure is on.</p>
 */
public final class ActualDamage {

    private static final ServerState.Slot<Notes> NOTES = new ServerState.Slot<>(Notes.class, Notes::new);

    /**
     * True once the note listener is made; the main class makes and registers it on the same line.
     * Without it an absorption-only hit would be missed, so {@link #isActual} throws instead: a
     * wiring mistake shows at the first hit (Forge rethrows it, so the server crashes) rather than hiding.
     */
    private static volatile boolean registered;

    public ActualDamage() {
        registered = true;
    }

    /**
     * True when a countermeasure that asks for actual damage is on. A new countermeasure is added
     * here; one that asks {@link #isActual} while this is not registered fails at once.
     */
    public static boolean isNeeded(Settings settings) {
        return settings.cobwebRelease() || settings.boatEscape();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            ServerState.get().get(NOTES).byEntity.put(entity, new Note(entity.getAbsorptionAmount(), ServerState.tick()));
        }
    }

    /**
     * True when the hit of this damage event is actual damage. Call it from a damage listener at the
     * lowest priority that does not receive canceled events; it changes nothing, so every
     * countermeasure can ask.
     */
    public static boolean isActual(LivingDamageEvent event) {
        if (!registered) {
            throw new IllegalStateException("ActualDamage is asked but not registered; add the countermeasure to isNeeded");
        }
        EntityLivingBase entity = event.getEntityLiving();
        if (isInvulnerabilityHit(entity.hurtResistantTime, entity.maxHurtResistantTime)) {
            return false;
        }
        Note note = ServerState.get().get(NOTES).byEntity.get(entity);
        float after = entity.getAbsorptionAmount();
        float before = note != null ? absorptionBefore(note.absorption, note.tick, ServerState.tick(), after) : after;
        return isActualDamage(event.getAmount(), before, after);
    }

    /**
     * The absorption when the hit started: the note's, when it was made in this tick (the hit's own
     * hurt event); otherwise the absorption now, so an old note never makes a hit count.
     */
    static float absorptionBefore(float noted, long noteTick, long now, float after) {
        return noteTick == now ? noted : after;
    }

    /**
     * True when a hit is actual damage: health was lost, or only absorption hearts were.
     *
     * @param amount the damage left after armor, potions and absorption
     * @param absorptionBefore the absorption when the hit started
     * @param absorptionAfter the absorption after the hit's absorption part was taken
     */
    static boolean isActualDamage(float amount, float absorptionBefore, float absorptionAfter) {
        return amount > 0F || absorptionAfter < absorptionBefore;
    }

    /**
     * True when a hit arrives during the entity's invulnerability time. A new hit sets the time to
     * its maximum just before the damage is applied, so the time is exactly at its maximum for a new
     * hit. While the time is above half its maximum (an earlier hit, or a time the game set higher, as
     * for the riders of a skeleton trap), the game lets a hit through only when it is stronger than
     * the last one, and then only the difference is applied: that is such a hit. In the game, damage
     * reaches a mob only through its hit logic; damage another mod applied directly to a mob hit in
     * the last half second would also be taken for such a hit.
     */
    static boolean isInvulnerabilityHit(int hurtResistantTime, int maxHurtResistantTime) {
        return hurtResistantTime > maxHurtResistantTime / 2.0F && hurtResistantTime != maxHurtResistantTime;
    }

    /** The absorption of an entity when its latest hit started, and the server tick of that hit. */
    private static final class Note {
        final float absorption;
        final long tick;

        Note(float absorption, long tick) {
            this.absorption = absorption;
            this.tick = tick;
        }
    }

    /**
     * The note of each entity's latest hit, per server. Every hit posts the hurt event before the
     * damage event in the same tick, so a damage event reads the note of its own hit; the next hurt
     * event replaces a note whose hit stopped in between (a shield block, or another mod setting the
     * damage to 0), and a note from an earlier tick is not used. Weak keys, so an entity that is gone
     * is not kept.
     */
    private static final class Notes {
        final Map<EntityLivingBase, Note> byEntity = new WeakHashMap<>();
    }
}
