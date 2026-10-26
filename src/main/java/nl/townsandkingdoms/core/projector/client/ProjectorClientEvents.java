package nl.townsandkingdoms.core.projector.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid="tnkcore", value=Dist.CLIENT)
public final class ProjectorClientEvents {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { RemoteImages.clear(); QuestBridge.clear(); }
}
