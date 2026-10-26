# Validation — Workshop 0.4.1

Metadata-only dependency hotfix based on the tested 0.4.0 release.

- Updated TOML parses and declares version 0.4.1.
- KubeJS, Twilight Forest and Deeper and Darker use `[0,)` instead of empty
  version ranges. Every other dependency value and mixin setting is unchanged.
- Maven VersionRange checks exercise the affected version formats and confirm
  the explicit range accepts them. Empty ranges fail containsVersion checks.
- Repackage the production 0.4.0 JAR with updated mod metadata only. Every other
  archive entry is byte-identical, including every compiled class and asset.
- Source build version and runtime mod version both read 0.4.1.

No new gameplay build or GameTest run is claimed for this metadata-only patch.
The 49 passing tests below apply to the unchanged 0.4.0 classes. A full launch
with the user's specific mod versions remains to be checked in their instance.

## Previous release validation

# Validation — Workshop 0.4.0

Minecraft 1.21.1 / NeoForge 21.1.219 / Java 21 / GeckoLib 4.9.3 compile target.
Imported full-skills branch `eb22c12763d7fce264e0ab343100410e8a7fe040` into
main based on `a09d9d193a0f6f51abae3baa48ed6546c83963fd`.

- Production mod compiles and builds as 0.4.0.
- All **49** required headless GameTests pass: previous 46 tests plus three
  Goliath skill-library/parser/callback checks.
- All **49** pass again with GeckoLib removed from the runtime classpath and
  the castle mixin explicitly enabled. The mod list contains only Minecraft,
  NeoForge and Workshop. The log confirms CastlePaletteMixin applies to
  StructurePiece; the corrected MutableBlockPos shadow matches Minecraft 1.21.1.
- Skill data retains all 115 definitions, 1,370 action lines and 48 phase
  triggers. Nested references, guard names and projectile callbacks resolve,
  including the documented native healing callback alias. Implicit damage
  actions inherit their callback target; explicit parent targets remain intact.
- All four original model, animation and texture sets and skill JSON are
  byte-identical to the imported branch. Resource JSON and mod TOML parse.
- Main's projector implementation, hex palette, motion, construction tools and
  existing assets are retained. The Goliath is excluded from Mob Cage capture.
- The production JAR contains both feature sets and all boss resources, and
  excludes development tests and test structures.

Limits: the complete TNK3 modpack, a graphical client and a live Final Castle
fight were not available. These checks do not verify full combat/animation
parity, progression gating, patrol behavior, castle appearance or multiplayer
balance. Use the full-skill audit and in-game checklist for those checks. The
boss remains pack-gated; the KubeJS enable marker is shipped in the repository's
`pack/` folder rather than silently installed by the JAR.

The sandbox's process self-detection required a local build-cache workaround
for NeoForm. It is not part of the repository or distributed mod. Headless tests
use a local placeholder asset-index properties file; production resources are
built normally.

## Previous releases

# Validation — Workshop 0.3.0

Minecraft 1.21.1 / NeoForge 21.1.219 / Java 21.

46 headless NeoForge GameTests pass, including the previous 21 Spawner Cage
and Projector checks and 25 construction/utility checks. The suite covers:

- Two-point measurement dimensions, distance and clearing.
- Satchel block-only restrictions, inventory persistence, quick transfer,
  locking the open item, partial refill, last-empty-stack refill and refill toggle.
- Brush ghost templates without item creation/consumption, weighted selection,
  real inventory consumption, satchel supply, missing materials, canceled placement,
  and avoiding the same material across duplicate palette slots.
- Mob name, health, UUID and persistent data preserved through capture/release;
  blocked release, denied capture, canceled entity join and vanilla boss exclusion.
- Independent mirror waypoints and rejection of an unbound mirror.
- Exact forty living ticks before held-use teleportation; early release cancels.
- Single-user travel; AoE travel with mobs, dropped items, nearby players and
  intact riding groups; exclusion outside the spherical five-block radius.
- Blocked waypoint planning moves nobody.
- Cross-dimension player/mob travel preserving mob UUID, custom name and health.
  The destination entity's added-to-level state is checked directly because fresh
  chunk sections can be registered before UUID tracking lookups become visible.

Client and server code compile. New recipes/models/lang JSON are parsed, all
new textures are native 16x16, and five source sprites are byte-identical to the
supplied packs. Only empty-lantern window pixels and purple mirror leaf pixels
are edited. Development tests and their structure templates are excluded from
the production JAR.

