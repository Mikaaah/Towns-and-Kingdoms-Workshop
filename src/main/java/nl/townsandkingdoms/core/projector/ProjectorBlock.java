package nl.townsandkingdoms.core.projector;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

public final class ProjectorBlock extends BaseEntityBlock {
    public static final MapCodec<ProjectorBlock> CODEC = simpleCodec(ProjectorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static Consumer<BlockPos> OPEN_EDITOR = pos -> {};
    public ProjectorBlock(Properties props) { super(props); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(0,0,0,16,9,16); }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new ProjectorBlockEntity(p,s); }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (l.isClientSide) OPEN_EDITOR.accept(p);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level,pos,state,entity,stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ProjectorBlockEntity be) {
            // Ownership is assigned to the placer, never copied from item data.
            if (entity instanceof Player player) be.setOwner(player.getUUID());
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (data.contains("projector")) be.update(ProjectorSettings.read(data.getCompound("projector")));
        }
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        ItemStack stack = new ItemStack(this);
        BlockEntity blockEntity = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ProjectorBlockEntity be) {
            CompoundTag tag = new CompoundTag(); tag.put("projector", be.settings().save());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return java.util.List.of(stack);
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
}
