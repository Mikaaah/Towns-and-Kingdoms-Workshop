package nl.townsandkingdoms.core;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.CommonHooks;

/** Item mutations and block changes stay inside useOn so NeoForge can roll both
 * back together when a claim/protection mod cancels the placement transaction. */
public final class SpawnerCoreItem extends Item {
    private static final String PAYLOAD = "tnkcore_spawner";

    public SpawnerCoreItem(Properties properties) { super(properties); }

    public static boolean isFilled(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .contains(PAYLOAD);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!isFilled(stack) && !context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.SPAWNER))
            return InteractionResult.PASS;
        // Use the normal NeoForge transaction before vanilla's Creative branch
        // restores the old stack count. This also lets a filled core place against
        // an interactive block without opening that block's menu.
        return stack.useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.getAbilities().mayBuild || player.isSpectator()) return InteractionResult.FAIL;
        return isFilled(context.getItemInHand()) ? placeSpawner(context) : captureSpawner(context);
    }

    private InteractionResult captureSpawner(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var player = context.getPlayer();
        if (!level.getBlockState(pos).is(Blocks.SPAWNER)
                || !(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (CommonHooks.fireBlockBreak(level, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer,
                pos, level.getBlockState(pos)).isCanceled()) return InteractionResult.FAIL;

        // Full metadata retains spawn potentials, entity NBT, components, attachments,
        // NeoForge persistent data and Apotheosis' saved spawner settings.
        CompoundTag payload = spawner.saveWithFullMetadata(level.registryAccess());
        CompoundTag data = context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.put(PAYLOAD, payload);
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS))
            return InteractionResult.FAIL;
        context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.25F);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        return InteractionResult.CONSUME;
    }

    private InteractionResult placeSpawner(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        BlockPlaceContext placement = new BlockPlaceContext(context);
        BlockPos pos = placement.getClickedPos();
        var state = Blocks.SPAWNER.defaultBlockState();
        if (!placement.canPlace() || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())
                || !level.isUnobstructed(state, pos, CollisionContext.of(player))) return InteractionResult.FAIL;
        CompoundTag data = context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.contains(PAYLOAD, Tag.TAG_COMPOUND)) return InteractionResult.FAIL;
        CompoundTag payload = data.getCompound(PAYLOAD).copy();
        if (!"minecraft:mob_spawner".equals(payload.getString("id"))) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        payload.putInt("x", pos.getX());
        payload.putInt("y", pos.getY());
        payload.putInt("z", pos.getZ());
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner))
            throw new IllegalStateException("Spawner placement did not create a spawner block entity");
        spawner.loadWithComponents(payload, level.registryAccess());
        spawner.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        // Deliberately consume even in Creative. No durability or reusable remainder.
        context.getItemInHand().shrink(1);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.7F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            CompoundTag payload = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(PAYLOAD);
            SpawnerTooltip.append(payload, tooltip);
        }
        tooltip.add(Component.translatable(isFilled(stack) ? "tooltip.tnkcore.spawner_core.filled" : "tooltip.tnkcore.spawner_core.empty")
                .withStyle(isFilled(stack) ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tnkcore.spawner_core.single_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
