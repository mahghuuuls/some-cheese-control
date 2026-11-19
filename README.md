# Some Cheese Control

A Minecraft 1.12.2 Forge mod that makes common low-risk combat and survival tricks ("cheese") harder: liquid placed in the air, buckets carried outside the hands, fighting from ladders, cobweb and boat traps, boat and minecart fall clutches, and bed explosions. Each rule is on by default and has its own switch. It replaces three earlier mods by the same author and adds further rules.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- License: [LICENSE](LICENSE)
- Third-party notices: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

## Identity And Install Sides

- Mod id: `somecheesecontrol`. Version: 1.0.0. Minecraft 1.12.2, Forge; runs on standard Forge and on [Cleanroom](https://github.com/CleanroomMC/Cleanroom).
- Install on the server and on every client, in the same version. A client without the mod is refused at join with a message naming the mod.
- The server makes every decision. The client also refuses its own player's actions early (early client refusal), using the server's settings, so the client does not show them (no liquid, sound, arm movement, draw or item use). Exceptions: hiding the swing of a refused melee attack needs MixinBooter, and the Spartan Shields bash and items that cast through their own client messages are not refused early. The click still reaches the server, which decides and sends the message.
- No other mod is required. Every integration is optional, and a missing integrated mod never stops the other rules.
- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) is optional and not declared as a dependency. Cleanroom includes it. Every rule works without it except these two, which are off on standard Forge without MixinBooter:
  - the [Spartan Shields](https://www.curseforge.com/minecraft/mc-mods/spartan-shields) bash refusal; the server log and `/scc status` say it is not active;
  - hiding the arm swing of a refused melee attack: the swing shows as a missed swing does, and the client log says once that swing hiding is inactive.

## Rules

- **Player restrictions** act on a player's own action: unsupported liquid placement, airborne bucket emptying, hand-only carrying, the climbing restrictions (melee, ranged, [Electroblob's Wizardry](https://www.curseforge.com/minecraft/mc-mods/electroblobs-wizardry) casting, climbing item blocklist), vehicle fall damage and bed-explosion prevention. They skip Creative players unless `general.restrictionsApplyInCreative` is true; bed-explosion prevention applies in every game mode. Spectators and fake players (machines acting as players) are exempt. Operator status gives no bypass.
- **Countermeasures** act on a mob after it takes actual damage, whoever caused it: cobweb release and boat escape. They never act on players.
- **Restricted climbing state:** on a climbable block (ladders, vines, modded blocks that declare themselves climbable, and `climbing.extraClimbableBlocks`) and not standing on the ground, plus the release delay after leaving it. Standing on the ground ends it at once. A riding player is never in it.
- **Airborne:** not on the ground and not in water or lava. Holding on to a climbable block without ground counts. A rider is airborne when the vehicle is neither on the ground nor in water.
- **Unsupported liquid placement:** the block below the liquid's destination is air or has no collision. A block the player cannot walk through, or a liquid, is support.
- **Hand-only carrying:** a restricted carried item outside an allowed hand moves into the offhand within 1 second when the offhand is allowed and empty, otherwise it drops at the player's feet. The selected hotbar slot is never a move target. In any screen except the Creative inventory, a click, drag, shift-click or number key that would put a restricted item outside an allowed hand does nothing. Pickup goes only into an empty allowed hand. Only the player's own inventory is inspected. Death drops and keepInventory are unchanged.
- **Vehicle fall damage:** a player who boards a boat or minecart mid-fall keeps the height fallen as a pending fall; the vehicle's further descent adds to it. A boat that falls onto a block gives its rider the fall. A minecart that lands off rails passes its fall to the rider, as in vanilla, plus any pending fall. Water landings and minecart landings on rails clear the fall. A pending fall continues as a normal fall when the player leaves the vehicle in the air, and is cleared on death, logout, teleport, dimension change, and switching to Creative or Spectator. Damage to the vehicle and mounting mobs mid-fall are unchanged.
- **Bed-explosion prevention:** judged by the dimension's own sleep rule, not by a list of dimension ids. The refused bed stays; nothing explodes; spawn is unchanged.
- **Cobweb release:** every cobweb block intersecting the damaged mob's body is removed, with break particles and sound and no drops. Covers the vanilla cobweb, modded webs that are variants of it, and `cobwebRelease.extraWebBlocks`.
- **Boat escape:** the damaged mob leaves a vanilla boat (every wood type), is placed at a free spot within 2 blocks of the boat (or at the boat's position when none is free), and cannot board any boat for `boatEscape.reboardSeconds`. Minecarts are not affected.
- Refused actions spend no item, ammunition or durability. Resources spent before an interruption (mana of a channeled spell, a channeled scroll) are not refunded. Without MixinBooter, a refused melee attack drops the attack bar as a missed swing does.
- Messages are plain English text built on the server, sent to the action bar, at most one per player per `general.messageIntervalSeconds`. Cobweb release, boat escape, a move into the offhand and a placement blocked in a screen show no message.
- The mod adds no saved data of its own to worlds, players or beds. Climbing state, message timers, pending falls and reboarding blocks reset on restart.

## Configuration

File: `config/somecheesecontrol.cfg` (Forge configuration). It is read at game or server start; **changes require a restart**. On a server, the server's file decides the rules for every client; a client's own file has no effect on rules.

An invalid item or block id in a list is skipped and named in a log warning and in `/scc status`. A number outside its range is clamped to the nearest bound with a warning. An `allowedHand` value that allows no hand is logged as an error and `either` is used.

| Category | Key | Default | Meaning |
| --- | --- | --- | --- |
| `general` | `restrictionsApplyInCreative` | `false` | Apply the player restrictions in Creative too. Bed-explosion prevention always applies. |
| `general` | `diagnosticLogging` | `false` | Write one log line per decision (refusals and countermeasures). |
| `general` | `messagesEnabled` | `true` | Show action-bar messages for refused actions and forced drops. |
| `general` | `messageIntervalSeconds` | `2.0` | Shortest time between two messages to the same player, 0 to 10. |
| `unsupportedLiquidPlacement` | `enabled` | `true` | Refuse emptying a bucket without support below the liquid. |
| `airborneEmptying` | `enabled` | `true` | Refuse emptying a bucket while airborne. |
| `handOnlyCarrying` | `enabled` | `true` | Keep the listed items in an allowed hand. |
| `handOnlyCarrying` | `items` | `minecraft:water_bucket`, `minecraft:lava_bucket` | Restricted carried items, one `modid:name` per line. |
| `handOnlyCarrying` | `allowedHand` | `either` | `main` (selected hotbar slot), `off` (offhand) or `either`. |
| `climbing` | `releaseDelaySeconds` | `0.5` | How long the climbing restrictions last after letting go, 0 to 5. |
| `climbing` | `extraClimbableBlocks` | empty | Blocks that also count as climbable. |
| `climbingMelee` | `enabled` | `true` | Refuse melee attacks while climbing (and the Spartan Shields bash). |
| `climbingRanged` | `enabled` | `true` | Refuse firing, throwing and fishing rods while climbing. |
| `climbingWizardry` | `enabled` | `true` | Refuse Electroblob's Wizardry casting while climbing. |
| `climbingItemBlocklist` | `enabled` | `true` | Refuse every use of the listed items while climbing. |
| `climbingItemBlocklist` | `items` | empty | The climbing item blocklist. |
| `vehicleFall` | `enabled` | `true` | Vehicle fall damage for boats and minecarts. |
| `cobwebRelease` | `enabled` | `true` | Cobweb release. |
| `cobwebRelease` | `extraWebBlocks` | empty | Blocks that also count as cobwebs. List web-like blocks only: a block that holds items still spills them. |
| `cobwebRelease` | `followMobGriefing` | `false` | When true, no web is removed while `mobGriefing` is false. |
| `boatEscape` | `enabled` | `true` | Boat escape. |
| `boatEscape` | `reboardSeconds` | `5.0` | Reboarding block, 0 to 60. The game itself blocks reboarding for 3 seconds, so a value under 3 acts as 3. |
| `bedExplosion` | `enabled` | `true` | Bed-explosion prevention. |

Message texts, category `general.messageTexts`, each replaceable:

| Key | Default text |
| --- | --- |
| `unsupportedLiquid` | You cannot place liquid without support below. |
| `airborneEmptying` | You cannot empty a bucket while in the air. |
| `forcedDrop` | You can only carry that item in your hand. |
| `climbingMelee` | You cannot attack while climbing. |
| `climbingRanged` | You cannot shoot or throw while climbing. |
| `climbingWizardry` | You cannot cast spells while climbing. |
| `climbingItem` | You cannot use that item while climbing. |
| `bedExplosion` | You cannot use a bed here. |

## Diagnostics

- `/somecheesecontrol` (alias `/scc`), operator permission level 2.
- `/scc status`: the feature switches, every configuration value, the integrated mods and whether each is installed and active, whether the shield bash refusal is active, any installed replaced mods, and configuration problems.
- `/scc inspect <player>`: the player's climbing state, remaining release delay, the block giving climbing support, airborne state and pending fall; for the mob the sender looks at, its remaining boat reboarding block.
- `general.diagnosticLogging`: one log line per decision (refusal, forced drop, move into the offhand, cobweb removal, ejection, pending fall applied or cleared, refused bed use); nothing per tick. A refused [RLCombat](https://www.curseforge.com/minecraft/mc-mods/rlcombat) offhand attack is logged with the main-hand item.

## Integrations

Optional. Each is active only when the other mod is installed; an error inside one integration is logged once and disables only that integration.

- **Electroblob's Wizardry:** casts from a held wand, scroll or other spell-casting item are refused while climbing, at no mana, scroll or cooldown cost; a charge-up stops; a channeled spell stops as if released. Casts by minions and golems, contingency spells and `/cast` are not refused.
- **[Spartan Weaponry](https://www.curseforge.com/minecraft/mc-mods/spartan-weaponry):** its melee weapons are covered by the melee refusal; longbows, crossbows, throwing weapons and dynamite by the ranged refusal.
- **Spartan Shields:** the shield bash is refused while climbing (no damage, knockback, durability or bash cooldown) through one Mixin, loaded by MixinBooter only when Spartan Shields is present.
- **[RLCombat](https://www.curseforge.com/minecraft/mc-mods/rlcombat):** main-hand and offhand attacks are refused while climbing. With MixinBooter, a client Mixin hides the swing of a refused RLCombat attack for the attacking player; other players still see the swing, because RLCombat's server shows it.
- **[Comforts](https://www.curseforge.com/minecraft/mc-mods/comforts):** sleeping bags and hammocks are covered by bed-explosion prevention, including Comforts' automatic sleeping bag use when a bag is placed.
- **[Fluidlogged API](https://www.curseforge.com/minecraft/mc-mods/fluidlogged-api):** for vanilla water and lava buckets and Forge's universal bucket, the liquid placement rules judge the block where Fluidlogged API puts the liquid.
- **[JAWMS](https://www.curseforge.com/minecraft/mc-mods/just-another-wizardry-mana-system)** and **[JASS](https://www.curseforge.com/minecraft/mc-mods/just-another-stamina-system):** no dedicated code. A refused Wizardry cast spends no JAWMS mana. With [JASS Integrations](https://www.curseforge.com/minecraft/mc-mods/jass-integrations), a refused Spartan Shields bash spends no stamina.

Tested versions: Electroblob's Wizardry 4.3.19, Spartan Weaponry 1.6.1, Spartan Shields 1.5.5, RLCombat 2.2.6 (with [ReachFix](https://www.curseforge.com/minecraft/mc-mods/reach-fix) 1.1.3), Comforts 1.4.1.3, JAWMS 1.5.0, JASS 1.3.0 with JASS Integrations 1.5.0, Fluidlogged API 3.3.3. Runtimes: Cleanroom 0.7.14-alpha, and Forge 14.23.5.2864 with MixinBooter 12.3 and without MixinBooter. Nothing is claimed for other versions.

## Replaced Mods

Some Cheese Control replaces [Water Placement Nerf](https://www.curseforge.com/minecraft/mc-mods/water-placement-nerf) (`waterplacementnerf`), [Hold that Bucket!](https://www.curseforge.com/minecraft/mc-mods/hold-that-bucket) (`hold-that-bucket`) and [No Attacking on Ladders!](https://www.curseforge.com/minecraft/mc-mods/no-attacking-on-ladders) (`noattackingonladders`). When one of them is installed, Some Cheese Control writes one warning to the game log at startup naming it. Neither mod is disabled, so remove the old one.

## Scope Boundaries

Not affected by design:

- Dispensers, machines, fake players and world-generated liquids.
- Backpacks, chests and items inside other items (an accepted bypass of hand-only carrying).
- Wall climbing from equipment or spells.
- Projectiles and spell effects already produced.
- Minecarts for boat escape; mounting mobs mid-fall for vehicle fall damage.
- Shallow-water fall protection; explosions other than beds.

Unsupported:

- **Buckets:** modded buckets that do not fire Forge's bucket event or do not expose their fluid through Forge's fluid handler (for example modded `ItemBucket` subclasses without Forge's fluid wrapper) are not refused. Vanilla water and lava buckets and Forge's universal bucket with any fluid are covered.
- **Casting paths:** [Ancient Spellcraft](https://www.curseforge.com/minecraft/mc-mods/ancient-spellcraft) passive artefact casts (cryostasis charm, prismarine ring, random minion cast); items that cast through their own client messages ([Wizardry Tales](https://www.curseforge.com/minecraft/mc-mods/wizardry-tales) casting ring and key casting, Ancient Spellcraft warlock spell packets); Ancient Spellcraft battlemage shield casts. Ancient Spellcraft's ancient wand, arcane mirror and magic shield may not be refused; refusal depends on whether they implement Wizardry's spell-casting item interface.
- **Boats:** modded boats and ships, including [Davinci's Vessels](https://www.curseforge.com/minecraft/mc-mods/davincis-vessels) ships (also not tracked by vehicle fall damage).
- **Beds:** Davinci's Vessels' secured bed and beds with their own explosion rule. Covered are vanilla beds, beds that use the vanilla bed block, and Comforts sleeping bags and hammocks.

Known limits:

- With Comforts' "Leisure Hammocks" on, a hammock only seats the player but is still refused where beds explode.
- A click on a bed where beds explode is refused even when a held item's own first use (for example some wrenches) would have acted instead of the bed.
- With a climbing-blocklisted item in the hand used for a click, the whole click on a block or entity is refused while climbing (doors, levers, trades, mounting).
- A restricted item placed in a crafting grid returns to the first free inventory slot when the screen closes, then moves into the offhand or drops. Closing a screen with a restricted item on the cursor drops it, as vanilla does.
- Another mod's screen action, such as inventory sorting, can get past the screen placement check; the server then moves or drops the item within 1 second.
- Filling one bucket from a held stack of empty buckets can send the filled bucket to the offhand or the ground. Fill with a single empty bucket in hand.
- Fluidlogged API's bucket setting is not synced by Fluidlogged API. Keep it the same on the server and clients; otherwise a client can refuse early a placement the server allows.
- Near the start or end of a jump, or at the end of the release delay, the client and server can judge the state differently for about one tick; when the client refuses a right click that the server allows, that click can act with both the main hand and the offhand.
- With [Universal Tweaks](https://www.curseforge.com/minecraft/mc-mods/universal-tweaks)' boat breaking above 0, a pending fall from boarding a boat mid-fall is lost when the boat breaks, and a second fall event can occur. At 0, the whole fall is applied.
- A teleport shorter than 24 blocks while riding keeps a pending fall. Switching mid-fall from a boat or minecart to a mob drops the fall.
- If the server crashes, is killed, or is stopped by a signal (such as Ctrl+C) after a save while a player rides a falling minecart, the pending fall stays saved in that minecart. An orderly `/stop` clears it.
- With MixinBooter, the swing of a refused melee attack still shows for a Spartan Weaponry long-reach hit without RLCombat, and where another mod changes the same client code so that a swing Mixin cannot apply (the client log then says so).
- With Forge's `fullBoundingBoxLadders` option on, the game counts a ladder anywhere in the player's body; `/scc inspect` names the block at the feet.

## License And Attribution

MIT License, see [LICENSE](LICENSE). Build scripts, Gradle wrapper and project layout come from [CleanroomMC/ForgeDevEnv](https://github.com/CleanroomMC/ForgeDevEnv) under the MIT License; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
