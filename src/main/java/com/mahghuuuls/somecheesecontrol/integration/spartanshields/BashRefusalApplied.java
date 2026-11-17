package com.mahghuuuls.somecheesecontrol.integration.spartanshields;

/**
 * Marker the shield bash Mixin adds to Spartan Shields' bash packet class. When the class
 * implements it, the Mixin was applied; without it the bash is not refused. It lives outside the
 * Mixin package, because Mixin does not load ordinary classes from a package a Mixin configuration
 * owns.
 */
public interface BashRefusalApplied {
}
