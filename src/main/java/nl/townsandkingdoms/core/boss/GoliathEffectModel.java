package nl.townsandkingdoms.core.boss;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GoliathEffectModel extends GeoModel<GoliathEffectEntity> {
    private String base(GoliathEffectEntity effect) {
        return effect.isGroundBreak() ? "sculk_goliath_groundbreak" : "sculk_goliath_dig";
    }

    private ResourceLocation resource(String folder, GoliathEffectEntity effect, String ending) {
        return ResourceLocation.fromNamespaceAndPath("tnkcore", folder + "/" + base(effect) + ending);
    }

    @Override
    public ResourceLocation getModelResource(GoliathEffectEntity effect) {
        return resource("geo", effect, ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GoliathEffectEntity effect) {
        return resource("textures/entity", effect, ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoliathEffectEntity effect) {
        return resource("animations", effect, ".animation.json");
    }
}
