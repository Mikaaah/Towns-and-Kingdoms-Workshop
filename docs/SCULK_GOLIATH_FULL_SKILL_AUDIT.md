# TNK3 Sculk Goliath — private full-skill test branch

Branch: `feature/tnk3-sculk-goliath-full-skills`

The supplied HypactC MythicMobs and ModelEngine source was mechanically
converted to `data/tnkcore/sculk_goliath/skills.json` and a bounded native
NeoForge/GeckoLib runtime. The separate prototype branch stays untouched.

## Source preservation and automated checks

- **115 / 115** named MythicMobs skill definitions are preserved verbatim
  as action/condition data. Includes 26 cooldown-only guard definitions.
- **1,370 / 1,370** original skill action lines remain in source order.
- **48 / 48** phase-one/phase-two boss event triggers are represented.
- **27 / 27** original mechanic verbs have runtime handlers (including the
  intentionally non-griefing alternatives below).
- All `skill{...}` nested references and `skillOnCooldown{...}` guard names
  resolve within the original 115 definitions.
- GameTests cover counts, references, parser delay/repeat and target behavior.

## Functional native translation

The skill runner handles original per-skill cooldowns, global cooldowns,
health and distance conditions, per-tick and per-damage triggers, conditional
cooldown guard skills, repeats, sequential delays, nested skill dispatch,
animation state names, sounds, particles, effects, healing, damage,
invulnerability windows, knockback, pulls, speed changes, AI locks, grabs,
projectile/missile tracks, helper summons, cinematic entrance and phase-two
death. Both phases use their original health pools (850 and 400 HP).

The two supplied ModelEngine helper models (ground break and dig) are now
spawned as transient native GeckoLib visual entities. The 4 original models,
their animations and textures are included privately inside Workshop.

## Explicit deviations from MythicMobs/ModelEngine

1. The original `breakblock` instructions are visual block-fracture particles
   only. They never change the castle, structures, player builds or terrain.
2. ModelEngine `brightness` bones are approximated with lightweight sculk
   particles, rather than native emissive bone lighting.
3. Mythic `recoil` is represented by a subtle impact pulse. Exact client
   camera shake has not been implemented.
4. Dynamic `@modelpart` socket positions are approximated relative to the
   entity's orientation on the server. Actual animated-bone tracking needs
   in-game visual QA and a specialized synchronized socket system.
5. Projectile and missile callbacks are functional, but their flight follows
   straight native vectors, not every Mythic-specific trajectory modifier.
6. Mount-model grabs use vanilla passenger riding, rather than ModelEngine's
   custom seat transform. Check multiplayer dismount safety during playtesting.
7. Some source helper entities are recreated as temporary particle/physics
   effects instead of invisible silverfish with Mythic scripts. Ground-break
   and dig have their own renderer; falling rocks and soul shooters use
   bounded native visual/attack simulation.

**A green CI build is not proof of exact combat, visuals or game balance.**
Run a real TNK3 client/server pair on a disposable newly generated Twilight
Forest Final Castle, verify every attack in each phase and inspect TPS before
shipping. Keep the original content private and pack-specific as licensed.

### Server protection

- No ongoing sculk spread or permanent block destruction.
- Per-boss limits: 6,000 pending events, 320 executed mechanics per tick,
  80 concurrent projectiles, 100 helper effects, 1,500 particles per tick.
- All helper visual entities despawn after 80 ticks, and the boss never spawns
  outside the pack-gated Final Castle encounter.
