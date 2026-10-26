package nl.townsandkingdoms.core.projector;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.townsandkingdoms.core.TownsAndKingdomsCore;

public final class ProjectorBlockEntity extends BlockEntity {
    private ProjectorSettings settings = ProjectorSettings.DEFAULT;
    private UUID owner;
    private int revision;
    public ProjectorBlockEntity(BlockPos pos, BlockState state) { super(TownsAndKingdomsCore.PROJECTOR_ENTITY.get(), pos, state); }
    public ProjectorSettings settings() { return settings; }
    public int revision() { return revision; }
    public UUID owner() { return owner; }
    public void setOwner(UUID id) { owner = id; setChanged(); }
    public boolean canEdit(Player player) { return player.hasPermissions(2) || owner != null && owner.equals(player.getUUID()); }
    public void update(ProjectorSettings value) {
        settings = value; revision++; setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.put("projector", settings.save()); tag.putInt("revision", revision);
        if (owner != null) tag.putUUID("owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); settings = ProjectorSettings.read(tag.getCompound("projector"));
        revision = tag.getInt("revision"); owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        // Tint is baked into the chunk mesh. Rebuild it when a block-entity update arrives.
        if (level != null && level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
