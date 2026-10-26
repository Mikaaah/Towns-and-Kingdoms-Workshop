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
