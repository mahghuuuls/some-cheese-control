package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingItemRules.Moment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which climbing rule using an item breaks, the one decision both sides apply. The arguments are
 * the item facts: whether it is a ranged item (with the ranged rule on), whether it acts at the click,
 * and whether the blocklist lists it.
 */
class ClimbingItemRulesTest {

    @Test
    void aThrownItemIsRefusedAtTheClick() {
        assertEquals(Rule.CLIMBING_RANGED, ClimbingItemRules.decide(true, true, false, Moment.CLICK), "a snowball or a pearl");
    }

    @Test
    void aDrawnItemIsLetStartAndStoppedWhileInUse() {
        assertNull(ClimbingItemRules.decide(true, false, false, Moment.CLICK), "the server lets a bow start");
        assertEquals(Rule.CLIMBING_RANGED, ClimbingItemRules.decide(true, false, false, Moment.IN_USE), "and stops it in use");
    }

    @Test
    void onABlockOrEntityOnlyTheBlocklistCounts() {
        assertNull(ClimbingItemRules.decide(true, true, false, Moment.BLOCK_OR_ENTITY), "a pearl clicking a door is not refused there");
        assertEquals(Rule.CLIMBING_ITEM, ClimbingItemRules.decide(false, true, true, Moment.BLOCK_OR_ENTITY), "a lead on a fence");
    }

    @Test
    void aBlocklistedItemIsRefusedAtEveryMoment() {
        for (Moment moment : Moment.values()) {
            assertEquals(Rule.CLIMBING_ITEM, ClimbingItemRules.decide(false, false, true, moment), moment.name());
        }
    }

    @Test
    void theRangedRuleWinsForAListedRangedItemOutsideBlocksAndEntities() {
        assertEquals(Rule.CLIMBING_RANGED, ClimbingItemRules.decide(true, true, true, Moment.CLICK));
        assertEquals(Rule.CLIMBING_RANGED, ClimbingItemRules.decide(true, false, true, Moment.IN_USE));
    }

    @Test
    void anOrdinaryItemBreaksNoRule() {
        for (Moment moment : Moment.values()) {
            assertNull(ClimbingItemRules.decide(false, true, false, moment), moment.name());
        }
    }

    @Test
    void aClientRefusesADrawnItemAtTheClickInsteadOfLettingItStart() {
        assertEquals(Rule.CLIMBING_RANGED, ClimbingItemRules.decideClientClick(true, false, false), "a bow: no draw shows");
        assertEquals(Rule.CLIMBING_RANGED, ClimbingItemRules.decideClientClick(true, true, false), "a pearl, as on the server");
        assertEquals(Rule.CLIMBING_ITEM, ClimbingItemRules.decideClientClick(false, false, true), "a listed food");
        assertNull(ClimbingItemRules.decideClientClick(false, false, false), "an ordinary item");
    }

    @Test
    void onlyARangedItemThatDoesNotActAtOnceIsDrawn() {
        assertTrue(ClimbingItemRules.isDrawn(true, false), "a bow");
        assertFalse(ClimbingItemRules.isDrawn(true, true), "a pearl");
        assertFalse(ClimbingItemRules.isDrawn(false, false), "a shield, or a bow with the ranged rule off");
    }
}
