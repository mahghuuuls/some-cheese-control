package com.mahghuuuls.somecheesecontrol.integration.spartanshields;

import javax.annotation.Nullable;

/**
 * The plain decisions of the shield bash refusal, with no Spartan Shields or Minecraft types, so
 * they can be tested on their own.
 */
final class ShieldBashRules {

    private ShieldBashRules() {
    }

    /**
     * Why the refusal cannot act, or null when it can. Checked once, at the integration's start.
     *
     * @param meleeOn      the climbing melee refusal switch, which governs the bash
     * @param methodExists the bash handler the Mixin patches exists (another Spartan Shields version may lack it)
     * @param applied      the bash packet class carries the Mixin's marker
     * @param mixinBooter  MixinBooter, which loads the Mixin, is installed
     */
    @Nullable
    static String inactiveReason(boolean meleeOn, boolean methodExists, boolean applied, boolean mixinBooter) {
        if (!meleeOn) {
            return "climbing melee refusal is off";
        }
        if (!methodExists) {
            return "this Spartan Shields version has no bash handler to patch";
        }
        if (!applied) {
            return mixinBooter ? "the bash Mixin was not applied" : "MixinBooter is not installed";
        }
        return null;
    }

    /** True when the reason is the owner's own setting, not a problem worth a log warning. */
    static boolean isSwitchedOff(boolean meleeOn) {
        return !meleeOn;
    }

    /**
     * True when a bash is refused. It must be one Spartan Shields would act on, by its own checks:
     * the player is blocking, the bash's hand holds a Spartan shield, and that shield's bash is not
     * cooling down. And the climbing melee rule must refuse the player now.
     */
    static boolean refuses(boolean blocking, boolean spartanShield, boolean coolingDown, boolean ruleApplies, boolean restricted) {
        return blocking && spartanShield && !coolingDown && ruleApplies && restricted;
    }
}
