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