Practical limits: no graphical client was available. Screen appearance, live
item predicate rendering and the world measurement outline still need an in-game
visual check. The vanilla server round trips do not prove every modded mob's
transient runtime state; Mob Cage preserves the entity's saved NBT. Boss exclusions
can be extended via the datapack tag. Native travel hooks can reject individual
entities after a group has otherwise passed safe-position planning.

The build sandbox hides process self-detection, so a local-only NeoForm runtime
patch supplied the Java executable path. No build-tool patch is included in the
mod or repository; ordinary developer machines use the standard Gradle wrapper.

## Earlier release validation

# Validation — Workshop 0.2.1

Java 21 / Minecraft 1.21.1 / NeoForge 21.1.219. Built from an isolated snapshot
of main at ab01003d2670dfbc3d285322940417ee63757506. Only the main branch is
updated for this feature; boss development branches are not used or changed.

Twenty-one headless NeoForge GameTests pass. Existing settings persistence,
drop/re-placement and save-permission tests now use a custom palette and motion
disabled. Added tests cover six-digit hex parsing (including black, white and
leading zeros), malformed/out-of-range rejection, old 0.2.0 settings migration,
full settings-packet round trips, and the bobbing amplitude/period/off toggle.

Client and server source compilation passes. Asset checks retain the projector
PNG, animation metadata and complete model unchanged. The body uses only its
existing tint-index faces. A client chunk rebuild is requested when settings
synchronize so the placed model's new body color can be baked into its mesh.

No graphical Minecraft client is available here. The live color editor, block
tint refresh and projection animation still need in-game visual verification.

## Previous validation

# Validation — Workshop 0.2.0

Java 21; Minecraft 1.21.1 / NeoForge 21.1.219.

The build compiles both client and server code. Seventeen headless NeoForge
GameTests pass, including all eight Spawner Cage tests and nine projector tests:

- Full block-entity settings/owner/revision persistence.
- Configured drops retain display data and omit the old owner.
- New placement keeps display data and assigns the placer as owner.
- Owner versus unrelated survival-player edit permissions.
- Valid network saves, remote/non-owner rejection and stale revision rejection.
- Claim denial through the NeoForge RightClickBlock event.
- Maximum-five selection, text limits, invalid URL schemes and invalid dimensions.
- Bold, color, italic, underline and newline parsing with no executable events.
- Private, loopback, link-local and other non-public image targets rejected before connection.

The original projector PNG and animation metadata are retained byte-for-byte.
Model element geometry/UVs/rotations are retained; texture paths and cutout render
type are adapted. Development test classes/templates are excluded from the JAR.

## Practical limits

A graphical Minecraft client was not available in this environment. Projector
appearance, editor interaction, real image downloads and live FTB Quests rendering
still require an in-game check. FTB Quests binaries were not available here;
the optional bridge was checked against the official 1.21.1/main source API
(2101.1.37). The headless tests run without FTB Quests and verify that Workshop
works on a dedicated server without it.

External images show the first frame and are session-cached; animated GIF playback
is not implemented. Quest progress is read from the viewing client's synchronized
FTB team data. Task submission and rewards use the normal FTB quest details.

## Prior Spawner Cage validation

# Validation — 0.1.1

Compiled with Java 21 against Minecraft 1.21.1 / NeoForge 21.1.219.
Five NeoForge GameTests passed through ServerPlayerGameMode's item interaction path:

- Survival capture/place preserves the complete block-entity NBT apart from position.
- Creative capture/place also consumes the core.
- Blocked placement retains the captured spawner.
- Canceled break events leave the original spawner and empty core intact.
- Canceled placement events restore the target and retain the filled core.

Resource checks: supplied texture and animation metadata are byte-identical;
JSON is valid; no recipes; development tests excluded from release JAR.

Apotheosis itself was not installed for this test run. Round-trip tests include
custom persistent mod data and altered spawner parameters; compatibility relies
on retaining the complete saved block-entity payload.

Environment note: NeoForm's process self-detection is unavailable in this build
sandbox. The build used existing matching NeoForge 21.1.219 generated Minecraft
artifacts, skipping regeneration. Compilation and server GameTests ran normally.
A standard development machine can use the Gradle wrapper without those skips.

0.1.1: client-only model predicate compiled; dedicated-server GameTests all passed.
Verified static 16x16 neutral grey empty texture (max 110 brightness), filled
texture and animation metadata byte-identical to supplied originals, and model
override wiring. Client appearance inspected as a pixel preview; not run in a
graphical Minecraft client.

