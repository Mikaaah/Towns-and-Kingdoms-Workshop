package nl.townsandkingdoms.core.test;

import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import nl.townsandkingdoms.core.*;

@GameTestHolder("tnkcore")
@PrefixGameTestTemplate(false)
public class SpawnerCoreTests {
    static final BlockPos SOURCE = new BlockPos(1, 2, 1), BASE = new BlockPos(4, 1, 4);
    record Setup(ServerPlayer player, ItemStack stack, CompoundTag saved) {}

    static Setup setup(GameTestHelper h, boolean creative) {
        h.setBlock(SOURCE, Blocks.SPAWNER);
        h.setBlock(BASE, Blocks.STONE);
        var be = (SpawnerBlockEntity) h.getBlockEntity(SOURCE);
        var nbt = be.saveWithFullMetadata(h.getLevel().registryAccess());
        nbt.putShort("MinSpawnDelay", (short) 73);
        nbt.putShort("MaxSpawnDelay", (short) 243);
        nbt.putShort("SpawnCount", (short) 7);
        nbt.putShort("MaxNearbyEntities", (short) 11);
        nbt.putShort("RequiredPlayerRange", (short) 23);
        nbt.putShort("Delay", (short) 123);
        var entity = new CompoundTag();
        entity.putString("id", "minecraft:blaze");
        var spawnData = new CompoundTag();
        spawnData.put("entity", entity);
        nbt.put("SpawnData", spawnData);
        be.loadWithComponents(nbt, h.getLevel().registryAccess());
        be.getPersistentData().putString("tnk_test_mod_data", "preserved");
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        player.setPos(h.absolutePos(new BlockPos(0, 2, 0)).getCenter());
        var stack = TownsAndKingdomsCore.SPAWNER_CORE.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new Setup(player, stack, be.saveWithFullMetadata(h.getLevel().registryAccess()));
    }

    static InteractionResult use(GameTestHelper h, Setup s, BlockPos relative) {
        var pos = h.absolutePos(relative);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return s.player.gameMode.useItemOn(s.player, h.getLevel(), s.stack, InteractionHand.MAIN_HAND, hit);
    }

