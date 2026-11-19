Makes common low-risk combat and survival tricks harder: liquid placed in the air, buckets carried outside the hands, fighting from ladders, cobweb and boat traps, boat fall clutches and bed explosions. Each rule can be switched off.

Every rule is on by default. A refused action spends no item, ammunition or durability, and usually a short message appears above the hotbar.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

---

## Rules

**Player restrictions.** These apply to players in Survival and Adventure mode. Creative players skip them unless the configuration says otherwise; bed-explosion prevention applies in every game mode. Operators get no bypass.

- **Unsupported liquid placement:** a bucket cannot be emptied where the block below the liquid gives no support. Air and blocks you can walk through, such as tall grass, flowers and torches, are no support. Solid and partial blocks, such as slabs and fences, are support, and so is liquid, so filling a pool still works. Filling a bucket is never refused, and dispensers and machines are not affected.
- **Airborne bucket emptying:** a bucket cannot be emptied while you are in the air: jumping, falling, holding on to a ladder above air, or riding something that is in the air, such as a boat, a minecart or a jumping horse. Swimming and standing on the ground are fine.
- **Hand-only carrying:** by default, water and lava buckets may only be carried in a hand: the selected hotbar slot or the offhand. Screens do not let you put one anywhere else in your inventory. When one ends up elsewhere, for example because you switched to another hotbar slot while holding it, it moves into your empty offhand, or drops at your feet when the offhand is full. A bucket on the ground is picked up only into an empty hand. Buckets already in your inventory when the mod is added are handled the same way.
- **Climbing restrictions:** while you are on a ladder, vine or other climbable block and not standing on the ground, and for half a second after letting go (by default), you cannot attack in melee with either hand, shoot or throw (bows, snowballs, eggs, ender pearls, splash and lingering potions, experience bottles and fishing rods), or use the items in the climbing item blocklist, which is empty by default. A refused melee attack deals no damage or knockback and costs no durability. A bow draw in progress stops when you start climbing, without firing and without cost. Arrows already in flight still hit. Standing on the ground ends the restrictions at once, and riding never counts as climbing.
- **Vehicle fall damage:** boarding a boat or minecart while falling keeps your fall, and you take it on landing. Riding a boat that falls onto a block deals fall damage. Landing in water and a minecart landing on rails stay safe. Feather Falling applies as for a normal fall. Mounting a horse or other mob mid-fall works as in vanilla.
- **Bed-explosion prevention:** using a bed where it would explode (the Nether, the End, or any dimension whose own rule says so) does nothing. The bed stays and nothing explodes. Sleeping in the Overworld is unchanged.

**Countermeasures.** These act on mobs (any living creature except players) when they take damage from any source.

- **Cobweb release:** when a mob takes damage, the cobwebs inside its body are removed, with no item drops. This includes webs you placed. A trapped mob that takes damage breaks free, and a new web can trap it again.
- **Boat escape:** when a mob riding a vanilla boat takes damage, it leaves the boat and is placed next to it where it cannot suffocate. For 5 seconds (by default) it cannot board any boat. Other passengers stay, and the boat is not damaged. Moving undamaged mobs by boat still works. Minecarts are not affected.

## Configuration

