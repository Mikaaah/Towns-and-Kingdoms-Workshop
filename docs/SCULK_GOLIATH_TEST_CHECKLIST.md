# Private TNK3 Sculk Goliath integration: test checklist

**Status:** compile and Workshop baseline game tests in GitHub CI. The actual
Sculk Goliath encounter and castle corruption are **not yet validated in-game**
with the full pack. Do not ship it to the main TNK3 servers until this passes.

## Dependencies and separation

The standalone Workshop features (Spawner Cage and Quest Projector) must work
without TNK3. The Sculk Goliath only activates if all of the following are
installed on both client and server:

- NeoForge 1.21.1 and this Workshop build
- GeckoLib 4 (NeoForge 1.21.1 build)
- Twilight Forest (tested against 4.7.3196 sources)
- Deeper and Darker (tested against 1.4.1 sources)
- KubeJS and the pack-only marker below

Copy `pack/kubejs/config/tnk3_goliath_enabled.json` into the **modpack** at
`kubejs/config/tnk3_goliath_enabled.json`. The marker is checked at runtime.
Keep Workshop's main mod ID `tnkcore` unchanged.

## Manual game test (new, disposable world)

1. Launch client and dedicated server with the TNK3 dependencies and marker.
   Verify the existing Spawner Cage and Quest Projector still work.
2. Generate a previously unexplored Twilight Forest Final Castle. Check that
   some castle masonry is replaced with static sculk/deepslate and that most
   structure spawners have been removed **without** changing castle paths,
   forcefields, doors or runes.
3. Verify sparse Deeper and Darker patrols inside the castle, with no runaway
   spawning, and inspect server TPS/chunk generation time.
4. Enter the final boss gazebo. The boss should spawn **once**, play its full
   entrance animation, display a boss bar and target a survival player.
5. Fight through phase one (850 HP) and the destroyed-model second phase
   (400 HP). Check attacks, sound cues, positioning, collision, animation
   alignment, hit detection and client/server synchronization.
6. Kill the boss, confirm its death animation and loot table. Leave and return;
   that castle's boss must not respawn. A different castle must have its own
   encounter.
7. Test a server restart during the boss fight, during the introduction and
   during the phase transition. Test with a second player joining mid-fight.
8. Remove the TNK3-only mods and marker in a **separate clean instance**.
   Workshop must still load with the Spawner Cage and Projector available.

## Important scope and limitations

- This is a native NeoForge conversion with four original models, textures
  and animations, two health phases, an initial set of native attacks and
  original-inspired Warden sound effects. **Not all 115 original MythicMobs
  skill definitions have been ported**. In particular, the visual helper
  effects and exact attack sequence parity still need further implementation.
- Animation and geometry conversion is structurally verified, but visual
  rendering in the full game has not yet been inspected.
- The Final Castle corruption affects structure-piece placement, not all
  jigsaw templates; existing generated castles are unchanged.
- No runtime sculk spread or boss attacks that permanently break castle blocks.
- All files must stay in the private Workshop project, and the boss must not
  be redistributed or promoted as an independent mod.
