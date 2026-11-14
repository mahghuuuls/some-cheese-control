package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * The only owner of which climbing rule using an item breaks, for one side's settings.
 *
 * <p>The server builds it from its own settings; a client builds it from the copy of its server's
 * settings, so both sides refuse the same items. Whether the player is in the climbing state is a
 * separate question, answered on each side by its own clock.</p>
 */
public final class ClimbingItemRules {

    /** When in an item's use a refusal is decided. */
    public enum Moment {
        /** A right click in the air, or the start of a held use. */
        CLICK,
        /** A right click on a block or an entity: only the blocklist counts. */
        BLOCK_OR_ENTITY,
        /** A use already running: the tick stop and the release. */
        IN_USE
    }

    private final boolean melee;
    private final boolean ranged;
    private final Set<Item> blockedItems;

    private ClimbingItemRules(boolean melee, boolean ranged, Set<Item> blockedItems) {
        this.melee = melee;
        this.ranged = ranged;
        this.blockedItems = blockedItems;
    }

    public static ClimbingItemRules of(Settings settings) {
        return new ClimbingItemRules(settings.climbingMelee(), settings.climbingRanged(),
                settings.climbingItemBlocklist() ? settings.blockedItems() : Collections.<Item>emptySet());
    }

    public boolean melee() {
        return melee;
    }

    public boolean ranged() {
        return ranged;
    }

    /** True when the climbing item blocklist is on and lists at least one item. */
    public boolean hasBlockedItems() {
        return !blockedItems.isEmpty();
    }

    /**
     * Which rule using this item breaks at this moment, or null for none. At a click in the air or a
     * use start, a ranged item that acts at once breaks the ranged rule and a blocklisted item the
     * blocklist rule; a ranged item that is drawn or charged first is let start. At a click on a block
     * or an entity, only a blocklisted item breaks a rule. During a use, a ranged item breaks the
     * ranged rule and a blocklisted item the blocklist rule.
     */
    @Nullable
    public Rule ruleFor(ItemStack stack, Moment moment) {
        if (stack.isEmpty()) {
            return null;
        }
        return decide(ranged && RangedItems.isRanged(stack), RangedItems.actsAtOnce(stack),
                blockedItems.contains(stack.getItem()), moment);
    }

    /**
     * True for a ranged item that is drawn or charged before it acts (a bow, a crossbow, a throwing
     * weapon). The server lets its use start and stops it at the next tick, so the server marks its
     * click as refused when the use starts; the client refuses it at the click ({@link #clientClickRule}).
     */
    public boolean isDrawnRanged(ItemStack stack) {
        return !stack.isEmpty() && isDrawn(ranged && RangedItems.isRanged(stack), RangedItems.actsAtOnce(stack));
    }

    /**
     * The rule a client applies at a right click in the air or a use start: {@link #ruleFor} at
     * {@link Moment#CLICK}, and also the ranged rule for a drawn item, which the client refuses at
     * once instead of letting it start, so no draw shows.
     */
    @Nullable
    public Rule clientClickRule(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return decideClientClick(ranged && RangedItems.isRanged(stack), RangedItems.actsAtOnce(stack),
                blockedItems.contains(stack.getItem()));
    }

    @Nullable
    static Rule decideClientClick(boolean rangedItem, boolean actsAtOnce, boolean listed) {
        Rule rule = decide(rangedItem, actsAtOnce, listed, Moment.CLICK);
        return rule == null && isDrawn(rangedItem, actsAtOnce) ? Rule.CLIMBING_RANGED : rule;
    }

    static boolean isDrawn(boolean rangedItem, boolean actsAtOnce) {
        return rangedItem && !actsAtOnce;
    }

    @Nullable
    static Rule decide(boolean rangedItem, boolean actsAtOnce, boolean listed, Moment moment) {
        if (moment == Moment.BLOCK_OR_ENTITY) {
            return listed ? Rule.CLIMBING_ITEM : null;
        }
        if (rangedItem && (moment == Moment.IN_USE || actsAtOnce)) {
            return Rule.CLIMBING_RANGED;
        }
        return listed ? Rule.CLIMBING_ITEM : null;
    }
}
