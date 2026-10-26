package nl.townsandkingdoms.core;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = TownsAndKingdomsCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoreClient {
    private CoreClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> nl.townsandkingdoms.core.projector.ProjectorBlock.OPEN_EDITOR = pos -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.level.getBlockEntity(pos) instanceof nl.townsandkingdoms.core.projector.ProjectorBlockEntity be)
                mc.setScreen(new nl.townsandkingdoms.core.projector.client.ProjectorScreen(be));
        });
        event.enqueueWork(() -> ItemProperties.register(TownsAndKingdomsCore.SPAWNER_CORE.get(),
                ResourceLocation.fromNamespaceAndPath(TownsAndKingdomsCore.MOD_ID, "filled"),
                (stack, level, entity, seed) -> SpawnerCoreItem.isFilled(stack) ? 1.0F : 0.0F));
    }

    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TownsAndKingdomsCore.PROJECTOR_ENTITY.get(), nl.townsandkingdoms.core.projector.client.ProjectorRenderer::new);
        nl.townsandkingdoms.core.boss.SculkGoliathBootstrap.registerRenderers(event);
    }
    @SubscribeEvent public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> 0xFF9D5BFF, TownsAndKingdomsCore.PROJECTOR.get());
    }
    @SubscribeEvent public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> 0xFF9D5BFF, TownsAndKingdomsCore.PROJECTOR_ITEM.get());
    }

    @SubscribeEvent public static void reload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, resources, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor) ->
            barrier.wait(null).thenRunAsync(nl.townsandkingdoms.core.projector.client.RemoteImages::clear, gameExecutor));
    }
}
