package com.mahghuuuls.somecheesecontrol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * What the client swing Mixins ask before they let the client show an attack: the arm swing, the
 * attack bar reset and RLCombat's hand animation. They are skipped for an attack the client refused
 * in the same click, so a refused hit shows only the server's message. Every other swing (a block
 * hit, a swing at the air, an allowed attack) is shown.
 *
 * <p>The record is set when the client refuses an attack: a vanilla attack and RLCombat's main-hand
 * attack post the client's attack event before their swing, where {@link EarlyClientRefusal} refuses
 * it; RLCombat's offhand attack posts none, so it asks the climbing rule when it sends its attack. It
 * is cleared at the start of every attack click and at the end of the client tick. It lives here,
 * and not in {@link EarlyClientRefusal}, because the Mixin handlers are static. Everything here runs
 * on the client thread.</p>
 *
 * <p>The Mixins exist only where MixinBooter is present. Which of them changed their code is kept in
 * {@link SwingPaths}; the client log says once, at startup, which swing paths stay visible.</p>
 */
public final class SwingHiding {

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Client");
    private static final String RLCOMBAT_MOD_ID = "bettercombatmod";
    /** Classes the swing Mixins change that the game may not have loaded at startup; loading them lets their Mixins apply. */
    private static final String[] VANILLA_TARGETS = {"net.minecraft.client.multiplayer.PlayerControllerMP"};
    private static final String[] RLCOMBAT_TARGETS = {
            "bettercombat.mod.client.handler.EventHandlersClient", "bettercombat.mod.client.animation.util.BetterCombatHand"};

    private static boolean refusedAttack;
    @Nullable
    private static EarlyClientRefusal refusal;

    private SwingHiding() {
    }

    /** At the start of an attack click: a new click starts with no refused attack. */
    public static void clearAttack() {
        refusedAttack = false;
    }

    /** True when the client may show this attack: its swing, attack bar reset and hand animation. */
    public static boolean showsAttack() {
        return !refusedAttack;
    }

    /** RLCombat's offhand attack, just sent to the server: it posts no client event, so the climbing rule is asked here. */
    public static void onOffhandAttackSent() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (refusal != null && player != null && refusal.refusesMeleeNow(player)) {
            refusedAttack = true;
        }
    }

    /**
     * Once, at the end of startup: loads the target classes the game has not loaded yet, so their
     * Mixins apply, and writes one log line naming the swing paths that stay visible (MixinBooter
     * missing, or another mod changed the same code), or nothing when every path was changed.
     */
    public static void reportInactivePaths() {
        List<String> expected = new ArrayList<>(Arrays.asList(SwingPaths.VANILLA_CLICK, SwingPaths.VANILLA_ATTACK_BAR));
        load(VANILLA_TARGETS);
        if (Loader.isModLoaded(RLCOMBAT_MOD_ID)) {
            load(RLCOMBAT_TARGETS);
            expected.add(SwingPaths.RLCOMBAT_CLICK);
            expected.add(SwingPaths.RLCOMBAT_ANIMATION);
        }
        Set<String> inactive = SwingPaths.inactive(expected.toArray(new String[0]));
        if (!inactive.isEmpty()) {
            LOGGER.info("Swing hiding is inactive for: {}. A refused hit there shows the swing as a miss "
                    + "(MixinBooter is missing, or another mod changed the same code).", String.join(", ", inactive));
        }
    }

    static void attach(EarlyClientRefusal earlyRefusal) {
        refusal = earlyRefusal;
    }

    /** The client refused an attack in this click. */
    static void markRefusedAttack() {
        refusedAttack = true;
    }

    private static void load(String[] classNames) {
        for (String name : classNames) {
            try {
                Class.forName(name, false, SwingHiding.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                // Another version of the target mod; its path is then reported inactive.
            }
        }
    }
}