All settings are in `config/somecheesecontrol.cfg`. Changes require a restart. In multiplayer the server's settings apply to every player. Each option's comment in the file gives its default and range, and the [README](https://github.com/mahghuuuls/some-cheese-control#readme) lists every key.

- **Switches:** each rule has its own `enabled` switch. The climbing restrictions are switched separately for melee (including the Spartan Shields bash), shooting and throwing, the climbing item blocklist, and Wizardry casting. The messages have one switch too.
- **Lists:** the items that must be carried in a hand, the climbing item blocklist, extra climbable blocks and extra cobweb blocks, as `modid:name` entries.
- **Values:** the allowed hand (main, off or either), the release delay after letting go of a ladder, the boat reboarding time, and the minimum time between two messages.
- **Other options:** apply the player restrictions in Creative too; make cobweb release follow the `mobGriefing` game rule; replace the text of any message; diagnostic logging, off by default, which writes one log line for each decision.
- **Commands:** `/scc status` lists the active rules, settings, detected mods and configuration problems. `/scc inspect` with a player name shows that player's climbing state and pending fall, and the boat reboarding time of the mob you look at. Both need operator rights.

## Integrations

Each integration is optional and works only when the other mod is installed.

### [Electroblob's Wizardry](https://www.curseforge.com/minecraft/mc-mods/electroblobs-wizardry)

- Casting a spell from a held wand, scroll or other casting item is refused while climbing, with no mana, scroll or cooldown spent.
- A spell charging up stops when you start climbing, at no cost.
- A channeled spell stops when you start climbing, as if released. Mana already spent is not refunded.
- Casts by minions, contingency spells and the `/cast` command are not refused. Using a wand only to pick a target for your minions is refused while climbing.

### [Spartan Weaponry](https://www.curseforge.com/minecraft/mc-mods/spartan-weaponry)

- Melee attacks with its weapons are refused while climbing.
- Longbows, crossbows, throwing weapons and dynamite are refused while climbing, with no ammunition or item spent. A loaded crossbow stays loaded.

### [Spartan Shields](https://www.curseforge.com/minecraft/mc-mods/spartan-shields)

- The shield bash is refused while climbing: no damage, knockback, durability or bash cooldown. Needs MixinBooter on standard Forge.

### [RLCombat](https://www.curseforge.com/minecraft/mc-mods/rlcombat)

- Main-hand and offhand attacks are both refused while climbing.

### [Comforts](https://www.curseforge.com/minecraft/mc-mods/comforts)

- Sleeping bags and hammocks are covered by bed-explosion prevention.
- With Comforts' automatic sleeping bag use on, placing a sleeping bag where it would explode does not explode it, and the bag stays placed.

### [Fluidlogged API](https://www.curseforge.com/minecraft/mc-mods/fluidlogged-api)

- Unsupported liquid placement and airborne bucket emptying judge the block where Fluidlogged API puts the liquid: a water bucket emptied on a fence over stone fills the fence, and on a fence over air it is refused.
- Keep Fluidlogged API's bucket setting the same on the server and on every client; otherwise a client can refuse a placement the server allows.

### [Just Another Wizardry Mana System (JAWMS)](https://www.curseforge.com/minecraft/mc-mods/just-another-wizardry-mana-system)

- A refused Wizardry cast spends no mana.

### [Just Another Stamina System (JASS)](https://www.curseforge.com/minecraft/mc-mods/just-another-stamina-system)

- A refused Spartan Shields bash spends no stamina with [JASS Integrations](https://www.curseforge.com/minecraft/mc-mods/jass-integrations).

## Replaced Mods

This mod covers the behavior of [Water Placement Nerf](https://www.curseforge.com/minecraft/mc-mods/water-placement-nerf), [Hold that Bucket!](https://www.curseforge.com/minecraft/mc-mods/hold-that-bucket) and [No Attacking on Ladders!](https://www.curseforge.com/minecraft/mc-mods/no-attacking-on-ladders). If one of them is installed, a warning in the game log names it. Neither mod is disabled, so remove the old one.

## Installation

Install it on the server and on every client, in the same version. A client without it cannot join the server. [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) is optional: without it, the Spartan Shields bash refusal is off, and a refused melee attack shows the arm swing and drops the attack bar as a missed swing does. [Cleanroom](https://github.com/CleanroomMC/Cleanroom) includes MixinBooter.

## Limitations

- Backpacks, chests and items inside other items are not checked by hand-only carrying. Another mod's inventory sorting can move a bucket out of the hand; it then moves into the offhand or drops.
- Filling one bucket from a stack of empty buckets can move the filled bucket into the offhand or drop it. Fill with a single empty bucket in hand.
- A bucket placed in a crafting grid returns to the inventory when the screen closes, and then moves into the offhand or drops.
- While a blocklisted item is in the hand used for a click, the whole click is refused while climbing, including doors, levers, trades and mounting.
- Other mods' ranged weapons are refused while climbing only when they are built on a vanilla bow, thrown item or fishing rod, or come from Spartan Weaponry; add others to the climbing item blocklist.
- Not covered by unsupported liquid placement or airborne bucket emptying: modded buckets that do not use the vanilla or Forge bucket code.
- Not refused while climbing: [Ancient Spellcraft](https://www.curseforge.com/minecraft/mc-mods/ancient-spellcraft) passive artefact casts, battlemage shield casts and warlock spell casts, and [Wizardry Tales](https://www.curseforge.com/minecraft/mc-mods/wizardry-tales) casting ring and key casting.
- Not covered by boat escape: modded boats and ships, such as [Davinci's Vessels](https://www.curseforge.com/minecraft/mc-mods/davincis-vessels) ships.
- Not covered by bed-explosion prevention: the Davinci's Vessels secured bed and modded beds with their own explosion rule.
- With Comforts' "Leisure Hammocks" option on, hammocks are still refused where beds explode, although they would only seat the player.

## Source

[github.com/mahghuuuls/some-cheese-control](https://github.com/mahghuuuls/some-cheese-control)
