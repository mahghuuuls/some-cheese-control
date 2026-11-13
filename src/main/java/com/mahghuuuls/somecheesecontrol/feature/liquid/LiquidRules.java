package com.mahghuuuls.somecheesecontrol.feature.liquid;

/** The placement rules of player buckets, as plain decisions over block facts. */
final class LiquidRules {

    private LiquidRules() {
    }

    /**
     * Whether the liquid goes into the clicked block itself instead of the block on the clicked side.
     *
     * <p>Vanilla buckets fill the clicked block only when it is replaceable and its top face was
     * clicked; Forge's universal bucket always uses the block on the clicked side.</p>
     */
    static boolean placesIntoClickedBlock(boolean universalBucket, boolean clickedReplaceable, boolean clickedTopFace) {
        return !universalBucket && clickedReplaceable && clickedTopFace;
    }

    /**
     * Whether the block below the destination supports a placed liquid: a liquid, or any block the
     * player cannot walk through. Air and blocks without collision (grass, flowers, torches, rails)
     * give no support.
     */
    static boolean isSupported(boolean belowIsLiquid, boolean belowHasCollision) {
        return belowIsLiquid || belowHasCollision;
    }
}
