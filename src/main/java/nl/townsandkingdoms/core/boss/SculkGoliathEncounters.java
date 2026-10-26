package nl.townsandkingdoms.core.boss;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Single-encounter activation in the Final Castle's existing boss gazebo.
 *
 * This runs on the server thread after players enter the Twilight Forest,
 * never in the worldgen thread (where structure lookup could deadlock).
 * Checks are throttled to once every five seconds per player.
 */
public final class SculkGoliathEncounters {
    private static final ResourceLocation FINAL_CASTLE_ID =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "final_castle");
    private static final ResourceLocation TWILIGHT_ID =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "twilight_forest");

    private SculkGoliathEncounters() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 100 != 0 || !SculkGoliathBootstrap.packEnabled()) return;
        ServerLevel level = player.serverLevel();
        if (!level.dimension().location().equals(TWILIGHT_ID)) return;

        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure castle = structures.get(FINAL_CASTLE_ID);
        if (castle == null) return;

        StructureStart start = level.structureManager().getStructureAt(player.blockPosition(), castle);
        if (!start.isValid()) return;

        maybeSpawnPatrol(level, player);

        for (StructurePiece piece : start.getPieces()) {
            if (!piece.getClass().getSimpleName().equals("FinalCastleBossGazeboComponent")) continue;
            BoundingBox bbox = piece.getBoundingBox();
            BlockPos center = bbox.getCenter();
            if (Math.abs(player.getX() - center.getX()) > 12 ||
                    Math.abs(player.getZ() - center.getZ()) > 12 ||
                    player.getY() < bbox.minY() - 3 || player.getY() > bbox.maxY() + 4) return;

            CastleRecord data = level.getDataStorage().computeIfAbsent(CastleRecord.FACTORY, "tnkcore_goliath_castles");
            long castleKey = center.asLong();
            if (data.defeated.contains(castleKey)) return;

            boolean alreadySpawned = !level.getEntitiesOfClass(SculkGoliathEntity.class,
                    new net.minecraft.world.phys.AABB(center).inflate(35), mob -> mob.isAlive()).isEmpty();
            if (alreadySpawned) return;

            BlockPos spawnAt = new BlockPos(center.getX(), bbox.minY() + 2, center.getZ());
            if (!level.hasChunkAt(spawnAt)) return;
            SculkGoliathEntity boss = SculkGoliathBootstrap.SCULK_GOLIATH.get().create(level);
            if (boss == null) return;
            boss.moveTo(spawnAt.getX() + 0.5, spawnAt.getY(), spawnAt.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            boss.setCastleKey(castleKey);
            level.addFreshEntity(boss);
            return;
        }
    }

    /**
     * Sparse Otherside patrols replace the majority of castle spawners. A
     * nearby cap prevents extra patrols from accumulating with more players.
     */
    private static void maybeSpawnPatrol(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator() || level.random.nextInt(3) != 0) return;
        int nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(28),
                mob -> BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace()
                        .equals("deeperdarker")).size();
        if (nearby >= 3) return;

        String[] pool = {"shattered", "sculk_leech", "shattered", "stalker"};
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("deeperdarker",
                pool[level.random.nextInt(pool.length)]);
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (type.isEmpty()) return;

        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = level.random.nextInt(25) - 12;
            int dz = level.random.nextInt(25) - 12;
            if (dx * dx + dz * dz < 36) continue;
            BlockPos candidate = player.blockPosition().offset(dx, 0, dz);
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos p = candidate.offset(0, dy, 0);
                if (!level.hasChunkAt(p) || !level.getBlockState(p).isAir()
                        || !level.getBlockState(p.above()).isAir()) continue;
                BlockState floor = level.getBlockState(p.below());
                if (!floor.isFaceSturdy(level, p.below(), Direction.UP)) continue;
                Entity entity = type.get().create(level);
                if (!(entity instanceof Mob mob)) return;
                mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5,
                        level.random.nextFloat() * 360.0F, 0);
                level.addFreshEntity(mob);
                return;
            }
        }
    }

    public static void defeated(SculkGoliathEntity boss) {
        if (!(boss.level() instanceof ServerLevel level) || boss.getCastleKey() == 0L) return;
        CastleRecord data = level.getDataStorage().computeIfAbsent(CastleRecord.FACTORY, "tnkcore_goliath_castles");
        if (data.defeated.add(boss.getCastleKey())) data.setDirty();
    }

    private static final class CastleRecord extends SavedData {
        private final Set<Long> defeated = new HashSet<>();

        private static final Factory<CastleRecord> FACTORY =
                new Factory<>(CastleRecord::new, CastleRecord::load, null);

        private static CastleRecord load(CompoundTag tag, HolderLookup.Provider provider) {
            CastleRecord record = new CastleRecord();
            for (long id : tag.getLongArray("DefeatedCastles")) record.defeated.add(id);
            return record;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putLongArray("DefeatedCastles", defeated.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }
    }
}
