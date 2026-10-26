# 0.4.0

- Integrate the full-skills Sculk Goliath branch into main, preserving Workshop
  0.3.0 construction tools, mirrors, Mob Cage and the current projector features.
- Include all 115 named skills, 1,370 action lines, 48 phase triggers, four
  animated models, two boss phases and Final Castle encounter integration.
- Include static castle corruption, capped Deeper and Darker patrols, loot and
  a separate TNK3 KubeJS enable marker. Existing features remain independent.
- Isolate optional entity initialization behind dependency checks; exclude the
  boss from Mob Cage capture; eject grabbed players on phase changes/death.
- Repair inherited projectile targets, resolve the source healing callback
  alias and select healing/phase-two helper callbacks correctly.
- Correct the castle mixin shadow signature for Minecraft 1.21.1 and add
  a CI run without optional boss mods that explicitly applies the mixin.
- Retain the documented native skill approximations and full-pack QA checklist.

# 0.3.0

- Add Measuring Pencil with two-point dimensions/distance and a held-item world outline.
- Add Builder’s Satchel: 27 building-block slots and optional main-hand restocking.
- Add Builder’s Brush using the tassel texture: nine ghost templates, weights, random/sequence/coordinate modes, avoid-repeats and satchel sourcing.
- Add a reusable Mob Cage with dark empty and lit occupied lantern textures and full saved mob data.
- Add Magic Mirror with a bound waypoint, two-second held channel and five-second successful-use cooldown.
- Add purple-leaf Chorus Mirror with a five-block spherical entity radius and riding-group support.
- Plan safe mirror landings before travel and preserve native dimension travel hooks.
- Add crafting recipes and datapack-configurable uncageable entity tags.
- Use the supplied PixelMine graphics without resampling; only empty cage panes and Chorus Mirror leaves are edited.
- Keep existing Spawner Cage, Projector and KubeJS items intact. Changes are based on main only.

# 0.2.1

- Add an Appearance tab with per-projector hex colors and live color swatches.
- Tint the model's original tintable faces and configured dropped/inventory items; preserve untinted metal faces.
- Configure projection accent, default text, completed-task and backdrop colors independently.
- Add optional gentle bobbing of the entire hologram, with a seven-second loop.
- Preserve the original two-frame animated texture and all supplied model geometry/UVs.
- Persist colors and motion through saves, settings synchronization, breaking and re-placement.
- Read older projector data with defaults; reject invalid hex/RGB values.
- Rebuild the client chunk mesh when updated body tint arrives.
- Update the settings packet to protocol version 2; requires matching client/server versions.

# 0.2.0

- Rename the mod and creative tab to Towns and Kingdoms - Workshop; preserve existing IDs.
- Add the Quest Projector with the supplied Polaroid Studio / KidTokio model and animated texture.
- Add a command-free editor with searchable selection of up to five FTB Quests.
- Project quest titles, task icons, required quantities and the viewing team's live progress.
- Add paginated task display and buttons to open the normal FTB quest details.
- Add direct-link images, formatted multiline text, projection size/elevation and orientation controls.
- Preserve display settings when broken and replaced; validate ownership, distance, revision and claim permissions when saving.
- Add a crafting recipe; keep existing KubeJS content and Spawner Cage behavior.
- Bound image downloads, dimensions, cache, redirects and connection/transfer timeouts.

# 0.1.2

- Filled Spawner Cages show the captured mob name and any additional spawn-pool mob types.
- Show the actual saved spawn count, delay range, nearby mob limit, activation range and spawn range.
- Localize vanilla/modded mob names; fall back to the entity ID if its mod is missing.
- Existing cages gain the tooltip automatically without recapturing their spawner.

# 0.1.1

- Renamed the item to Spawner Cage.
- Empty cages use a static, dull grey texture with no flame animation.
- Capturing a spawner switches to the original animated orange texture.
- Removed the filled-state enchantment glint so the flame colors remain clear.
- Kept the existing item ID for save and script compatibility.

# 0.1.0

- Initial Towns and Kingdoms Core release for NeoForge 1.21.1.
- Added the animated Spawner Core, a single-use spawner mover.
- Capture a monster spawner and place it elsewhere while retaining its saved data.
- Consume the core after successful placement; no durability or recipe.
- Added a Core creative tab, filled-state glint and English/Dutch tooltips.
