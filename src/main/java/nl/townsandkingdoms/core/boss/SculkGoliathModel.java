package nl.townsandkingdoms.core.boss;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SculkGoliathModel extends GeoModel<SculkGoliathEntity> {
    private static ResourceLocation asset(String folder, String basename, String suffix) {
        return ResourceLocation.fromNamespaceAndPath("tnkcore", folder + "/" + basename + suffix);
    }

    private static String basename(SculkGoliathEntity entity) {
        return entity.isPhaseTwo() ? "sculk_goliath_destroyed" : "sculk_goliath";
    }

    @Override
    public ResourceLocation getModelResource(SculkGoliathEntity entity) {
        return asset("geo", basename(entity), ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SculkGoliathEntity entity) {
        return asset("textures/entity", basename(entity), ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(SculkGoliathEntity entity) {
        return asset("animations", basename(entity), ".animation.json");
    }
}
