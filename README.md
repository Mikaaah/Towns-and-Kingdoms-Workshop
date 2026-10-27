# Towns and Kingdoms - Workshop 0.5.0

Minecraft 1.21.1 / NeoForge 21.1.219 or newer (21.1.x) / Java 21.
Install the JAR on the server and all clients. The existing Workshop features require no other mods.
The optional TNK3 Goliath encounter has the requirements below.

## Dependency hotfix (0.4.1)

Replace empty version ranges for KubeJS, Twilight Forest and Deeper and Darker
with explicit open-ended `[0,)` ranges. These mods remain optional for Workshop
as a whole and required by the TNK3 boss presence check. All other dependency
ranges, load ordering and runtime behavior are unchanged.

## TNK3 Sculk Goliath (0.4.0)

Imported from `feature/tnk3-sculk-goliath-full-skills` at
`eb22c12763d7fce264e0ab343100410e8a7fe040`, retaining the current main-branch
projector hex colors, projection motion, construction tools and utility items.
All 115 named skill definitions (1,370 action lines and 48 phase triggers),
the native runtime, four model/animation/texture sets, Final Castle encounter,
static castle palette and sparse Deeper and Darker patrols are included.

Install GeckoLib 4.9.3 or compatible GeckoLib 4, Twilight Forest, Deeper and
Darker, and KubeJS on client and server. Copy
`pack/kubejs/config/tnk3_goliath_enabled.json` from this repository into the
modpack's `kubejs/config/` folder. This pack marker is intentionally separate
from the mod JAR. No commands are needed: approach a Final Castle boss gazebo.
New castle generation gets static sculk/deepslate replacements; previously
generated castles are not rewritten. The boss has 850 HP then 400 HP, remembers
defeated castles, and cannot be stored in Workshop's Mob Cage.

See [full-skill audit](docs/SCULK_GOLIATH_FULL_SKILL_AUDIT.md) for native runtime
approximations, and [in-game checklist](docs/SCULK_GOLIATH_TEST_CHECKLIST.md)
for full-pack visual/combat testing still required.

## Construction and utility set

All six new items use the supplied native 16x16 PixelMine textures. Open their
screens or use the controls below; no setup commands are required. Default
crafting recipes are included and can be adjusted normally by the modpack.

| Item | ID | Controls and behavior |
|---|---|---|
| Measuring Pencil | `tnkcore:measuring_pencil` | Right-click two blocks to measure their inclusive X/Y/Z dimensions and straight-line distance. A purple outline is shown while holding the pencil. Third click starts a new measurement; sneak-right-click clears it. |
| Builder’s Satchel | `tnkcore:builders_satchel` | Right-click for 27 slots of building blocks. Shift-click transfers real stacks. The held-block refill toggle replenishes partial stacks and the last emptied main-hand stack from the satchel. |
| Builder’s Brush | `tnkcore:builders_brush` | Right-click air or sneak-right-click a block to configure. Set up to nine ghost templates, weights from 0–64, selection mode, avoid-repeats and satchel supply. Normal block use places one real material from inventory or a carried satchel. |
| Mob Cage | `tnkcore:mob_cage` | Right-click a mob to capture; right-click a block to release. Empty lantern panes are dark; filled cages use the original lit texture. Reusable, with no durability. |
| Magic Mirror | `tnkcore:magic_mirror` | Sneak-right-click binds your current location and dimension. Hold right-click for two seconds to teleport. Releasing early cancels; successful use has a five-second cooldown. |
| Chorus Mirror | `tnkcore:chorus_mirror` | Purple-leaf version of the mirror. Same binding and two-second held use, bringing eligible entities within a spherical five-block radius: players, mobs, dropped items, XP and riding groups. |

### Builder’s Brush

The name replaces the earlier Block Palette idea. It uses the supplied tassel
artwork. Clicking a palette slot with a carried block copies a template without
consuming that block; clicking with an empty cursor clears it. Shift-clicking a
block in your inventory adds a template to the first empty slot. Templates cannot
be extracted as real blocks. A weight of zero disables a slot.

Modes: weighted random, sequence, and a deterministic coordinate pattern.
Avoid-repeats excludes the previous choice when another enabled material is
available. Satchel supply is a separate brush option from a satchel's automatic
held-block refill setting. Unavailable materials are skipped. The brush does not
create blocks, and normal NeoForge placement/claim cancellation is honored.

Satchels accept building BlockItems, excluding items carrying container or
block-entity data. They do not hold other satchels, tools or filled storage blocks.
An open satchel/brush is locked against hotbar/offhand swaps into itself.

### Mob Cage

Full entity NBT is retained, including name, health, UUID and saved mod data.
Players, dead mobs, occupied vehicles/passengers and leashed mobs cannot be
captured. Unleash/dismount first. Vanilla dragon and wither are excluded, along
with common boss tags. Extend `tnkcore:uncageable` in the modpack to exclude more
modded mobs. Mob data is limited to 64 KiB per cage.

A blocked or canceled release keeps the filled cage. A mob whose UUID already
exists cannot be released again. Entity interactions and joins retain their
normal protection/event checks. Successful release leaves an empty reusable cage.

### Mirrors

Each mirror stores one independent waypoint. Binding records your current feet
position, facing and dimension; it does not bind the block under your crosshair.
The normal mirror only brings its user and requires dismounting first.

The Chorus Mirror includes complete riding groups when a member is selected.
It plans safe landing space for every group before moving any entity; insufficient
space leaves the group at the source. Landings avoid collisions, world-border
violations and obvious hazards. Small offsets are allowed to find nearby safe
floor space. A maximum of 256 entities/passengers is supported per use.