    static void checkRoundTrip(GameTestHelper h, boolean creative) {
        var s = setup(h, creative);
        h.assertTrue(use(h, s, SOURCE).consumesAction(), "Capture failed");
        h.assertBlockPresent(Blocks.AIR, SOURCE);
        h.assertTrue(SpawnerCoreItem.isFilled(s.stack), "No captured payload");
        h.assertTrue(use(h, s, BASE).consumesAction(), "Placement failed");
        h.assertBlockPresent(Blocks.SPAWNER, BASE.above());
        var actual = h.getBlockEntity(BASE.above()).saveWithFullMetadata(h.getLevel().registryAccess());
        var expected = s.saved.copy();
        var pos = h.absolutePos(BASE.above());
        expected.putInt("x", pos.getX()); expected.putInt("y", pos.getY()); expected.putInt("z", pos.getZ());
        h.assertTrue(expected.equals(actual), "Full spawner data changed: expected " + expected + " got " + actual);
        h.assertTrue(s.stack.isEmpty(), "Core was not consumed");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void survivalRoundTrip(GameTestHelper h) { checkRoundTrip(h, false); }

    @GameTest(template="empty")
    public static void creativeAlsoConsumes(GameTestHelper h) { checkRoundTrip(h, true); }

    @GameTest(template="empty")
    public static void blockedPlacementKeepsSpawner(GameTestHelper h) {
        var s = setup(h, false);
        use(h, s, SOURCE);
        h.setBlock(BASE.above(), Blocks.STONE);
        use(h, s, BASE);
        h.assertBlockPresent(Blocks.STONE, BASE.above());
        h.assertTrue(s.stack.getCount() == 1 && SpawnerCoreItem.isFilled(s.stack), "Blocked placement lost payload");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void canceledBreakKeepsOriginal(GameTestHelper h) {
        var s = setup(h, false);
        Consumer<BlockEvent.BreakEvent> listener = event -> {
            if (event.getPos().equals(h.absolutePos(SOURCE))) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(h, s, SOURCE); } finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertBlockPresent(Blocks.SPAWNER, SOURCE);
        h.assertTrue(!SpawnerCoreItem.isFilled(s.stack), "Denied capture created a duplicate payload");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void canceledPlacementKeepsPayload(GameTestHelper h) {
        var s = setup(h, false);
        use(h, s, SOURCE);
        Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
            if (event.getPos().equals(h.absolutePos(BASE.above()))) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(h, s, BASE); } finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertBlockPresent(Blocks.AIR, BASE.above());
        h.assertTrue(s.stack.getCount() == 1 && SpawnerCoreItem.isFilled(s.stack), "Denied placement lost payload");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void tooltipUsesCapturedSpawner(GameTestHelper h) {
        var s = setup(h, false);
        use(h, s, SOURCE);
        var before = s.stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        s.stack.getItem().appendHoverText(s.stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        var mob = (net.minecraft.network.chat.Component) ((net.minecraft.network.chat.contents.TranslatableContents) lines.get(0).getContents()).getArgs()[0];
        h.assertTrue(mob.equals(net.minecraft.world.entity.EntityType.BLAZE.getDescription()), "Tooltip did not identify captured Blaze");
        String text = lines.stream().filter(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents)
                .map(line -> java.util.Arrays.toString(((net.minecraft.network.chat.contents.TranslatableContents) line.getContents()).getArgs()))
                .collect(java.util.stream.Collectors.joining("\n"));
        h.assertTrue(text.contains("7") && text.contains("11") && text.contains("3.65") && text.contains("12.15") && text.contains("23"),
                "Tooltip did not use this spawner's actual settings: " + text);
        h.assertTrue(before.equals(s.stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag()),
                "Reading tooltip modified captured data");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void tooltipSupportsMixedPoolsAndMissingMods(GameTestHelper h) {
        var payload = new CompoundTag();
        var spawnData = new CompoundTag();
        var entity = new CompoundTag(); entity.putString("id", "minecraft:skeleton"); spawnData.put("entity", entity);
        payload.put("SpawnData", spawnData);
        var pool = new net.minecraft.nbt.ListTag();
        for (String id : java.util.List.of("minecraft:skeleton", "missingmod:custom_mob")) {
            var entry = new CompoundTag(); var data = new CompoundTag(); var mob = new CompoundTag();
            mob.putString("id", id); data.put("entity", mob); entry.put("data", data); entry.putInt("weight", 1); pool.add(entry);
        }
        payload.put("SpawnPotentials", pool);
        var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        SpawnerTooltip.append(payload, lines);
        h.assertTrue(lines.size() == 2, "Duplicate mob types or invented missing statistics");
        var first = (net.minecraft.network.chat.Component) ((net.minecraft.network.chat.contents.TranslatableContents) lines.get(0).getContents()).getArgs()[0];
        var second = (net.minecraft.network.chat.Component) ((net.minecraft.network.chat.contents.TranslatableContents) lines.get(1).getContents()).getArgs()[0];
        h.assertTrue(first.equals(net.minecraft.world.entity.EntityType.SKELETON.getDescription()), "Current mob was not listed first");
        h.assertTrue(second.getString().equals("missingmod:custom_mob"), "Missing entity falsely identified");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void tooltipHandlesMissingOrMalformedData(GameTestHelper h) {
        var payload = new CompoundTag(); payload.putString("SpawnCount", "invalid"); payload.putInt("SpawnRange", -1);
        var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        SpawnerTooltip.append(payload, lines);
        h.assertTrue(lines.size() == 1, "Malformed payload invented statistics");
        var unknown = (net.minecraft.network.chat.Component) ((net.minecraft.network.chat.contents.TranslatableContents) lines.get(0).getContents()).getArgs()[0];
        h.assertTrue(((net.minecraft.network.chat.contents.TranslatableContents) unknown.getContents()).getKey().endsWith(".unknown"),
                "Invalid payload should show unknown mob");
        h.succeed();
    }

}
