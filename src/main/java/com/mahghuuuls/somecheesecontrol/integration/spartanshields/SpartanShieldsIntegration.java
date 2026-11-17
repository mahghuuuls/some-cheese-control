package com.mahghuuuls.somecheesecontrol.integration.spartanshields;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Spartan Shields: the shield bash refusal in the climbing state. Loaded by {@link IntegrationGate}
 * only when Spartan Shields is present. The refusal itself is a Mixin that MixinBooter queues; this
 * class checks at start that the patched handler exists and the Mixin was applied. It then says in
 * the log that the refusal is active, or why not, and the status command names the reason. With the
 * climbing melee switch off it only notes that, with no warning.
 *
 * <p>The packet class is looked up by name, so a Spartan Shields version without it is reported as
 * having no handler to patch rather than as a failure.</p>
 */
public final class SpartanShieldsIntegration implements IntegrationGate.Integration {

    static final String MOD_ID = "spartanshields";

    private static final String PACKET_CLASS = "com.oblivioussp.spartanshields.network.PacketShieldBash";
    private static final String PLAYER_CLASS = "net.minecraft.entity.player.EntityPlayerMP";

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Integrations");

    private final List<String> problems = new ArrayList<>();

    /** A class-loading error met while checking, named in the warning so it is not mistaken for another version. */
    private Throwable lookupError;

    @Override
    public void start() {
        boolean meleeOn = Settings.get().climbingMelee();
        Class<?> packet = packetClass();
        String reason = ShieldBashRules.inactiveReason(meleeOn, packet != null && hasBashHandler(packet),
                packet != null && BashRefusalApplied.class.isAssignableFrom(packet), Loader.isModLoaded("mixinbooter"));
        if (reason == null) {
            ShieldBashRefusal.activate();
            LOGGER.info("Spartan Shields: the shield bash refusal is active");
            return;
        }
        problems.add(IntegrationGate.INACTIVE + reason);
        if (ShieldBashRules.isSwitchedOff(meleeOn)) {
            LOGGER.info("Spartan Shields: the shield bash refusal is off ({})", reason);
        } else {
            LOGGER.warn("Spartan Shields: the shield bash refusal is not active ({}{})", reason,
                    lookupError != null ? "; while checking: " + lookupError : "");
        }
    }

    @Override
    public List<String> problems() {
        return problems;
    }

    private Class<?> packetClass() {
        try {
            return Class.forName(PACKET_CLASS, false, SpartanShieldsIntegration.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError e) {
            lookupError = e;
            return null;
        }
    }

    private boolean hasBashHandler(Class<?> packet) {
        try {
            packet.getDeclaredMethod("handleServerSide", packet, Class.forName(PLAYER_CLASS, false, packet.getClassLoader()));
            return true;
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            return false;
        } catch (LinkageError e) {
            lookupError = e;
            return false;
        }
    }
}