Dimension changes use Minecraft/NeoForge's native transition path. Other mods may
prevent individual entities from travelling through their normal hooks. No claim
or dimension-travel restrictions are deliberately bypassed.

The regular mirror's original leaves, wooden frame and mirror pane are unchanged.
Only green leaf pixels are recolored on the Chorus Mirror. Item graphics are flat
inventory sprites; there is no additional 3D item model dependency.

## Quest Projector

A placeable projector using the supplied Polaroid Studio / KidTokio model and
original animated texture. Place it and right-click: all setup uses the screen.

- **Quests:** search by quest/chapter name and select up to five quests. The
  hologram shows titles, task item icons, required amounts and live progress for
  the viewing player's FTB Quests team. Hidden quests/details stay hidden.
  Long lists automatically turn pages every eight seconds, repeating the quest
  heading on continued pages. The arrow beside a quest opens its FTB details.
- **Image:** paste a direct HTTP/HTTPS image link and press Preview / reload.
  PNG, JPG and GIF are supported (first frame for GIF). Aspect ratio is preserved.
- **Formatted text:** multiline text with Minecraft `&0`–`&f` colors,
  `&#RRGGBB`, `&l` bold, `&o` italic, `&n` underline, `&m` strike-through,
  `&r` reset, plus `**bold**` and `*italic*`. Literal `\n` also creates a newline.
- **Appearance:** separate six-digit hex inputs (`#RRGGBB`, or without `#`) with
  color swatches for the model's tintable parts, projection accent, default text,
  completed tasks and backdrop. The original untinted metal parts stay neutral.
  Each placed projector stores its own palette; configured drops use its body tint.
  Inline text formatting still overrides the default text color.
- Gentle bobbing moves the entire projected field together (text, task icons,
  images and backdrop), ±0.035 blocks over a seven-second cycle. Toggle it on/off
  in Appearance. It is enabled by default, including on older projector settings.
  The supplied two-frame texture animation remains unchanged; each frame lasts
  ten ticks (half a second). The block model itself has no moving parts.
- Adjustable projection width (1–8 blocks), elevation (0.5–6 blocks), backdrop
  and optional face-player orientation. Otherwise it faces its placement direction.
- Only the placer or an operator can change settings. Other players can view
  the selection and open linked quests. Claim interaction denial is respected.
- Breaking the projector preserves its display settings on the dropped item;
  placing it assigns ownership to the new placer.

Crafting pattern (I = iron ingot, G = glass, A = amethyst shard, R = redstone):

| | | |
|---|---|---|
| I | G | I |
| A | R | A |
| I | I | I |

FTB Quests is optional: required only for quest mode. The bridge targets its
Minecraft 1.21.1 / 2101 API and reads the already synchronized local team data.
It does not submit tasks, take items, change progression or auto-claim rewards.
Use the normal quest details for those actions. Your KubeJS items stay intact.

Images are fetched on clients with two bounded background workers, a 32-image
session cache, an 8 MB transfer cap and a 4096-pixel / 8-megapixel decode cap.
Public internet hosts only; redirects receive the same address validation.
A failed image displays a status message and can be retried with Preview / reload.
Images and formatting do not require FTB Quests.

Stable registry namespace: `tnkcore`. Projector item/block: `tnkcore:quest_projector`.
Existing Spawner Cage IDs and saved data are unchanged.
Existing 0.2.0 projector settings load with the default purple palette.
Install matching Workshop 0.3.0 on the server and clients: the projector settings
packet now carries colors and the animation toggle (protocol version 2).

## Spawner Cage

![Empty cage (left) and filled cage (right)](docs/spawner-cage-textures.png)

Empty: dull grey and static. Filled: the original animated orange flame.

Item ID: `tnkcore:spawner_core`

`/give @s tnkcore:spawner_core`

Also available in the Towns and Kingdoms - Workshop creative tab.
No recipe is included. Add a late-game recipe or quest reward in the modpack.

1. Right-click a regular monster spawner with an empty core to capture it.
2. Right-click a block with the filled core to place the spawner at the target face.
3. The core is consumed after successful placement, including in Creative.

Filled cages show the captured mob, additional spawn-pool types, spawn count,
delay range, activation range and spawn range from the saved spawner data.

One core carries one spawner. No durability, repairs or reusable remainder.
The empty cage uses a static, dull grey version of the supplied texture.
A filled cage switches to the original animated orange texture.
The original PNG and animation metadata are included unchanged for the filled state.
The registry ID remains `tnkcore:spawner_core` for existing worlds and scripts.

Full spawner block-entity data is retained, including spawn potentials, delays,
entity data, persistent mod data and attachments. This is intended to preserve
Apotheosis settings on vanilla spawner blocks; no direct Apotheosis dependency.
Custom spawner blocks and trial spawners are outside this first feature's scope.

Capture checks NeoForge's block-break protection event. Both capture and placement
participate in NeoForge's standard item-use snapshot transaction, allowing claim
mods to cancel and roll back the operation. Failed placement keeps the filled core.
No XP or spawner item drops are generated by capture.

## Development

`./gradlew build`

`./gradlew runGameTestServer`

GameTests cover Survival/Creative data round trips, blocked placement and canceled
break/place events. Test classes/templates are excluded from the release JAR.

The source layout keeps feature classes separate so more Core features can be
added under the same stable `tnkcore` namespace.


## Unified Workshop interfaces (0.5.0)

Projector, Builder's Brush and Builder's Satchel share parchment panels, walnut frames and amethyst controls inspired by TNKLoot NPC counters. Buttons, sliders, fields and inventory slots use matching colors. Brush/satchel controls display their current state. Existing settings and inventory actions are preserved.

