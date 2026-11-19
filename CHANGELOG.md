# Changelog

## [1.0.0] - 2026-11-18

Initial release.

### Added

- Unsupported liquid placement: emptying a bucket where the block below the liquid gives no support is refused.
- Airborne bucket emptying: emptying a bucket while jumping, falling, holding on to a ladder above air, or riding a vehicle in the air is refused. Filling is never refused.
- Hand-only carrying: water and lava buckets (configurable list) stay in an allowed hand. A screen does not let the player put one elsewhere in the inventory; one that gets there anyway moves into an empty allowed offhand, or drops.
- Climbing restrictions: on a ladder, vine or other climbable block without ground, and for 0.5 seconds (by default) after letting go, melee attacks, ranged attacks, [Electroblob's Wizardry](https://www.curseforge.com/minecraft/mc-mods/electroblobs-wizardry) casting and items in the climbing item blocklist are refused. A draw, load or charge in progress stops without cost.
- Vehicle fall damage: boarding a boat or minecart while falling keeps the fall, and a boat that falls onto a block gives its rider the fall. Water and rail landings stay safe.
- Cobweb release: a mob that takes damage frees itself from the cobwebs inside its body.
- Boat escape: a mob in a vanilla boat that takes damage leaves the boat and cannot board a boat for 5 seconds (by default).
- Bed-explosion prevention: using a bed where it would explode does nothing, in every game mode.
- Action-bar messages for refused actions and forced drops, rate-limited and with configurable text.
- Configuration file `config/somecheesecontrol.cfg` with a switch for every rule, and separate switches for the climbing restrictions on melee, ranged, the climbing item blocklist and Wizardry casting; changes require a restart.
- Operator commands `/scc status` and `/scc inspect <player>`, and an optional diagnostic log.
- A log warning when [Water Placement Nerf](https://www.curseforge.com/minecraft/mc-mods/water-placement-nerf), [Hold that Bucket!](https://www.curseforge.com/minecraft/mc-mods/hold-that-bucket) or [No Attacking on Ladders!](https://www.curseforge.com/minecraft/mc-mods/no-attacking-on-ladders) is installed; this mod replaces them. Some Cheese Control does not disable them, so remove the old mod.

### Compatibility

- Required on the server and on every client, in the same version.
- Optional integrations: Electroblob's Wizardry, [Spartan Weaponry](https://www.curseforge.com/minecraft/mc-mods/spartan-weaponry), [Spartan Shields](https://www.curseforge.com/minecraft/mc-mods/spartan-shields), [RLCombat](https://www.curseforge.com/minecraft/mc-mods/rlcombat), [Comforts](https://www.curseforge.com/minecraft/mc-mods/comforts) and [Fluidlogged API](https://www.curseforge.com/minecraft/mc-mods/fluidlogged-api). A refused Wizardry cast spends no [JAWMS](https://www.curseforge.com/minecraft/mc-mods/just-another-wizardry-mana-system) mana, and a refused Spartan Shields bash spends no [JASS](https://www.curseforge.com/minecraft/mc-mods/just-another-stamina-system) stamina with [JASS Integrations](https://www.curseforge.com/minecraft/mc-mods/jass-integrations).
- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) is optional. Without it, the Spartan Shields bash refusal is off and a refused melee attack shows the arm swing. [Cleanroom](https://github.com/CleanroomMC/Cleanroom) includes MixinBooter.
- Tested with Electroblob's Wizardry 4.3.19, Spartan Weaponry 1.6.1, Spartan Shields 1.5.5, RLCombat 2.2.6 with [ReachFix](https://www.curseforge.com/minecraft/mc-mods/reach-fix) 1.1.3, Comforts 1.4.1.3, JAWMS 1.5.0, JASS 1.3.0, JASS Integrations 1.5.0 and Fluidlogged API 3.3.3, on Cleanroom 0.7.14-alpha and on Forge 14.23.5.2864 with MixinBooter 12.3 and without MixinBooter.

### Limitations

- Backpacks and chests are not checked by hand-only carrying, by design.
- Not refused while climbing: [Ancient Spellcraft](https://www.curseforge.com/minecraft/mc-mods/ancient-spellcraft) passive artefact, battlemage shield and warlock spell casts, and [Wizardry Tales](https://www.curseforge.com/minecraft/mc-mods/wizardry-tales) casting ring and key casting.
- Modded boats and ships (such as [Davinci's Vessels](https://www.curseforge.com/minecraft/mc-mods/davincis-vessels)) are not covered by boat escape, and modded beds with their own explosion rule are not covered by bed-explosion prevention.
- With Comforts' "Leisure Hammocks" on, hammocks are still refused where beds explode.
- If the server crashes, is killed, or is stopped by a signal (such as Ctrl+C) after a save while a player rides a falling minecart, the pending fall stays saved in that minecart. An orderly `/stop` clears it.
