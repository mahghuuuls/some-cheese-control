package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The one gate for diagnostic lines. Off by default; when off, a call costs one boolean check.
 *
 * <p>Every line starts with {@code [SCC]}, then the event name, then {@code key=value} pairs, so a
 * reader can search the log for one event. Callers write one line per decision, never per tick.</p>
 */
public final class DiagnosticLog {

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Diagnostics");

    private DiagnosticLog() {
    }

    public static boolean enabled() {
        return Settings.get().diagnosticLogging();
    }

    /**
     * Writes {@code [SCC] <event> k1=v1 k2=v2 ...} when diagnostics are on.
     *
     * @param keyValues alternating keys and values
     */
    public static void decision(String event, Object... keyValues) {
        if (!enabled()) {
            return;
        }
        LOGGER.info(format(event, keyValues));
    }

    /** The registry id of an entity's type, such as {@code minecraft:zombie}, for a diagnostic line. */
    public static String entityName(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity);
        return key != null ? key.toString() : entity.getClass().getSimpleName();
    }

    static String format(String event, Object... keyValues) {
        StringBuilder line = new StringBuilder("[SCC] ").append(event);
        for (int i = 0; i < keyValues.length; i += 2) {
            Object value = i + 1 < keyValues.length ? keyValues[i + 1] : "?";
            line.append(' ').append(keyValues[i]).append('=').append(value);
        }
        return line.toString();
    }
}
