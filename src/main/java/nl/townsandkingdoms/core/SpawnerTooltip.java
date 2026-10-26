package nl.townsandkingdoms.core;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Reads only the captured payload; never creates an entity or changes spawner data. */
public final class SpawnerTooltip {
    private SpawnerTooltip() {}

    public static void append(CompoundTag spawner, List<Component> tooltip) {
        Set<String> ids = new LinkedHashSet<>();
        addEntityId(spawner.getCompound("SpawnData"), ids);
        var potentials = spawner.getList("SpawnPotentials", Tag.TAG_COMPOUND);
        for (int i = 0; i < potentials.size(); i++) addEntityId(potentials.getCompound(i).getCompound("data"), ids);
        if (ids.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.tnkcore.spawner_core.contains",
                    Component.translatable("tooltip.tnkcore.spawner_core.unknown")).withStyle(ChatFormatting.GOLD));
        } else {
            boolean first = true;
            for (String id : ids) {
                tooltip.add(Component.translatable(first ? "tooltip.tnkcore.spawner_core.contains" : "tooltip.tnkcore.spawner_core.also_spawns",
                        entityName(id)).withStyle(first ? ChatFormatting.GOLD : ChatFormatting.GRAY));
                first = false;
            }
        }
        addNumber(spawner, tooltip, "SpawnCount", "spawn_count");
        addNumber(spawner, tooltip, "MaxNearbyEntities", "max_nearby");
        if (nonnegativeNumber(spawner, "MinSpawnDelay") && nonnegativeNumber(spawner, "MaxSpawnDelay")) {
            tooltip.add(Component.translatable("tooltip.tnkcore.spawner_core.spawn_delay",
                    seconds(spawner.getInt("MinSpawnDelay")), seconds(spawner.getInt("MaxSpawnDelay")))
                    .withStyle(ChatFormatting.GRAY));
        }
        addNumber(spawner, tooltip, "RequiredPlayerRange", "player_range");
        addNumber(spawner, tooltip, "SpawnRange", "spawn_range");
    }

    private static void addEntityId(CompoundTag spawnData, Set<String> ids) {
        String id = spawnData.getCompound("entity").getString("id");
        if (!id.isBlank()) ids.add(id);
    }

    private static Component entityName(String id) {
        var key = ResourceLocation.tryParse(id);
        // getOptional avoids the entity registry's default (Pig) for missing mods.
        return key == null ? Component.literal(id) : BuiltInRegistries.ENTITY_TYPE.getOptional(key)
                .map(type -> type.getDescription()).orElseGet(() -> Component.literal(id));
    }

    private static boolean nonnegativeNumber(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) && tag.getInt(key) >= 0;
    }

    private static void addNumber(CompoundTag tag, List<Component> tooltip, String nbtKey, String translation) {
        if (nonnegativeNumber(tag, nbtKey)) tooltip.add(Component.translatable("tooltip.tnkcore.spawner_core." + translation,
                tag.getInt(nbtKey)).withStyle(ChatFormatting.GRAY));
    }

    private static String seconds(int ticks) {
        return BigDecimal.valueOf(ticks).divide(BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
    }
}
