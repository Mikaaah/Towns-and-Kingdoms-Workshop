# Sculk Goliath integration

Target: NeoForge 1.21.1, within the existing `tnkcore` mod ID.
The Sculk Goliath feature is inactive outside TNK3. Existing Workshop features remain usable.

Source: HypactC Sculk Goliath pack supplied by the user (private integration only).

Models: regular, destroyed (phase two), dig effect, groundbreak effect.
Gameplay: 850 HP first phase, 400 HP second phase; 115 MythicMobs skill definitions.
Spawn: Twilight Forest Final Castle only; do not port the Ancient City random-spawn configuration.
Do not replace vanilla Wardens globally. Never publish the boss as an independent mod.

Original block-breaking instructions use castle-safe impact particles in the native runtime.
Workshop 0.4.0 imports the full-skills branch; see the skill audit for approximations.

