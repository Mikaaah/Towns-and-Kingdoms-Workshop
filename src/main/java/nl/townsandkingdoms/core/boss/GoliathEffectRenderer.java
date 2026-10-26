package nl.townsandkingdoms.core.boss;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GoliathEffectRenderer extends GeoEntityRenderer<GoliathEffectEntity> {
    public GoliathEffectRenderer(EntityRendererProvider.Context context) {
        super(context, new GoliathEffectModel());
        shadowRadius = 0;
    }
}
