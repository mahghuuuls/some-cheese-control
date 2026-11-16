package com.mahghuuuls.somecheesecontrol.integration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starting integrations: an absent mod loads nothing, a working entry class starts, and a missing or
 * failing one is recorded as failed without stopping the others.
 */
class IntegrationGateTest {

    static int started;

    /** A working integration with one note. */
    public static final class Working implements IntegrationGate.Integration {
        @Override
        public void start() {
            started++;
        }

        @Override
        public List<String> problems() {
            return Collections.singletonList("a note");
        }
    }

    /** An integration whose start fails, as one would when its mod changed. */
    public static final class Failing implements IntegrationGate.Integration {
        @Override
        public void start() {
            throw new NoSuchMethodError("c4.comforts.Changed.method");
        }

        @Override
        public List<String> problems() {
            return Collections.emptyList();
        }
    }

    private static IntegrationGate.Entry entry(String className) {
        return new IntegrationGate.Entry("somemod", "Some Mod", className);
    }

    @Test
    void anAbsentModLoadsNothing() {
        int before = started;
        IntegrationGate.Record record = IntegrationGate.start(entry(Working.class.getName()), id -> false);
        assertEquals(IntegrationGate.State.ABSENT, record.state());
        assertEquals(before, started, "the entry class was not started");
    }

    @Test
    void aPresentModStartsItsIntegration() {
        int before = started;
        IntegrationGate.Record record = IntegrationGate.start(entry(Working.class.getName()), id -> true);
        assertEquals(IntegrationGate.State.ACTIVE, record.state());
        assertEquals(before + 1, started);
        assertEquals(Collections.singletonList("a note"), record.problems());
    }

    @Test
    void aFailingStartIsRecordedAndDoesNotEscape() {
        IntegrationGate.Record record = IntegrationGate.start(entry(Failing.class.getName()), id -> true);
        assertEquals(IntegrationGate.State.FAILED, record.state());
        assertTrue(record.problems().get(0).contains("NoSuchMethodError"), record.problems().toString());
    }

    @Test
    void aMissingEntryClassIsRecordedAsFailed() {
        IntegrationGate.Record record = IntegrationGate.start(entry("com.example.NoSuchIntegration"), id -> true);
        assertEquals(IntegrationGate.State.FAILED, record.state());
    }

    @Test
    void everyEntryNamesAnIntegrationClassInItsOwnPackage() throws ClassNotFoundException {
        for (IntegrationGate.Entry entry : IntegrationGate.ENTRIES) {
            assertTrue(entry.entryClass.startsWith("com.mahghuuuls.somecheesecontrol.integration." + entry.modId + "."), entry.entryClass);
            Class<?> type = Class.forName(entry.entryClass, false, IntegrationGateTest.class.getClassLoader());
            assertTrue(IntegrationGate.Integration.class.isAssignableFrom(type), entry.entryClass);
        }
    }

    @Test
    void oneFailingIntegrationDoesNotStopTheNext() {
        IntegrationGate.startAll(Arrays.asList(
                new IntegrationGate.Entry("broken", "Broken", Failing.class.getName()),
                new IntegrationGate.Entry("somemod", "Some Mod", Working.class.getName())), id -> true);
        List<IntegrationGate.Record> records = IntegrationGate.records();
        assertEquals(IntegrationGate.State.FAILED, records.get(0).state());
        assertEquals(IntegrationGate.State.ACTIVE, records.get(1).state());
    }

    @Test
    void anErrorAfterStartIsRecordedOnce() {
        IntegrationGate.startAll(Collections.singletonList(entry(Working.class.getName())), id -> true);
        IntegrationGate.fail("somemod", new IllegalStateException("first"));
        IntegrationGate.fail("somemod", new IllegalStateException("second"));
        IntegrationGate.Record record = IntegrationGate.records().get(0);
        assertEquals(IntegrationGate.State.FAILED, record.state());
        assertEquals(1, record.problems().size());
        assertTrue(record.problems().get(0).contains("first"), record.problems().toString());
    }
}
