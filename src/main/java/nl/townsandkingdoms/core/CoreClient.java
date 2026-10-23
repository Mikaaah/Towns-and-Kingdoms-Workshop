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
        event.enqueueWork(() -> ItemProperties.register(TownsAndKingdomsCore.SPAWNER_CORE.get(),
                ResourceLocation.fromNamespaceAndPath(TownsAndKingdomsCore.MOD_ID, "filled"),
                (stack, level, entity, seed) -> SpawnerCoreItem.isFilled(stack) ? 1.0F : 0.0F));
    }
}
