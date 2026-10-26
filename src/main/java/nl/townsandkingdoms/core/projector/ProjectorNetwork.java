package nl.townsandkingdoms.core.projector;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ProjectorNetwork {
    public record Save(BlockPos pos, int revision, ProjectorSettings settings) implements CustomPacketPayload {
        public static final Type<Save> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tnkcore", "projector_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC = StreamCodec.of((b, value) -> {
            b.writeBlockPos(value.pos); b.writeVarInt(value.revision);
            var s = value.settings; b.writeEnum(s.mode()); b.writeVarInt(s.quests().size());
            s.quests().forEach(b::writeLong); b.writeUtf(s.text(),ProjectorSettings.MAX_TEXT); b.writeUtf(s.imageUrl(),ProjectorSettings.MAX_URL);
            b.writeFloat(s.width()); b.writeFloat(s.elevation()); b.writeBoolean(s.billboard()); b.writeBoolean(s.background());
            var colors = s.palette();
            b.writeInt(colors.body()); b.writeInt(colors.accent()); b.writeInt(colors.text());
            b.writeInt(colors.completed()); b.writeInt(colors.backdrop()); b.writeBoolean(s.bobbing());
        }, b -> {
            BlockPos p=b.readBlockPos(); int revision=b.readVarInt(); var mode=b.readEnum(ProjectorSettings.Mode.class);
            int count=b.readVarInt(); if(count<0 || count>5) throw new io.netty.handler.codec.DecoderException("Too many quests");
            var ids=new ArrayList<Long>(); for(int i=0;i<count;i++) ids.add(b.readLong());
            return new Save(p,revision,new ProjectorSettings(mode,ids,b.readUtf(ProjectorSettings.MAX_TEXT),b.readUtf(ProjectorSettings.MAX_URL),
                    b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean(),
                    new ProjectorPalette(b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt()),b.readBoolean()));
        });
        @Override public Type<Save> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(Save.TYPE, Save.CODEC, ProjectorNetwork::save);
    }
    private static void save(Save packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            apply(packet,player);
        });
    }
    public static void apply(Save packet, ServerPlayer player) {
            var level=player.serverLevel();
            if (!player.isAlive() || player.isSpectator() || !player.getAbilities().mayBuild
                    || !level.hasChunkAt(packet.pos) || player.distanceToSqr(packet.pos.getCenter())>64
                    || !level.mayInteract(player,packet.pos)) return;
            if (level.getBlockEntity(packet.pos) instanceof ProjectorBlockEntity be && be.canEdit(player)) {
                if (be.revision()!=packet.revision) {
                    player.displayClientMessage(Component.literal("Projector changed. Reopen it before saving."),false); return;
                }
                // Honor the normal interaction event for claim/protection integrations.
                var hit=new net.minecraft.world.phys.BlockHitResult(packet.pos.getCenter(),net.minecraft.core.Direction.UP,packet.pos,false);
                var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,
                        net.minecraft.world.InteractionHand.MAIN_HAND,packet.pos,hit);
                if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()
                        || event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.FALSE) return;
                be.update(packet.settings);
            }
    }
}
