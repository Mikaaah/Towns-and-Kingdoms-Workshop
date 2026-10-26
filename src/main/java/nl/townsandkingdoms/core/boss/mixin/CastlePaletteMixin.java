package nl.townsandkingdoms.core.boss.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import nl.townsandkingdoms.core.boss.SculkGoliathBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A deterministic, one-time palette alteration while Final Castle pieces are
 * generated. No scheduled corruption tasks or ongoing block updates.
 *
 * Only the standard castle masonry and unwanted monster spawners are changed.
 * Forcefields, doors, runes, stairs, paths and structure markers stay intact.
 * Pieces placed through the separate template system are left unchanged.
 */
@Mixin(StructurePiece.class)
public abstract class CastlePaletteMixin {
    @Shadow
    protected abstract BlockPos getWorldPos(int x, int y, int z);

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void tnkcore$corruptCastle(WorldGenLevel world, BlockState state, int x, int y, int z,
                                       BoundingBox box, CallbackInfo ci) {
        if (!this.getClass().getName().startsWith(
                "twilightforest.world.components.structures.finalcastle.")) return;
        // This mixin runs for every vanilla StructurePiece. Keep the filesystem
        // marker and mod-list checks off the hot path for unrelated structures.
        if (!SculkGoliathBootstrap.packEnabled()) return;

        BlockPos worldPos = getWorldPos(x, y, z);
        if (!box.isInside(worldPos)) return;
        BlockState replacement = replacement(state, worldPos);
        if (replacement == null) return;

        world.setBlock(worldPos, replacement, 2);
        ci.cancel();
    }

    private static BlockState replacement(BlockState state, BlockPos pos) {
        int hash = pos.getX() * 73428767 ^ pos.getY() * 912931 ^ pos.getZ() * 19349663;
        int chance = Math.floorMod(hash, 100);

        if (state.is(Blocks.SPAWNER)) {
            // Preserve only one in ten existing spawners. The castle's new
            // Deeper and Darker patrols are managed with a separate small cap.
            return chance < 90 ? Blocks.SCULK.defaultBlockState() : null;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals("twilightforest")) return null;
        String name = id.getPath();
        if (!name.equals("castle_brick") && !name.equals("worn_castle_brick")
                && !name.equals("cracked_castle_brick") && !name.equals("thick_castle_brick")
                && !name.equals("mossy_castle_brick")) return null;

        // About 27% of masonry is replaced. Original castle detail remains
        // readable, and all replacement materials are static full blocks.
        if (chance < 8) return Blocks.SCULK.defaultBlockState();
        if (chance < 17) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        if (chance < 27) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        return null;
    }
}
