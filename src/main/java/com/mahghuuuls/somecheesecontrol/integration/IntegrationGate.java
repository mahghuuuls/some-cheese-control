package com.mahghuuuls.somecheesecontrol.integration;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * The only owner of the optional integrations: which exist, whether their mod is present, loading
 * their entry class by name, isolating their failures, and the record the status command reads.
 *
 * <p>An integration's classes name another mod's classes, so they are loaded only by name and only
 * when that mod is present; nothing else in this mod refers to them. A failure while starting, or
 * later inside an integration's handlers ({@link #fail}), is logged once and disables only that
 * integration; every other feature keeps working.</p>
 */
public final class IntegrationGate {

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Integrations");

    /**
     * Every integrated mod, in the order the status command lists them: mod id, display name, and the
     * entry class, or null for a mod this version only detects.
     */
    static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry("ebwizardry", "Electroblob's Wizardry", null),
            new Entry("spartanweaponry", "Spartan Weaponry", null),
            new Entry("spartanshields", "Spartan Shields", null),
            new Entry("bettercombatmod", "RLCombat", null),
            new Entry("comforts", "Comforts", "com.mahghuuuls.somecheesecontrol.integration.comforts.ComfortsIntegration"),
            new Entry("jass", "JASS", null),
            new Entry("jawms", "JAWMS", null)));

    private static final Map<String, Record> RECORDS = new LinkedHashMap<>();

    private IntegrationGate() {
    }

    /** What an integration's entry class does. It has a public constructor without arguments. */
    public interface Integration {
        /** Registers the integration's handlers. Called once, at initialization, when its mod is present. */
        void start();

        /** Notes for the status command, such as a feature this integration serves being off; empty when none. */
        List<String> problems();
    }

    /** Where an integration stands. */
    public enum State {
        /** Its mod is not installed; nothing was loaded. */
        ABSENT,
        /** Installed; this version has no integration code for it (covered by Forge events, or detected only). */
        DETECTED,
        /** Started. */
        ACTIVE,
        /** Failed while starting or later; disabled. */
        FAILED
    }

    /** Starts every integration whose mod is present. Call once, at initialization. */
    public static synchronized void startAll() {
        startAll(ENTRIES, Loader::isModLoaded);
    }

    static synchronized void startAll(List<Entry> entries, Predicate<String> modLoaded) {
        RECORDS.clear();
        for (Entry entry : entries) {
            RECORDS.put(entry.modId, start(entry, modLoaded));
        }
    }

    static Record start(Entry entry, Predicate<String> modLoaded) {
        if (!modLoaded.test(entry.modId)) {
            return new Record(entry, State.ABSENT, Collections.<String>emptyList());
        }
        if (entry.entryClass == null) {
            return new Record(entry, State.DETECTED, Collections.<String>emptyList());
        }
        try {
            Integration integration = (Integration) Class.forName(entry.entryClass).getConstructor().newInstance();
            integration.start();
            LOGGER.info("{} integration started", entry.name);
            return new Record(entry, State.ACTIVE, new ArrayList<>(integration.problems()));
        } catch (Throwable t) {
            LOGGER.error("{} integration failed to start and is disabled; every other feature keeps working", entry.name, t);
            return new Record(entry, State.FAILED, Collections.singletonList("failed to start: " + t));
        }
    }

    /**
     * Disables a started integration after an error inside its handlers: logs it the first time and
     * records it. The integration stops acting itself; this records why.
     */
    public static synchronized void fail(String modId, Throwable error) {
        Record record = RECORDS.get(modId);
        if (record == null || record.state == State.FAILED) {
            return;
        }
        LOGGER.error("{} integration failed and is disabled; every other feature keeps working", record.entry.name, error);
        RECORDS.put(modId, new Record(record.entry, State.FAILED, Collections.singletonList("failed: " + error)));
    }

    /** The integrations as they stand, for the status command. */
    public static synchronized List<Record> records() {
        return Collections.unmodifiableList(new ArrayList<>(RECORDS.values()));
    }

    /** One integration. */
    static final class Entry {
        final String modId;
        final String name;
        @Nullable
        final String entryClass;

        Entry(String modId, String name, @Nullable String entryClass) {
            this.modId = modId;
            this.name = name;
            this.entryClass = entryClass;
        }
    }

    /** One integration's state and notes. */
    public static final class Record {
        private final Entry entry;
        private final State state;
        private final List<String> problems;

        Record(Entry entry, State state, List<String> problems) {
            this.entry = entry;
            this.state = state;
            this.problems = Collections.unmodifiableList(problems);
        }

        public String modId() {
            return entry.modId;
        }

        public String name() {
            return entry.name;
        }

        public State state() {
            return state;
        }

        public List<String> problems() {
            return problems;
        }
    }
}
