package nl.townsandkingdoms.core.boss;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class SculkGoliathRenderer extends GeoEntityRenderer<SculkGoliathEntity> {
    public SculkGoliathRenderer(EntityRendererProvider.Context context) {
        super(context, new SculkGoliathModel());
        this.shadowRadius = 2.5F;
    }
}
