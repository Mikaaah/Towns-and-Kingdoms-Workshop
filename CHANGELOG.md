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
