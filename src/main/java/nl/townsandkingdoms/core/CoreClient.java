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
        event.enqueueWork(() -> ItemProperties.register(nl.townsandkingdoms.core.construction.ConstructionSet.CAGE.get(),
                ResourceLocation.fromNamespaceAndPath("tnkcore", "occupied"),
                (stack, level, entity, seed) -> nl.townsandkingdoms.core.construction.MobCageItem.filled(stack) ? 1F : 0F));
        event.enqueueWork(() -> ItemProperties.register(TownsAndKingdomsCore.SPAWNER_CORE.get(),
                ResourceLocation.fromNamespaceAndPath(TownsAndKingdomsCore.MOD_ID, "filled"),
                (stack, level, entity, seed) -> SpawnerCoreItem.isFilled(stack) ? 1.0F : 0.0F));
    }

    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TownsAndKingdomsCore.PROJECTOR_ENTITY.get(), nl.townsandkingdoms.core.projector.client.ProjectorRenderer::new);
        if (nl.townsandkingdoms.core.boss.GoliathIntegration.dependenciesPresent())
            nl.townsandkingdoms.core.boss.SculkGoliathBootstrap.registerRenderers(event);
    }
    @SubscribeEvent public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            var colors = nl.townsandkingdoms.core.projector.ProjectorPalette.DEFAULT;
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof nl.townsandkingdoms.core.projector.ProjectorBlockEntity be)
                colors = be.settings().palette();
            return nl.townsandkingdoms.core.projector.ProjectorPalette.opaque(colors.body());
        }, TownsAndKingdomsCore.PROJECTOR.get());
    }
    @SubscribeEvent public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            var colors = data.contains("projector")
                    ? nl.townsandkingdoms.core.projector.ProjectorSettings.read(data.getCompound("projector")).palette()
                    : nl.townsandkingdoms.core.projector.ProjectorPalette.DEFAULT;
            return nl.townsandkingdoms.core.projector.ProjectorPalette.opaque(colors.body());
        }, TownsAndKingdomsCore.PROJECTOR_ITEM.get());
    }

    @SubscribeEvent public static void reload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, resources, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor) ->
            barrier.wait(null).thenRunAsync(nl.townsandkingdoms.core.projector.client.RemoteImages::clear, gameExecutor));
    }
    @SubscribeEvent public static void kitMenus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        nl.townsandkingdoms.core.construction.client.ConstructionClient.menus(event);
    }
}
