package nl.townsandkingdoms.core.construction.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.AABB;
import nl.townsandkingdoms.core.construction.*;

public final class ConstructionClient {
    public static void menus(RegisterMenuScreensEvent event){event.register(ConstructionSet.KIT_MENU.get(),KitScreen::new);}
    @EventBusSubscriber(modid="tnkcore",value=Dist.CLIENT)
    public static final class WorldEvents {
        @SubscribeEvent public static void lines(RenderLevelStageEvent event){
            if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            var stack=mc.player.getMainHandItem();if(!(stack.getItem() instanceof PencilItem)){stack=mc.player.getOffhandItem();if(!(stack.getItem() instanceof PencilItem))return;}
            var data=KitData.read(stack);if(!mc.level.dimension().location().toString().equals(data.getString("measure_dimension")))return;
            var a=NbtUtils.readBlockPos(data,"measure_a");if(a.isEmpty())return;var b=NbtUtils.readBlockPos(data,"measure_b").orElse(a.get());
            if(mc.player.distanceToSqr(a.get().getCenter())>256*256&&mc.player.distanceToSqr(b.getCenter())>256*256)return;
            var pose=event.getPoseStack();var camera=event.getCamera().getPosition();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
            var source=mc.renderBuffers().bufferSource();LevelRenderer.renderLineBox(pose,source.getBuffer(RenderType.lines()),AABB.encapsulatingFullBlocks(a.get(),b),.72F,.42F,1F,1F);pose.popPose();source.endBatch(RenderType.lines());
        }
    }
}
